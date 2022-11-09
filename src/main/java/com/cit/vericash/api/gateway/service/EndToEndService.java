package com.cit.vericash.api.gateway.service;

import com.cit.vericash.api.gateway.model.Constants;
import com.cit.vericash.api.gateway.util.DateFormatterUtil;
import com.cit.vericash.portal.backend.data.PersistService;
import com.cit.vericash.portal.backend.data.QueryService;
import com.cit.vericash.portal.backend.model.dao.*;
import com.cit.vericash.portal.backend.model.message.Message;
import com.cit.vericash.portal.backend.util.PropertyLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@EnableAsync
@Scope( proxyMode = ScopedProxyMode.TARGET_CLASS )
public class EndToEndService {

    @Autowired
    PersistService persistService;

    @Autowired
    QueryService queryService;

    @Autowired
    PropertyLoader propertyLoader;

    @Async("threadPoolTaskExecutor")
    public void logEndToEnd(Message message , String dateStr) {
        Boolean endToEndLoggingEnabled = propertyLoader.getPropertyAsBoolean("end.to.end.logging.enabled", Constants.GATEWAY_FOLDER_NAME, Constants.GATEWAY_CONFIG_FILE_NAME);
        String serviceCode = message.getHeader().getAttributeAsString(Constants.HEADER_SERVICE_CODE);
        if(serviceCode==null)
        {
            return;
        }
        String endToEndServiceLoggingEnabledProperty = new StringBuilder(serviceCode).append(".log.enabled").toString();
        String endToEndServiceLoggingEnabledStr=propertyLoader.getPropertyAsString(endToEndServiceLoggingEnabledProperty,Constants.GATEWAY_FOLDER_NAME,"endtoend");
        Boolean endToEndServiceLoggingEnabled = propertyLoader.getPropertyAsBoolean(endToEndServiceLoggingEnabledProperty, Constants.GATEWAY_FOLDER_NAME, "endtoend");
        if (!endToEndLoggingEnabled)
            return;
        if ((endToEndServiceLoggingEnabledStr!=null && !endToEndServiceLoggingEnabledStr.isEmpty()) && !endToEndServiceLoggingEnabled) {
            return;
        }
        EntityModel endToEndModel = getEndToEndModel(message , dateStr);

        PersistMessage persistMessage = new PersistMessage();
        persistMessage.setEntityCode(Constants.END_TO_END_ENTITY_MODEL);
        persistMessage.setPersistentObject(endToEndModel);

        persistService.persist(persistMessage);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    private EntityModel getEndToEndModel(Message message , String startDateStr) {
        Date endDate = new Date();
//        System.out.println("end date"+endDate);
        String serviceCode = message.getHeader().getAttributeAsString(Constants.HEADER_SERVICE_CODE);
        String serviceName = getServiceName(serviceCode);
        String userName;
        if(serviceCode.equals("5000") ) {
            userName = message.getPayload().getAttributeAsString("username");
        }else {
            userName = message.getHeader().getAttributeAsString(Constants.HEADER_USER_NAME);
        }
        String leg = propertyLoader.getPropertyAsString("leg", Constants.GATEWAY_FOLDER_NAME, Constants.GATEWAY_CONFIG_FILE_NAME);
        Long walletId = message.getHeader().getAttributeAsLong(Constants.HEADER_WALLET_ID);
        //String startDateStr = message.getAdditionalData().getAttributeAsString("startDate");
        Date startDate = DateFormatterUtil.parse(startDateStr);

        Long latencyInMilliSeconds = endDate.getTime() - startDate.getTime();
        Double latency = latencyInMilliSeconds / 1000d;
        latency = round(latency, 3);
        String failedReason = message.getAdditionalData().getAttributeAsString("failedReason");
        EndToEndStatus endToEndStatus = failedReason == null ? EndToEndStatus.Succeeded : EndToEndStatus.Failed;

        EntityModel endToEndModel = new EntityModel();
        endToEndModel.setAttribute("walletId", walletId);
        endToEndModel.setAttribute("userName" ,userName );
        endToEndModel.setAttribute("serviceName", serviceName);
        endToEndModel.setAttribute("serviceCode", serviceCode);
        endToEndModel.setAttribute("startDate", startDateStr);
        endToEndModel.setAttribute("leg", leg);
        endToEndModel.setAttribute("status", endToEndStatus.ordinal());
        endToEndModel.setAttribute("failedReason", failedReason);
        endToEndModel.setAttribute("endDate", DateFormatterUtil.format(endDate));
        endToEndModel.setAttribute("latency", latency);

        return endToEndModel;
    }

    public enum EndToEndStatus {
        Succeeded,
        Failed
    }

    private String getServiceName(String serviceCode) {
        String serviceName = null;
        String serviceNameQuery = "select service_name from portal_services where service_code='" + serviceCode + "'";
        QueryMessage queryMessage = new QueryMessage();
        queryMessage.setQuery(serviceNameQuery);
        ResultSet resultSet = queryService.executeQuery(queryMessage);
        if (resultSet.getRecords() != null && resultSet.getRecords().size() > 0) {
            Record commonRecord = resultSet.getRecords().get(0);
            serviceName = commonRecord.get("service_name") + "";
        }
        return serviceName;
    }
}
