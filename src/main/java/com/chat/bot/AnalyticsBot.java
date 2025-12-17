package com.chat.bot;

import com.chat.model.ChatMessage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
// исправленный analytics
public class AnalyticsBot implements Runnable {
    private final BlockingQueue<ChatMessage> analyticsQueue = new LinkedBlockingQueue<>();
    private final StatsCalculator statsCalculator = new StatsCalculator();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public AnalyticsBot() {
        // Запуск периодического отчета (каждую минуту) - по заданию
        scheduler.scheduleAtFixedRate(this::printReport, 1, 1, TimeUnit.MINUTES);
    }

    public void processMessage(ChatMessage message) {
        analyticsQueue.offer(message);
    }

    public StatsCalculator getStatsCalculator() {
        return statsCalculator;
    }

    private void printReport() {
        System.out.println("--- ANALYTICS REPORT ---");
        System.out.println("Total: " + statsCalculator.getTotalMessages());
        System.out.println("Top User: " + statsCalculator.getTopActiveUser());
        System.out.println("------------------------");
    }
    // Дивный комментарий
    @Override
    public void run() {
        System.out.println("Analytics Bot started...");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ChatMessage msg = analyticsQueue.take();
                // Обновляем статистику
                statsCalculator.incrementTotal();
                if (msg.getUser() != null) {
                    statsCalculator.incrementUser(msg.getUser());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}