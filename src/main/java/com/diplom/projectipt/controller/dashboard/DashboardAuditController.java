package com.diplom.projectipt.controller.dashboard;

import com.diplom.projectipt.dashboard.AuditService;
import com.diplom.projectipt.dashboard.dto.AuditEntryDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardAuditController {

    private final AuditService auditService;

    public DashboardAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/audit")
    public List<AuditEntryDto> audit(@RequestParam(defaultValue = "50") int limit) {
        return auditService.recent(limit);
    }
}
