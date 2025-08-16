package com.example.chatapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ChattyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Column(unique = true, name = "USERNAME")
    private String username;

    @Column(unique = true, name = "EMAIL")
    private String email;

    @Column(unique = true, name = "PHONE_NUMBER")
    private String phoneNumber;
}
