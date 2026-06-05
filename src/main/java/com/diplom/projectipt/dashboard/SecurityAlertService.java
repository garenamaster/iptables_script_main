package com.diplom.projectipt.dashboard;

import com.diplom.projectipt.dashboard.dto.SecurityAlertDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SecurityAlertService {

    private static final Pattern SSH_ACCEPTED = Pattern.compile(
            "Accepted\\s+\\w+\\s+for\\s+(\\S+)\\s+from\\s+(\\S+)\\s+port",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SSH_FAILED = Pattern.compile(
            "Failed\\s+\\w+\\s+for\\s+(?:invalid\\s+user\\s+)?(\\S+)\\s+from\\s+(\\S+)\\s+port",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SSH_INVALID = Pattern.compile(
            "Invalid\\s+user\\s+(\\S+)\\s+from\\s+(\\S+)\\s+port",
            Pattern.CASE_INSENSITIVE
    );

    private final SshLogReader sshLogReader;

    public SecurityAlertService(SshLogReader sshLogReader) {
        this.sshLogReader = sshLogReader;
    }

    public List<SecurityAlertDto> recentSshAlerts(int limit) {
        List<String> lines = sshLogReader.readSshLines();
        List<SecurityAlertDto> alerts = new ArrayList<>();
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (!SshLogReader.isSshLine(line)) {
                continue;
            }
            SecurityAlertDto alert = parseSshLine(line);
            if (alert != null) {
                alerts.add(alert);
            }
            if (alerts.size() >= limit) {
                break;
            }
        }
        Collections.reverse(alerts);
        return alerts;
    }

    private SecurityAlertDto parseSshLine(String line) {
        String timestamp = SshLogReader.extractTimestamp(line);

        Matcher invalid = SSH_INVALID.matcher(line);
        if (invalid.find()) {
            return new SecurityAlertDto(
                    timestamp,
                    "БЛОКИР",
                    "Неизвестный пользователь SSH",
                    "Попытка входа как " + invalid.group(1) + " с " + invalid.group(2),
                    invalid.group(2),
                    invalid.group(1)
            );
        }

        Matcher failed = SSH_FAILED.matcher(line);
        if (failed.find()) {
            return new SecurityAlertDto(
                    timestamp,
                    "ПРЕДУПР",
                    "Неудачная попытка SSH",
                    "Неудачный вход для " + failed.group(1) + " с " + failed.group(2),
                    failed.group(2),
                    failed.group(1)
            );
        }

        Matcher accepted = SSH_ACCEPTED.matcher(line);
        if (accepted.find()) {
            return new SecurityAlertDto(
                    timestamp,
                    "ИНФО",
                    "Успешный вход SSH",
                    "Пользователь " + accepted.group(1) + " вошёл с " + accepted.group(2),
                    accepted.group(2),
                    accepted.group(1)
            );
        }

        return null;
    }
}
