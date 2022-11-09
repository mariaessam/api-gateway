package com.cit.vericash.api.gateway.security;


import com.cit.vericash.api.gateway.model.Constants;
import com.cit.vericash.portal.backend.data.QueryService;
import com.cit.vericash.portal.backend.data.service.BaseDaoService;
import com.cit.vericash.portal.backend.model.dao.QueryMessage;
import com.cit.vericash.portal.backend.model.dao.Record;
import com.cit.vericash.portal.backend.model.dao.ResultSet;
import com.cit.vericash.portal.backend.model.message.Message;
import com.cit.vericash.portal.backend.util.PropertyLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class DuplicateRequestPreventerComponent {

    @Autowired
    PropertyLoader propLoader;

    @Autowired
    BaseDaoService baseDaoService;

    @Autowired
    QueryService queryService;

    /*
     *Checks if the timestamp in the request is the same timestamp
     *in memory database for the given userId in the message header
     *in the database for the given userId in the message header
     */
    public boolean isDuplicateRequest(Message message) {
        Boolean isEnabled = propLoader.getPropertyAsBoolean("duplicate.request.preventer.enabled", Constants.GATEWAY_FOLDER_NAME,Constants.GATEWAY_CONFIG_FILE_NAME);
        Long timestamp = message.getHeader().getAttributeAsLong(Constants.HEADER_TIMESTAMP);
        Long requestTimestamp = message.getHeader().getAttributeAsLong(Constants.HEADER_TIMESTAMP);
        Long userId =  message.getHeader().getAttributeAsLong(Constants.HEADER_USER_ID);
        if(!isEnabled){
            return false;
        } else {
            if(timestamp == null || userId == null){
                return true;
            }
        }
        if(requestTimestamp == null || userId == null){
            return true;
        }
        Date requestTimestampDate=new Date(requestTimestamp);
        Date dateNow=new Date();
        long diffTime = dateNow.getTime() - requestTimestampDate.getTime();
        long diffDays = diffTime / (1000 * 60 * 60 * 24);

        if(diffDays>=1) // duplicate request detected
        {
            return true;
        }
        else
        {
            Long savedTimestamp=getTimestampByUserId(userId,timestamp);
            if(savedTimestamp!=null) // duplicate request detected
            {
                return true;
            }
            else
            {
                savedTimestamp=requestTimestamp;
                insertUserTimestamp(userId, requestTimestamp);
            }
        }
        return false;
    }
    private Long getTimestampByUserId(Long userId,Long timestamp)
    {
        QueryMessage queryMessage=new QueryMessage();
        queryMessage.setQuery("select timestamp from PORTAL_USER_TIMESTAMP where business_user_id="+userId+"and timestamp="+timestamp);
        ResultSet resultSet=queryService.executeQuery(queryMessage);
        if(resultSet.getRecords()!=null && resultSet.getRecords().size()>0)
        {
            Record commonRecord =resultSet.getRecords().get(0);
            return commonRecord.getFieldAsLong("TIMESTAMP");
        }
        return null;
    }

    private void insertUserTimestamp(Long userId, Long timestamp)
    {
        String nowDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String databaseConnectionType=propLoader.getPropertyAsString("databaseConnectionType");
        if(databaseConnectionType.equalsIgnoreCase("oracle")) {
            baseDaoService.executeNativeQuery("insert into PORTAL_USER_TIMESTAMP values(" + userId + "," + timestamp + ",TO_DATE('" + nowDate + "','yyyy-MM-dd'))", true);
        }
    }
}

