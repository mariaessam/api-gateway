package com.cit.vericash.api.gateway.communication;

import com.cit.vericash.api.gateway.model.KeyValueModel;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


/*
*Represents a feign client of the redis in memory service
*/
@FeignClient("in-memory-service")
public interface RedisClient {
    @RequestMapping(value = "/setValue", method = RequestMethod.POST, consumes = "application/json")
    String setValue(@RequestBody KeyValueModel model);

    @RequestMapping(value = "/getValue", method = RequestMethod.POST, consumes = "application/json")
    String getValue(@RequestBody KeyValueModel model);

}
