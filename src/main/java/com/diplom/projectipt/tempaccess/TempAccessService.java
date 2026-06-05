package com.diplom.projectipt.tempaccess;

import com.diplom.projectipt.dashboard.AuditService;
import com.diplom.projectipt.service.FirewallService;
import com.diplom.projectipt.tempaccess.dto.CreateTempAccessRequest;
import com.diplom.projectipt.tempaccess.dto.TempAccessDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TempAccessService {

    private final Map<Long, TempAccessRecord> records = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);
    private final FirewallService firewallService;
    private final AuditService auditService;

    public TempAccessService(FirewallService firewallService, AuditService auditService) {
        this.firewallService = firewallService;
        this.auditService = auditService;
    }

    public TempAccessDto grant(CreateTempAccessRequest request) {
        if (request.ttlMinutes() < 1) {
            throw new IllegalArgumentException("ttlMinutes должен быть >= 1");
        }
        long expiresAt = Instant.now().getEpochSecond() + request.ttlMinutes() * 60L;
        firewallService.allowIpToPort(request.ip(), request.port());
        long id = idSeq.getAndIncrement();
        TempAccessRecord record = new TempAccessRecord(id, request.ip(), request.port(), expiresAt);
        records.put(id, record);
        auditService.record(
                "GRANT_TEMP_ACCESS",
                "ip=" + request.ip() + ", port=" + request.port() + ", ttl=" + request.ttlMinutes() + "m",
                true
        );
        return toDto(record);
    }

    public List<TempAccessDto> listActive() {
        long now = Instant.now().getEpochSecond();
        return records.values().stream()
                .filter(r -> r.expiresAtEpochSec() > now)
                .sorted(Comparator.comparingLong(TempAccessRecord::expiresAtEpochSec))
                .map(this::toDto)
                .toList();
    }

    public void revokeNow(long id) {
        TempAccessRecord record = records.remove(id);
        if (record == null) {
            throw new IllegalArgumentException("Временное правило не найдено");
        }
        firewallService.removeIpFromPort(record.ip(), record.port());
        auditService.record("REVOKE_TEMP_ACCESS", "id=" + id, true);
    }

    public int expireDue() {
        long now = Instant.now().getEpochSecond();
        List<Long> expiredIds = new ArrayList<>();
        for (TempAccessRecord record : records.values()) {
            if (record.expiresAtEpochSec() <= now) {
                expiredIds.add(record.id());
            }
        }
        int count = 0;
        for (Long id : expiredIds) {
            TempAccessRecord record = records.remove(id);
            if (record != null) {
                firewallService.removeIpFromPort(record.ip(), record.port());
                auditService.record(
                        "EXPIRE_TEMP_ACCESS",
                        "ip=" + record.ip() + ", port=" + record.port(),
                        true
                );
                count++;
            }
        }
        return count;
    }

    private TempAccessDto toDto(TempAccessRecord record) {
        long now = Instant.now().getEpochSecond();
        long remaining = Math.max(0, record.expiresAtEpochSec() - now);
        return new TempAccessDto(
                record.id(),
                record.ip(),
                record.port(),
                record.expiresAtEpochSec(),
                remaining,
                remaining > 0 ? "активен" : "истёк"
        );
    }
}
