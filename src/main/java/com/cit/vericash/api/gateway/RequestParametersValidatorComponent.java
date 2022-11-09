package com.cit.vericash.api.gateway;

import com.cit.vericash.api.gateway.exception.ApiGatewayException;
import com.cit.vericash.api.gateway.model.Constants;
import com.cit.vericash.api.gateway.util.DocumentationUtil;
import com.cit.vericash.api.gateway.util.ErrorsUtil;
import com.cit.vericash.portal.backend.model.dao.Record;
import com.cit.vericash.portal.backend.model.dao.ResultSet;
import com.cit.vericash.portal.backend.model.message.Message;
import com.cit.vericash.portal.backend.util.PropertyLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;

@Component
public class RequestParametersValidatorComponent {

    @Autowired
    DocumentationUtil documentationUtil;

    @Autowired
    PropertyLoader propertyLoader;

    @Autowired
    ErrorsUtil errorsUtil;

    public void validate(Message message){
        String serviceCode = message.getHeader().getAttributeAsString(Constants.HEADER_SERVICE_CODE);
        if(serviceCode==null)
        {
            return;
        }
        if(!isEnabled()){
            return;
        }
        checkHeaderRequiredParams(message);
//        String serviceCode = message.getHeader().getAttributeAsString(Constants.HEADER_SERVICE_CODE);
//        ResultSet serviceInfo = documentationUtil.getResultSetByServiceCode("documentation.service.info.query",serviceCode);
//        if(serviceInfo==null || serviceInfo.getRecords()==null || serviceInfo.getRecords().isEmpty()){
//            throw new ApiGatewayException(Constants.SERVICE_ERROR_TITLE,1007,errorsUtil.getDescription("API1007"));
//        }
//        ResultSet requestParameters = documentationUtil.getResultSetByServiceCode("documentation.service.parameters.query",serviceCode);
//        if(requestParameters!=null){
//            validateParameters(message,requestParameters);
//        }
    }

    public String checkHeaderRequiredParams(Message message){
        String requiredParams = propertyLoader.getPropertyAsString("header.required.params",Constants.GATEWAY_FOLDER_NAME,Constants.GATEWAY_CONFIG_FILE_NAME);
        return checkParams(message,requiredParams);
    }

    private void validateParameters(Message message, ResultSet requestParameters){
        for(Record requestParameter:requestParameters.getRecords()){
            validateMandatoryField(message,requestParameter);
            validateParameterType(message,requestParameter);
        }
    }

