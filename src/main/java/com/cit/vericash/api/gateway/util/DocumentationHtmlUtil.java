package com.cit.vericash.api.gateway.util;

import com.cit.vericash.api.gateway.config.ConfigFile;
import com.cit.vericash.api.gateway.model.Constants;
import com.cit.vericash.portal.backend.model.dao.Record;
import com.cit.vericash.portal.backend.model.dao.ResultSet;
import com.cit.vericash.portal.backend.util.PropertyLoader;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DocumentationHtmlUtil {

    private static ConfigFile headerDescriptionFile;

    public static String getParametersHtmlContent(ResultSet requestParameters, String serviceCode){
        StringBuilder parametersHtmlContent = new StringBuilder();
        List<Record> header = new ArrayList<Record>();
        List<Record> payload = new ArrayList<Record>();
        List<Record> additionalData = new ArrayList<Record>();
        for(Record commonRecord : requestParameters.getRecords()){
            String category = commonRecord.get("CATEGORY")+"";
            if("payload".equals(category)){
                payload.add(commonRecord);
            } else if("additionalData".equals(category)){
                additionalData.add(commonRecord);
            }

        }
        fillHeaderParamsInfo(parametersHtmlContent,serviceCode);
        appendSection("Payload",payload,parametersHtmlContent);
        appendSection("Additional Data",additionalData,parametersHtmlContent);
        return parametersHtmlContent.toString();
    }

   /* public static String getResponseErrorCodesHtmlContent(ResultSet errorCodes){
        StringBuilder responseErrorCodeHtmlContent = new StringBuilder();
        if(errorCodes!=null && errorCodes.getRecords()!=null){
            for(Record record:errorCodes.getRecords()){
                String code = record.get("CODE")+"";
                String description = record.get("DESCRIPTION")+"";
                responseErrorCodeHtmlContent.append("<tr>")
                        .append("<td class=\"errorStatus\">").append(code).append("</td>")
                        .append("<td>").append(description)
                        .append("</td></tr>");
            }
        }
        return responseErrorCodeHtmlContent.toString();
    }*/

    private static void appendParameterData(Record commonRecord, StringBuilder parametersHtmlContent){
        String paramName = commonRecord.get("NAME")+"";
        String paramType = commonRecord.get("TYPE")+"";
        String paramDescription = commonRecord.get("DESCRIPTION")+"";
        Integer mandatory = Integer.valueOf(commonRecord.get("MANDATORY")+"");
        String mandatoryStr = ((mandatory==1)?"Yes":"No");
        appendParameterData(paramName,paramType,paramDescription,mandatoryStr,parametersHtmlContent);
    }

    private static void appendParameterData(String name,String type,String description,String mandatory,StringBuilder parametersHtmlContent){
        parametersHtmlContent.append("<tr>")
                .append("<td>").append(name).append("</td>")
                .append("<td>").append(type).append("</td>")
                .append("<td>").append(description).append("</td>")
                .append("<td class=\"alignCenter\">").append(mandatory).append("</td>")
                .append("<tr>");
    }

    private static void appendSection(String sectionName, List<Record> sectionData, StringBuilder parametersHtmlContent ){
        if(!sectionData.isEmpty()){
            parametersHtmlContent.append("<td colspan=\"4\" class=\"tableSection\">").append(sectionName).append("</td></tr>");
            for(Record commonRecord :sectionData){
                appendParameterData(commonRecord,parametersHtmlContent);
            }
        }
    }

    private static void fillHeaderParamsInfo(StringBuilder parametersHtmlContent,String serviceCode){
        loadHeaderDescriptionFile();
        parametersHtmlContent.append("<td colspan=\"4\" class=\"tableSection\">").append("Header").append("</td></tr>");
        Set<Object> headerParams = headerDescriptionFile.keySet();
        for(Object paramName:headerParams){
            String paramNameStr = paramName+"";
            String type = "String";
            String mandatory = "Yes";
            if(paramNameStr.equals(Constants.HEADER_WALLET_ID) ||
                    paramNameStr.equals(Constants.HEADER_USER_ID) ||
                    paramNameStr.equals(Constants.HEADER_TIMESTAMP)){
                type="Integer";
            }
            if("5000".equals(serviceCode) && paramNameStr.equals(Constants.HEADER_USER_ID)){
                mandatory="No";
            }
            String description = headerDescriptionFile.getProperty(paramNameStr);
            if("5000".equals(serviceCode)
                    && (Constants.HEADER_WALLET_ID.equals(paramNameStr)
                    || Constants.HEADER_WALLET_SHORT_CODE.equals(paramNameStr)
                    || Constants.HEADER_USER_NAME.equals(paramNameStr)
                    || Constants.HEADER_USER_FULL_NAME.equals(paramNameStr))){
                continue;
            }
            appendParameterData(paramNameStr,type,description,mandatory,parametersHtmlContent);

        }
    }

    private static void loadHeaderDescriptionFile(){
        StringBuilder filePath = PropertyLoader.getFilePathFromEnvironmentVar("services/shared-config","header-description");
        File currentHeaderDescriptionFile = new File(filePath.toString());
        if(currentHeaderDescriptionFile.exists()){
            long currentModifiedTimestamp = currentHeaderDescriptionFile.lastModified();
            if(headerDescriptionFile==null
                    || currentModifiedTimestamp != headerDescriptionFile.getLastModifiedDate()){
                try(FileInputStream fis = new FileInputStream(currentHeaderDescriptionFile)){
                    headerDescriptionFile = new ConfigFile(currentModifiedTimestamp,fis);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

        }
    }
}
