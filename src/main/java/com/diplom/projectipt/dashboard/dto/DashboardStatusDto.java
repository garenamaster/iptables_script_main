package com.diplom.projectipt.dashboard.dto;

public record DashboardStatusDto(
        StatusItemDto firewall,
        StatusItemDto api
) {
}
