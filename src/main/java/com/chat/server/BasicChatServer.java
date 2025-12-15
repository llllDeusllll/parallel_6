package com.chat.server;

import com.chat.broker.ClientManager;
import com.chat.broker.MessageBroker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BasicChatServer {
    private final int port;
    // Фиксированный пул потоков (как в задании 1.1)
    private final ExecutorService threadPool = Executors.newFixedThreadPool(100);
    private final MessageBroker messageBroker;
    private final ClientManager clientManager;

    public BasicChatServer(int port, MessageBroker messageBroker, ClientManager clientManager) {
        this.port = port;
        this.messageBroker = messageBroker;
        this.clientManager = clientManager;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chat Server started on port " + port);

            while (true) {
                // Блокирующий accept()
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection: " + clientSocket.getInetAddress());

                // Создаем обработчик и отдаем его в пул потоков
                ClientHandler handler = new ClientHandler(clientSocket, messageBroker, clientManager);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}