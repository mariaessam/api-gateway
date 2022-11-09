package com.cit.vericash.api.gateway.filter;

import com.cit.vericash.api.gateway.util.CommonUtil;
import com.cit.vericash.api.gateway.util.JsonUtil;
import com.cit.vericash.portal.backend.model.request.Response;
import com.google.gson.Gson;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class ErrorFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return "error";
    }

    @Override
    public int filterOrder() {
        return 3;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        InputStream responseInputStream=null;
        InputStream requestInputStream=null;
        try {
            responseInputStream=ctx.getResponseDataStream();
            requestInputStream = ctx.getRequest().getInputStream();
            if(requestInputStream!=null) {
                requestInputStream.close();
            }
            if(responseInputStream!=null) {
                responseInputStream.close();
            }
//            ctx.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ctx;
    }
}
