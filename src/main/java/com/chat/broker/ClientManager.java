package com.chat.broker;

import com.chat.model.ChatMessage;
import com.chat.server.ClientHandler;
import com.chat.server.PerformanceMonitor; // Импорт
import com.google.gson.Gson;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClientManager {
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final PerformanceMonitor monitor; // Ссылка на монитор

    // Обновили конструктор
    public ClientManager(PerformanceMonitor monitor) {
        this.monitor = monitor;
    }

    public void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        monitor.incrementConnections(); // +1 к подключениям
        System.out.println("Client registered: " + username);
    }

    public void removeClient(String username) {
        if (username != null && clients.containsKey(username)) {
            clients.remove(username);
            monitor.decrementConnections(); // -1 к подключениям
            System.out.println("Client disconnected: " + username);
        }
    }

    public void broadcast(ChatMessage message) {
        String jsonMessage = gson.toJson(message);
        for (ClientHandler client : clients.values()) {
            client.sendMessage(jsonMessage);
        }
    }

    public void sendPrivate(String username, ChatMessage message) {
        ClientHandler client = clients.get(username);
        if (client != null) {
            client.sendMessage(gson.toJson(message));
        }
    }

    // НОВЫЙ МЕТОД ДЛЯ КОМАНДЫ /users
    public String getActiveUsersList() {
        if (clients.isEmpty()) return "No active users.";
        return String.join(", ", clients.keySet());
    }
}