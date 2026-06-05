package com.diplom.projectipt.dashboard.dto;

public record StatusItemDto(
        boolean ok,
        String label,
        String reason
) {
    public static StatusItemDto ok(String label) {
        return new StatusItemDto(true, label, null);
    }

    public static StatusItemDto fail(String label, String reason) {
        return new StatusItemDto(false, label, reason);
    }
}
