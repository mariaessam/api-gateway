package com.cit.vericash.api.gateway.filter;

import com.cit.vericash.api.gateway.exception.ErrorHandlerComponent;
import com.cit.vericash.api.gateway.model.Constants;
import com.cit.vericash.api.gateway.service.ApiDocumentationService;
import com.cit.vericash.api.gateway.service.AuditService;
import com.cit.vericash.api.gateway.service.EndToEndService;
import com.cit.vericash.api.gateway.util.CommonUtil;
import com.cit.vericash.api.gateway.util.DocumentationUtil;
import com.cit.vericash.api.gateway.util.JsonUtil;
import com.cit.vericash.portal.backend.data.QueryService;
import com.cit.vericash.portal.backend.model.dao.Criteria;
import com.cit.vericash.portal.backend.model.dao.QueryMessage;
import com.cit.vericash.portal.backend.model.dao.ResultSet;
import com.cit.vericash.portal.backend.model.message.Message;
import com.cit.vericash.portal.backend.model.request.Response;
import com.cit.vericash.portal.backend.model.request.SslRequest;
import com.cit.vericash.portal.backend.util.PropertyLoader;
import com.cit.vericash.portal.backend.util.VericashLogger;
import com.google.gson.Gson;
import org.json.JSONObject;
import com.google.gson.internal.LinkedTreeMap;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

public class PostFilter extends ZuulFilter {

    @Autowired
    private ErrorHandlerComponent errorHandlerComponent;

    @Autowired
    ApiDocumentationService apiDocumentationService;

    @Autowired
    AuditService auditService;

    @Autowired
    EndToEndService endToEndService;

    @Autowired
    PropertyLoader propertyLoader;

    @Autowired
    QueryService queryService;

    @Autowired
    DocumentationUtil documentationUtil;

