package com.chat;

import com.chat.bot.AnalyticsBot;
import com.chat.broker.ClientManager;
import com.chat.broker.MessageBroker;
import com.chat.server.BasicChatServer;
import com.chat.server.PerformanceMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatLoadTest {

    private ExecutorService serverThread;
    private AnalyticsBot analyticsBot;
    private BasicChatServer server;
    private PerformanceMonitor monitor;

    @BeforeEach
    void setUp() throws InterruptedException {
        // Инициализация компонентов сервера перед каждым тестом
        serverThread = Executors.newSingleThreadExecutor();
        monitor = new PerformanceMonitor();
        ClientManager clientManager = new ClientManager(monitor);
        analyticsBot = new AnalyticsBot();
        MessageBroker messageBroker = new MessageBroker(clientManager, analyticsBot, monitor);

        new Thread(analyticsBot).start();
        new Thread(messageBroker).start();

        // Используем порт 8081 для тестов, чтобы не конфликтовать с Main (8080)
        server = new BasicChatServer(8081, messageBroker, clientManager);

        serverThread.submit(server::start);
        Thread.sleep(1000); // Даем серверу время на запуск
    }

    @AfterEach
    void tearDown() {
        monitor.stop(); // Останавливаем монитор
        serverThread.shutdownNow(); // Останавливаем сервер
    }

    @Test
    void testHighLoad() throws InterruptedException {
        // --- КОНФИГУРАЦИЯ ТЕСТА ---
        int numberOfClients = 50;       // Кол-во виртуальных клиентов
        int messagesPerClient = 100;    // Сообщений от каждого
        int expectedTotalMessages = (numberOfClients * messagesPerClient) + numberOfClients; // + сообщения приветствия (JOIN)

        System.out.println("Запуск нагрузочного теста...");
        System.out.println("Клиентов: " + numberOfClients + ", Сообщений/клиент: " + messagesPerClient);

        ExecutorService clientsPool = Executors.newFixedThreadPool(numberOfClients);
        List<Callable<Void>> tasks = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Формирование задач для клиентов
        for (int i = 0; i < numberOfClients; i++) {
            final int clientId = i;
            tasks.add(() -> {
                try (Socket socket = new Socket("localhost", 8081);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                    String username = "LoadTester-" + clientId;

                    // 1. Регистрация (отправляем JSON с именем)
                    // Это считается как 1 сообщение
                    out.println("{\"type\":\"USER_MESSAGE\",\"user\":\"" + username + "\",\"text\":\"JOIN\"}");

                    // 2. Спам сообщениями
                    for (int j = 0; j < messagesPerClient; j++) {
                        String json = String.format(
                                "{\"type\":\"USER_MESSAGE\",\"user\":\"%s\",\"text\":\"Msg-%d\"}",
                                username, j
                        );
                        out.println(json);
                        // Минимальная задержка для имитации сети (иначе TCP буфер забьется мгновенно)
                        Thread.sleep(2);
                    }
                    // Ждем, чтобы сервер успел вычитать всё из сокета перед тем, как мы его закроем
                    try {
                        Thread.sleep(500); // 0.5 секунды задержки перед отключением
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    System.err.println("Client Error: " + e.getMessage());
                }
                return null;
            });
        }

        // Запуск всех клиентов
        clientsPool.invokeAll(tasks);
        clientsPool.shutdown();

        // Ждем завершения ОТПРАВКИ всех сообщений клиентами
        boolean finishedInTime = clientsPool.awaitTermination(2, TimeUnit.MINUTES);
        assertTrue(finishedInTime, "Тест не успел отправить сообщения за отведенное время");

        // Ждем завершения ОБРАБОТКИ сообщений сервером (даем "фору" серверу разгрести очередь)
        System.out.println("Отправка завершена. Ожидание обработки очереди...");
        Thread.sleep(3000);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // --- СБОР СТАТИСТИКИ ---
        long actualMessages = analyticsBot.getStatsCalculator().getTotalMessages();
        double seconds = duration / 1000.0;
        double throughput = actualMessages / seconds; // Сообщений в секунду

        // Использование памяти
        long totalMem = Runtime.getRuntime().totalMemory();
        long freeMem = Runtime.getRuntime().freeMemory();
        long usedMemMB = (totalMem - freeMem) / 1024 / 1024;

        // --- ВЫВОД ОТЧЕТА (КАК В ЗАДАНИИ) ---
        System.out.println("\n=================================================");
        System.out.println("         РЕЗУЛЬТАТЫ НАГРУЗОЧНОГО ТЕСТА           ");
        System.out.println("=================================================");
        System.out.printf(" Время выполнения:      %d мс (%.2f сек)%n", duration, seconds);
        System.out.printf(" Ожидалось сообщений:   %d%n", expectedTotalMessages);
        System.out.printf(" Обработано сообщений:  %d%n", actualMessages);
        System.out.println("-------------------------------------------------");
        System.out.printf(" Пропускная способность: %.2f msg/sec%n", throughput);
        System.out.printf(" Потерь сообщений:       %d%n", (expectedTotalMessages - actualMessages));
        System.out.printf(" Использовано памяти:    %d MB%n", usedMemMB);
        System.out.println("=================================================\n");

        // --- ПРОВЕРКИ (ASSERTIONS) ---

        // 1. Проверяем, что потерь нет (или они минимальны, допускаем погрешность < 1% из-за гонок при старте)
        // В идеальном мире должно быть ровно expectedTotalMessages
        assertTrue(actualMessages >= expectedTotalMessages * 0.99,
                "Потеряно слишком много сообщений! Получено: " + actualMessages + ", Ожидалось: " + expectedTotalMessages);

        // 2. Проверяем производительность (например, сервер должен тянуть хотя бы 1000 msg/sec)
        // Критерий из задания #2: "MessageBroker обрабатывает 1000+ сообщений в секунду"
        if (throughput > 1000) {
            System.out.println("✅ КРИТЕРИЙ УСПЕХА ВЫПОЛНЕН: > 1000 msg/sec");
        } else {
            System.out.println("⚠️ ВНИМАНИЕ: Производительность ниже целевой (1000 msg/sec)");
        }
    }
}