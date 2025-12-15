package com.chat.server;

import com.chat.broker.ClientManager;
import com.chat.broker.MessageBroker;
import com.chat.model.ChatMessage;
import com.chat.model.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final MessageBroker messageBroker;
    private final ClientManager clientManager;
    private PrintWriter out;
    private String username;
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket, MessageBroker messageBroker, ClientManager clientManager) {
        this.socket = socket;
        this.messageBroker = messageBroker;
        this.clientManager = clientManager;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true)
        ) {
            this.out = output;

            // 1. Регистрация (простой вариант: первое сообщение - имя)
            // В идеале можно реализовать handshake, но сделаем через команду или авто-генерацию
            this.username = "User-" + socket.getPort(); // Временное имя
            clientManager.addClient(username, this);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                try {
                    // Парсим JSON от клиента
                    ChatMessage message = gson.fromJson(inputLine, ChatMessage.class);

                    // Обновляем имя, если оно пришло в сообщении
                    if (message.getUser() != null && !message.getUser().isEmpty()) {
                        if (!message.getUser().equals(this.username)) {
                            // Перерегистрация имени
                            clientManager.removeClient(this.username);
                            this.username = message.getUser();
                            clientManager.addClient(this.username, this);
                        }
                    } else {
                        // Если в JSON нет имени, ставим текущее
                        // (немного костыльно, но для примера сойдет)
                    }

                    // Определяем тип сообщения, если это команда начинающаяся с /
                    if (message.getText().startsWith("/")) {
                        message.setType(MessageType.COMMAND);
                    } else {
                        message.setType(MessageType.USER_MESSAGE);
                    }

                    System.out.println("------------------------------------------------");
                    System.out.println("LOG: Получено сообщение от " + username);
                    System.out.println("LOG: Текст: " + message.getText());
                    System.out.println("LOG: Присвоен тип: " + message.getType()); // Здесь увидим COMMAND
                    System.out.println("------------------------------------------------");

                    // Отправляем в брокер
                    messageBroker.putMessage(message);

                } catch (JsonSyntaxException e) {
                    System.err.println("Invalid JSON received: " + inputLine);
                }
            }

        } catch (IOException e) {
            System.err.println("Client handler exception: " + e.getMessage());
        } finally {
            clientManager.removeClient(username);
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    // Метод для отправки сообщения этому клиенту
    public void sendMessage(String jsonMessage) {
        if (out != null) {
            out.println(jsonMessage);
        }
    }
}