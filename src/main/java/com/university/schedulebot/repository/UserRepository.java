package com.university.schedulebot.repository;

import com.university.schedulebot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.university.schedulebot.entity.RegistrationState;
import java.util.List;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByChatId(Long chatId);
    boolean existsByChatId(Long chatId);
    void deleteByChatId(Long chatId);
    List<User> findAllByGroupIdAndRoleCodeOrderByFullNameAsc(Long groupId, String roleCode);
    List<User> findAllByRegistrationStateAndRoleCodeOrderByFullNameAsc(
            RegistrationState state, String roleCode);
    boolean existsByGroupId(Long groupId);
}