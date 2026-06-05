package com.diplom.projectipt.dashboard;

import com.diplom.projectipt.dashboard.dto.MonitoringDto;
import com.diplom.projectipt.dashboard.dto.MonitoringMetricDto;
import com.diplom.projectipt.service.FirewallService;
import com.diplom.projectipt.tempaccess.TempAccessService;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MonitoringService {

    private final FirewallService firewallService;
    private final TempAccessService tempAccessService;
    private final AuditService auditService;

    public MonitoringService(
            FirewallService firewallService,
            TempAccessService tempAccessService,
            AuditService auditService
    ) {
        this.firewallService = firewallService;
        this.tempAccessService = tempAccessService;
        this.auditService = auditService;
    }

    public MonitoringDto getMonitoring() {
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMb = runtime.maxMemory() / (1024 * 1024);
        int memoryPercent = maxMb > 0 ? (int) Math.min(100, (usedMb * 100) / maxMb) : 0;

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        String uptime = formatUptime(uptimeMs);

        int tempRules = tempAccessService.listActive().size();
        int firewallRules;
        String inputPolicy;
        try {
            var status = firewallService.getFirewallStatus();
            firewallRules = status.rulesCount();
            inputPolicy = status.inputPolicy();
        } catch (Exception e) {
            firewallRules = 0;
            inputPolicy = "UNKNOWN";
        }

        long hourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        long auditLastHour = auditService.recent(300).stream()
                .filter(e -> e.timestamp() >= hourAgo)
                .count();

        List<MonitoringMetricDto> metrics = new ArrayList<>();
        metrics.add(new MonitoringMetricDto("memory", "Память JVM", usedMb + " / " + maxMb + " МБ", memoryPercent));
        metrics.add(new MonitoringMetricDto("uptime", "Время работы", uptime, 0));
        metrics.add(new MonitoringMetricDto("temp", "Временные доступы", String.valueOf(tempRules), Math.min(100, tempRules * 10)));
        metrics.add(new MonitoringMetricDto("rules", "Правил firewall", String.valueOf(firewallRules), Math.min(100, firewallRules)));
        metrics.add(new MonitoringMetricDto("policy", "Политика INPUT", inputPolicy, "DROP".equalsIgnoreCase(inputPolicy) ? 100 : 30));
        metrics.add(new MonitoringMetricDto("audit", "События за час", String.valueOf(auditLastHour), Math.min(100, (int) auditLastHour)));

        return new MonitoringDto(metrics);
    }

    private String formatUptime(long uptimeMs) {
        long sec = uptimeMs / 1000;
        long days = sec / 86400;
        long hours = (sec % 86400) / 3600;
        long minutes = (sec % 3600) / 60;
        if (days > 0) {
            return days + "д " + hours + "ч " + minutes + "м";
        }
        if (hours > 0) {
            return hours + "ч " + minutes + "м";
        }
        return minutes + "м";
    }
}
