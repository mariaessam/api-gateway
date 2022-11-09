package com.cit.vericash.api.gateway.config;

import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.ZuulProxyAutoConfiguration;
import org.springframework.cloud.netflix.zuul.filters.post.SendResponseFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.DebugFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.FormBodyWrapperFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.Servlet30WrapperFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCircuitBreaker
@EnableDiscoveryClient
public class MyZuulConfiguration extends ZuulProxyAutoConfiguration {
    /**
     * Disable these filters, as they attempt to either parse the entire request body, or end up wrapping the
     * request in a wrapper which attempts to parse the body - either of which is a BAD idea for large request bodies
     */
    @Bean
    @Override
    public DebugFilter debugFilter() {
        return new DebugFilter() {
            @Override
            public boolean shouldFilter() {
                return false;
            }
        };
    }

    @Bean
    @Override
    public Servlet30WrapperFilter servlet30WrapperFilter() {
        return new Servlet30WrapperFilter() {
            @Override
            public boolean shouldFilter() {
                return false;
            }
        };
    }

    @Bean
    @Override
    public FormBodyWrapperFilter formBodyWrapperFilter() {
        return new FormBodyWrapperFilter() {
            @Override
            public boolean shouldFilter() {
                return false;
            }
        };
    }

    /**
     * This was actually only needed before our patch was merged into master
     **/
    @Bean
    @Override
    public SendResponseFilter sendResponseFilter() {
        return new InputStreamClosingSendResponsePostFilter();
    }
}