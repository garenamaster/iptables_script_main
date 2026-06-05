package com.diplom.projectipt.tempaccess;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TempAccessScheduler {

    private final TempAccessService tempAccessService;

    public TempAccessScheduler(TempAccessService tempAccessService) {
        this.tempAccessService = tempAccessService;
    }

    @Scheduled(fixedRate = 60_000)
    public void expireRules() {
        tempAccessService.expireDue();
    }
}
