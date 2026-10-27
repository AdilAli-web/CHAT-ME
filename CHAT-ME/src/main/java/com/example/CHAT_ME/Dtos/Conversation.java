package com.example.CHAT_ME.Dtos;

import com.example.CHAT_ME.Entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
//this class is used for conversation
//    chatmessage will save the message
    private ChatMessage LastMessage;
//    who is sending the message
    private Person person;


}
