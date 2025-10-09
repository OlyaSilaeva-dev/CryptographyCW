package com.cryptography.messenger.repository;

import com.cryptography.messenger.enity.Chat;
import com.cryptography.messenger.enity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    public List<Chat> getChatsByFirstUser(User user);
    public List<Chat> getChatsBySecondUser(User user);
}
