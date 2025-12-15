package com.chat.bot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// Класс для Thread-safe подсчета метрик
public class StatsCalculator {
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final Map<String, Long> userActivity = new ConcurrentHashMap<>();

    public void incrementTotal() {
        totalMessages.incrementAndGet();
    }

    public void incrementUser(String username) {
        userActivity.merge(username, 1L, Long::sum);
    }

    public long getTotalMessages() {
        return totalMessages.get();
    }

    public String getTopActiveUser() {
        return userActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey() + " (" + entry.getValue() + " msgs)")
                .orElse("No data");
    }
}