    @Autowired
    PropertyLoader propLoader;

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 2;
    }

    @Override
    public boolean shouldFilter() {
        return !RequestContext.getCurrentContext().getRequest().getMethod().equalsIgnoreCase("get");
    }
    /**
     * Wraps the response from the service in a generic Response Object
     */
    @Override
    public Object run() {

        final RequestContext ctx = RequestContext.getCurrentContext();
        String requestURI=ctx.getRequest().getRequestURI();
        double before=System.currentTimeMillis();
        String uploadedFilesURI = propLoader.getPropertyAsString("upload.file.uri", Constants.GATEWAY_FOLDER_NAME, Constants.GATEWAY_CONFIG_FILE_NAME);

        if(CommonUtil.isUploadFileService(uploadedFilesURI))
        {
            return ctx;
        }
        String startDateStr=(String)ctx.get("startDate");
        Boolean approvalCycleEnabled=Boolean.parseBoolean(""+ctx.get("approvalCycleEnabled"));
        String serviceCode = "";
        Response responseObj = new Response();
        String serviceName = "";
        Gson gson = JsonUtil.getGson();
        boolean oldWorkflowException=false;
        InputStream responseInputStream=null;
        InputStream requestInputStream=null;
        try {
            responseInputStream=ctx.getResponseDataStream();
            requestInputStream = ctx.getRequest().getInputStream();
            String requestBody = StreamUtils.copyToString(requestInputStream, StandardCharsets.UTF_8);
            Message requestMessage = gson.fromJson(requestBody,Message.class);
            serviceName =  requestMessage.getHeader().getAttributeAsString(Constants.HEADER_SERVICE_NAME);
            serviceCode =  requestMessage.getHeader().getAttributeAsString(Constants.HEADER_SERVICE_CODE);
            String responseBody = StreamUtils.copyToString(responseInputStream, StandardCharsets.UTF_8);
            String newWorkflowFlag = requestMessage.getAdditionalData().getAttributeAsString("newWorkflow");
            if(serviceName!=null && serviceName.equals("workflow-service") && serviceCode!=null && serviceCode.equals(Constants.APPROVAL_SERVICE) && newWorkflowFlag == null  ){
                String apprvalcycleServiceName = requestMessage.getAdditionalData().getAttributeAsString("apprvalcycle-serviceName");
                serviceName = apprvalcycleServiceName!=null && !apprvalcycleServiceName.equals("")? apprvalcycleServiceName : serviceName;
                oldWorkflowException=true;
            }
            String fullName="";
            if (HttpStatus.INTERNAL_SERVER_ERROR.value() == ctx.getResponse().getStatus()) {
//                System.ou t.println("responseBody: "+responseBody);
                if (responseBody.contains("timed-out") || responseBody.contains("GENERAL")) {
                    responseObj = errorHandlerComponent.resolve("service.unavailable.message");
                } else if (responseBody.contains("short-circuited")||responseBody.toUpperCase().contains("SHORTCIRCUIT")) {
                    responseObj = errorHandlerComponent.resolve("service.down.message");
                } else if (responseBody.contains("error")) {
                    String message = getExceptionMessage(responseBody);
                    if(oldWorkflowException){
                        responseObj = errorHandlerComponent.resolveOldWorkflow(message);
                   }else
                        responseObj = errorHandlerComponent.resolve(message);

                }
                if(responseObj.getErrorCode()!=null && !responseObj.getErrorCode().equals("")) {
                    requestMessage.getAdditionalData().setAttribute("failedReason", responseObj.getErrorCode() + " - " + responseObj.getMessage());
                } else
                {
                    requestMessage.getAdditionalData().setAttribute("failedReason", responseObj.getMessage());
                }
//                audit(requestMessage, fullName,responseObj.getMessage());

            }
            else {
                if(serviceCode!=null && serviceCode.equals("5000")) {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    Object walletObj=jsonObject.get("walletId");
                    if(walletObj!=null) {
                        int walletId = Integer.parseInt(walletObj.toString());
                        requestMessage.getHeader().setAttribute("walletId", walletId);
                    }
                }
                responseObj.setStatus("success");
                String successMessage = getServiceSuccessMessage(serviceCode);
                responseObj.setSuccessMessage(successMessage);
                responseObj.setResult(gson.fromJson(responseBody,Object.class));
                responseObj.setApprovedCycle(approvalCycleEnabled);
                SslRequest sslRequest = (SslRequest) ctx.get("originalRequest");
                if (documentationUtil.isDocumentationEnabled()) {
                    document(sslRequest, responseObj, ctx);
                }
                if(serviceCode!=null && serviceCode.equals(Constants.LOGIN_SERVICE_CODE)) {
                    LinkedTreeMap<String, String> record = (LinkedTreeMap<String, String>) responseObj.getResult();
                    fullName  = record.get("fullName");
                }
            }

            if(serviceCode!=null) {
                audit(requestMessage, fullName,responseObj.getMessage(), requestURI);
            }
            responseObj.setTimestamp(new Date().getTime()+"");
            responseObj.setServiceCode(serviceCode);
            ctx.setResponseBody(gson.toJson(responseObj));
            //requestMessage.getAdditionalData().setAttribute("startDate", startDateStr);
            // end to end
            endToEndService.logEndToEnd(requestMessage , startDateStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        VericashLogger.info(ctx.getResponseBody());
        try {
            if(responseInputStream!=null) {
                responseInputStream.close();
            }
            if(requestInputStream!=null) {
                requestInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        double after=System.currentTimeMillis();
//        System.out.println("PostFilter: estimated time: "+((after-before)/1000));
        return ctx;
    }

    private void document(SslRequest sslRequest, Response response, RequestContext context){
        apiDocumentationService.document(sslRequest,response,context);
    }

    private void audit(Message message, String fullName, String failureReason, String requestURI){
        if(isAuditingEnabled()){
            try{
                auditService.audit(message,fullName,failureReason,requestURI);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private Boolean isAuditingEnabled(){
        return propertyLoader.getPropertyAsBoolean("audit.enabled",Constants.GATEWAY_FOLDER_NAME,Constants.GATEWAY_CONFIG_FILE_NAME);
    }

    private String getExceptionMessage (String responseBody){
        try {
            String message = responseBody.substring(responseBody.lastIndexOf("\"message"), responseBody.lastIndexOf("\"path"));
            String[] responseBodyParts =message.split("[:]");
            String errorMessage =  responseBodyParts[2];
            errorMessage=errorMessage.replace("\"","");
            StringBuffer errorMessageSB=new StringBuffer(errorMessage);
            errorMessageSB.delete(errorMessageSB.length()-1,errorMessageSB.length());
            return errorMessageSB.toString();
        }
        catch(Exception e){
            return "";
        }
    }

    private String getServiceSuccessMessage(String serviceCode){
        String successMessage=propertyLoader.getPropertyAsString(serviceCode,Constants.GATEWAY_FOLDER_NAME,"success-messages");
        return successMessage;

    }

}
