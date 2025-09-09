package com.mugtaba.pixl.models;

import java.util.Date;

public class User {
    private int id;
    private String username;
    private String password;
    private String createdAt;

    public User(int id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.createdAt = new Date().toString();
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}