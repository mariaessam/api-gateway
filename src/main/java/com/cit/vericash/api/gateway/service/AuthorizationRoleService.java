package com.cit.vericash.api.gateway.service;

import com.cit.vericash.api.gateway.exception.ApiGatewayException;
import com.cit.vericash.api.gateway.model.Constants;
import com.cit.vericash.api.gateway.util.CommonUtil;
import com.cit.vericash.api.gateway.util.ErrorsUtil;
import com.cit.vericash.portal.backend.data.QueryService;
import com.cit.vericash.portal.backend.model.dao.Criteria;
import com.cit.vericash.portal.backend.model.dao.QueryMessage;
import com.cit.vericash.portal.backend.model.dao.ResultSet;
import com.cit.vericash.portal.backend.model.message.Message;
import com.cit.vericash.portal.backend.util.PropertyLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


@Service
public class AuthorizationRoleService {
    @Autowired
    QueryService queryService;
    @Autowired
    PropertyLoader propertyLoader;
    @Autowired
    ErrorsUtil errorsUtil;

    public void check(Message message) {

        String serviceCode = message.getHeader().getAttributeAsString(Constants.HEADER_SERVICE_CODE);
        if(serviceCode==null)
            return;
        Long userId = message.getHeader().getAttributeAsLong(Constants.HEADER_USER_ID);
        String query = propertyLoader.loadQuery("checkRole.query");
        if (!isEnabled() || skipAuthorizationCheckServices(serviceCode)) {
            return;
        }
        //create criteria List
        List<Criteria> criteriaList = new ArrayList<Criteria>();
        criteriaList.add(new Criteria("BUSINESS_USERS.BUSINESS_USER_ID", userId, Criteria.Type.Number, Criteria.Operator.Equals));
        String parentServiceCode = skipAuthorizationCheckForStepServices(serviceCode);
        if(parentServiceCode != null){
            criteriaList.add(new Criteria("\"ROLES\".SERVICE_TYPE", parentServiceCode, Criteria.Type.Text, Criteria.Operator.Equals));
        }else{
            criteriaList.add(new Criteria("\"ROLES\".SERVICE_TYPE", serviceCode, Criteria.Type.Text, Criteria.Operator.Equals));
        }
        // build message
        QueryMessage queryMessage = CommonUtil.getQueryMessage(query, criteriaList, null);

        //execute native query_service
        ResultSet resultSet = queryService.executeQuery(queryMessage);
        if (resultSet.getRecords() != null && resultSet.getRecords().size() == 0) {
            throw new ApiGatewayException(Constants.AUTHORIZATION_ROLE_ERROR, 1015, errorsUtil.getDescription("API1015"));
        }

    }

    private Boolean isEnabled() {
        return propertyLoader.getPropertyAsBoolean("authorization.role.check.enabled", Constants.GATEWAY_FOLDER_NAME, Constants.GATEWAY_CONFIG_FILE_NAME);
    }

    private Boolean skipAuthorizationCheckServices(String serviceCode) {
        String[] serviceCodes = propertyLoader.getPropertyAsString("authorization.skip.services", "services" + File.separator + "shared-config", Constants.GATEWAY_CONFIG_FILE_NAME).split(",");

        for (String code: serviceCodes
             ) {
            if(serviceCode.equals(code)){
                return true;
            }
        }
        return false;
    }
    private String skipAuthorizationCheckForStepServices(String serviceCode) {
        String parentServiceCode = propertyLoader.getPropertyAsString(serviceCode +".parent.authorized.serviceCode", "services" + File.separator + "shared-config", Constants.GATEWAY_CONFIG_FILE_NAME);
        return parentServiceCode;
    }
}
