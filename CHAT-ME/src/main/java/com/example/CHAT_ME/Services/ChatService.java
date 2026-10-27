package com.example.CHAT_ME.Services;
import com.example.CHAT_ME.Dtos.Conversation;
import com.example.CHAT_ME.Dtos.OutGoing;
import com.example.CHAT_ME.Dtos.Person;
import com.example.CHAT_ME.Dtos.Registration;
import com.example.CHAT_ME.Entity.Account;
import com.example.CHAT_ME.Entity.ChatMessage;
import com.example.CHAT_ME.Exception.UserAlreadyExists;
import com.example.CHAT_ME.Exception.UserNotFound;
import com.example.CHAT_ME.Repo.AccountRepo;
import com.example.CHAT_ME.Repo.ChatMessageRepo;
import com.example.CHAT_ME.webSocket.LiveUpdates;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.LinkedHashMap;

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
//validation to check does user already exists?

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


    public Person getPerson(String userName){
        Account account=accountRepo.findById(userName).orElseThrow(
                ()-> new UserNotFound(userName)
        );
        return new Person(account.userName,account.displayName,account.phoneNo);

    }


//    jo bhi meri search query -query
    public List<Person> search(String query,String currentUsername)
    {

        if (query == null || query.isBlank()) {
            return List.of();
        }
        String prefix=query.trim().toLowerCase(Locale.ROOT);
        List<Person> people=new ArrayList<>();
        if(!prefix.matches("[a-z0-9_]{1,24}")){
return people;
        }

        return accountRepo
                .findTop20ByUserNameStartingWithAndUserNameNotOrderByUserName(
                        prefix, currentUsername
                )
                .stream()
                .map(account -> new Person(
                        account.userName,
                        account.displayName
                ))
                .toList();
    }


//    conversations() service method is responsible for getting a user's chat list,
//    similar to the conversation list you see in WhatsApp.

//    public List<Conversation>  conversations(String currentUserName){
//        List<Conversation> conversations=new ArrayList<>();
//        for(ChatMessage message:chatRepo.conversations(currentUserName)){
//            String peer=message.sender.equals(currentUserName)
//                    ? message.recipient:message.sender;
//            conversations.add(new Conversation(message, getPerson(peer)));
//        }
//        return conversations;
//    }

//    Lists people and their latest messages loads the conversation list
    public List<Conversation> conversations(String currentUserName) {

        // Step 1: Get the latest messages
        List<ChatMessage> messages =
                chatRepo.conversations(currentUserName);

        // Step 2: Keep only one message per person
        Map<String, ChatMessage> latestByPeer = new LinkedHashMap<>();

        for (ChatMessage message : messages) {
            String peer = message.sender.equals(currentUserName)
                    ? message.recipient
                    : message.sender;

            latestByPeer.putIfAbsent(peer, message);
        }

        // Step 3: Fetch all peer accounts in one query
        List<Account> accounts = accountRepo.findByUserNameIn(
                new ArrayList<>(latestByPeer.keySet())
        );

        Map<String, Account> accountMap = new HashMap<>();

        for (Account account : accounts) {
            accountMap.put(account.userName, account);
        }

        // Step 4: Build the response
        List<Conversation> result = new ArrayList<>();

        for (Map.Entry<String, ChatMessage> entry : latestByPeer.entrySet()) {
            Account account = accountMap.get(entry.getKey());

            if (account != null) {
                Person person = new Person(
                        account.userName,
                        account.displayName,
                        null
                );

                result.add(new Conversation(entry.getValue(), person));
            }
        }

        return result;
    }


//    How it works
//    Suppose Adil opens his chat with Rahul.
//
//1. currentUserName and peer
//- currentUserName = "adil"
//            - peer = "rahul"

    public List<ChatMessage> history(String currentUserName,String peer,long before){
        List<ChatMessage> messages=new ArrayList<>(chatRepo.history(currentUserName,peer,before, PageRequest.of(0,50)));
        Collections.reverse(messages);
        return messages;
    }


    @Transactional
    public ChatMessage send(OutGoing outGoing,String CurrentUserName){
        String recipient=outGoing.getRecipient();
        String body=outGoing.getBody();

        if(recipient==null || recipient.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"choose a recipient");
        }


ChatMessage message=chatRepo.saveAndFlush(new ChatMessage(CurrentUserName,recipient,body.trim()));

        updates.publish(message);
        return message;
    }


}
