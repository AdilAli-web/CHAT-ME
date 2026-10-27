package com.example.CHAT_ME.Dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Registration {
//    this class is for creating Account

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 24,
            message = "Username must be between 3 and 24 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$",
            message = "Username can contain only letters, numbers, and underscores")
    private String userName;

    @NotBlank(message = "Display name is required")
    @Size(max = 50,
            message = "Display name cannot exceed 50 characters")
    private String displayName;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72,
            message = "Password must be between 8 and 72 characters")

    private String password;

    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Enter a valid 10-digit Indian mobile number"
    )
    private String phoneNo;



}
