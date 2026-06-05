package com.diplom.projectipt.dashboard;

import com.diplom.projectipt.config.DashboardLogProperties;
import com.diplom.projectipt.service.CommandResult;
import com.diplom.projectipt.service.SystemService;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SshLogReader {

    private static final Pattern ISO_TS = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})");
    private static final Pattern AUTH_TS = Pattern.compile("^(\\w{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})");

    private final SystemService systemService;
    private final DashboardLogProperties logProperties;

    public SshLogReader(SystemService systemService, DashboardLogProperties logProperties) {
        this.systemService = systemService;
        this.logProperties = logProperties;
    }

    public List<String> readSshLines() {
        int lines = logProperties.getTailLines() * 2;
        Set<String> unique = new LinkedHashSet<>();

        addLines(unique, readAuthFile(lines));
        if (unique.isEmpty()) {
            addLines(unique, readJournalUnit("ssh", lines));
        }
        if (unique.isEmpty()) {
            addLines(unique, readJournalUnit("sshd", lines));
        }
        if (unique.isEmpty()) {
            addLines(unique, readJournalComm("sshd", lines));
        }

        return new ArrayList<>(unique);
    }

    public static boolean isSshLine(String line) {
        return line.toLowerCase(Locale.ROOT).contains("sshd");
    }

    public static String extractTimestamp(String line) {
        Matcher iso = ISO_TS.matcher(line);
        if (iso.find()) {
            return iso.group(1).replace('T', ' ');
        }
        Matcher auth = AUTH_TS.matcher(line);
        if (auth.find()) {
            return auth.group(1);
        }
        if (line.length() >= 19 && line.charAt(4) == '-') {
            return line.substring(0, 19);
        }
        return "-";
    }

    private void addLines(Set<String> target, List<String> lines) {
        for (String line : lines) {
            if (!line.isBlank()) {
                target.add(line);
            }
        }
    }

    private List<String> readAuthFile(int lines) {
        Path path = Path.of(logProperties.getAuthPath());
        if (!Files.exists(path)) {
            return List.of();
        }
        try {
            if (Files.size(path) == 0) {
                return List.of();
            }
        } catch (Exception e) {
            return List.of();
        }
        CommandResult result = systemService.executeCommand(List.of(
                "tail", "-n", String.valueOf(lines), path.toString()
        ), false);
        if (!result.isSuccess() || result.getOutput().isBlank()) {
            return List.of();
        }
        return result.getOutput().lines().toList();
    }

    private List<String> readJournalUnit(String unit, int lines) {
        CommandResult result = systemService.executeCommand(List.of(
                "journalctl",
                "-u", unit,
                "-n", String.valueOf(lines),
                "--no-pager",
                "-o", "short-iso"
        ), false);
        return parseJournal(result);
    }

    private List<String> readJournalComm(String comm, int lines) {
        CommandResult result = systemService.executeCommand(List.of(
                "journalctl",
                "_COMM=" + comm,
                "-n", String.valueOf(lines),
                "--no-pager",
                "-o", "short-iso"
        ), false);
        return parseJournal(result);
    }

    private List<String> parseJournal(CommandResult result) {
        if (!result.isSuccess() || result.getOutput().isBlank()) {
            return List.of();
        }
        return result.getOutput().lines().toList();
    }
}
