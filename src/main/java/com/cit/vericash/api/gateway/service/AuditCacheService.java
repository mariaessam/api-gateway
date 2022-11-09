package com.cit.vericash.api.gateway.service;

import com.cit.vericash.api.gateway.model.Constants;
import com.cit.vericash.portal.backend.data.QueryService;
import com.cit.vericash.portal.backend.model.dao.QueryMessage;
import com.cit.vericash.portal.backend.model.dao.ResultSet;
import com.cit.vericash.portal.backend.model.message.Message;
import com.cit.vericash.portal.backend.util.JexlUtil;
import com.cit.vericash.portal.backend.util.PropertyLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.HashMap;
import java.util.Map;

@Service
@ApplicationScope
public class AuditCacheService {

    @Autowired
    PropertyLoader propertyLoader;

    @Autowired
    QueryService queryService;

    private ResultSet approvalConfiguration =null;

    private static Map<Long, Map<String,Boolean>> cache=new HashMap<Long,Map<String,Boolean>>();

    public boolean isAuditEnabled(Message message)
    {
        String serviceCode = message.getHeader().getAttributeAsString(Constants.HEADER_SERVICE_CODE);
        Long walletId=message.getHeader().getAttributeAsLong("walletId");
        Map<String,Boolean> auditMap=cache.get(walletId);
        if(auditMap!=null && auditMap.containsKey(serviceCode))
        {
            return auditMap.get(serviceCode);
        }
        else
        {
            String query = propertyLoader.getPropertyAsString("get.enable.audit","api-gateway","queries");
            try {
                query = JexlUtil.evaluateExpression(query, "message", message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            QueryMessage queryMessage = new QueryMessage();
            queryMessage.setQuery(query);
            ResultSet resultSet = queryService.executeQuery(queryMessage);
            Boolean audtiServiceEnabled = false;
            if (resultSet.getRecords().size() > 0) {
                String enableAudit = resultSet.getRecords().get(0).get("ENABLE_AUDIT").toString();
                if (enableAudit.equals("1")) {
                    audtiServiceEnabled = true;
                }
            }
            if(auditMap==null)
            {
                auditMap=new HashMap<String,Boolean>();
            }
            auditMap.put(serviceCode,audtiServiceEnabled);
            cache.put(walletId,auditMap);
            return audtiServiceEnabled;
        }
    }
    public void clearCache(Long walletId)
    {
        cache.put(walletId,new HashMap<String,Boolean>());
    }
}
