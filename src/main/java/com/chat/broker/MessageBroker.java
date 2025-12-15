package com.chat.broker;

import com.chat.bot.AnalyticsBot;
import com.chat.bot.CommandProcessor;
import com.chat.model.ChatMessage;
import com.chat.model.MessageType;
import com.chat.server.PerformanceMonitor; // Импорт

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageBroker implements Runnable {
    private final BlockingQueue<ChatMessage> incomingQueue = new LinkedBlockingQueue<>();
    private final ClientManager clientManager;
    private final AnalyticsBot analyticsBot;
    private final CommandProcessor commandProcessor;
    private final PerformanceMonitor monitor; // Ссылка на монитор

    public MessageBroker(ClientManager clientManager, AnalyticsBot analyticsBot, PerformanceMonitor monitor) {
        this.clientManager = clientManager;
        this.analyticsBot = analyticsBot;
        this.monitor = monitor;
        // Передаем clientManager в CommandProcessor
        this.commandProcessor = new CommandProcessor(analyticsBot.getStatsCalculator(), clientManager);
    }

    public void putMessage(ChatMessage message) {
        try {
            incomingQueue.put(message);
            monitor.incrementQueue(); // +1 в очереди
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        System.out.println("Message Broker started...");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ChatMessage message = incomingQueue.take();
                monitor.decrementQueue(); // -1 из очереди (взяли в обработку)
                monitor.incrementMessages(); // Увеличиваем счетчик обработанных

                if (message.getType() != MessageType.SYSTEM_MESSAGE) {
                    analyticsBot.processMessage(message);
                }

                if (message.getType() == MessageType.COMMAND) {
                    ChatMessage response = commandProcessor.processCommand(message);
                    clientManager.sendPrivate(message.getUser(), response);
                } else {
                    clientManager.broadcast(message);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}