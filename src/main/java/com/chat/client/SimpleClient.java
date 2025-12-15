package com.chat.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class SimpleClient {
    public static void main(String[] args) {
        try {
            // Подключаемся к локальному серверу
            Socket socket = new Socket("localhost", 8080);
            System.out.println("Подключено к серверу! Введите свое имя:");

            Scanner scanner = new Scanner(System.in);
            String username = scanner.nextLine();

            // Поток для чтения ответов от сервера
            new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        System.out.println("\n[SERVER]: " + serverResponse);
                        System.out.print("> "); // Просто для красоты ввода
                    }
                } catch (Exception e) {
                    System.out.println("Соединение разорвано.");
                }
            }).start();

            // Поток для отправки сообщений
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Можете писать сообщения. Для статистики введите /stats");
            System.out.print("> ");

            while (true) {
                String text = scanner.nextLine();

                // Формируем JSON вручную, чтобы не тащить Gson в этот класс
                // Формат: {"type":"USER_MESSAGE", "user":"ИМЯ", "text":"ТЕКСТ"}
                String jsonMessage = String.format(
                        "{\"type\":\"USER_MESSAGE\", \"user\":\"%s\", \"text\":\"%s\"}",
                        username,
                        text
                );

                out.println(jsonMessage);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}