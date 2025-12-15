package com.chat.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class PerformanceMonitor {
    // Метрики
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong messagesProcessed = new AtomicLong(0);
    private final LongAdder queueSize = new LongAdder(); // Текущий размер (или накопленный, используем как текущий для задания)

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public PerformanceMonitor() {
        // Запуск периодического вывода статистики (каждые 5 секунд)
        scheduler.scheduleAtFixedRate(this::printStats, 5, 5, TimeUnit.SECONDS);
    }

    public void incrementConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementConnections() {
        activeConnections.decrementAndGet();
    }

    public void incrementMessages() {
        messagesProcessed.incrementAndGet();
    }

    // Методы для обновления размера очереди (вызываются из Broker)
    public void incrementQueue() {
        queueSize.increment();
    }
    public void decrementQueue() {
        queueSize.decrement();
    }

    private void printStats() {
        long currentQueue = queueSize.sum();
        long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;

        System.out.println("\n[MONITOR] Status Report:");
        System.out.println(" - Active Connections: " + activeConnections.get());
        System.out.println(" - Messages Processed: " + messagesProcessed.get());
        System.out.println(" - Queue Size: " + currentQueue);
        System.out.println(" - Memory (Free/Total/Max): " + freeMemory + "MB / " + totalMemory + "MB / " + maxMemory + "MB");

        // Детектор проблем
        if (currentQueue > 50) {
            System.err.println("!!! WARNING: High queue size! System might be overloaded.");
        }
        if (freeMemory < 10) { // Меньше 10 МБ свободно
            System.err.println("!!! WARNING: Low memory!");
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}