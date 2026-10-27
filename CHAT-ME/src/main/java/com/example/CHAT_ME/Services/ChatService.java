package com.example.CHAT_ME.Services;

import com.example.CHAT_ME.Repo.AccountRepo;
import com.example.CHAT_ME.Repo.ChatMessageRepo;
import com.example.CHAT_ME.webSocket.LiveUpdates;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final AccountRepo accountRepo;
    private final ChatMessageRepo chatRepo;
    private final PasswordEncoder passwordEncoder;
    private final LiveUpdates updates;

    public ChatService(AccountRepo accountRepo, ChatMessageRepo chatRepo, PasswordEncoder passwordEncoder, LiveUpdates updates) {
        this.accountRepo = accountRepo;
        this.chatRepo = chatRepo;
        this.passwordEncoder = passwordEncoder;
        this.updates = updates;
    }
}
