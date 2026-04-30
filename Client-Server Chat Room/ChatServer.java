package server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 8888;
    private static final int MAX_CLIENTS = 5;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private List<ClientHandler> clients;
    private UserManager userManager;
    
    public ChatServer() {
        threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
        clients = new CopyOnWriteArrayList<>();
        userManager = new UserManager();
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("聊天服务器已启动，监听端口: " + PORT);
            
            // 启动控制台命令处理线程
            new Thread(this::handleConsoleCommands).start();
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                if (clients.size() >= MAX_CLIENTS) {
                    PrintWriter tempOut = new PrintWriter(clientSocket.getOutputStream(), true);
                    tempOut.println("服务器已达到最大连接数，请稍后再试");
                    clientSocket.close();
                    continue;
                }
                
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("服务器错误: " + e.getMessage());
        } finally {
            stop();
        }
    }
    
    private void handleConsoleCommands() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("服务器控制台已启动，输入命令 (list/listall/quit):");
            
            while (true) {
                String command = scanner.nextLine().trim().toLowerCase();
                switch (command) {
                    case "list":
                        System.out.println("在线用户: " + userManager.getOnlineUsers());
                        break;
                    case "listall":
                        System.out.println("所有用户: " + userManager.getAllUsers());
                        break;
                    case "quit":
                        System.out.println("正在关闭服务器...");
                        stop();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("未知命令，可用命令: list, listall, quit");
                }
            }
        }
    }
    
    public void broadcast(String message, ClientHandler excludeClient) {
        for (ClientHandler client : clients) {
            if (client != excludeClient) {
                client.sendMessage(message);
            }
        }
    }
    
    public void sendPrivateMessage(String sender, String recipientName, String message) {
        for (ClientHandler client : clients) {
            User user = client.getCurrentUser();
            if (user != null && user.getUsername().equals(recipientName)) {
                client.sendMessage("[私聊] " + sender + ": " + message);
                return;
            }
        }
        
        // 如果收件人不在线，通知发送者
        for (ClientHandler client : clients) {
            User user = client.getCurrentUser();
            if (user != null && user.getUsername().equals(sender)) {
                client.sendMessage("用户 " + recipientName + " 不在线");
            }
        }
    }
    
    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }
    
    public void logEvent(String username, String event, String ipAddress) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("server.log", true))) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.println(timestamp + " - " + username + " - " + event + " - IP: " + ipAddress);
        } catch (IOException e) {
            System.err.println("日志写入失败: " + e.getMessage());
        }
    }
    
    public UserManager getUserManager() {
        return userManager;
    }
    
    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            threadPool.shutdown();
            System.out.println("服务器已关闭");
        } catch (IOException e) {
            System.err.println("关闭服务器时出错: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        new ChatServer().start();
    }
}