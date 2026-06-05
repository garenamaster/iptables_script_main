package com.diplom.projectipt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dashboard.log")
public class DashboardLogProperties {

    private String authPath = "/var/log/auth.log";
    private String journalUnit = "projectipt-api";
    private int tailLines = 80;

    public String getAuthPath() {
        return authPath;
    }

    public void setAuthPath(String authPath) {
        this.authPath = authPath;
    }

    public String getJournalUnit() {
        return journalUnit;
    }

    public void setJournalUnit(String journalUnit) {
        this.journalUnit = journalUnit;
    }

    public int getTailLines() {
        return tailLines;
    }

    public void setTailLines(int tailLines) {
        this.tailLines = tailLines;
    }
}
