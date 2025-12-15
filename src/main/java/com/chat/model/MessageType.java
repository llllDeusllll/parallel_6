package com.chat.model;

public enum MessageType {
    USER_MESSAGE,   // Обычное сообщение
    SYSTEM_MESSAGE, // Сообщение от сервера/системы
    COMMAND,        // Команда (/stats, /help)
    STATISTICS      // Ответ от бота
}