package server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ChatServer server;
    private PrintWriter out;
    private BufferedReader in;
    private User currentUser;
    private boolean anonymousMode = false;
    
    public ClientHandler(Socket socket, ChatServer server) {
        this.clientSocket = socket;
        this.server = server;
    }
    
    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
            // 认证过程
            authenticateUser();
            
            // 聊天循环
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("@@")) {
                    handleCommand(inputLine.substring(2));
                } else if (inputLine.startsWith("@")) {
                    handlePrivateMessage(inputLine);
                } else {
                    handlePublicMessage(inputLine);
                }
            }
        } catch (IOException e) {
            System.err.println("客户端连接错误: " + e.getMessage());
        } finally {
            disconnectClient();
        }
    }
    
    private void authenticateUser() throws IOException {
        out.println("请输入用户名:");
        String username = in.readLine();
        out.println("请输入密码:");
        String password = in.readLine();
        
        while (!server.getUserManager().authenticate(username, password)) {
            server.logEvent(username, "登录失败", clientSocket.getInetAddress().getHostAddress());
            out.println("用户名或密码错误，请重试 (或输入quit退出)");
            username = in.readLine();
            if (username.equalsIgnoreCase("quit")) {
                out.println("再见!");
                throw new IOException("用户取消认证");
            }
            out.println("请输入密码:");
            password = in.readLine();
        }
        
        currentUser = server.getUserManager().getUser(username);
        currentUser.setOnline(true);
        currentUser.setIpAddress(clientSocket.getInetAddress().getHostAddress());
        server.logEvent(username, "登录成功", clientSocket.getInetAddress().getHostAddress());
        out.println("认证成功! 欢迎来到聊天室, " + username);
        server.broadcast(username + " 加入了聊天室", this);
    }
    
    private void handleCommand(String command) {
        switch (command.toLowerCase()) {
            case "list":
                out.println("在线用户: " + server.getUserManager().getOnlineUsers());
                break;
            case "quit":
                out.println("quit");
                disconnectClient();
                break;
            case "showanonymous":
                out.println("当前匿名模式: " + anonymousMode);
                break;
            case "anonymous":
                anonymousMode = !anonymousMode;
                out.println("匿名模式已" + (anonymousMode ? "开启" : "关闭"));
                break;
            default:
                out.println("未知命令: " + command);
        }
    }
    
    private void handlePrivateMessage(String message) {
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) {
            out.println("私聊格式错误，请使用 @用户名 消息内容");
            return;
        }
        
        String recipientName = parts[0].substring(1);
        String content = parts[1];
        
        server.sendPrivateMessage(
            anonymousMode ? "匿名用户" : currentUser.getUsername(),
            recipientName,
            content
        );
    }
    
    private void handlePublicMessage(String message) {
        server.broadcast(
            (anonymousMode ? "匿名用户" : currentUser.getUsername()) + ": " + message,
            this
        );
    }
    
    private void disconnectClient() {
        try {
            if (currentUser != null) {
                currentUser.setOnline(false);
                server.logEvent(currentUser.getUsername(), "退出", clientSocket.getInetAddress().getHostAddress());
                server.broadcast(currentUser.getUsername() + " 离开了聊天室", this);
            }
            
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null) clientSocket.close();
            
            server.removeClient(this);
        } catch (IOException e) {
            System.err.println("关闭客户端连接时出错: " + e.getMessage());
        }
    }
    
    public void sendMessage(String message) {
        out.println(message);
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
}