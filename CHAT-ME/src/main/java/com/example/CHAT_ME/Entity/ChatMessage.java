package com.example.CHAT_ME.Entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "chat_messages",indexes = {@Index(name = "idx_sender_recipient_id",
        columnList = "sender ,recipient, id"),
    @Index(name = "idx_recipient_id",columnList = "recipient,id")
})
//ChatMessage entity, you're using database indexes to make chat message
// retrieval faster, especially when a user opens a conversation or fetches their received messages.
public class ChatMessage {

//    this class is used for saving messages when you close your whatsapp does your messages delete? no
//    exactly this class is used for saving messages even after closing your application
//    account class is for your info and this is your messages where saved
//indexing db by using jpa

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;

@Column(nullable = false,length = 25)
    public String sender;

@Column(nullable = false,length = 25)
    public String recipient;

@Column(nullable = false,length = 40000)
    public String body;

    public Instant sentAt;

    @Column(nullable = false, updatable = false)
    public Instant createdAt;

    protected ChatMessage(){

    }



    public ChatMessage(String sender, String recipient, String body) {
        this.sender = sender;
        this.recipient = recipient;
        this.body = body;
        this.sentAt=Instant.now();
        this.createdAt=Instant.now();
    }
}
