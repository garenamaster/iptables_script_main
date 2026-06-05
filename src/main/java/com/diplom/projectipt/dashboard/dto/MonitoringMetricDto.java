package com.diplom.projectipt.dashboard.dto;

public record MonitoringMetricDto(
        String key,
        String label,
        String value,
        int percent
) {
}
