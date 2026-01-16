package com.tarun.upi.service;

import com.tarun.upi.exception.UPIException;
import com.tarun.upi.model.User;

import java.util.HashMap;
import java.util.Map;

public class UserService {

    public Map<String, User> usersByPhone = new HashMap<>();
    public Map<String, User> usersById = new HashMap<>();


    public User createUser(String id, String name, String phone) throws UPIException {
        if (usersByPhone.containsKey(phone))
            throw new UPIException("Phone already linked");
        User u = new User(id, name, phone);
        usersByPhone.put(phone, u);
        usersById.put(id, u);
        return u;
    }
}
