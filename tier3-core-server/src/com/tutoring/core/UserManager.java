package com.tutoring.core;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe user authentication management
 * In production, this would use proper password hashing and a database
 */
public class UserManager {
    // Map: Username -> Password (in production, use hashed passwords)
    private final ConcurrentHashMap<String, String> users;

    public UserManager() {
        this.users = new ConcurrentHashMap<>();

        // Add some default users for testing
        users.put("teacher1", "pass123");
        users.put("teacher2", "pass123");
        users.put("student1", "pass123");
        users.put("student2", "pass123");
        users.put("student3", "pass123");
        users.put("student4", "pass123");
    }

    public boolean registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            return false;
        }

        // putIfAbsent returns null if the key was not present
        return users.putIfAbsent(username, password) == null;
    }

    public boolean authenticateUser(String username, String password) {
        String storedPassword = users.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }

    public boolean userExists(String username) {
        return users.containsKey(username);
    }
}
