package com.example.CHAT_ME.Exception;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiErrors {

@ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException e){
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",e.getMessage()==null?"request Failed" : e.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExists.class)
    public ResponseEntity<ExceptionResponse> handleUserAlreadyExists(
            UserAlreadyExists e,
            HttpServletRequest request) {

        ExceptionResponse response = new ExceptionResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationErrors(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {

        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        ExceptionResponse response = new ExceptionResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

//we are not letting one user register two times
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of("error","that username is already taken"));
    }
    }

