package com.diplom.projectipt.service;

import java.util.List;

public interface FirewallService {

    boolean allowIp(String ip);

    boolean allowIpToPort(String ip, int port);

    boolean removeIpFromPort(String ip, int port);

    boolean blockIp(String ip);

    boolean unblockIp(String ip);

    boolean openPort(int port);

    boolean closePort(int port);

    void setChainPolicy(String chain, String policy);

    void closeAll();

    List<String> getRules();

    List<String> getRulesDetailed();

    boolean ruleExists(String ip, int port);

    boolean deleteRuleByLineNumber(String chain, int lineNumber);

    void flushChain(String chain);

    void createChain(String chainName);

    void deleteChain(String chainName);

    String saveRules();

    List<Integer> getOpenPorts();

    FirewallStatus getFirewallStatus();

    void setupBasicFirewall(int sshPort);

    boolean isBaselineActive(int sshPort);

    void enterLockdownMode(int sshPort);

    void resetFirewallToDefault();
}