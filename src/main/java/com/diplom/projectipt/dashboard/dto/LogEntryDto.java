package com.diplom.projectipt.dashboard.dto;

public record LogEntryDto(
        String timestamp,
        String level,
        String message,
        String source
) {
}
