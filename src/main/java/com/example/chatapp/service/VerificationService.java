package com.example.chatapp.service;

import com.example.chatapp.controller.WebSocketController;
import com.example.chatapp.entity.ChattyEntity;
import com.example.chatapp.repository.ChattyRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationService {
    private final Map<String, String> verificationCodes = new ConcurrentHashMap<>();

    @Autowired
    private ChattyRepository chattyRepository;

    public ResponseEntity<String> checkVerificationCode(Map<String, String> payload, HttpSession session) {
        String identity = payload.get("phoneNumber");
        String code = payload.get("code");

        String username = getUsernameFromIdentity(identity);

        String storedCode = verificationCodes.get(username);
        if (storedCode == null || !storedCode.equals(code)) {
            return new ResponseEntity<>("Invalid Code", HttpStatus.BAD_REQUEST);
        }

        WebSocketController.addVerifiedUser(username);

        verificationCodes.remove(username);
        session.setAttribute("verifiedUser", username);

        return new ResponseEntity<>(username, HttpStatus.OK);
    }

    public ResponseEntity<String> sendVerificationCode(Map<String, String> payload) {
        String identity = payload.get("phoneNumber");
        if (identity == null || identity.trim().isEmpty()) {
            return new ResponseEntity<>("Phone number is required.", HttpStatus.BAD_REQUEST);
        }

        String username = getUsernameFromIdentity(identity);

        if(username.isEmpty()) {
            return new ResponseEntity<>("No chatty exists with this username or phone number.", HttpStatus.BAD_REQUEST);
        }

        String code = "123";
        verificationCodes.put(username, code);
        System.out.println("Verification code for " + username + " is: " + code);

        return new ResponseEntity<>("Verification Code Sent Successfully!", HttpStatus.OK);
    }

    public void removeVerificationCode(String username) {
        verificationCodes.remove(username);
    }

    private String getUsernameFromIdentity(String identity) {
        Optional<ChattyEntity> chattyByPhoneNumber = Optional.ofNullable(chattyRepository.findByPhoneNumber(identity));
        Optional<ChattyEntity> chattyByUsername = Optional.ofNullable(chattyRepository.findByUsername(identity));
        Optional<ChattyEntity> chattyByEmail = Optional.ofNullable(chattyRepository.findByEmail(identity));

        return chattyByUsername.isPresent() ? chattyByUsername.get().getUsername() : chattyByPhoneNumber.isPresent() ? chattyByPhoneNumber.get().getUsername() : chattyByEmail.isPresent() ? chattyByEmail.get().getUsername() : "";
    }
}