/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mavenproject3;

/**
 *
 * @author Hamza
 */


import java.io.*;
import java.net.*;

public class TCPClient {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean running;

    public interface MessageListener {
        void onMessageReceived(String message);
    }

    private MessageListener listener;

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    public void start(String remoteIp, int remotePort) throws IOException {
        socket = new Socket(remoteIp, remotePort);  // Connect to server
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        running = true;

        // Start a thread to listen for messages from the server
        new Thread(() -> {
    while (running) {
        try {
            String message = reader.readLine();  // Read message from server
            if (message != null && listener != null) {
                System.out.println("Debug: Received message from server - " + message); // Debug log
                listener.onMessageReceived(message);  // Notify listener
            }
        } catch (IOException e) {
            e.printStackTrace();  // Handle error
            close();
        }
    }
}).start();
    }

    public void sendMessage(String message) throws IOException {
        if (writer != null) {
            writer.println(message);  // Send message to server
        } else {
            throw new IOException("Connection is not established.");
        }
    }

    public void close() {
        try {
            running = false;
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
        return running;
    }
}


