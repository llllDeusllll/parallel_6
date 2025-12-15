package com.chat.broker;

import com.chat.model.ChatMessage;
import com.chat.server.ClientHandler;
import com.chat.server.PerformanceMonitor;
import com.google.gson.Gson;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager {
    // ConcurrentHashMap обеспечивает потокобезопасность хранения клиентов
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final PerformanceMonitor monitor;

    public ClientManager(PerformanceMonitor monitor) {
        this.monitor = monitor;
    }

    public void addClient(String username, ClientHandler handler) {
        // Метод put возвращает предыдущее значение, если ключ уже был
        ClientHandler existing = clients.put(username, handler);

        // Увеличиваем счетчик только если это НОВЫЙ пользователь,
        // а не переподключение старого
        if (existing == null) {
            monitor.incrementConnections();
            System.out.println("Client registered: " + username);
        } else {
            System.out.println("Client re-connected: " + username);
        }
    }

    public void removeClient(String username) {
        if (username == null) return;

        // Оптимизация: remove возвращает удаленный объект.
        // Если он не null, значит удаление реально произошло.
        // Это избавляет от лишнего вызова containsKey() и делает операцию атомарной.
        ClientHandler removed = clients.remove(username);

        if (removed != null) {
            monitor.decrementConnections();
            System.out.println("Client disconnected: " + username);
        }
    }

    public void broadcast(ChatMessage message) {
        // Сериализуем сообщение один раз для всех (экономия ресурсов)
        String jsonMessage = gson.toJson(message);

        // Отправляем всем активным клиентам
        clients.values().forEach(client -> client.sendMessage(jsonMessage));
    }

    public void sendPrivate(String username, ChatMessage message) {
        ClientHandler client = clients.get(username);
        if (client != null) {
            client.sendMessage(gson.toJson(message));
        }
    }

    public String getActiveUsersList() {
        if (clients.isEmpty()) return "No active users.";
        // String.join — удобный способ склеить список через запятую
        return String.join(", ", clients.keySet());
    }
}