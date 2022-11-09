package com.cit.vericash.api.gateway.communication;

import com.cit.vericash.portal.backend.model.message.Message;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient("workflow-service")
public interface WorkflowService {

    @RequestMapping(value = "/start-approvalCycle", method = RequestMethod.POST)
    Object startApprovalCycle(@RequestBody Message message);

}