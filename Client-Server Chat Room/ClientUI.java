package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientUI {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private boolean anonymousMode = false;
    
    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("输入服务器地址 (默认localhost): ");
            String host = scanner.nextLine().trim();
            if (host.isEmpty()) host = "localhost";
            
            socket = new Socket(host, 8888);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // 读取服务器消息
            new Thread(this::readServerMessages).start();
            
            // 认证过程
            System.out.println(in.readLine()); // "请输入用户名:"
            username = scanner.nextLine();
            out.println(username);
            
            System.out.println(in.readLine()); // "请输入密码:"
            String password = scanner.nextLine();
            out.println(password);
            
            // 等待认证结果
            while (true) {
                String response = in.readLine();
                System.out.println(response);
                if (response.startsWith("认证成功") || response.equals("quit")) {
                    break;
                }
                username = scanner.nextLine();
                out.println(username);
                System.out.println(in.readLine()); // "请输入密码:"
                password = scanner.nextLine();
                out.println(password);
            }
            
            // 聊天循环
            System.out.println("输入消息 (普通消息广播, @用户名 私聊, @@命令):");
            while (true) {
                String input = scanner.nextLine();
                out.println(input);
                
                if (input.equalsIgnoreCase("@@quit")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("客户端错误: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    private void readServerMessages() {
        try {
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                System.out.println(serverMessage);
            }
        } catch (IOException e) {
            System.err.println("读取服务器消息错误: " + e.getMessage());
        }
    }
    
    private void closeConnection() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("关闭连接时出错: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        new ClientUI().start();
    }
}