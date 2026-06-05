package com.diplom.projectipt.controller.dashboard;

import com.diplom.projectipt.dashboard.MonitoringService;
import com.diplom.projectipt.dashboard.SecurityAlertService;
import com.diplom.projectipt.dashboard.dto.MonitoringDto;
import com.diplom.projectipt.dashboard.dto.SecurityAlertDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardMonitoringController {

    private final MonitoringService monitoringService;
    private final SecurityAlertService securityAlertService;

    public DashboardMonitoringController(
            MonitoringService monitoringService,
            SecurityAlertService securityAlertService
    ) {
        this.monitoringService = monitoringService;
        this.securityAlertService = securityAlertService;
    }

    @GetMapping("/monitoring")
    public MonitoringDto monitoring() {
        return monitoringService.getMonitoring();
    }

    @GetMapping("/security-alerts")
    public List<SecurityAlertDto> securityAlerts(@RequestParam(defaultValue = "15") int limit) {
        return securityAlertService.recentSshAlerts(limit);
    }
}
