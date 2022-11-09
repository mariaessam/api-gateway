package com.cit.vericash.api.gateway.util;

import com.cit.vericash.api.gateway.model.Constants;
import com.cit.vericash.portal.backend.util.PropertyLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ErrorsUtil {
    @Autowired
    PropertyLoader propertyLoader;

    public String getDescription(String errorCode){
        String errorMessage = propertyLoader.getPropertyAsString(errorCode, Constants.GATEWAY_FOLDER_NAME,Constants.GATEWAY_ERROR_FILE_NAME);
        return errorMessage;
    }

    public String getDescription(String errorCode,String paramName){
        String errorMessage = getDescription(errorCode);
        if(errorMessage!=null && errorMessage.contains("{paramName}")){
            errorMessage = errorMessage.replace("{paramName}",paramName);
        }
        return errorMessage;
    }


}
