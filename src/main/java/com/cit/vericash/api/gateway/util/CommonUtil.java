package com.cit.vericash.api.gateway.util;

import com.cit.vericash.api.gateway.model.Constants;
import com.cit.vericash.portal.backend.model.dao.Criteria;
import com.cit.vericash.portal.backend.model.dao.Paging;
import com.cit.vericash.portal.backend.model.dao.QueryMessage;
import com.cit.vericash.portal.backend.model.dao.ResultSet;
import com.cit.vericash.portal.backend.util.PropertyLoader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;


public class CommonUtil {

    public static Object getParameterFromResultSet(ResultSet resultSet, String paramName) {
        Object paramValue = null;
        if (resultSet != null && resultSet.getRecords() != null && resultSet.getRecords().size() > 0) {
            paramValue = resultSet.getRecords().get(0).get(paramName);
        }
        return paramValue;
    }

    public static Long getParameterFromResultSetAsLong(ResultSet resultSet, String paramName) {
        Long paramValue = null;
        if (resultSet != null && resultSet.getRecords() != null && resultSet.getRecords().size() > 0) {
            Object paramValueObj = resultSet.getRecords().get(0).get(paramName);
            paramValue = (paramValueObj == null ? null : Long.valueOf(paramValueObj.toString()));
        }
        return paramValue;
    }

    public static Integer getParameterFromResultSetAsInteger(ResultSet resultSet, String paramName) {
        Integer paramValue = null;
        if (resultSet != null && resultSet.getRecords() != null && resultSet.getRecords().size() > 0) {
            Object paramValueObj = resultSet.getRecords().get(0).get(paramName);
            paramValue = (paramValueObj == null ? null : Integer.valueOf(paramValueObj.toString()));
        }
        return paramValue;
    }

    public static String getParameterFromResultSetAsString(ResultSet resultSet, String paramName) {
        String paramValueStr = null;
        if (resultSet != null && resultSet.getRecords() != null && resultSet.getRecords().size() > 0) {
            Object paramValue = resultSet.getRecords().get(0).get(paramName);
            paramValueStr = (paramValue == null ? null : paramValue.toString());
        }
        return paramValueStr;
    }

    public static List<Criteria> getCriteriaList(List<LinkedHashMap<String, Object>> criteriaList) {
        if (criteriaList != null) {
            ObjectMapper mapper = new ObjectMapper();
            List<Criteria> myCriteriaList = mapper.convertValue(criteriaList, new TypeReference<List<Criteria>>() {
            });
            return myCriteriaList;
        }
        return null;
    }

    public static TreeMap<String, Object> convertObjectToMap(Object obj) {
        TreeMap<String, Object> map = null;
        try {
            if (obj != null) {
                ObjectMapper oMapper = new ObjectMapper();
                map = oMapper.convertValue(obj, TreeMap.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new TreeMap<String, Object>();
        }
        return map;
    }

    public static QueryMessage getQueryMessage(String query, List<Criteria> criteriaList, Paging paging) {
        QueryMessage queryMessage = new QueryMessage();
        queryMessage.setCriteriaList(criteriaList);
        queryMessage.setQuery(query);
        queryMessage.setPaging(paging);
        return queryMessage;
    }

    public static boolean isUploadFileService(String configurableUploadedFilesURI) {
        String uploadURI = RequestContext.getCurrentContext().getRequest().getRequestURI();
        String[] mappingUploadedFilesURI = configurableUploadedFilesURI.split("\\,");
        for (int i = 0; i < mappingUploadedFilesURI.length; i++) {
            if (uploadURI != null && (uploadURI.contains(mappingUploadedFilesURI[i]))) {
                return true;
            }
        }
        return false;
    }
}
