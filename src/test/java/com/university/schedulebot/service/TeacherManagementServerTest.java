package com.university.schedulebot.service;

import com.university.schedulebot.dto.BotResponse;
import com.university.schedulebot.entity.*;
import com.university.schedulebot.keyboard.InlineKeyboardFactory;
import com.university.schedulebot.repository.GroupRepository;
import com.university.schedulebot.repository.LessonRepository;
import com.university.schedulebot.repository.RoleRepository;
import com.university.schedulebot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private InlineKeyboardFactory keyboards;

    private TeacherManagementService service;
    private User teacher;

    @Mock
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2025-01-01T12:00:00Z"),
                ZoneOffset.UTC
        );

        service = new TeacherManagementService(
                userRepository,
                groupRepository,
                lessonRepository,
                roleRepository,
                keyboards,
                clock
        );

        teacher = User.builder()
                .id(10L)
                .chatId(123L)
                .role(Role.builder().code(Role.TEACHER).build())
                .registrationState(RegistrationState.COMPLETED)
                .build();
    }

    @Test
    void approveStudentChangesStateToCompleted() {
        User student = User.builder()
                .id(20L)
                .chatId(456L)
                .fullName("Иван Иванов")
                .role(Role.builder().code(Role.STUDENT).build())
                .registrationState(RegistrationState.PENDING_APPROVAL)
                .build();

        when(userRepository.findById(20L)).thenReturn(Optional.of(student));

        BotResponse response = service.approveStudent(teacher, 20L);

        assertEquals(RegistrationState.COMPLETED, student.getRegistrationState());
        assertNotNull(student.getRegisteredAt());
        assertTrue(response.getText().contains("подтверждена"));

        verify(userRepository).save(student);
    }

    @Test
    void approveStudentRejectsUserWhoIsNotPending() {
        User student = User.builder()
                .id(20L)
                .role(Role.builder().code(Role.STUDENT).build())
                .registrationState(RegistrationState.COMPLETED)
                .build();

        when(userRepository.findById(20L)).thenReturn(Optional.of(student));

        BotResponse response = service.approveStudent(teacher, 20L);

        assertEquals(RegistrationState.COMPLETED, student.getRegistrationState());
        assertTrue(response.getText().contains("уже подтверждена")
                || response.getText().contains("не ожидает"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void nonTeacherCannotApproveStudent() {
        User studentActingAsTeacher = User.builder()
                .id(30L)
                .chatId(789L)
                .role(Role.builder().code(Role.STUDENT).build())
                .registrationState(RegistrationState.COMPLETED)
                .build();

        BotResponse response = service.approveStudent(studentActingAsTeacher, 20L);

        assertNotNull(response);
        assertFalse(response.getText().isBlank());
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteGroupIsRejectedWhenStudentsAreAssigned() {
        Group group = Group.builder()
                .id(3L)
                .name("ИВТ-21")
                .build();

        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(userRepository.existsByGroupId(3L)).thenReturn(true);

        BotResponse response = service.deleteGroup(teacher, 3L);

        assertTrue(response.getText().contains("Нельзя удалить группу"));
        verify(groupRepository, never()).delete(any(Group.class));
        verify(lessonRepository, never()).existsByGroupId(3L);
    }

    @Test
    void deleteGroupIsRejectedWhenLessonsAreAssigned() {
        Group group = Group.builder()
                .id(3L)
                .name("ИВТ-21")
                .build();

        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(userRepository.existsByGroupId(3L)).thenReturn(false);
        when(lessonRepository.existsByGroupId(3L)).thenReturn(true);

        BotResponse response = service.deleteGroup(teacher, 3L);

        assertTrue(response.getText().contains("Нельзя удалить группу"));
        verify(groupRepository, never()).delete(any(Group.class));
    }

    @Test
    void deleteEmptyGroupSucceeds() {
        Group group = Group.builder()
                .id(3L)
                .name("ИВТ-21")
                .build();

        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(userRepository.existsByGroupId(3L)).thenReturn(false);
        when(lessonRepository.existsByGroupId(3L)).thenReturn(false);

        BotResponse response = service.deleteGroup(teacher, 3L);

        assertTrue(response.getText().contains("удалена"));
        verify(groupRepository).delete(group);
    }
}