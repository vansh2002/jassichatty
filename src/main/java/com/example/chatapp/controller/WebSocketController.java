package com.example.chatapp.controller;

import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.repository.ChattyRepository;
import com.example.chatapp.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.example.chatapp.service.HeartbeatService.*;

@RestController
public class WebSocketController {

    private static final Set<String> verifiedUsers = ConcurrentHashMap.newKeySet();
    private final List<ChatMessage> publicChatHistory = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private VerificationService verificationService;
    @Autowired
    private ChattyRepository chattyRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/api/public-chat/history")
    public List<ChatMessage> getPublicChatHistory() {
        return publicChatHistory;
    }

    @GetMapping("/api/users/online")
    public Set<String> getOnlineUsers() {
        return redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        if (!verifiedUsers.contains(chatMessage.getSender())) {
            return null;
        }
        publicChatHistory.add(chatMessage);
        return chatMessage;
    }

    @SendTo("/topic/public")
    @MessageMapping("/chat.addUser")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();
        String sessionId = headerAccessor.getSessionId();

        if (sessionId == null || !verifiedUsers.contains(username) || chattyRepository.findByUsername(username).isEmpty()) {
            return null;
        }

        boolean isAdded = redisTemplate.opsForSet().add(ONLINE_USERS_KEY, username) > 0;

        if (isAdded) {
            redisTemplate.opsForHash().put("active_sessions", sessionId, username);
            redisTemplate.opsForHash().put(USER_SESSIONS_KEY, username, sessionId);
            redisTemplate.opsForValue().set(HEARTBEAT_KEY_PREFIX + username, "active", 75, TimeUnit.SECONDS);

            messagingTemplate.convertAndSend("/topic/users", getOnlineUsers());

            ChatMessage joinMessage = new ChatMessage(username + " has joined the chat!", "System", ChatMessage.MessageType.JOIN);
            publicChatHistory.add(joinMessage);
            return joinMessage;
        }
        return null;
    }

    @MessageMapping("/heartbeat.pong")
    public void handlePong(@Payload ChatMessage message) {
        String username = message.getSender();
        if (username != null) {
            String heartbeatKey = HEARTBEAT_KEY_PREFIX + username;
            redisTemplate.opsForValue().set(heartbeatKey, "active", 75, TimeUnit.SECONDS);
        }
    }

    public void handleUserDisconnect(String sessionId) {
        String username = (String) redisTemplate.opsForHash().get("active_sessions", sessionId);

        if (username != null) {
            System.out.println("User disconnecting: " + username);

            redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, username);
            redisTemplate.opsForHash().delete("active_sessions", sessionId);
            redisTemplate.opsForHash().delete(USER_SESSIONS_KEY, username);
            redisTemplate.delete(HEARTBEAT_KEY_PREFIX + username);

            verificationService.removeVerificationCode(username);

            ChatMessage leaveMessage = new ChatMessage(username + " has left the chat.", "System", ChatMessage.MessageType.LEAVE);
            messagingTemplate.convertAndSend("/topic/public", leaveMessage);
            messagingTemplate.convertAndSend("/topic/users", getOnlineUsers());
            publicChatHistory.add(leaveMessage);
        }
    }

    public static void addVerifiedUser(String username) {
        verifiedUsers.add(username);
    }
}