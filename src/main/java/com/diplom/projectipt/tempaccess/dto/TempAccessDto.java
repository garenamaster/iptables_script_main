package com.diplom.projectipt.tempaccess.dto;

public record TempAccessDto(
        long id,
        String ip,
        int port,
        long expiresAt,
        long remainingSec,
        String status
) {
}
