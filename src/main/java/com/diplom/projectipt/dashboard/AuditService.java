package com.diplom.projectipt.dashboard;

import com.diplom.projectipt.dashboard.dto.AuditEntryDto;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Service
public class AuditService {

    private static final int MAX_ENTRIES = 300;
    private final Deque<AuditEntryDto> entries = new ArrayDeque<>();

    public synchronized void record(String action, String details, boolean success) {
        entries.addFirst(new AuditEntryDto(System.currentTimeMillis(), action, details, success));
        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }

    public synchronized List<AuditEntryDto> recent(int limit) {
        int size = Math.min(limit, entries.size());
        List<AuditEntryDto> result = new ArrayList<>(size);
        int i = 0;
        for (AuditEntryDto entry : entries) {
            result.add(entry);
            i++;
            if (i >= size) {
                break;
            }
        }
        return result;
    }
}
