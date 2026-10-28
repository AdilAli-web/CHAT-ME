package com.example.CHAT_ME.Dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Person {
    private String userName;
    private String displayName;
    private String phoneNo;


    public Person(String userName, String displayName) {
        this.userName = userName;
        this.displayName = displayName;
    }
}
