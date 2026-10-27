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

@Column(nullable = false,unique = true)
public String phoneNo;

    @Column(nullable = false, length = 255)
    public String passwordHash;


    @Version
    private Long version;


// we used protected account so we can use it in child classes
    protected Account(){

    }

    public Account(String userName, String displayName, String passwordHash,String phoneNo) {
        this.userName = userName;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.phoneNo=phoneNo;
    }
}
