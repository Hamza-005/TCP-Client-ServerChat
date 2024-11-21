/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.mavenproject3;

/**
 *
 * @author Hamza
 */

import java.net.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.JTextArea;


public class TCPServer {
    private ServerSocket serverSocket;
    private Map<String, String> users;  // Registered users
    private Map<String, ClientHandler> activeUsers = new HashMap<>();  // Connected users
    private JTextArea logArea;  // For logging messages to the GUI
    private GUIServerTCP guiServer;  // Reference to the GUI server for updating the user list
    
    private String getCurrentTimestamp() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy--hh:mm:ss a");
    return LocalDateTime.now().format(formatter);
}

    public TCPServer(int port, Map<String, String> users, JTextArea logArea, GUIServerTCP guiServer) {
        this.users = users;  // Registered users
        this.logArea = logArea;  // Reference to GUI log area
        this.guiServer = guiServer;  // Reference to GUIServerTCP

        try {
            serverSocket = new ServerSocket(port);
            log("Server started on port: " + port);
            new ClientAcceptThread().start();  // Accept incoming clients
        } catch (IOException e) {
            log("Error starting server: " + e.getMessage());
        }
    }

    // Thread to accept incoming clients
    private class ClientAcceptThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new ClientHandler(clientSocket).start();  // Handle new client
                } catch (IOException e) {
                    log("Error accepting client: " + e.getMessage());
                }
            }
        }
    }
    
    // Broadcast a message to all users
    private void broadcast(String message) {
        for (ClientHandler handler : activeUsers.values()) {
            handler.out.println(message); // Send the message to each client
            log("Message sent to client: " + message); // Debug log
        }
    }

    // Broadcast the updated user list to all clients
    private void broadcastUserList() {
    StringBuilder userList = new StringBuilder("/users ");
    for (String user : activeUsers.keySet()) {
        ClientHandler handler = activeUsers.get(user);
        if (handler != null) {
            String ip = handler.socket.getInetAddress().getHostAddress();
            int port = handler.socket.getPort();
            userList.append(user).append(":").append(ip).append(":").append(port).append(" ");
        }
    }
    String userListString = userList.toString().trim();
    log("Broadcasting user list: " + userListString); // Debug log
    broadcast(userListString);
}

    // ClientHandler handles each connected user
    private class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Authenticate the user
                if (!authenticateUser()) {
                    socket.close();  // Close if authentication fails
                    return;
                }

                synchronized (activeUsers) {
                    activeUsers.put(username, this);  // Register user as active
                    guiServer.userLoggedIn(username);  // Update GUI with logged-in user
                    broadcastUserList();  // Notify all users
                }

                // Handle incoming messages
                String message;
                while ((message = in.readLine()) != null) {
            if (message.startsWith("LOGOUT:")) {
                String userToLogout = message.substring("LOGOUT:".length());
                if (userToLogout.equals(username)) {
                    disconnectUser();  // Log out the user
                    break;
                }
            } else if (message.startsWith("/send")) {
                sendMessage(message);  // Handle multi-user messaging
            } else if (message.startsWith("DELETE: ")) {
                broadcast(message); // Broadcast DELETE message to all clients
            } else if (message.equals("DELETE_ALL")) {
                broadcast(message); // Broadcast DELETE_ALL command to all clients
            } else if (message.startsWith("RESTORE: ")) {
                broadcast(message); // Broadcast RESTORE command to all clients
            }
        }
            } catch (IOException e) {
                log("Connection error with user: " + username);
            } finally {
                disconnectUser();  // Handle disconnection
            }
        }

        // Authenticate user by matching username and password
        private boolean authenticateUser() throws IOException {
            String user = in.readLine();  // Username from client
            String pass = in.readLine();  // Password from client

            log("Received login attempt - Username: " + user + ", Password: " + pass); // Debug log

            // Validate credentials
            if (users.containsKey(user) && users.get(user).equals(pass)) {
                username = user;
                out.println("LOGIN_SUCCESS");
                log(username + " logged in.");
                
                return true;
            } else {
                out.println("LOGIN_FAILED");
                log("Failed login attempt for Username: " + user + ", with Password: " + pass); // Debug log
                return false;
            }
        }

        // Handle disconnection
        private void disconnectUser() {
            try {
                socket.close();
                synchronized (activeUsers) {
                    if (username != null) {
                        activeUsers.remove(username);  // Remove from active users
                        guiServer.userLoggedOut(username); // Notify the GUI about the logout
                        broadcastUserList();  // Update user list after disconnect
                        log(username + " disconnected.");
                    }
                }
            } catch (IOException e) {
                log("Error disconnecting user: " + username);
            }
        }

        // Handle sending a message to multiple users
        
        private void sendMessage(String message) {
            String[] parts = message.split(" ", 3);  // Format: /send user1,user2 Message
            if (parts.length < 3) {
              out.println("Invalid message format. Use: /send user1,user2 Message");
              return;
            }

            String[] recipients = parts[1].split(",");
            String msg = parts[2];

            // Get the sender's IP and port
            String senderIP = socket.getInetAddress().getHostAddress();
            int senderPort = socket.getPort();

            // Modify the message format to include sender IP and port
            String formattedMessage = username + ": " + msg + " [" + senderPort + "--" + senderIP + "--" + getCurrentTimestamp() + "]";

            for (String recipient : recipients) {
                ClientHandler handler = activeUsers.get(recipient);
                if (handler != null) {
                 handler.out.println(formattedMessage);
                } else {
                    out.println("User " + recipient + " is not online.");
                }
            }
        }



        
    }

    // Log messages to the GUI
    private void log(String message) {
        logArea.append(message + "\n");
    }
}








