package com.university.schedulebot.repository;

import com.university.schedulebot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByChatId(Long chatId);
    boolean existsByChatId(Long chatId);
    void deleteByChatId(Long chatId);
}