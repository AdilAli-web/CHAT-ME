package com.example.CHAT_ME.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(length = 24)
    public String userName;

    @Column(nullable = false,length = 50)
    public String displayName;


    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Column(nullable = false,length = 50)
    public String passwordHash;


    @Version
    private Long version;


// we used protected account so we can use it in child classes
    protected Account(){

    }

    public Account(String userName, String displayName, String passwordHash) {
        this.userName = userName;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
    }
}
