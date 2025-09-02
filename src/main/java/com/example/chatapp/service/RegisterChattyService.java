package com.example.chatapp.service;

import com.example.chatapp.entity.ChattyEntity;
import com.example.chatapp.model.ChattyRequest;
import com.example.chatapp.repository.ChattyRepository;
// CORRECTED: Import Spring's Transactional annotation
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RegisterChattyService {

    @Autowired
    private ChattyRepository chattyRepository;

    @Transactional
    public ResponseEntity<String> registerChatty(ChattyRequest chattyRequest) {

        Optional<ChattyEntity> existingByEmail = chattyRepository.findByEmail(chattyRequest.getEmail());
        if(existingByEmail.isPresent()) {
            return new ResponseEntity<>("This phone number is already registered", HttpStatus.BAD_REQUEST);
        }

        ChattyEntity chattyEntity = ChattyEntity.builder()
                .email(chattyRequest.getEmail())
                .username(chattyRequest.getUsername())
                .password(chattyRequest.getPassword())
                .isVerified(false)
                .build();

        chattyRepository.save(chattyEntity);

        return new ResponseEntity<>("User registered successfully.", HttpStatus.CREATED);
    }
}