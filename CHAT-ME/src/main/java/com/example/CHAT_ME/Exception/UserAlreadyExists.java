package com.example.CHAT_ME.Exception;

public class UserAlreadyExists extends RuntimeException{

    public UserAlreadyExists(String userName) {
        super("Username '" + userName + "' already exists");
    }
}
