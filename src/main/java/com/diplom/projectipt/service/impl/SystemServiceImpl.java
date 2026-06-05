package com.diplom.projectipt.service.impl;

import com.diplom.projectipt.exception.CommandExecutionException;
import com.diplom.projectipt.service.CommandResult;
import com.diplom.projectipt.service.SystemService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class SystemServiceImpl implements SystemService {

    @Override
    public CommandResult executeCommand(List<String> command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();

        try {
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            return new CommandResult(exitCode, output.toString());

        } catch (Exception e) {
            throw new CommandExecutionException("Ошибка выполнения команды: " + String.join(" ", command), e);
        }
    }
}