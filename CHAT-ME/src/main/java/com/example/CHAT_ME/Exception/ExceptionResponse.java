package com.example.CHAT_ME.Exception;

import java.time.Instant;

public record ExceptionResponse(Instant timestamp,
                                int status,
                                String error,
                                String message,
                                String path) {
}
