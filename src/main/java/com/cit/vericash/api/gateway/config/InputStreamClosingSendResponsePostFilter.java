package com.cit.vericash.api.gateway.config;

import com.netflix.zuul.context.RequestContext;
import org.springframework.cloud.netflix.zuul.filters.post.SendResponseFilter;

import java.io.IOException;
import java.io.InputStream;

/**
 * The whole point of this filter is to ensure that the InputStream coming out of the HttpClient
 * is ALWAYS closed. The super class never calls close() under the assumption that the stream will
 * always be read to the end, even though it won't in exceptional cases
 *
 */
public class InputStreamClosingSendResponsePostFilter extends SendResponseFilter {
    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 1000;
    }

    @Override
    public Object run() {
        try {
            return super.run();
        }
        finally {
            // Simply force a close for every request...
            doCloseSourceInputStream();
        }
    }

    void doCloseSourceInputStream() {
        RequestContext context = RequestContext.getCurrentContext();
        InputStream inputStream = context.getResponseDataStream();
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // This should actually never happen
            }
        }
    }
}