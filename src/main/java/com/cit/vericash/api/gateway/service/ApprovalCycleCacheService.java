package com.cit.vericash.api.gateway.service;

import com.cit.vericash.api.gateway.model.Constants;
import com.cit.vericash.portal.backend.data.QueryService;
import com.cit.vericash.portal.backend.model.dao.Criteria;
import com.cit.vericash.portal.backend.model.dao.CriteriaList;
import com.cit.vericash.portal.backend.model.dao.QueryMessage;
import com.cit.vericash.portal.backend.model.dao.ResultSet;
import com.cit.vericash.portal.backend.model.message.Message;
import com.cit.vericash.portal.backend.util.JexlUtil;
import com.cit.vericash.portal.backend.util.PropertyLoader;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.HashMap;
import java.util.Map;

@Service
@ApplicationScope
public class ApprovalCycleCacheService {

    @Autowired
    PropertyLoader propertyLoader;

    @Autowired
    QueryService queryService;

    private ResultSet approvalConfiguration =null;

    private Map<Long,Map<String,Boolean>> cache=new HashMap<Long,Map<String,Boolean>>();

    public  ResultSet getApprovalConfiguration()
    {
        return approvalConfiguration;
    }
    public  void setApprovalConfiguration(ResultSet approvalConfiguration)
    {
        this.approvalConfiguration = approvalConfiguration;
    }
    public void clearCache(Long walletId)
    {
        cache.put(walletId,new HashMap<String,Boolean>());
    }
    public ResultSet refreshApprovalConfigurationData(Message message)
    {
        String approvalConfig=propertyLoader.loadQuery("approvalConfig");
        QueryMessage queryMessage=new QueryMessage();
        queryMessage.setQuery(approvalConfig);
        Integer walletId = message.getHeader().getAttributeAsInteger("walletId");
        queryMessage.setCriteriaList(new CriteriaList(
                new Criteria("AC.WALLET_ID", walletId, Criteria.Type.Number, Criteria.Operator.Equals)
        ));
        this.approvalConfiguration =  queryService.executeQuery(queryMessage);
        return approvalConfiguration;
    }

    public boolean isApprovalCycleEnabled(Message message)
    {
        String serviceCode = message.getHeader().getAttributeAsString(Constants.HEADER_SERVICE_CODE);
        Long walletId=message.getHeader().getAttributeAsLong("walletId");
        Map<String,Boolean> approvalCycleMap=cache.get(walletId);
        if(approvalCycleMap!=null && approvalCycleMap.containsKey(serviceCode))
        {
            return approvalCycleMap.get(serviceCode);
        }
        else
        {
            String query = propertyLoader.loadQuery("get.enable.approval");
            try {
                query = JexlUtil.evaluateExpression(query, "message", message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            QueryMessage queryMessage = new QueryMessage();
            queryMessage.setQuery(query);
            ResultSet resultSet = queryService.executeQuery(queryMessage);
            boolean isEnabledWorkFlow = false;
            if(resultSet.getRecords().size() >0){
                String enableWorkflow = resultSet.getRecords().get(0).get("ENABLE_APPROVAL").toString();
                if(enableWorkflow.equals("1")){
                    isEnabledWorkFlow = true;
                }
            }
            if(approvalCycleMap==null)
            {
                approvalCycleMap=new HashMap<String,Boolean>();
            }
            approvalCycleMap.put(serviceCode,isEnabledWorkFlow);
            cache.put(walletId,approvalCycleMap);
            return isEnabledWorkFlow;
        }
    }
}
