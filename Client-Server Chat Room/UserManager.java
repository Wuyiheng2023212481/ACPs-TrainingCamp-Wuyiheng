package server;

import java.io.*;
import java.util.*;

public class UserManager {
    private Map<String, User> users;
    private static final String USER_FILE = "users.txt";
    
    public UserManager() {
        users = new HashMap<>();
        loadUsers();
    }
    
    private void loadUsers() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    users.put(parts[0], new User(parts[0], parts[1]));
                }
            }
        } catch (IOException e) {
            System.err.println("无法加载用户文件: " + e.getMessage());
        }
    }
    
    public boolean authenticate(String username, String password) {
        User user = users.get(username);
        return user != null && user.getPassword().equals(password);
    }
    
    public User getUser(String username) {
        return users.get(username);
    }
    
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    public List<User> getOnlineUsers() {
        List<User> onlineUsers = new ArrayList<>();
        for (User user : users.values()) {
            if (user.isOnline()) {
                onlineUsers.add(user);
            }
        }
        return onlineUsers;
    }
}