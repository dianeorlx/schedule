package com.university.schedulebot.repository;

import com.university.schedulebot.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class DatabaseRepositoryTest {

    @Autowired private RoleRepository roleRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private LessonRepository lessonRepository;

    private Role studentRole;
    private Role teacherRole;
    private Department department;
    private Group group;
    private User teacher;

    @BeforeEach
    void setUp() {
        studentRole = roleRepository.findByCode(Role.STUDENT)
                .orElseThrow(() -> new IllegalStateException(
                        "В миграции не найдена роль STUDENT"));

        teacherRole = roleRepository.findByCode(Role.TEACHER)
                .orElseThrow(() -> new IllegalStateException(
                        "В миграции не найдена роль TEACHER"));

        department = departmentRepository.save(
                Department.builder().name("Тестовая кафедра").build());

        group = groupRepository.save(
                Group.builder()
                        .name("TEST-DB-01")
                        .department(department)
                        .build());

        teacher = userRepository.save(User.builder()
                .chatId(70001L)
                .fullName("Тестовый преподаватель")
                .role(teacherRole)
                .department(department)
                .registrationState(RegistrationState.COMPLETED)
                .zoneId("Europe/Moscow")
                .build());
    }

    @Test
    void savesAndLoadsUserByChatId() {
        User student = userRepository.save(User.builder()
                .chatId(70002L)
                .fullName("Тестовый студент")
                .role(studentRole)
                .group(group)
                .registrationState(RegistrationState.PENDING_APPROVAL)
                .zoneId("Europe/Moscow")
                .build());

        User loaded = userRepository.findByChatId(70002L).orElseThrow();

        assertEquals(student.getId(), loaded.getId());
        assertEquals(RegistrationState.PENDING_APPROVAL,
                loaded.getRegistrationState());
        assertEquals(group.getId(), loaded.getGroup().getId());
    }

    @Test
    void findsLessonsByGroupAndDateRange() {
        LocalDate date = LocalDate.now().plusDays(2);

        Lesson saved = lessonRepository.save(Lesson.builder()
                .title("Тестовое занятие")
                .lessonDate(date)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .status(LessonStatus.ACTIVE)
                .group(group)
                .department(department)
                .teacher(teacher)
                .createdBy(teacher)
                .build());

        var result = lessonRepository.findByGroupAndPeriod(
                group.getId(), date, date);

        assertEquals(1, result.size());
        assertEquals(saved.getId(), result.get(0).getId());
    }

    @Test
    void findsLessonsByTeacherAndDateRange() {
        LocalDate date = LocalDate.now().plusDays(3);

        lessonRepository.save(Lesson.builder()
                .title("Занятие преподавателя")
                .lessonDate(date)
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(13, 0))
                .status(LessonStatus.ACTIVE)
                .group(group)
                .department(department)
                .teacher(teacher)
                .createdBy(teacher)
                .build());

        var result = lessonRepository.findByTeacherAndPeriod(
                teacher.getId(), date, date);

        assertEquals(1, result.size());
        assertEquals("Занятие преподавателя", result.get(0).getTitle());
    }

    @Test
    void detectsGroupsUsedByStudentsAndLessons() {
        userRepository.save(User.builder()
                .chatId(70003L)
                .fullName("Студент группы")
                .role(studentRole)
                .group(group)
                .registrationState(RegistrationState.PENDING_APPROVAL)
                .zoneId("Europe/Moscow")
                .build());

        assertTrue(userRepository.existsByGroupId(group.getId()));

        LocalDate date = LocalDate.now().plusDays(4);
        lessonRepository.save(Lesson.builder()
                .title("Занятие группы")
                .lessonDate(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .status(LessonStatus.ACTIVE)
                .group(group)
                .department(department)
                .teacher(teacher)
                .createdBy(teacher)
                .build());

        assertTrue(lessonRepository.existsByGroupId(group.getId()));
    }

    @Test
    void cancelledLessonIsSavedWithCancelledStatus() {
        LocalDate date = LocalDate.now().plusDays(5);

        Lesson lesson = lessonRepository.save(Lesson.builder()
                .title("Отменённое занятие")
                .lessonDate(date)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .status(LessonStatus.CANCELLED)
                .group(group)
                .department(department)
                .teacher(teacher)
                .createdBy(teacher)
                .build());

        Lesson loaded = lessonRepository.findById(lesson.getId()).orElseThrow();

        assertEquals(LessonStatus.CANCELLED, loaded.getStatus());
    }
}