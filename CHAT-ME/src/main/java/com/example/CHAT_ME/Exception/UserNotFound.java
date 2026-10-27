package com.example.CHAT_ME.Exception;

public class UserNotFound extends RuntimeException {

    public UserNotFound(String userName) {
        super("Username '" + userName + "' already exists");
    }
}