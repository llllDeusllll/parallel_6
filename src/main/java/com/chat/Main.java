package com.chat;

import com.chat.bot.AnalyticsBot;
import com.chat.broker.ClientManager;
import com.chat.broker.MessageBroker;
import com.chat.server.BasicChatServer;
import com.chat.server.PerformanceMonitor;

public class Main {
    public static void main(String[] args) {
        // 1. Создаем монитор
        PerformanceMonitor monitor = new PerformanceMonitor();

        // 2. Создаем менеджеры с монитором
        ClientManager clientManager = new ClientManager(monitor);
        AnalyticsBot analyticsBot = new AnalyticsBot();

        // 3. Брокеру тоже нужен монитор
        MessageBroker messageBroker = new MessageBroker(clientManager, analyticsBot, monitor);

        new Thread(analyticsBot, "AnalyticsBot-Thread").start();
        new Thread(messageBroker, "MessageBroker-Thread").start();

        BasicChatServer server = new BasicChatServer(8080, messageBroker, clientManager);
        server.start();
    }
}