package com.example.CHAT_ME.Dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class OutGoing {
//    jab aap kisi ko message bhej rahe ho tab ye class use hoga

    @NotBlank(message = "Recipient is required")
    @Size(max = 24, message = "Recipient is too long")
    private String recipient;

    @NotBlank(message = "Message body cannot be empty")
    @Size(max = 40000, message = "Message is too long")
    private String body;



}
