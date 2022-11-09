package com.cit.vericash.api.gateway.util;

import com.cit.vericash.api.gateway.exception.ApiGatewayException;
import com.cit.vericash.api.gateway.model.Constants;
import com.cit.vericash.portal.backend.data.QueryService;
import com.cit.vericash.portal.backend.model.dao.Criteria;
import com.cit.vericash.portal.backend.model.dao.QueryMessage;
import com.cit.vericash.portal.backend.model.dao.ResultSet;
import com.cit.vericash.portal.backend.util.PropertyLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentationUtil {
    @Autowired
    PropertyLoader propertyLoader;

    @Autowired
    QueryService queryService;

    @Autowired
    ErrorsUtil errorsUtil;

    public ResultSet getResultSetByServiceCode(String queryName, String serviceCode){
        String query = propertyLoader.loadQuery(queryName);
        String databaseConnectionType = System.getenv("DATABASE_CONNECTION_TYPE");
        QueryMessage queryMessage = new QueryMessage();
        queryMessage.setQuery(query);
        queryMessage.setCriteriaList(getCriteriaList(serviceCode));
        ResultSet requestParameters = null;
        try {
            requestParameters = queryService.executeQuery(queryMessage);
        }catch(Exception e){
            throw new ApiGatewayException(Constants.DOCUMENTATION_ERROR_TITLE,1013,errorsUtil.getDescription("API1013"));
        }
        return requestParameters;
    }

    private List<Criteria> getCriteriaList(String serviceCode){
        List<Criteria> criteriaList = new ArrayList<Criteria>();
        criteriaList.add(new Criteria("SERVICE_CODE",serviceCode, Criteria.Type.Text, Criteria.Operator.Equals));
        return criteriaList;
    }
    public Boolean isDocumentationEnabled(){
        return propertyLoader.getPropertyAsBoolean("documentation.enabled", Constants.GATEWAY_FOLDER_NAME,Constants.GATEWAY_CONFIG_FILE_NAME);
    }
}
