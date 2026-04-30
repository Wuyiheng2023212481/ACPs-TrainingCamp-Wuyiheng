package server;

import java.io.Serializable;

public class User implements Serializable {
    private String username;
    private String password;
    private boolean isOnline;
    private String ipAddress;
    
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.isOnline = false;
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    @Override
    public String toString() {
        return username + (isOnline ? " (在线)" : " (离线)");
    }
}