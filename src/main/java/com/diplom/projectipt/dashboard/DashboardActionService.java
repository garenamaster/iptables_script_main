package com.diplom.projectipt.dashboard;

import com.diplom.projectipt.config.FirewallProperties;
import com.diplom.projectipt.dashboard.dto.ActionResultDto;
import com.diplom.projectipt.service.FirewallService;
import org.springframework.stereotype.Service;

@Service
public class DashboardActionService {

    private final FirewallService firewallService;
    private final FirewallProperties firewallProperties;
    private final AuditService auditService;

    public DashboardActionService(
            FirewallService firewallService,
            FirewallProperties firewallProperties,
            AuditService auditService
    ) {
        this.firewallService = firewallService;
        this.firewallProperties = firewallProperties;
        this.auditService = auditService;
    }

    public ActionResultDto lockdown() {
        int sshPort = firewallProperties.getSshPort();
        return runAction("LOCKDOWN", "Режим блокировки применён (SSH " + sshPort + ")", () ->
                firewallService.enterLockdownMode(sshPort));
    }

    public ActionResultDto resetFirewall() {
        return runAction("RESET_FIREWALL", "Все правила iptables сброшены", () ->
                firewallService.resetFirewallToDefault());
    }

    public ActionResultDto applyBaseline(Integer sshPortOverride) {
        int sshPort = sshPortOverride != null ? sshPortOverride : firewallProperties.getSshPort();
        if (sshPortOverride != null) {
            firewallProperties.setSshPort(sshPort);
        }
        return runAction("APPLY_BASELINE", "Базовая защита применена (SSH " + sshPort + ")", () ->
                firewallService.setupBasicFirewall(sshPort));
    }

    public ActionResultDto openPort(int port) {
        validatePortAllowed(port);
        return runAction("OPEN_PORT", () -> {
            boolean changed = firewallService.openPort(port);
            return changed ? "Порт " + port + " открыт" : "Порт " + port + " уже был открыт";
        });
    }

    public ActionResultDto closePort(int port) {
        validatePortAllowed(port);
        return runAction("CLOSE_PORT", () -> {
            boolean changed = firewallService.closePort(port);
            return changed ? "Порт " + port + " закрыт" : "Порт " + port + " уже был закрыт";
        });
    }

    private void validatePortAllowed(int port) {
        if (firewallProperties.blockedPortSet().contains(port)) {
            throw new IllegalArgumentException("Порт " + port + " запрещён для ручного открытия/закрытия");
        }
    }

    private ActionResultDto runAction(String action, String successMessage, Runnable task) {
        return runAction(action, () -> {
            task.run();
            return successMessage;
        });
    }

    private ActionResultDto runAction(String action, java.util.function.Supplier<String> task) {
        try {
            String message = task.get();
            auditService.record(action, message, true);
            return new ActionResultDto(true, message);
        } catch (Exception e) {
            auditService.record(action, e.getMessage(), false);
            throw e;
        }
    }
}
