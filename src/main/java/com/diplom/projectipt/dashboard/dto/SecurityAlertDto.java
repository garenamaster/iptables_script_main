package com.diplom.projectipt.dashboard.dto;

public record SecurityAlertDto(
        String timestamp,
        String level,
        String title,
        String message,
        String sourceIp,
        String user
) {
}
