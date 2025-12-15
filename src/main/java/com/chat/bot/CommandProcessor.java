package com.chat.bot;

import com.chat.broker.ClientManager; // Импорт
import com.chat.model.ChatMessage;
import com.chat.model.MessageType;

public class CommandProcessor {
    private final StatsCalculator stats;
    private final ClientManager clientManager; // Ссылка на менеджер клиентов

    public CommandProcessor(StatsCalculator stats, ClientManager clientManager) {
        this.stats = stats;
        this.clientManager = clientManager;
    }

    public ChatMessage processCommand(ChatMessage message) {
        String cmd = message.getText().trim().toLowerCase();
        String responseText;
        MessageType responseType = MessageType.SYSTEM_MESSAGE;

        switch (cmd) {
            case "/stats":
                responseText = "Total messages: " + stats.getTotalMessages();
                responseType = MessageType.STATISTICS;
                break;
            case "/top":
                responseText = "Top user: " + stats.getTopActiveUser();
                responseType = MessageType.STATISTICS;
                break;
            case "/users": // <--- НОВАЯ КОМАНДА
                responseText = "Active users: " + clientManager.getActiveUsersList();
                responseType = MessageType.STATISTICS;
                break;
            case "/help":
                responseText = "Available commands: /stats, /top, /users, /help";
                break;
            default:
                responseText = "Unknown command. Try /help";
        }

        return new ChatMessage(responseType, "Bot", responseText);
    }
}