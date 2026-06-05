package com.diplom.projectipt.service.impl;

import com.diplom.projectipt.exception.CommandExecutionException;
import com.diplom.projectipt.service.CommandResult;
import com.diplom.projectipt.service.FirewallService;
import com.diplom.projectipt.service.FirewallStatus;
import com.diplom.projectipt.service.SystemService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FirewallServiceImpl implements FirewallService {

    private static final String IPTABLES = "/usr/sbin/iptables";
    private static final String IPTABLES_SAVE = "/usr/sbin/iptables-save";
    private static final String INPUT_CHAIN = "INPUT";

    private final SystemService systemService;
    private final ReentrantLock lock = new ReentrantLock();
    private final Set<String> deletedLineKeys = ConcurrentHashMap.newKeySet();

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$"
    );
    private static final Pattern CHAIN_PATTERN = Pattern.compile("^[A-Z0-9_]{1,29}$");
    private static final Pattern POLICY_PATTERN = Pattern.compile("^(ACCEPT|DROP|REJECT)$");
    private static final Pattern OPEN_PORT_RULE_PATTERN = Pattern.compile(".*--dport\\s+(\\d+)\\s+-j\\s+ACCEPT.*");
    private static final Pattern INPUT_POLICY_PATTERN = Pattern.compile("^-P\\s+INPUT\\s+(\\S+)$");

    public FirewallServiceImpl(SystemService systemService) {
        this.systemService = systemService;
    }


    @Override
    public boolean allowIp(String ip) {
        validateIp(ip);

        lock.lock();
        try {
            boolean a = allowIpToPort(ip, 80);
            boolean b = allowIpToPort(ip, 443);
            return a || b;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean allowIpToPort(String ip, int port) {
        validateIp(ip);
        validatePort(port);

        lock.lock();
        try {
            List<String> check = List.of(
                    IPTABLES,
                    "-C", INPUT_CHAIN,
                    "-s", ip,
                    "-p", "tcp",
                    "--dport", String.valueOf(port),
                    "-j", "ACCEPT"
            );
            if (systemService.executeCommand(check).isSuccess()) {
                return false;
            }

            List<String> add = List.of(
                    IPTABLES,
                    "-A", INPUT_CHAIN,
                    "-s", ip,
                    "-p", "tcp",
                    "--dport", String.valueOf(port),
                    "-j", "ACCEPT"
            );
            runOrThrow("Не удалось добавить правило allow", add);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeIpFromPort(String ip, int port) {
        validateIp(ip);
        validatePort(port);
        lock.lock();
        try {
            boolean removedAny = false;
            List<String> del = List.of(
                    IPTABLES,
                    "-D", INPUT_CHAIN,
                    "-s", ip,
                    "-p", "tcp",
                    "--dport", String.valueOf(port),
                    "-j", "ACCEPT"
            );
            while (ruleExists(ip, port)) {
                runOrThrow("Не удалось удалить правило разрешения IP к порту", del);
                removedAny = true;
            }
            return removedAny;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean blockIp(String ip) {
        validateIp(ip);

        lock.lock();
        try {

            List<String> check = List.of(
                    IPTABLES,
                    "-C", INPUT_CHAIN,
                    "-s", ip,
                    "-j", "DROP"
            );

            if (systemService.executeCommand(check).isSuccess()) {
                return false;
            }

            List<String> add = List.of(
                    IPTABLES,
                    "-A", INPUT_CHAIN,
                    "-s", ip,
                    "-j", "DROP"
            );
            runOrThrow("Не удалось заблокировать IP", add);
            return true;

        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean unblockIp(String ip) {
        validateIp(ip);
        lock.lock();
        try {
            boolean removedAny = false;
            List<String> del = List.of(
                    IPTABLES,
                    "-D", INPUT_CHAIN,
                    "-s", ip,
                    "-j", "DROP"
            );
            while (dropRuleExists(ip)) {
                runOrThrow("Не удалось разблокировать IP", del);
                removedAny = true;
            }
            return removedAny;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean openPort(int port) {
        validatePort(port);

        lock.lock();
        try {

            List<String> check = List.of(
                    IPTABLES,
                    "-C", INPUT_CHAIN,
                    "-p", "tcp",
                    "--dport", String.valueOf(port),
                    "-j", "ACCEPT"
            );

            if (systemService.executeCommand(check).isSuccess()) {
                return false;
            }

            List<String> add = List.of(
                    IPTABLES,
                    "-A", INPUT_CHAIN,
                    "-p", "tcp",
                    "--dport", String.valueOf(port),
                    "-j", "ACCEPT"
            );
            runOrThrow("Не удалось открыть порт", add);
            return true;

        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean closePort(int port) {
        validatePort(port);
        lock.lock();

        try {
            boolean removedAny = false;
            List<String> del = List.of(
                    IPTABLES,
                    "-D", INPUT_CHAIN,
                    "-p", "tcp",
                    "--dport", String.valueOf(port),
                    "-j", "ACCEPT"
            );
            while (openPortRuleExists(port)) {
                runOrThrow("Не удалось закрыть порт", del);
                removedAny = true;
            }
            return removedAny;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setChainPolicy(String chain, String policy) {
        String normalizedChain = normalizeChain(chain);
        String normalizedPolicy = normalizePolicy(policy);
        lock.lock();
        try {
            runOrThrow("Не удалось установить политику цепочки",
                    List.of(IPTABLES, "-P", normalizedChain, normalizedPolicy));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void closeAll() {
        setChainPolicy(INPUT_CHAIN, "DROP");
    }

    @Override
    public List<String> getRules() {
        List<String> command = List.of(IPTABLES, "-S");
        CommandResult res = systemService.executeCommand(command);
        if (!res.isSuccess()) {
            throw commandError("Не удалось получить правила firewall", command, res);
        }
        return splitLines(res.getOutput());
    }

    @Override
    public List<String> getRulesDetailed() {
        List<String> command = List.of(IPTABLES, "-L", "--line-numbers", "-n", "-v");
        CommandResult res = systemService.executeCommand(command);
        if (!res.isSuccess()) {
            throw commandError("Не удалось получить подробные правила firewall", command, res);
        }
        return splitLines(res.getOutput());
    }

    @Override
    public boolean ruleExists(String ip, int port) {
        validateIp(ip);
        validatePort(port);
        List<String> check = List.of(
                IPTABLES,
                "-C", INPUT_CHAIN,
                "-s", ip,
                "-p", "tcp",
                "--dport", String.valueOf(port),
                "-j", "ACCEPT"
        );
        return systemService.executeCommand(check).isSuccess();
    }

    @Override
    public boolean deleteRuleByLineNumber(String chain, int lineNumber) {
        String normalizedChain = normalizeChain(chain);
        if (lineNumber < 1) {
            throw new IllegalArgumentException("Некорректный номер строки");
        }
        lock.lock();
        try {
            String key = normalizedChain + ":" + lineNumber;
            if (deletedLineKeys.contains(key)) {
                return false;
            }
            List<String> command = List.of(
                    IPTABLES,
                    "-D", normalizedChain, String.valueOf(lineNumber)
            );
            CommandResult res = systemService.executeCommand(command);
            if (!res.isSuccess()) {
                if (res.getOutput() != null && !res.getOutput().isBlank()) {
                    return false;
                }
                throw commandError("Не удалось удалить правило по номеру строки", command, res);
            }
            deletedLineKeys.add(key);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void flushChain(String chain) {
        String normalizedChain = normalizeChain(chain);
        lock.lock();
        try {
            runOrThrow("Не удалось очистить цепочку", List.of(IPTABLES, "-F", normalizedChain));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void createChain(String chainName) {
        String normalizedChain = normalizeChain(chainName);
        lock.lock();
        try {
            if (chainExists(normalizedChain)) {
                return;
            }
            runOrThrow("Не удалось создать цепочку", List.of(IPTABLES, "-N", normalizedChain));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteChain(String chainName) {
        String normalizedChain = normalizeChain(chainName);
        lock.lock();
        try {
            if (!chainExists(normalizedChain)) {
                return;
            }
            runOrThrow("Не удалось удалить цепочку", List.of(IPTABLES, "-X", normalizedChain));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String saveRules() {
        List<String> command = List.of(IPTABLES_SAVE);
        CommandResult res = systemService.executeCommand(command);
        if (!res.isSuccess()) {
            throw commandError("Не удалось сохранить текущие правила firewall", command, res);
        }
        return res.getOutput();
    }

    @Override
    public List<Integer> getOpenPorts() {
        List<String> rules = getRules();
        List<Integer> ports = new ArrayList<>();
        for (String rule : rules) {
            if (rule.contains("-A INPUT") && rule.contains("-j ACCEPT") && !rule.contains("-s ")) {
                Matcher matcher = OPEN_PORT_RULE_PATTERN.matcher(rule);
                if (matcher.matches()) {
                    ports.add(Integer.parseInt(matcher.group(1)));
                }
            }
        }
        return ports;
    }

    @Override
    public FirewallStatus getFirewallStatus() {
        List<String> rules = getRules();
        String inputPolicy = "UNKNOWN";
        for (String rule : rules) {
            Matcher matcher = INPUT_POLICY_PATTERN.matcher(rule);
            if (matcher.matches()) {
                inputPolicy = matcher.group(1);
                break;
            }
        }
        return new FirewallStatus(inputPolicy, rules.size(), getOpenPorts(), rules);
    }

    @Override
    public void setupBasicFirewall(int sshPort) {
        validatePort(sshPort);
        lock.lock();
        try {
            applyBaselineRules(sshPort, true);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isBaselineActive(int sshPort) {
        validatePort(sshPort);
        try {
            if (!"DROP".equalsIgnoreCase(getInputPolicy())) {
                return false;
            }
            return ruleCheckExists(List.of(IPTABLES, "-C", INPUT_CHAIN, "-i", "lo", "-j", "ACCEPT"))
                    && ruleCheckExists(List.of(
                    IPTABLES, "-C", INPUT_CHAIN, "-m", "conntrack", "--ctstate", "ESTABLISHED,RELATED", "-j", "ACCEPT"))
                    && ruleCheckExists(List.of(
                    IPTABLES, "-C", INPUT_CHAIN, "-p", "tcp", "--dport", String.valueOf(sshPort), "-j", "ACCEPT"))
                    && ruleCheckExists(List.of(IPTABLES, "-C", INPUT_CHAIN, "-p", "tcp", "--dport", "80", "-j", "ACCEPT"))
                    && ruleCheckExists(List.of(IPTABLES, "-C", INPUT_CHAIN, "-p", "tcp", "--dport", "443", "-j", "ACCEPT"));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void enterLockdownMode(int sshPort) {
        validatePort(sshPort);
        lock.lock();
        try {
            applyBaselineRules(sshPort, false);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void resetFirewallToDefault() {
        lock.lock();
        try {
            runOrThrow("Не удалось очистить INPUT", List.of(IPTABLES, "-F", INPUT_CHAIN));
            runOrThrow("Не удалось очистить FORWARD", List.of(IPTABLES, "-F", "FORWARD"));
            runOrThrow("Не удалось очистить OUTPUT", List.of(IPTABLES, "-F", "OUTPUT"));
            runOrThrow("Не удалось установить политику INPUT", List.of(IPTABLES, "-P", INPUT_CHAIN, "ACCEPT"));
            runOrThrow("Не удалось установить политику FORWARD", List.of(IPTABLES, "-P", "FORWARD", "ACCEPT"));
            runOrThrow("Не удалось установить политику OUTPUT", List.of(IPTABLES, "-P", "OUTPUT", "ACCEPT"));
        } finally {
            lock.unlock();
        }
    }

    private void applyBaselineRules(int sshPort, boolean includeWebPorts) {
        runOrThrow("Не удалось очистить INPUT", List.of(IPTABLES, "-F", INPUT_CHAIN));
        runOrThrow("Не удалось очистить FORWARD", List.of(IPTABLES, "-F", "FORWARD"));
        runOrThrow("Не удалось очистить OUTPUT", List.of(IPTABLES, "-F", "OUTPUT"));

        runOrThrow("Не удалось установить политику INPUT", List.of(IPTABLES, "-P", INPUT_CHAIN, "DROP"));
        runOrThrow("Не удалось установить политику FORWARD", List.of(IPTABLES, "-P", "FORWARD", "DROP"));
        runOrThrow("Не удалось установить политику OUTPUT", List.of(IPTABLES, "-P", "OUTPUT", "ACCEPT"));

        runOrThrow("Не удалось разрешить loopback", List.of(
                IPTABLES, "-A", INPUT_CHAIN, "-i", "lo", "-j", "ACCEPT"
        ));
        runOrThrow("Не удалось разрешить established connections", List.of(
                IPTABLES, "-A", INPUT_CHAIN, "-m", "conntrack", "--ctstate", "ESTABLISHED,RELATED", "-j", "ACCEPT"
        ));
        runOrThrow("Не удалось разрешить SSH", List.of(
                IPTABLES, "-A", INPUT_CHAIN, "-p", "tcp", "--dport", String.valueOf(sshPort), "-j", "ACCEPT"
        ));
        if (includeWebPorts) {
            runOrThrow("Не удалось разрешить HTTP", List.of(
                    IPTABLES, "-A", INPUT_CHAIN, "-p", "tcp", "--dport", "80", "-j", "ACCEPT"
            ));
            runOrThrow("Не удалось разрешить HTTPS", List.of(
                    IPTABLES, "-A", INPUT_CHAIN, "-p", "tcp", "--dport", "443", "-j", "ACCEPT"
            ));
        }
    }

    private String getInputPolicy() {
        for (String rule : getRules()) {
            Matcher matcher = INPUT_POLICY_PATTERN.matcher(rule);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return "UNKNOWN";
    }

    private boolean ruleCheckExists(List<String> command) {
        return systemService.executeCommand(command).isSuccess();
    }

    private void validateIp(String ip) {
        if (ip == null || !IP_PATTERN.matcher(ip).matches()) {
            throw new IllegalArgumentException("Некорректный IP");
        }
    }

    private void validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Некорректный порт");
        }
    }

    private String normalizeChain(String chain) {
        if (chain == null) {
            throw new IllegalArgumentException("Цепочка не может быть пустой");
        }
        String normalized = chain.trim().toUpperCase(Locale.ROOT);
        if (!CHAIN_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Некорректное имя цепочки");
        }
        return normalized;
    }

    private String normalizePolicy(String policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Политика не может быть пустой");
        }
        String normalized = policy.trim().toUpperCase(Locale.ROOT);
        if (!POLICY_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Некорректная политика");
        }
        return normalized;
    }

    private List<String> splitLines(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        return output.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private void runOrThrow(String message, List<String> command) {
        CommandResult result = systemService.executeCommand(command);
        if (!result.isSuccess()) {
            throw commandError(message, command, result);
        }
    }

    private boolean openPortRuleExists(int port) {
        List<String> check = List.of(
                IPTABLES,
                "-C", INPUT_CHAIN,
                "-p", "tcp",
                "--dport", String.valueOf(port),
                "-j", "ACCEPT"
        );
        return systemService.executeCommand(check).isSuccess();
    }

    private boolean dropRuleExists(String ip) {
        List<String> check = List.of(
                IPTABLES,
                "-C", INPUT_CHAIN,
                "-s", ip,
                "-j", "DROP"
        );
        return systemService.executeCommand(check).isSuccess();
    }

    private boolean chainExists(String chain) {
        List<String> command = List.of(IPTABLES, "-S", chain);
        return systemService.executeCommand(command).isSuccess();
    }

    private CommandExecutionException commandError(String message, List<String> command, CommandResult result) {
        return new CommandExecutionException(
                message + ": " + String.join(" ", command) + ", output=" + result.getOutput()
        );
    }
}