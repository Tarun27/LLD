package com.tarun.service;

import com.tarun.model.User;

import java.util.HashMap;
import java.util.Map;

public class UserService {
    private final Map<String, User> users;

    public UserService() {
        this.users = new HashMap<>();
    }

    public void onboardUser(String userName) {
        if (users.containsKey(userName)) {
            System.out.println("User already exists: " + userName);
            return;
        }
        users.put(userName, new User(userName));
        System.out.println("User onboarded successfully: " + userName);
    }

    public User getUser(String userName) {
        return users.get(userName);
    }

    public boolean userExists(String userName) {
        return users.containsKey(userName);
    }

    public String getUserStats(String userName) {
        User user = users.get(userName);
        if (user == null) {
            return "User not found: " + userName;
        }
        return user.toString();
    }
}