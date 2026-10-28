package com.example.CHAT_ME.Controller;

import com.example.CHAT_ME.Dtos.Conversation;
import com.example.CHAT_ME.Dtos.Person;
import com.example.CHAT_ME.Dtos.Registration;
import com.example.CHAT_ME.Entity.ChatMessage;
import com.example.CHAT_ME.Services.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

@GetMapping("/csrf")
    public Map<String,String> csrf(CsrfToken token){
        return Map.of("Token", token.getToken(),"headerName", token.getHeaderName());
}

//registering new users/creating a new account
    @PostMapping("/register")
    public ResponseEntity<Person> register(
            @Valid @RequestBody Registration registration
    ) {
        Person person = chatService.register(registration);

        return ResponseEntity.status(HttpStatus.CREATED).body(person);
    }

//get
    @GetMapping("/me")
    public Person csrf(Principal principal){
        return chatService.getPerson(principal.getName());
    }

@GetMapping("/users")
    public
    List<Person> search(@RequestParam String query,Principal principal){
        return chatService.search(query,principal.getName());
}

    @GetMapping("/conversations")
    public
    List<Conversation> conversations( Principal principal){
        return chatService.conversations(principal.getName());
    }


    @GetMapping("/message/{peer}")
    public
    List<ChatMessage> chatMessages( @PathVariable String peer, @RequestParam long before,Principal principal){
        return chatService.history(principal.getName(),peer,before);
    }




}
