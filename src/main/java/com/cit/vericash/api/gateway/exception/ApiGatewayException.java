package com.cit.vericash.api.gateway.exception;

import com.netflix.zuul.exception.ZuulException;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;

public class ApiGatewayException extends ZuulRuntimeException {
    public ApiGatewayException(String error,int statusCode,String cause){
        super(new ZuulException(error,statusCode,cause));
    }
}
