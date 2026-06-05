package com.diplom.projectipt.dashboard.dto;

import java.util.List;

public record MonitoringDto(
        List<MonitoringMetricDto> metrics
) {
}
