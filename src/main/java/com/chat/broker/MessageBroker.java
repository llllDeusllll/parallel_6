package com.chat.broker;

import com.chat.bot.AnalyticsBot;
import com.chat.bot.CommandProcessor;
import com.chat.model.ChatMessage;
import com.chat.model.MessageType;
import com.chat.server.PerformanceMonitor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageBroker implements Runnable {
    private final BlockingQueue<ChatMessage> incomingQueue = new LinkedBlockingQueue<>();
    private final ClientManager clientManager;
    private final AnalyticsBot analyticsBot;
    private final CommandProcessor commandProcessor;
    private final PerformanceMonitor monitor;

    public MessageBroker(ClientManager clientManager, AnalyticsBot analyticsBot, PerformanceMonitor monitor) {
        this.clientManager = clientManager;
        this.analyticsBot = analyticsBot;
        this.monitor = monitor;
        // Инициализация процессора команд с передачей калькулятора статистики
        this.commandProcessor = new CommandProcessor(analyticsBot.getStatsCalculator(), clientManager);
    }

    // Метод добавления сообщения в очередь (вызывается из ClientHandler)
    public void putMessage(ChatMessage message) {
        try {
            incomingQueue.put(message);
            monitor.incrementQueue(); // Фиксируем рост очереди
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        System.out.println("Message Broker started...");

        // Бесконечный цикл обработки сообщений
        while (!Thread.currentThread().isInterrupted()) {
            try {
                processNextMessage();
            } catch (InterruptedException e) {
                // Если поток прервали во время ожидания (take), корректно завершаем работу
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Вынесли логику обработки одного сообщения в отдельный метод
    private void processNextMessage() throws InterruptedException {
        // 1. Извлечение сообщения (здесь поток блокируется, если очередь пуста)
        ChatMessage message = incomingQueue.take();

        // 2. Обновление технических метрик
        updateMetrics();

        // 3. Отправка в аналитику (кроме системных сообщений)
        if (message.getType() != MessageType.SYSTEM_MESSAGE) {
            analyticsBot.processMessage(message);
        }

        // 4. Маршрутизация сообщения (Команда или Рассылка)
        routeMessage(message);
    }

    private void updateMetrics() {
        monitor.decrementQueue();   // Сообщение ушло из очереди
        monitor.incrementMessages(); // Сообщение обработано
    }

    // Логика выбора: это команда или обычное сообщение
    private void routeMessage(ChatMessage message) {
        if (message.getType() == MessageType.COMMAND) {
            // Если это команда — обрабатываем и шлем ответ лично автору
            ChatMessage response = commandProcessor.processCommand(message);
            clientManager.sendPrivate(message.getUser(), response);
        } else {
            // Иначе — рассылаем всем подключенным клиентам
            clientManager.broadcast(message);
        }
    }
}