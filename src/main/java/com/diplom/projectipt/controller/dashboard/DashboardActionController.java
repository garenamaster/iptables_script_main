package com.diplom.projectipt.controller.dashboard;

import com.diplom.projectipt.dashboard.DashboardActionService;
import com.diplom.projectipt.dashboard.dto.ActionResultDto;
import com.diplom.projectipt.dashboard.dto.ApplyBaselineRequest;
import com.diplom.projectipt.dashboard.dto.PortActionRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard/actions")
public class DashboardActionController {

    private final DashboardActionService dashboardActionService;

    public DashboardActionController(DashboardActionService dashboardActionService) {
        this.dashboardActionService = dashboardActionService;
    }

    @PostMapping("/lockdown")
    public ActionResultDto lockdown() {
        return dashboardActionService.lockdown();
    }

    @PostMapping("/reset-firewall")
    public ActionResultDto resetFirewall() {
        return dashboardActionService.resetFirewall();
    }

    @PostMapping("/apply-baseline")
    public ActionResultDto applyBaseline(@RequestBody(required = false) ApplyBaselineRequest request) {
        Integer sshPort = request != null ? request.sshPort() : null;
        return dashboardActionService.applyBaseline(sshPort);
    }

    @PostMapping("/open-port")
    public ActionResultDto openPort(@RequestBody PortActionRequest request) {
        return dashboardActionService.openPort(request.port());
    }

    @PostMapping("/close-port")
    public ActionResultDto closePort(@RequestBody PortActionRequest request) {
        return dashboardActionService.closePort(request.port());
    }
}
