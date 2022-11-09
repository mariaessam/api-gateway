package com.cit.vericash.api.gateway.controller;

import com.cit.vericash.api.gateway.service.ApprovalCycleCacheService;
import com.cit.vericash.api.gateway.service.AuditCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApigatewayController {

    @Autowired
    ApprovalCycleCacheService approvalCycleCacheService;

    @Autowired
    AuditCacheService auditCacheService;

    @RequestMapping(value="/clear-approvalConfCache/{walletId}", method= RequestMethod.GET)
    public Object  clearApprovalConfigurationCache(@PathVariable Long walletId){
        approvalCycleCacheService.clearCache(walletId);
        return "Approval configuration cache is cleared";
    }

    @RequestMapping(value="/clear-auditCache/{walletId}", method= RequestMethod.GET)
    public Object  clearAuditCache(@PathVariable Long walletId){
        auditCacheService.clearCache(walletId);
        return "Audit configuration cache is cleared";
    }
}
