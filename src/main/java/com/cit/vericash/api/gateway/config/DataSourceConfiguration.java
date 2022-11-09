package com.cit.vericash.api.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.context.annotation.ApplicationScope;

import javax.sql.DataSource;
import java.sql.SQLException;

@org.springframework.context.annotation.Configuration
@PropertySource(value ={"file:${PORTAL_CONFIG_HOME}/${PROJECT}-config/services/shared-config/datasource.properties"} , ignoreResourceNotFound = false)
@ApplicationScope
public class DataSourceConfiguration {

    private static DriverManagerDataSource driverManagerDataSource=null;
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        if(driverManagerDataSource==null)
            driverManagerDataSource=new DriverManagerDataSource();
        return driverManagerDataSource;
    }
}