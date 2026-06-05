package com.diplom.projectipt.dashboard;

import com.diplom.projectipt.config.DashboardLogProperties;
import com.diplom.projectipt.dashboard.dto.LogEntryDto;
import com.diplom.projectipt.service.SystemService;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SystemLogService {

    private static final Pattern AUTH_TS = Pattern.compile(
            "^(\\w{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})"
    );
    private static final DateTimeFormatter AUDIT_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SystemService systemService;
    private final DashboardLogProperties logProperties;
    private final AuditService auditService;
    private final SshLogReader sshLogReader;

    public SystemLogService(
            SystemService systemService,
            DashboardLogProperties logProperties,
            AuditService auditService,
            SshLogReader sshLogReader
    ) {
        this.systemService = systemService;
        this.logProperties = logProperties;
        this.auditService = auditService;
        this.sshLogReader = sshLogReader;
    }

    public List<LogEntryDto> tailLogs() {
        List<LogEntryDto> entries = new ArrayList<>();
        entries.addAll(readAuthLog());
        entries.addAll(readSshJournalLog());
        entries.addAll(readJournal());
        entries.addAll(readAuditLog());
        entries.sort(Comparator.comparing(LogEntryDto::timestamp));
        if (entries.size() > logProperties.getTailLines()) {
            return entries.subList(entries.size() - logProperties.getTailLines(), entries.size());
        }
        return entries;
    }

    private List<LogEntryDto> readAuthLog() {
        Path path = Path.of(logProperties.getAuthPath());
        if (!Files.exists(path)) {
            return List.of();
        }
        var result = systemService.executeCommand(List.of(
                "tail", "-n", String.valueOf(logProperties.getTailLines()), path.toString()
        ), false);
        if (!result.isSuccess()) {
            return List.of();
        }
        return result.getOutput().lines()
                .map(line -> toEntry(line, "auth.log"))
                .toList();
    }

    private List<LogEntryDto> readSshJournalLog() {
        return sshLogReader.readSshLines().stream()
                .filter(SshLogReader::isSshLine)
                .map(line -> toEntry(line, "sshd-journal"))
                .toList();
    }

    private List<LogEntryDto> readAuditLog() {
        return auditService.recent(logProperties.getTailLines()).stream()
                .map(entry -> {
                    String ts = AUDIT_TS.format(
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.timestamp()), ZoneId.systemDefault())
                    );
                    String level = entry.success() ? "ИНФО" : "ПРЕДУПР";
                    String message = entry.action() + ": " + entry.details();
                    return new LogEntryDto(ts, level, message, "securegate");
                })
                .toList();
    }

    private List<LogEntryDto> readJournal() {
        var result = systemService.executeCommand(List.of(
                "journalctl",
                "-u", logProperties.getJournalUnit(),
                "-n", String.valueOf(logProperties.getTailLines()),
                "--no-pager",
                "-o", "short-iso"
        ), false);
        if (!result.isSuccess()) {
            return List.of();
        }
        return result.getOutput().lines()
                .map(line -> toEntry(line, "journal"))
                .toList();
    }

    private LogEntryDto toEntry(String line, String source) {
        String level = classify(line);
        String timestamp = extractTimestamp(line);
        return new LogEntryDto(timestamp, level, line, source);
    }

    private String classify(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.contains("failed") || lower.contains("invalid") || lower.contains("blocked")
                || lower.contains("denied") || lower.contains("refused")) {
            return "БЛОКИР";
        }
        if (lower.contains("warning") || lower.contains("warn")) {
            return "ПРЕДУПР";
        }
        return "ИНФО";
    }

    private String extractTimestamp(String line) {
        Matcher matcher = AUTH_TS.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (line.length() >= 19 && line.charAt(4) == '-') {
            return line.substring(0, 19);
        }
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        );
    }
}
