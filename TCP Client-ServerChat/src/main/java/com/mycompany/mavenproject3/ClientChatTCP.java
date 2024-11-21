package com.mycompany.mavenproject3;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.JOptionPane;
import javax.swing.text.*;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author Hamza
 */
public class ClientChatTCP extends javax.swing.JFrame {

    private List<String> chatHistory = new ArrayList<>();
    private final TCPClient Client = new TCPClient();
    private List<String> deletedMessages = new ArrayList<>();
    private List<String> archivedMessages = new ArrayList<>();
    private static final String LOG_FILE = "chatlog.txt"; // Log file path
    private String currentUsername;
    private Map<String, String> userIPMap = new HashMap<>(); // Store username to IP address mapping
    private Map<String, Integer> userPortMap = new HashMap<>(); // Store username to port mapping
    private DefaultListModel<String> chatListModel;

    
    
    // Add this inside your ClientChatTCP class
    private class ChatListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        // Get the default renderer component
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        String message = (String) value;
        String username = currentUsername + ": ";

        // Determine the color based on the sender
        Color color = message.startsWith(username) ? Color.BLUE : Color.RED;
        label.setForeground(color); // Set the text color

        return label;
    }
}


    
    private String getCurrentTimestamp() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy--hh:mm:ss a");
    return LocalDateTime.now().format(formatter);
}

    private void logEvent(String event) {
        String logEntry = getCurrentTimestamp() + " - " + event;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


public void onMessageReceived(String message) {
    logEvent("Received message: " + message); // Debug log for every message received

    // Handle user list updates
    if (message.startsWith("/users ")) {
        String[] userList = message.substring("/users ".length()).split(" ");
        updateActiveUsersList(userList); // Update JList with active users
        logEvent("Updated user list: " + Arrays.toString(userList)); // Log the received user list

    } else if (message.startsWith("LOGIN_SUCCESS")) {
        currentUsername = Username.getText().trim(); // Store the logged-in username
        logEvent("User logged in: " + currentUsername);
        JOptionPane.showMessageDialog(this, "Welcome " + currentUsername + "!", "Login Successful", JOptionPane.INFORMATION_MESSAGE);

    } else if (message.startsWith("LOGIN_FAILED")) {
        JOptionPane.showMessageDialog(this, "Login failed. Please check your credentials.", "Login Failed", JOptionPane.ERROR_MESSAGE);

    } else if (message.startsWith("DELETE: ")) {
        // Handle the deletion of a specific message
        String messageToDelete = message.substring("DELETE: ".length()).trim();
        reqdel(messageToDelete); // Use reqdel to delete the message

    } else if (message.equals("DELETE_ALL")) {
        // Handle deletion of all messages
        reqdelall(); // Use reqdelall to delete all messages

    } else if (message.startsWith("RESTORE: ")) {
        // Handle restoration of a message
        String messageToRestore = message.substring("RESTORE: ".length()).trim();
        reqrest(messageToRestore); // Use reqrest to restore the message

    } else {
        // Display any other message received
        addmsg(message); // Show received messages
    }
}









private void updateActiveUsersList(String[] users) {
    DefaultListModel<String> model = new DefaultListModel<>();
    userIPMap.clear();  // Clear old data
    userPortMap.clear(); // Clear old data

    for (String userDetails : users) {
        String[] parts = userDetails.split(":");
        if (parts.length == 3) {
            String username = parts[0];
            String ip = parts[1];
            int port = Integer.parseInt(parts[2]);

            // Skip adding the current user's username
            if (!username.equals(currentUsername)) {
                model.addElement(username); // Add username to the list
                userIPMap.put(username, ip); // Store IP
                userPortMap.put(username, port); // Store port
            }
        }
    }
    ActiveUsers.setModel(model); // Update the JList with the new list of users
}







//TEMP FUNCS TO HANDLE INCOMING REQUESTS
private void reqdelall(){
    archivedMessages.addAll(chatHistory); // Add all messages to archived messages
    chatHistory.clear(); // Clear chat history
    refreshChatDisplay(); // Refresh UI to show the updated chat history
    logEvent("All messages deleted from chat and archived.");
    startAutoRemoveFromArchive("AllMessages"); // Use a unique identifier
    logEvent("All messages permenantly deleted and removed from archived.");
    
}

private void reqdel(String selectedMessage) {
    String temp = extractUsernameAndMessage(selectedMessage);
    String timestampToMatch = extractTimeStamp(selectedMessage);
    String toarch = null;

    if (selectedMessage != null) {
        // Move the selected message to the archive
        for (String message : chatHistory) {
            // Log the comparison for debugging
            logEvent("Comparing message: " + message);
            logEvent("Expected message: " + temp + ", Expected timestamp: " + timestampToMatch);

            // Match only based on username + message + timestamp, ignoring other metadata
            if (extractUsernameAndMessage(message).equals(temp) && extractTimeStamp(message).equals(timestampToMatch)) {
                toarch = message;
                break; // Exit the loop after finding the first match
            }
        }

        if (toarch != null) {
            archivedMessages.add(toarch); // Add to archived messages
            // Remove using only username + message + timestamp
            chatHistory.removeIf(message -> extractUsernameAndMessage(message).equals(temp) && extractTimeStamp(message).equals(timestampToMatch));
            refreshChatDisplay(); // Refresh UI to show the updated chat history
            logEvent("Message deleted: " + toarch);
            startAutoRemoveFromArchive(toarch);
            logEvent("Message permanently deleted and removed from archive: " + toarch);
        } else {
            logEvent("No matching message found for deletion.");
        }
    }
}



private void reqrest(String msg1){
        // Restore the message
        String temp =extractUsernameAndMessage(msg1);
        String tochat=null;
        
        for (String message : archivedMessages) {
            if (message.startsWith(temp) && message.contains(extractTimeStamp(msg1))) {
                tochat = message;
                break; // Exit the loop after finding the first match
            }
        }
        
        if(tochat!=null){
        chatHistory.add(tochat); // Add to chat history
        //archivedMessages.removeIf(message -> message.startsWith(temp)&&message.contains(extractTimeStamp(msg1)));
        archivedMessages.remove(tochat);
        refreshChatDisplay(); // Refresh UI to show the updated chat history
        logEvent("Message restored: " + msg1);
        }else 
        logEvent("No msg found to restore");
}

private String extractUsernameAndMessage(String formattedMessage) {
    // Find the index of the first occurrence of ": " which separates the username and the message
    int separatorIndex = formattedMessage.indexOf(": ");
    
    if (separatorIndex != -1) {
        // Extract the username part (from the start to the ": ")
        String username = formattedMessage.substring(0, separatorIndex).trim();
        
        // Find the index of the start of metadata (i.e., "[")
        int metaStartIndex = formattedMessage.indexOf("[");
        
        // Extract the message part (from after ": " to before the "[")
        String message;
        if (metaStartIndex != -1) {
            message = formattedMessage.substring(separatorIndex + 2, metaStartIndex).trim();
        } else {
            // If there's no metadata, take the rest of the message after ": "
            message = formattedMessage.substring(separatorIndex + 2).trim();
        }
        
        // Return the result in the desired format
        return username + ": " + message;
    } else {
        // If no separator is found, return null or handle error
        return null;
    }
}

private String extractTimeStamp(String formattedMessage) {
     // Regular expression to match the timestamp pattern
        String regex = "\\[\\d+--\\d+\\.\\d+\\.\\d+\\.\\d+--(\\d{2}/\\d{2}/\\d{4}--\\d{2}:\\d{2}:\\d{2} (a\\.m\\.|p\\.m\\.))\\]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(formattedMessage);

        if (matcher.find()) {
            // Return the captured group for the timestamp
            return matcher.group(1);
        } else {
            return null; // No match found
        }
}








//END OF TEMP FUNCS TO HANDLE INCOMING REQUESTS





    
    private void populateAvailableInterfaces() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            AvailableInterfaces.removeAllItems();  // Clear previous items

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    AvailableInterfaces.addItem(networkInterface.getDisplayName());  // Add interface
                }
            }
        } catch (SocketException e) {
            JOptionPane.showMessageDialog(this, 
                "Error retrieving network interfaces: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addmsg(String message) {
    chatListModel.addElement(message); // Add the message to the list model
    chatHistory.add(message);  // Store in chat history
    logEvent("Message added to chat: " + message);

    // Scroll to the latest message
    chat.ensureIndexIsVisible(chatListModel.getSize() - 1);
}

   
    

private void appendMessage(String message, Color color, String sender, int port, String ip) {
    // Format: Me / Server: Message [port--ip--timestamp]
    String formattedMessage = String.format(
            "%s: %s [%d--%s--%s]", // Add unique ID to the message
            sender, message, port, ip, getCurrentTimestamp());

    // Add the formatted message to the chat list model
    chatListModel.addElement(formattedMessage);
    chatHistory.add(formattedMessage); // Store in chat history
    logEvent("Message added to chat: " + formattedMessage);

    // Scroll to the latest message
    chat.ensureIndexIsVisible(chatListModel.getSize() - 1);
}

    


    /**
     * Creates new form NewJFrame
     */
    public ClientChatTCP() {
    initComponents();
    populateAvailableInterfaces();
    
    // Initialize chatListModel
    chatListModel = new DefaultListModel<>();
    chat.setModel(chatListModel);

    // Set the custom renderer to handle color coding
    chat.setCellRenderer(new ChatListCellRenderer());

    // Existing code to fill IP and Port when a user is selected
    ActiveUsers.addListSelectionListener(e -> {
        if (!e.getValueIsAdjusting()) {
            String selectedUser = ActiveUsers.getSelectedValue(); // Get selected user
            if (selectedUser != null && userIPMap.containsKey(selectedUser) && userPortMap.containsKey(selectedUser)) {
                // Fill the RemoteIP and RemotePort fields with the user's details
                RemoteIP.setText(userIPMap.get(selectedUser));
                RemotePort.setText(String.valueOf(userPortMap.get(selectedUser)));
            }
        }
    });

    Client.setMessageListener(this::onMessageReceived);
}


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Send = new javax.swing.JButton();
        ConnectionStatusLabel = new javax.swing.JLabel();
        AvailableInterfaces = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        LocalIP = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        LocalPort = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        RemoteIP = new javax.swing.JTextField();
        RemotePort = new javax.swing.JTextField();
        TestConnectionBtn = new javax.swing.JButton();
        RetreiveInfo = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        msgtxt = new javax.swing.JTextArea();
        connectionname = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        Username = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        Password = new javax.swing.JPasswordField();
        jLabel10 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane3 = new javax.swing.JScrollPane();
        ActiveUsers = new javax.swing.JList<>();
        ServerIP = new javax.swing.JTextField();
        ServerPort = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        Login = new javax.swing.JButton();
        Logout = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        Delete = new javax.swing.JButton();
        Archive = new javax.swing.JButton();
        DeleteAll = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        chat = new javax.swing.JList<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        Send.setText("Send");
        Send.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SendActionPerformed(evt);
            }
        });

        ConnectionStatusLabel.setForeground(new java.awt.Color(255, 0, 0));
        ConnectionStatusLabel.setText("Connection Status");

        AvailableInterfaces.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        AvailableInterfaces.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AvailableInterfacesActionPerformed(evt);
            }
        });

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Available Interfaces");

        jLabel5.setText("Local IP :");

        jLabel6.setText("Local Port:");

        LocalPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LocalPortActionPerformed(evt);
            }
        });

        jLabel7.setText("Remote IP:");

        jLabel8.setText("Remote Port:");

        RemotePort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RemotePortActionPerformed(evt);
            }
        });

        TestConnectionBtn.setText("Test Connection");
        TestConnectionBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TestConnectionBtnActionPerformed(evt);
            }
        });

        RetreiveInfo.setText("Retreive Info");
        RetreiveInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RetreiveInfoActionPerformed(evt);
            }
        });

        msgtxt.setColumns(20);
        msgtxt.setRows(5);
        jScrollPane2.setViewportView(msgtxt);

        connectionname.setEditable(false);

        jLabel1.setText("Username:");

        jLabel2.setText("Password");

        jLabel3.setText("TCP Server IP:");

        jLabel9.setText("TCP Server Port:");

        jLabel10.setText("Connection Name:");

        jScrollPane3.setViewportView(ActiveUsers);

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setText("Active Users");

        Login.setForeground(new java.awt.Color(0, 255, 0));
        Login.setText("Login");
        Login.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoginActionPerformed(evt);
            }
        });

        Logout.setForeground(new java.awt.Color(255, 0, 0));
        Logout.setText("Logout");
        Logout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LogoutActionPerformed(evt);
            }
        });

        Delete.setText("Delete");
        Delete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteActionPerformed(evt);
            }
        });

        Archive.setText("Archive");
        Archive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ArchiveActionPerformed(evt);
            }
        });

        DeleteAll.setText("Delete All");
        DeleteAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteAllActionPerformed(evt);
            }
        });

        chat.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane4.setViewportView(chat);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(ConnectionStatusLabel)
                        .addGap(93, 93, 93))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(Send, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(Delete, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(connectionname, javax.swing.GroupLayout.PREFERRED_SIZE, 265, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 131, Short.MAX_VALUE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(DeleteAll, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(Archive, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE))))
                            .addComponent(jScrollPane4))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGap(18, 18, 18)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(RemoteIP)
                                            .addComponent(RemotePort, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addGap(55, 55, 55)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(RetreiveInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGap(60, 60, 60)))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addComponent(AvailableInterfaces, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(32, 32, 32))
                                .addGroup(layout.createSequentialGroup()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE))
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(Username, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(Password, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(Login)
                                                .addComponent(Logout)))
                                        .addGroup(layout.createSequentialGroup()
                                            .addGap(34, 34, 34)
                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(LocalIP)
                                                        .addComponent(LocalPort, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                    .addGroup(layout.createSequentialGroup()
                                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                            .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING))
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                            .addComponent(ServerIP)
                                                            .addComponent(ServerPort, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                    .addComponent(jSeparator2)
                                                    .addComponent(jSeparator1)))))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                            .addGroup(layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(TestConnectionBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(57, 57, 57)))))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(Username, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(Login)
                            .addComponent(jLabel11))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(Password, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(Logout))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel3)
                                    .addComponent(ServerIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel9)
                                    .addComponent(ServerPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(20, 20, 20)
                                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(LocalIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel6)
                                    .addComponent(LocalPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(RetreiveInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(AvailableInterfaces, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(Send))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(RemoteIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel8)
                                    .addComponent(RemotePort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(TestConnectionBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(14, 14, 14)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                            .addComponent(connectionname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(ConnectionStatusLabel)
                                .addGap(40, 40, 40))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 395, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 272, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(Delete, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(DeleteAll, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(Archive, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void SendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SendActionPerformed
       String msg = msgtxt.getText().trim();
    List<String> selectedUsers = ActiveUsers.getSelectedValuesList(); // Get selected users
    String username= Username.getText();

    if (!msg.isEmpty() && !selectedUsers.isEmpty()) {
        try {
            String recipients = String.join(",", selectedUsers); // Join usernames with commas
            String formattedMessage = "/send " + recipients + " " + msg; // Multi-user format
            
            Client.sendMessage(formattedMessage);  // Send message to server
            String localIP = LocalIP.getText().trim();
            int localPort = Integer.parseInt(LocalPort.getText().trim());

            // Append the sent message
            appendMessage(msg, Color.BLUE, username, localPort, localIP);
            logEvent("Message sent to " + recipients + ": " + msg);

            msgtxt.setText("");  // Clear input field
        } catch (IOException e) {
            appendMessage("Failed to send message: " + e.getMessage(), Color.RED, username, 0, "N/A");
        }
    } else {
        JOptionPane.showMessageDialog(this, "Please enter a message and select at least one recipient.", "Input Error", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_SendActionPerformed

    private void AvailableInterfacesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AvailableInterfacesActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_AvailableInterfacesActionPerformed

    private void LocalPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LocalPortActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_LocalPortActionPerformed

    private void RemotePortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RemotePortActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_RemotePortActionPerformed

    private void TestConnectionBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TestConnectionBtnActionPerformed
       String serverip = ServerIP.getText().trim();
    if (serverip.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Please enter a valid remote IP.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        return;
    }

    int serverport;
    try {
        serverport = Integer.parseInt(ServerPort.getText().trim());
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Please enter a valid remote port number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        return;
    }

    try {
        Client.start(serverip, serverport);  // Connect to server
        ConnectionStatusLabel.setForeground(new java.awt.Color(0, 255, 0));  // Green for connected
        connectionname.setText("Connected to " + serverip + ":" + serverport);
        logEvent("Connected to " + serverip + ":" + serverport);
    } catch (IOException e) {
        ConnectionStatusLabel.setForeground(new java.awt.Color(255, 0, 0));  // Red for error
        connectionname.setText("Failed to connect to " + serverip + ":" + serverport);
        logEvent("Failed to connect: " + e.getMessage());
        e.printStackTrace();
    }
    }//GEN-LAST:event_TestConnectionBtnActionPerformed

    private void RetreiveInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RetreiveInfoActionPerformed
        String selectedInterfaceName = (String) AvailableInterfaces.getSelectedItem();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (networkInterface.getDisplayName().equals(selectedInterfaceName)) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                            LocalIP.setText(address.getHostAddress());  // Set local IP
                            LocalPort.setText("50000");  // Default local port
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (SocketException e) {
            JOptionPane.showMessageDialog(this,
                "Error retrieving network information: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_RetreiveInfoActionPerformed

    private void startAutoRemoveFromArchive(String msg) {
    new Thread(() -> {
        try {
            Thread.sleep(2 * 60 * 1000); // Wait for 2 minutes (120000 milliseconds)

            // Remove the message from the archive
            if(msg.equals("AllMessages")){
                archivedMessages.clear();
                logEvent("All messages permenantly deleted and removed from archived.");
            }else
            archivedMessages.remove(msg);
            logEvent("Message permenantly deleted and removed from archive: " + msg);
            

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            e.printStackTrace();
        }
    }).start();
}
    
    private void refreshChatDisplay() {
    // Clear the chat display in the JList
    chatListModel.clear(); // Clear existing content

    // Re-add messages from chatHistory to the display
    for (String message : chatHistory) {
        chatListModel.addElement(message); // Add each message to the list model
    }

    // Scroll to the latest message
    if (!chatHistory.isEmpty()) {
        chat.ensureIndexIsVisible(chatListModel.getSize() - 1);
    }
}

    
    
    
    
    private void LogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LogoutActionPerformed
        // TODO add your handling code here:
        // Notify the server about the logout if the client is connected
    if (Client.isRunning()) {
        try {
            Client.sendMessage("LOGOUT:" + currentUsername); // Send logout message to the server
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error notifying server of logout: " + e.getMessage(), "Logout Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Clear client-side data
    chatHistory.clear();            // Clear chat history
    archivedMessages.clear();       // Clear archived messages
    refreshChatDisplay();           // Refresh chat UI

    // Reset UI elements
    Username.setText("");            // Clear the username field
    Password.setText("");            // Clear the password field
    connectionname.setText("Disconnected");  // Show disconnected status
    ConnectionStatusLabel.setForeground(Color.RED);  // Red color for disconnected status
    ConnectionStatusLabel.setText("Disconnected");

    // Close the client connection
    Client.close();
    
    // Log the event
    logEvent("User logged out: " + currentUsername);
    
    // Reset current username
    currentUsername = null;
    }//GEN-LAST:event_LogoutActionPerformed

    private void LoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoginActionPerformed
     String username = Username.getText().trim();
    String password = new String(Password.getPassword()).trim();  // Get password securely

    if (username.isEmpty() || password.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    // Log the login request
    logEvent("Attempting to log in with username: " + username + " and password: " + password); // Debug log

    // Send login request to the server
    try {
        Client.sendMessage(username);  // Send username
        Client.sendMessage(password);  // Send password
        // No need to block for response; handle in the listener
    } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Error connecting to server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_LoginActionPerformed

    private void DeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteActionPerformed
      // Get the selected message from the JList
    String selectedMessage = chat.getSelectedValue();

    if (selectedMessage != null) {
        // Use the exact selected message for deletion
        archivedMessages.add(selectedMessage); // Add to archived messages
        chatHistory.removeIf(message -> message.equals(selectedMessage)); // Delete only the exact match
        chatListModel.removeElement(selectedMessage); // Remove from the list model
        logEvent("Message deleted: " + selectedMessage);

        // Send delete command to the server for broadcasting
        try {
            Client.sendMessage("DELETE: " + selectedMessage); // Notify server about deletion
        } catch (IOException e) {
            e.printStackTrace(); // Handle exception
        }
        startAutoRemoveFromArchive(selectedMessage);
    } else {
        JOptionPane.showMessageDialog(this, "No message selected to delete.", "Error", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_DeleteActionPerformed

    private void ArchiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ArchiveActionPerformed
        showArchivedMessages();
    }//GEN-LAST:event_ArchiveActionPerformed

    private void DeleteAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteAllActionPerformed
        // Check if there are messages to delete
    if (!chatHistory.isEmpty()) {
        // Archive all messages
        archivedMessages.addAll(chatHistory); // Add all messages to archived messages
        chatHistory.clear(); // Clear chat history
        refreshChatDisplay(); // Refresh UI to show the updated chat history
        logEvent("All messages deleted from chat and archived.");

        // Send delete all command to the server for broadcasting
        try {
            Client.sendMessage("DELETE_ALL"); // Notify server about deletion of all messages
        } catch (IOException e) {
            e.printStackTrace(); // Handle exception
        }

        // Schedule removal from archive after 2 minutes
        startAutoRemoveFromArchive("AllMessages"); // Use a unique identifier
    } else {
        JOptionPane.showMessageDialog(this, "No messages found to delete.", "Error", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_DeleteAllActionPerformed

    private void showArchivedMessages() {
    // Create a new dialog for archived messages
    JDialog archiveDialog = new JDialog(this, "Archived Messages", true);
    archiveDialog.setSize(400, 300);
    archiveDialog.setLayout(new BorderLayout());

    // Create a list for archived messages
    JList<String> archiveList = new JList<>(archivedMessages.toArray(new String[0]));
    archiveDialog.add(new JScrollPane(archiveList), BorderLayout.CENTER);

    // Create Restore button
    JButton restoreButton = new JButton("Restore");
    restoreButton.addActionListener(e -> {
        String selectedMessage = archiveList.getSelectedValue(); // Get the selected message
        if (selectedMessage != null) {
            // Restore the selected message
            restoreMessage(selectedMessage);
            archiveDialog.dispose(); // Close the dialog after restoring
        } else {
            JOptionPane.showMessageDialog(archiveDialog, "No message was selected to restore.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    });
    archiveDialog.add(restoreButton, BorderLayout.SOUTH);

    archiveDialog.setLocationRelativeTo(this); // Center the dialog
    archiveDialog.setVisible(true); // Show the dialog
}
    
    private void restoreMessage(String message) {
    // Add the message back to chat history
    chatHistory.add(message); // Add to chat history
    archivedMessages.remove(message); // Remove from archived messages
    refreshChatDisplay(); // Refresh the chat display to show the restored message
    logEvent("Message restored: " + message);

    // Optionally, you can also send a restore command to other instances
    try {
        Client.sendMessage("RESTORE: " + message); // Broadcast restore message
    } catch (IOException e) {
        e.printStackTrace(); // Handle exception
    }
    }
    
    @Override
    public void dispose() {
    super.dispose();
    Client.close();  // Close the TCP connection
    logEvent("Client closed.");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ClientChatTCP.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ClientChatTCP.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ClientChatTCP.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ClientChatTCP.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ClientChatTCP().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList<String> ActiveUsers;
    private javax.swing.JButton Archive;
    private javax.swing.JComboBox<String> AvailableInterfaces;
    private javax.swing.JLabel ConnectionStatusLabel;
    private javax.swing.JButton Delete;
    private javax.swing.JButton DeleteAll;
    private javax.swing.JTextField LocalIP;
    private javax.swing.JTextField LocalPort;
    private javax.swing.JButton Login;
    private javax.swing.JButton Logout;
    private javax.swing.JPasswordField Password;
    private javax.swing.JTextField RemoteIP;
    private javax.swing.JTextField RemotePort;
    private javax.swing.JButton RetreiveInfo;
    private javax.swing.JButton Send;
    private javax.swing.JTextField ServerIP;
    private javax.swing.JTextField ServerPort;
    private javax.swing.JButton TestConnectionBtn;
    private javax.swing.JTextField Username;
    private javax.swing.JList<String> chat;
    private javax.swing.JTextField connectionname;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTextArea msgtxt;
    // End of variables declaration//GEN-END:variables
}
