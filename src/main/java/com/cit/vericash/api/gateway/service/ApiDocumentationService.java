package com.cit.vericash.api.gateway.service;

import com.cit.vericash.api.gateway.exception.ApiGatewayException;
import com.cit.vericash.api.gateway.model.*;
import com.cit.vericash.api.gateway.util.*;
import com.cit.vericash.portal.backend.model.dao.Record;
import com.cit.vericash.portal.backend.model.dao.ResultSet;
import com.cit.vericash.portal.backend.model.message.Header;
import com.cit.vericash.portal.backend.model.message.Message;
import com.cit.vericash.portal.backend.model.message.Payload;
import com.cit.vericash.portal.backend.model.request.Error;
import com.cit.vericash.portal.backend.model.request.Response;
import com.cit.vericash.portal.backend.model.request.SslRequest;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
@EnableAsync
public class ApiDocumentationService {
    private static final String SERVICE_NAME_PLACEHOLDER = "{{SERVICE_NAME}}";
    private static final String SERVICE_DESCRIPTION_PLACEHOLDER = "{{SERVICE_DESCRIPTION}}";
    private static final String REQUEST_JSON_PLACEHOLDER = "{{REQUEST_JSON}}";
    private static final String SERVICE_ENDPOINT_PLACEHOLDER = "{{SERVICE_ENDPOINT}}";
    private static final String PARAMETER_DATA_PLACEHOLDER = "{{PARAMETERS_DATA}}";
    private static final String RESPONSE_SUCCESS_PLACEHOLDER = "{{RESPONSE_SUCCESS_SAMPLE}}";
    private static final String RESPONSE_ERROR_PLACEHOLDER = "{{RESPONSE_ERROR_SAMPLE}}";
    private static final String RESPONSE_ERROR_CODES_PLACEHOLDER = "{{RESPONSE_ERROR_CODES}}";
    private static final String TEMPLATE_FILE_NAME = "api-template.html";
    private long templateLastModifiedTimestamp = -1L;
    private String templateContent;

    @Autowired
    ErrorsUtil errorsUtil;

    @Autowired
    DocumentationUtil documentationUtil;

    public void validateRequest(Message message, RequestContext context) {
        if (!documentationUtil.isDocumentationEnabled()) {
            return;
        }
        String serviceCode = message.getHeader().getAttributeAsString(Constants.HEADER_SERVICE_CODE);
        if(serviceCode==null)
        {
            return;
        }
        ResultSet requestParameters = new ResultSet();

        Header header = message.getHeader();
        Payload payload = message.getPayload();
        List<Record> commonRecords = new ArrayList<>();
        List<String> headerList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : header.entrySet()
        ) {
            String head = entry.getKey();
            headerList.add(head);
            Record e = new Record();
            e.put(entry.getKey(), entry.getValue());
            commonRecords.add(e);
        }
        for (Map.Entry<String, Object> entry : payload.entrySet()
        ) {
            String head = entry.getKey();
            headerList.add(head);
            Record e = new Record();
            e.put(entry.getKey(), entry.getValue());
            commonRecords.add(e);
        }
        com.cit.vericash.portal.backend.model.dao.Header headerToArr = new com.cit.vericash.portal.backend.model.dao.Header();
        headerToArr.setFields(headerList.toArray(new String[0]));
        requestParameters.setRecords(commonRecords);
        requestParameters.setHeader(headerToArr);

        /*if (requestParameters != null && requestParameters.getRecords() != null && !requestParameters.getRecords().isEmpty()) {
            throw new ApiGatewayException(Constants.DOCUMENTATION_ERROR_TITLE, 1006, errorsUtil.getDescription("API1006"));
        }*/

        ResultSet serviceInfo = documentationUtil.getResultSetByServiceCode("documentation.service.info.query", serviceCode);

