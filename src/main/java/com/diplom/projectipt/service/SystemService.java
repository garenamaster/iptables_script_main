package com.diplom.projectipt.service;

import java.util.List;

public interface SystemService {

    CommandResult executeCommand(List<String> command);

    default CommandResult executeCommand(List<String> command, boolean check) {
        CommandResult result = executeCommand(command);
        if (check && !result.isSuccess()) {
            throw new com.diplom.projectipt.exception.CommandExecutionException(
                    "Command failed: " + String.join(" ", command) + ", output=" + result.getOutput()
            );
        }
        return result;
    }
}