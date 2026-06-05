package com.diplom.projectipt.service;

import java.util.List;

public record FirewallStatus(
        String inputPolicy,
        int rulesCount,
        List<Integer> openPorts,
        List<String> rules
) {
}
