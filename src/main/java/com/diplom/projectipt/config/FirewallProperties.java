package com.diplom.projectipt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "firewall")
public class FirewallProperties {

    private int sshPort = 22;
    private String blockedPorts = "22,3306";

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public String getBlockedPorts() {
        return blockedPorts;
    }

    public void setBlockedPorts(String blockedPorts) {
        this.blockedPorts = blockedPorts;
    }

    public Set<Integer> blockedPortSet() {
        return Arrays.stream(blockedPorts.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }
}