        context.set("requestParameters", requestParameters);
        context.set("serviceInfo", serviceInfo);
    }

    @Async
    public void document(SslRequest request, Response response, RequestContext context) {
        ResultSet requestParameters = (ResultSet) context.get("requestParameters");
        ResultSet serviceInfo = (ResultSet) context.get("serviceInfo");
        buildHtmlDocumentation(request, response, requestParameters, serviceInfo);
    }

    private void validateRequestParameters(Message message, ResultSet requestParameters) {
        Set<String> messageParams = message.getPayload().keySet();
        validateRequestParameter(messageParams, requestParameters, "payload");
        messageParams = message.getAdditionalData().keySet();
        validateRequestParameter(messageParams, requestParameters, "additionalData");
    }

    private void validateRequestParameter(Set<String> messageParams, ResultSet requestParameters, String category) {
        for (String paramName : messageParams) {
            if (!requestParameterFound(paramName, requestParameters, category)) {
                throw new ApiGatewayException(Constants.DOCUMENTATION_ERROR_TITLE, 1008, errorsUtil.getDescription("API1008", paramName));
            }
        }
    }

    private boolean requestParameterFound(String messageParamName, ResultSet requestParameters, String category) {
        for (Record commonRecord : requestParameters.getRecords()) {
            String paramNameinDB = commonRecord.get("NAME") + "";
            String paramCategoryinDB = commonRecord.get("CATEGORY") + "";
            if (messageParamName.equals(paramNameinDB) && category.equals(paramCategoryinDB)) {
                return true;
            }
        }
        return false;
    }

    private void buildHtmlDocumentation(SslRequest request, Response response, ResultSet requestParameters, ResultSet serviceInfo) {
        Message message = request.getMessage();
        String serviceCode = message.getHeader().getAttributeAsString(Constants.HEADER_SERVICE_CODE);
        String project = System.getenv("PROJECT");
        String serviceFolderName = message.getHeader().getAttributeAsString(Constants.HEADER_SERVICE_NAME);
//        ResultSet errorCodes = documentationUtil.getResultSetByServiceCode("documentation.service.error.codes.query", serviceCode);
        String apiDocumentsFolder = System.getenv("PORTAL_DOCUMENTS");
        if (apiDocumentsFolder == null || !(new File(apiDocumentsFolder).exists())) {
            throw new ApiGatewayException(Constants.DOCUMENTATION_ERROR_TITLE, 1014, errorsUtil.getDescription("API1014"));
        }
        String apiDocumentsPath = new StringBuilder(apiDocumentsFolder).append(File.separator).append(project).append(File.separator).toString();
        String serviceFolderPath = createServiceFolder(apiDocumentsPath, serviceFolderName);
        String serviceName = (String) serviceInfo.getRecords().get(0).get("SERVICE_NAME");
        String serviceDocumentFilePath = new StringBuilder(serviceFolderPath).append(serviceName).append(".html").toString();
        File serviceDocumentFile = new File(serviceDocumentFilePath);
        if (!serviceDocumentFile.exists()) {
            try {
                serviceDocumentFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
                throw new ApiGatewayException(Constants.DOCUMENTATION_ERROR_TITLE, 1009, errorsUtil.getDescription("API1009"));
            }
        }
        String templatePath = new StringBuilder(apiDocumentsFolder).append(File.separator).append(TEMPLATE_FILE_NAME).toString();
        loadTemplateFile(templatePath);
        String documentationContent = fillTemplateData(request, response, requestParameters, serviceInfo, serviceCode);
        boolean writtenToFile = FileUtils.writeToFile(serviceDocumentFile, documentationContent);
        if (!writtenToFile) {
            throw new ApiGatewayException(Constants.DOCUMENTATION_ERROR_TITLE, 1010, errorsUtil.getDescription("API1010"));
        }
    }

    private String fillTemplateData(SslRequest request, Response response, ResultSet requestParameters, ResultSet serviceInfo, String serviceCode) {
        String documentationContent = templateContent;
        documentationContent = fillServiceInfo(serviceInfo, documentationContent);
        documentationContent = fillRequestData(request, serviceInfo, documentationContent);
        //documentationContent = fillRequestParametersData(requestParameters, documentationContent, serviceCode);
        documentationContent = fillResponseData(response, documentationContent);
        return documentationContent;
    }

    private String fillServiceInfo(ResultSet serviceInfo, String documentationContent) {
        String serviceName = (String) serviceInfo.getRecords().get(0).get("SERVICE_NAME");
        String description = (String) serviceInfo.getRecords().get(0).get("SERVICE_DESCRIPTION");
        if (documentationContent.contains(SERVICE_NAME_PLACEHOLDER)) {
            documentationContent = documentationContent.replace(SERVICE_NAME_PLACEHOLDER, serviceName);
        }

        if (documentationContent.contains(SERVICE_DESCRIPTION_PLACEHOLDER)) {
            documentationContent = documentationContent.replace(SERVICE_DESCRIPTION_PLACEHOLDER, description);
        }
        return documentationContent;
    }

    private String fillRequestData(SslRequest request, ResultSet serviceInfo, String documentationContent) {
        String serviceURL = (String) serviceInfo.getRecords().get(0).get("SERVICE_URL");
        LinkedHashMap<String, Object> requestMap = new LinkedHashMap<String, Object>();
        requestMap.put("mac", request.getMac());
        requestMap.put("message", request.getMessage());
        String requestJson = JsonUtil.getJsonForPrinting(requestMap);
        if (documentationContent.contains(SERVICE_ENDPOINT_PLACEHOLDER)) {
            documentationContent = documentationContent.replace(SERVICE_ENDPOINT_PLACEHOLDER, serviceURL);
        }
        if (documentationContent.contains(REQUEST_JSON_PLACEHOLDER)) {
            documentationContent = documentationContent.replace(REQUEST_JSON_PLACEHOLDER, requestJson);
        }
        return documentationContent;
    }

   /* private String fillRequestParametersData(ResultSet requestParameters, String documentationContent, String serviceCode) {
        if (documentationContent.contains(PARAMETER_DATA_PLACEHOLDER)) {
            documentationContent = documentationContent.replace(PARAMETER_DATA_PLACEHOLDER, DocumentationHtmlUtil.getParametersHtmlContent(requestParameters, serviceCode));
        }
        return documentationContent;
    }*/

    private String fillResponseData(Response response, String documentationContent) {
        Response errorResponse = new Response();
        errorResponse.setTimestamp(new Date().getTime() + "");
        errorResponse.setServiceCode(response.getServiceCode());
        errorResponse.setError(Error.BusinessError);
        errorResponse.setResult(new Object());
        //errorResponse.setErrorCode(CommonUtil.getParameterFromResultSetAsString(errorCodes, "CODE"));
        //errorResponse.setMessage(CommonUtil.getParameterFromResultSetAsString(errorCodes, "DESCRIPTION"));
        if (documentationContent.contains(RESPONSE_SUCCESS_PLACEHOLDER)) {
            documentationContent = documentationContent.replace(RESPONSE_SUCCESS_PLACEHOLDER, JsonUtil.getJsonForPrinting(response));
        }
        if (documentationContent.contains(RESPONSE_ERROR_PLACEHOLDER)) {
            documentationContent = documentationContent.replace(RESPONSE_ERROR_PLACEHOLDER, JsonUtil.getJsonForPrinting(errorResponse));
        }
        /*if (documentationContent.contains(RESPONSE_ERROR_CODES_PLACEHOLDER)) {
            documentationContent = documentationContent.replace(RESPONSE_ERROR_CODES_PLACEHOLDER, DocumentationHtmlUtil.getResponseErrorCodesHtmlContent(errorCodes));
        }*/
        return documentationContent;
    }

    private String createServiceFolder(String apiDocumentsPath, String serviceFolder) {
        String serviceFolderPath = new StringBuilder(apiDocumentsPath).append(serviceFolder).append(File.separator).toString();
        try {
            File serviceFolderDir = new File(serviceFolderPath);
            if (!serviceFolderDir.exists()) {
                serviceFolderDir.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApiGatewayException(Constants.DOCUMENTATION_ERROR_TITLE, 1011, errorsUtil.getDescription("API1011"));
        }
        return serviceFolderPath;
    }

    private void loadTemplateFile(String templatePath) {
        File templateFile = new File(templatePath);
        if (templateFile.exists()) {
            long currentTimestamp = templateFile.lastModified();
            if (templateContent == null || templateLastModifiedTimestamp != currentTimestamp) {
                templateLastModifiedTimestamp = currentTimestamp;
                this.templateContent = FileUtils.getFileContents(templatePath);
            }
        } else {
            throw new ApiGatewayException(Constants.DOCUMENTATION_ERROR_TITLE, 1012, errorsUtil.getDescription("API1012"));
        }
    }

}
