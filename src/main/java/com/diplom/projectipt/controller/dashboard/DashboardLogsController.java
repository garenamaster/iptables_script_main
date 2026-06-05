package com.diplom.projectipt.controller.dashboard;

import com.diplom.projectipt.dashboard.SystemLogService;
import com.diplom.projectipt.dashboard.dto.LogEntryDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardLogsController {

    private final SystemLogService systemLogService;

    public DashboardLogsController(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    @GetMapping("/logs")
    public List<LogEntryDto> logs() {
        return systemLogService.tailLogs();
    }
}
