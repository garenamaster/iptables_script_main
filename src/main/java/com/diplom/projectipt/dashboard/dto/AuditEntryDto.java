package com.diplom.projectipt.dashboard.dto;

public record AuditEntryDto(
        long timestamp,
        String action,
        String details,
        boolean success
) {
}
