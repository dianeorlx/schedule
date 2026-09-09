package com.university.schedulebot.service;

import com.university.schedulebot.dto.UserRequest;
import com.university.schedulebot.entity.*;
import com.university.schedulebot.repository.LessonRepository;
import com.university.schedulebot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final TimeZoneService timeZoneService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Optional<User> find(Long chatId) {
        return userRepository.findByChatId(chatId);
    }

    @Transactional(readOnly = true)
    public boolean isRegistered(Long chatId) {
        return userRepository.findByChatId(chatId)
                .map(User::isRegistered)
                .orElse(false);
    }

    /** Находит пользователя или создаёт «черновик». */
    @Transactional
    public User findOrCreate(UserRequest request) {
        return userRepository.findByChatId(request.getChatId())
                .orElseGet(() -> userRepository.save(User.builder()
                        .chatId(request.getChatId())
                        .username(request.getUsername())
                        .fullName(request.getFullName())
                        .registrationState(RegistrationState.NONE)
                        .zoneId(TimeZoneService.DEFAULT_ZONE)
                        .build()));
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Полное удаление данных пользователя:
     * сначала — связанные занятия, затем — сам пользователь.
     */
    @Transactional
    public boolean deleteAllUserData(Long chatId) {
        Optional<User> found = userRepository.findByChatId(chatId);
        if (found.isEmpty()) {
            return false;
        }
        User user = found.get();
        lessonRepository.deleteAllByTeacherId(user.getId());
        lessonRepository.deleteAllByCreatedById(user.getId());
        userRepository.delete(user);
        return true;
    }

    @Transactional(readOnly = true)
    public String profileText(User user) {
        StringBuilder sb = new StringBuilder("👤 *Профиль*\n\n");
        sb.append("Имя: ").append(nvl(user.getFullName())).append('\n');
        sb.append("Роль: ").append(user.isTeacher() ? "Преподаватель" : "Студент").append('\n');
        if (user.getGroup() != null) {
            sb.append("Группа: ").append(user.getGroup().getName()).append('\n');
        }
        if (user.getDepartment() != null) {
            sb.append("Кафедра: ").append(user.getDepartment().getName()).append('\n');
        }
        sb.append("Город: ").append(nvl(user.getLocation())).append('\n');
        sb.append("Часовой пояс: ").append(user.getZoneId())
                .append(" (").append(timeZoneService.offsetLabel(user.getZoneId(), clock)).append(")\n");
        if (user.getRegisteredAt() != null) {
            sb.append("Зарегистрирован: ").append(user.getRegisteredAt().toLocalDate());
        }
        return sb.toString();
    }

    private String nvl(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    public LocalDateTime nowUtc() {
        return LocalDateTime.now(clock);
    }
}