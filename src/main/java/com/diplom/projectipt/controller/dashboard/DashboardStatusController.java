package com.diplom.projectipt.controller.dashboard;

import com.diplom.projectipt.dashboard.DashboardStatusService;
import com.diplom.projectipt.dashboard.dto.DashboardStatusDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardStatusController {

    private final DashboardStatusService dashboardStatusService;

    public DashboardStatusController(DashboardStatusService dashboardStatusService) {
        this.dashboardStatusService = dashboardStatusService;
    }

    @GetMapping("/status")
    public DashboardStatusDto status() {
        return dashboardStatusService.getStatus();
    }
}
