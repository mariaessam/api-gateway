package com.cit.vericash.api.gateway.service;

import com.cit.vericash.api.gateway.communication.WorkflowService;
import com.cit.vericash.portal.backend.model.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

@Service
@EnableAsync
@Scope( proxyMode = ScopedProxyMode.TARGET_CLASS )
public class ApprovalCycleService {


    @Autowired
    WorkflowService workflowService;

    @Async("threadPoolTaskExecutor")
    public LinkedHashMap start(Message message)
    {
        return (LinkedHashMap)workflowService.startApprovalCycle(message);
    }
}
