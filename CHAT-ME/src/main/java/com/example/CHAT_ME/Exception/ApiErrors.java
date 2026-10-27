package com.example.CHAT_ME.Exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ApiErrors {

@ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException e){
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",e.getMessage()==null?"request Failed" : e.getMessage()));
    }

//we are not letting one user register two times
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of("error","that username is already taken"));
    }
    }