    private void validateMandatoryField(Message message, Record requestParameter){
        Integer mandatoryId = Integer.valueOf(requestParameter.get("MANDATORY")+"");
        Boolean mandatory=(mandatoryId==1);
        if(mandatory){
            String paramName = requestParameter.get("NAME")+"";
            Object messageParamValue = getMessageParamValue(message,requestParameter);
            if(messageParamValue==null || "".equals(messageParamValue+"")){
                throw new ApiGatewayException(Constants.REQUIRED_PARAM_ERROR_TITLE,1003,errorsUtil.getDescription("API1003",paramName));
            }
        }
    }
 private void validateParameterType(Message message, Record requestParameter){
        String type = requestParameter.get("TYPE")+"";
        String paramName = requestParameter.get("NAME")+"";
        Object messageParamValue = getMessageParamValue(message,requestParameter);
        if(type!=null){
            switch(type){
                case "String":
                    if(messageParamValue!=null && !(messageParamValue instanceof String)){
                        throw new ApiGatewayException(Constants.INVALID_PARAM_ERROR_TITLE,1004,errorsUtil.getDescription("API1004",paramName));
                    }
                    break;
                case "Integer":
                    if(messageParamValue!=null && messageParamValue instanceof Double){
                        BigDecimal value = BigDecimal.valueOf((Double)messageParamValue);
                        String messageParamValueStr = value.toString();
                        if(messageParamValueStr.contains(".")){
                            throw new ApiGatewayException(Constants.INVALID_PARAM_ERROR_TITLE,1004,errorsUtil.getDescription("API1004",paramName));
                        }
                    } else if(messageParamValue!=null && messageParamValue instanceof String){
                        String messageParamValueStr = (String) messageParamValue;
                        if(messageParamValueStr.contains(".")){
                            throw new ApiGatewayException(Constants.INVALID_PARAM_ERROR_TITLE,1004,errorsUtil.getDescription("API1004",paramName));
                        }
                        try{
                            new BigInteger(messageParamValueStr);
                        }
                        catch(NumberFormatException e){
                            throw new ApiGatewayException(Constants.INVALID_PARAM_ERROR_TITLE,1004,errorsUtil.getDescription("API1004",paramName));
                        }
                    }
                     else if(messageParamValue!=null && !(messageParamValue instanceof Integer)
                            && !(messageParamValue instanceof Long)
                            && !(messageParamValue instanceof Short)
                            && !(messageParamValue instanceof Byte)
                            && !(messageParamValue instanceof BigInteger)){
                    throw new ApiGatewayException(Constants.INVALID_PARAM_ERROR_TITLE,1004,errorsUtil.getDescription("API1004",paramName));
                    }
                    break;
                case "Decimal":
                    if(messageParamValue!=null && messageParamValue instanceof String){
                        try{
                            new BigDecimal((String)messageParamValue);
                        }
                        catch(NumberFormatException e){
                            throw new ApiGatewayException(Constants.INVALID_PARAM_ERROR_TITLE,1004,errorsUtil.getDescription("API1004",paramName));
                        }
                    }
                     else if(messageParamValue!=null && !(messageParamValue instanceof Float)
                            && !(messageParamValue instanceof Double)
                            && !(messageParamValue instanceof BigDecimal)){
                    throw new ApiGatewayException(Constants.INVALID_PARAM_ERROR_TITLE,1004,errorsUtil.getDescription("API1004",paramName));
                }
                break;
                case "Boolean":
                    if(messageParamValue!=null && !(messageParamValue instanceof Boolean)){
                        throw new ApiGatewayException(Constants.INVALID_PARAM_ERROR_TITLE,1004,errorsUtil.getDescription("API1004",paramName));
                    }
                 break;
            }
        }
    }

    private Object getMessageParamValue(Message message, Record requestParameter){
        Object messageParamValue = null;
        String category = requestParameter.get("CATEGORY")+"";
        String paramName = requestParameter.get("NAME")+"";
        if(category!= null && "payload".equals(category)){
            messageParamValue = message.getPayload().getAttribute(paramName);
        } else if(category!= null && "additionalData".equals(category)){
            messageParamValue = message.getAdditionalData().getAttribute(paramName);
        }
        return messageParamValue;
    }

    public String checkParams(Message message,String paramsProperty){
        String serviceCode = message.getHeader().getAttributeAsString(Constants.HEADER_SERVICE_CODE);
        String headerValue = null;
        if(paramsProperty==null|| "".equals(paramsProperty.trim())){
            return null;
        }
        String[] params = paramsProperty.trim().split(",");
        for(String param:params)
        {
            if(Constants.LOGIN_SERVICE_CODE.equals(serviceCode) && (
                    Constants.HEADER_WALLET_ID.equals(param)
                    || Constants.HEADER_WALLET_SHORT_CODE.equals(param)
                    || Constants.HEADER_USER_NAME.equals(param)
                    || Constants.HEADER_USER_FULL_NAME.equals(param)
                    || Constants.HEADER_USER_ID.equals(param))){
                    continue;
            }
            headerValue = message.getHeader().getAttributeAsString(param);
            if(headerValue==null|| "".equals(headerValue.trim()) || "null".equals(headerValue) )
            {
                throw new ApiGatewayException(Constants.REQUIRED_PARAM_ERROR_TITLE,1005,errorsUtil.getDescription("API1005",param));
            }
        }
        return null;
    }

    private Boolean isEnabled(){
        return propertyLoader.getPropertyAsBoolean("required.params.enabled",Constants.GATEWAY_FOLDER_NAME,Constants.GATEWAY_CONFIG_FILE_NAME);
    }
}
