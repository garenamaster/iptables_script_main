package com.diplom.projectipt.dashboard;

import com.diplom.projectipt.config.FirewallProperties;
import com.diplom.projectipt.dashboard.dto.DashboardStatusDto;
import com.diplom.projectipt.dashboard.dto.StatusItemDto;
import com.diplom.projectipt.service.FirewallService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class DashboardStatusService {

    private final FirewallService firewallService;
    private final FirewallProperties firewallProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public DashboardStatusService(FirewallService firewallService, FirewallProperties firewallProperties) {
        this.firewallService = firewallService;
        this.firewallProperties = firewallProperties;
    }

    public DashboardStatusDto getStatus() {
        return new DashboardStatusDto(firewallStatus(), apiStatus());
    }

    private StatusItemDto firewallStatus() {
        int sshPort = firewallProperties.getSshPort();
        if (firewallService.isBaselineActive(sshPort)) {
            return StatusItemDto.ok("Активен");
        }
        return StatusItemDto.fail(
                "Не активен",
                "Базовая защита setup-basic не применена или изменена (ожидается INPUT DROP, lo, ESTABLISHED, SSH "
                        + sshPort + ", 80, 443)"
        );
    }

    @SuppressWarnings("unchecked")
    private StatusItemDto apiStatus() {
        try {
            Map<String, Object> body = restTemplate.getForObject(
                    "http://127.0.0.1:8080/health",
                    Map.class
            );
            if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                return StatusItemDto.ok("Работает");
            }
            return StatusItemDto.fail("Недоступен", "Health endpoint вернул неуспешный ответ");
        } catch (Exception e) {
            return StatusItemDto.fail("Недоступен", e.getMessage());
        }
    }
}
