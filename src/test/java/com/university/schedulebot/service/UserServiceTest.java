package com.university.schedulebot.service;

import com.university.schedulebot.dto.UserRequest;
import com.university.schedulebot.entity.*;
import com.university.schedulebot.repository.LessonRepository;
import com.university.schedulebot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private TimeZoneService timeZoneService;

    private Clock clock;
    private UserService userService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                Instant.parse("2024-01-15T12:30:00Z"),
                ZoneOffset.UTC
        );

        userService = new UserService(
                userRepository,
                lessonRepository,
                timeZoneService,
                clock
        );
    }

    @Test
    void find_shouldReturnUserByChatId() {
        Long chatId = 100L;
        User user = User.builder()
                .id(1L)
                .chatId(chatId)
                .build();

        when(userRepository.findByChatId(chatId))
                .thenReturn(Optional.of(user));

        Optional<User> result = userService.find(chatId);

        assertThat(result).contains(user);
        verify(userRepository).findByChatId(chatId);
    }

    @Test
    void find_shouldReturnEmptyWhenUserNotFound() {
        Long chatId = 100L;

        when(userRepository.findByChatId(chatId))
                .thenReturn(Optional.empty());

        Optional<User> result = userService.find(chatId);

        assertThat(result).isEmpty();
        verify(userRepository).findByChatId(chatId);
    }

    @Test
    void isRegistered_shouldReturnTrueForRegisteredUser() {
        Long chatId = 100L;

        User user = mock(User.class);
        when(user.isRegistered()).thenReturn(true);
        when(userRepository.findByChatId(chatId))
                .thenReturn(Optional.of(user));

        boolean result = userService.isRegistered(chatId);

        assertThat(result).isTrue();
        verify(userRepository).findByChatId(chatId);
        verify(user).isRegistered();
    }

    @Test
    void isRegistered_shouldReturnFalseForUnregisteredUser() {
        Long chatId = 100L;

        User user = mock(User.class);
        when(user.isRegistered()).thenReturn(false);
        when(userRepository.findByChatId(chatId))
                .thenReturn(Optional.of(user));

        boolean result = userService.isRegistered(chatId);

        assertThat(result).isFalse();
    }

    @Test
    void isRegistered_shouldReturnFalseWhenUserNotFound() {
        Long chatId = 100L;

        when(userRepository.findByChatId(chatId))
                .thenReturn(Optional.empty());

        boolean result = userService.isRegistered(chatId);

        assertThat(result).isFalse();
    }

    @Test
    void findOrCreate_shouldReturnExistingUser() {
        Long chatId = 100L;

        UserRequest request = mock(UserRequest.class);
        when(request.getChatId()).thenReturn(chatId);

        User existingUser = User.builder()
                .id(1L)
                .chatId(chatId)
                .build();

        when(userRepository.findByChatId(chatId))
                .thenReturn(Optional.of(existingUser));

        User result = userService.findOrCreate(request);

        assertThat(result).isSameAs(existingUser);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findOrCreate_shouldCreateDraftUserWhenNotFound() {
        Long chatId = 100L;
        String username = "john";
        String fullName = "John Doe";

        UserRequest request = mock(UserRequest.class);
        when(request.getChatId()).thenReturn(chatId);
        when(request.getUsername()).thenReturn(username);
        when(request.getFullName()).thenReturn(fullName);

        when(userRepository.findByChatId(chatId))
                .thenReturn(Optional.empty());

        User savedUser = User.builder()
                .id(1L)
                .chatId(chatId)
                .username(username)
                .fullName(fullName)
                .registrationState(RegistrationState.NONE)
                .zoneId(TimeZoneService.DEFAULT_ZONE)
                .build();

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        User result = userService.findOrCreate(request);

        assertThat(result).isSameAs(savedUser);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User createdUser = captor.getValue();

        assertThat(createdUser.getChatId()).isEqualTo(chatId);
        assertThat(createdUser.getUsername()).isEqualTo(username);
        assertThat(createdUser.getFullName()).isEqualTo(fullName);
        assertThat(createdUser.getRegistrationState())
                .isEqualTo(RegistrationState.NONE);
        assertThat(createdUser.getZoneId())
                .isEqualTo(TimeZoneService.DEFAULT_ZONE);
    }

    @Test
    void save_shouldDelegateToRepository() {
        User user = User.builder()
                .id(1L)
                .chatId(100L)
                .build();

        when(userRepository.save(user)).thenReturn(user);

        User result = userService.save(user);

        assertThat(result).isSameAs(user);
        verify(userRepository).save(user);
    }

    @Test
    void deleteAllUserData_shouldReturnFalseWhenUserNotFound() {
        Long chatId = 100L;

        when(userRepository.findByChatId(chatId))
                .thenReturn(Optional.empty());

        boolean result = userService.deleteAllUserData(chatId);

        assertThat(result).isFalse();

        verify(lessonRepository, never()).deleteAllByTeacherId(anyLong());
        verify(lessonRepository, never()).deleteAllByCreatedById(anyLong());
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteAllUserData_shouldDeleteLessonsAndUser() {
        Long chatId = 100L;
        Long userId = 10L;

        User user = User.builder()
                .id(userId)
                .chatId(chatId)
                .build();

        when(userRepository.findByChatId(chatId))
                .thenReturn(Optional.of(user));

        boolean result = userService.deleteAllUserData(chatId);

        assertThat(result).isTrue();

        InOrder inOrder = inOrder(
                lessonRepository,
                userRepository
        );

        inOrder.verify(lessonRepository).deleteAllByTeacherId(userId);
        inOrder.verify(lessonRepository).deleteAllByCreatedById(userId);
        inOrder.verify(userRepository).delete(user);
    }

    @Test
    void profileText_shouldReturnStudentProfile() {
        User user = User.builder()
                .fullName("Иван Иванов")
                .location("Москва")
                .zoneId("Europe/Moscow")
                .registeredAt(LocalDateTime.of(2024, 1, 10, 15, 20))
                .build();

        when(timeZoneService.offsetLabel("Europe/Moscow", clock))
                .thenReturn("UTC+03:00");

        String result = userService.profileText(user);

        assertThat(result)
                .contains("👤 *Профиль*")
                .contains("Имя: Иван Иванов")
                .contains("Роль: Студент")
                .contains("Город: Москва")
                .contains("Часовой пояс: Europe/Moscow (UTC+03:00)")
                .contains("Зарегистрирован: 2024-01-10");

        verify(timeZoneService)
                .offsetLabel("Europe/Moscow", clock);
    }

    @Test
    void profileText_shouldReturnTeacherProfileWithDepartmentAndGroup() {
        Group group = mock(Group.class);
        when(group.getName()).thenReturn("ИВТ-21");

        Department department = mock(Department.class);
        when(department.getName()).thenReturn("Кафедра информатики");

        User user = mock(User.class);

        when(user.getFullName()).thenReturn("Петр Петров");
        when(user.isTeacher()).thenReturn(true);
        when(user.getGroup()).thenReturn(group);
        when(user.getDepartment()).thenReturn(department);
        when(user.getLocation()).thenReturn("Казань");
        when(user.getZoneId()).thenReturn("Europe/Moscow");
        when(user.getRegisteredAt())
                .thenReturn(LocalDateTime.of(2024, 2, 1, 10, 0));

        when(timeZoneService.offsetLabel("Europe/Moscow", clock))
                .thenReturn("UTC+03:00");

        String result = userService.profileText(user);

        assertThat(result)
                .contains("Имя: Петр Петров")
                .contains("Роль: Преподаватель")
                .contains("Группа: ИВТ-21")
                .contains("Кафедра: Кафедра информатики")
                .contains("Город: Казань")
                .contains("Часовой пояс: Europe/Moscow (UTC+03:00)")
                .contains("Зарегистрирован: 2024-02-01");
    }

    @Test
    void profileText_shouldReplaceBlankValuesWithDash() {
        User user = mock(User.class);

        when(user.getFullName()).thenReturn(" ");
        when(user.isTeacher()).thenReturn(false);
        when(user.getGroup()).thenReturn(null);
        when(user.getDepartment()).thenReturn(null);
        when(user.getLocation()).thenReturn("");
        when(user.getZoneId()).thenReturn("UTC");
        when(user.getRegisteredAt()).thenReturn(null);

        when(timeZoneService.offsetLabel("UTC", clock))
                .thenReturn("UTC+00:00");

        String result = userService.profileText(user);

        assertThat(result)
                .contains("Имя: —")
                .contains("Роль: Студент")
                .contains("Город: —")
                .contains("Часовой пояс: UTC (UTC+00:00)")
                .doesNotContain("Зарегистрирован:");
    }

    @Test
    void nowUtc_shouldReturnCurrentTimeFromClock() {
        LocalDateTime result = userService.nowUtc();

        assertThat(result)
                .isEqualTo(LocalDateTime.of(2024, 1, 15, 12, 30));
    }
}