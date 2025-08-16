package com.example.chatapp.controller;

import com.example.chatapp.entity.ChattyEntity;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.repository.ChattyRepository;
import com.example.chatapp.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class WebSocketController {

    private static final Set<String> verifiedUsers = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    private final List<ChatMessage> publicChatHistory = Collections.synchronizedList(new ArrayList<>());
    private final List<String> onlineUsers = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // MODIFIED: Inject the necessary service and repository
    @Autowired
    private VerificationService verificationService;
    @Autowired
    private ChattyRepository chattyRepository;


    @GetMapping("/api/public-chat/history")
    public List<ChatMessage> getPublicChatHistory() {
        return publicChatHistory;
    }

    @GetMapping("/api/users/online")
    public List<String> getOnlineUsers() {
        return onlineUsers;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(ChatMessage chatMessage) {
        if (!verifiedUsers.contains(chatMessage.getSender())) {
            System.out.println("Unverified user attempted to send a message: " + chatMessage.getSender());
            return null;
        }
        publicChatHistory.add(chatMessage);
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();
        String sessionId = headerAccessor.getSessionId();

        if (sessionId == null) {
            System.err.println("CRITICAL: Attempted to add user with a null session ID: " + username);
            return null;
        }

        if (!verifiedUsers.contains(username)) {
            System.out.println("Unverified user attempted to join the chat: " + username);
            return null;
        }

        if (!onlineUsers.contains(username)) {
            onlineUsers.add(username);
            activeSessions.put(sessionId, username);
            messagingTemplate.convertAndSend("/topic/users", onlineUsers);

            ChatMessage joinMessage = new ChatMessage();
            joinMessage.setContent(username + " has joined the chat!");
            joinMessage.setSender("System");
            joinMessage.setType(ChatMessage.MessageType.JOIN);
            return joinMessage;
        }
        return null;
    }

    public void handleUserDisconnect(String sessionId) {
        String username = activeSessions.get(sessionId);
        if (username != null) {
            onlineUsers.remove(username);
            activeSessions.remove(sessionId);

            verificationService.removeVerificationCode(username);

            ChatMessage leaveMessage = new ChatMessage();
            leaveMessage.setContent(username + " has left the chat.");
            leaveMessage.setSender("System");
            leaveMessage.setType(ChatMessage.MessageType.JOIN);

            messagingTemplate.convertAndSend("/topic/public", leaveMessage);
            messagingTemplate.convertAndSend("/topic/users", onlineUsers);

            System.out.println("User disconnected: " + username);
        }
    }

    public static void addVerifiedUser(String username) {
        verifiedUsers.add(username);
    }
}
