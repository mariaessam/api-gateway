package com.cit.vericash.api.gateway.preprocessing;

public class MessageHeaderUtil {
    public static String databaseConnectionType;
    public static String serviceCode;
    public static String project;
    public static String entityCode;
    public static String serviceName;

    public static String getServiceCode() {
        return serviceCode;
    }

    public static void setServiceCode(String serviceCode) {
        MessageHeaderUtil.serviceCode = serviceCode;
    }

    public static String getProject() {
        return project;
    }

    public static void setProject(String project) {
        MessageHeaderUtil.project = project;
    }

    public static String getDatabaseConnectionType() {
        return databaseConnectionType;
    }

    public static void setDatabaseConnectionType(String databaseConnectionType) {
        MessageHeaderUtil.databaseConnectionType = databaseConnectionType;
    }

    public static String getEntityCode() {
        return entityCode;
    }

    public static void setEntityCode(String entityCode) {
        MessageHeaderUtil.entityCode = entityCode;
    }

    public static String getServiceName() {
        return serviceName;
    }

    public static void setServiceName(String serviceName) {
        MessageHeaderUtil.serviceName = serviceName;
    }

}
