package com.example.CHAT_ME.Services;

import com.example.CHAT_ME.Dtos.Person;
import com.example.CHAT_ME.Dtos.Registration;
import com.example.CHAT_ME.Entity.Account;
import com.example.CHAT_ME.Exception.UserAlreadyExists;
import com.example.CHAT_ME.Repo.AccountRepo;
import com.example.CHAT_ME.Repo.ChatMessageRepo;
import com.example.CHAT_ME.webSocket.LiveUpdates;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    @Transactional
    public Person register(Registration registration) {

        String userName = registration.getUserName().trim();
        String displayName = registration.getDisplayName().trim();
String phoneNo=registration.getPhoneNo().trim();
        if (accountRepo.existsById(userName)) {
            throw new UserAlreadyExists(userName);
        }

        Account account = new Account(
                userName,
                displayName,
                passwordEncoder.encode(registration.getPassword()),
                phoneNo
        );

        Account savedAccount = accountRepo.save(account);

        return new Person(
                savedAccount.userName,
                savedAccount.displayName,
                savedAccount.phoneNo
        );
    }





}
