package com.chat.model;

import java.time.Instant;


public class ChatMessage {
    private MessageType type;
    private String user;
    private String text;
    private String timestamp; // Используем String для простоты JSON сериализации

    public ChatMessage(MessageType type, String user, String text) {
        this.type = type;
        this.user = user;
        this.text = text;
        this.timestamp = Instant.now().toString();
    }

    // Getters and Setters
    public MessageType getType() { return type; }
    public String getUser() { return user; }
    public String getText() { return text; }
    public String getTimestamp() { return timestamp; }
    // правки в chatMessage
    public void setType(MessageType type) { this.type = type; }
    public void setText(String text) { this.text = text; }
}