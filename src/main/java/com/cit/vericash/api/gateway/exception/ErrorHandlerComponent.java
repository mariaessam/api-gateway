package com.cit.vericash.api.gateway.exception;


import com.cit.shared.error.util.ExceptionResolver;
import com.cit.shared.error.util.ExceptionUtil;
import com.cit.vericash.api.gateway.model.Constants;
import com.cit.vericash.api.gateway.util.CommonUtil;
import com.cit.vericash.portal.backend.data.QueryService;
import com.cit.vericash.portal.backend.model.dao.Criteria;
import com.cit.vericash.portal.backend.model.dao.Paging;
import com.cit.vericash.portal.backend.model.dao.QueryMessage;
import com.cit.vericash.portal.backend.model.dao.ResultSet;
import com.cit.vericash.portal.backend.model.request.Error;
import com.cit.vericash.portal.backend.model.request.Response;
import com.cit.vericash.portal.backend.util.PropertyLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ErrorHandlerComponent {

        @Autowired
        private PropertyLoader propLoader;

        @Autowired
        QueryService queryService;

        /*
        *Resolves the Error response by providing the error code and the Service Name,
        * the error codes should be in ${user.home}/config/error-codes/serviceName.properties
        */
        public Response resolve (String errorCode) {
            Response response = new Response();
            String message = "";
            if(errorCode.equals("service.unavailable.message")){
                message = propLoader.getPropertyAsString(errorCode, Constants.GATEWAY_FOLDER_NAME,Constants.GATEWAY_CONFIG_FILE_NAME);
                response.setError(Error.ServiceUnavailableError);
            } else if (errorCode.equals("service.down.message")){
                message = propLoader.getPropertyAsString(errorCode, Constants.GATEWAY_FOLDER_NAME,Constants.GATEWAY_CONFIG_FILE_NAME);
                response.setError(Error.ServiceDownError);
            } else {
                message = errorCode;
                response.setError(Error.BusinessError);
            }
            response.setMessage(message);
            response.setErrorCode(errorCode);
            response.setStatus("error");
            response.setResult("{}");
            return response;
        }

    public Response resolveOldWorkflow (String errorCode) {
        Response response = new Response();
        String msgDesc = null;
        ExceptionResolver resolver = ExceptionUtil.handle(errorCode);
        msgDesc=resolver.getLocalDescription();
        if(msgDesc==null|| "".equals(msgDesc.trim())){
            response.setError(Error.RuntimeError);
            msgDesc = errorCode;
        }
        response.setMessage(msgDesc);
        response.setError(Error.BusinessError);
        response.setErrorCode(errorCode);
        response.setStatus("error");
        response.setResult("{}");
        return response;
    }
        private String getErrorDescription(String errorCode){
            String errorMessage = null;
            String query = propLoader.loadQuery("error.description.get.query");
            List<Criteria> criteriaList = new ArrayList<Criteria>();
            criteriaList.add(new Criteria("CODE",errorCode, Criteria.Type.Text, Criteria.Operator.Equals));
            Paging paging = new Paging();
            paging.setFrom(0);
            paging.setMaxResults(1);
            QueryMessage queryMessage = CommonUtil.getQueryMessage(query,criteriaList,paging);
            ResultSet errorResultSet = null;
            try{
                errorResultSet=queryService.executeQuery(queryMessage);
            }catch(Exception e){
                e.printStackTrace();
                return null;
            }
            if(errorResultSet!=null && errorResultSet.getRecords()!=null && !errorResultSet.getRecords().isEmpty()){
                errorMessage = CommonUtil.getParameterFromResultSetAsString(errorResultSet,"DESCRIPTION");
            }
            return errorMessage;
        }


}
