package com.diplom.projectipt.tempaccess.dto;

public record CreateTempAccessRequest(
        String ip,
        int port,
        int ttlMinutes
) {
}
