package com.example.CHAT_ME.Controller;

import com.example.CHAT_ME.Dtos.Person;
import com.example.CHAT_ME.Dtos.Registration;
import com.example.CHAT_ME.Services.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/register")
    public ResponseEntity<Person> register(
            @Valid @RequestBody Registration registration
    ) {
        Person person = chatService.register(registration);

        return ResponseEntity.status(HttpStatus.CREATED).body(person);
    }




}
