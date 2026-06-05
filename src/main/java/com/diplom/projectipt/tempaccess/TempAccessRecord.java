package com.diplom.projectipt.tempaccess;

public record TempAccessRecord(
        long id,
        String ip,
        int port,
        long expiresAtEpochSec
) {
}
