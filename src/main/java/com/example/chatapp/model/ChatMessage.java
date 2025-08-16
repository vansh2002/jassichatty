package com.example.chatapp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatMessage {
    private String content;
    private String sender;
    private MessageType type;

    public enum MessageType {
        CHAT,
        JOIN
    }

    // Default constructor for Jackson serialization
    public ChatMessage() {}

    @JsonCreator
    public ChatMessage(@JsonProperty("content") String content,
                       @JsonProperty("sender") String sender,
                       @JsonProperty("type") MessageType type) {
        this.content = content;
        this.sender = sender;
        this.type = type;
    }

    // Getters and setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }
}