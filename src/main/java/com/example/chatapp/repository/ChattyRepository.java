package com.example.chatapp.repository;

import com.example.chatapp.entity.ChattyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChattyRepository extends JpaRepository<ChattyEntity, Long> {
    ChattyEntity findByPhoneNumber(String phoneNumber);

    ChattyEntity findByUsername(String username);

    ChattyEntity findByEmail(String username);
}
