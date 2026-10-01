package com.university.schedulebot.service;

import com.university.schedulebot.dto.BotResponse;
import com.university.schedulebot.entity.*;
import com.university.schedulebot.keyboard.InlineKeyboardFactory;
import com.university.schedulebot.repository.GroupRepository;
import com.university.schedulebot.repository.LessonRepository;
import com.university.schedulebot.repository.RoleRepository;
import com.university.schedulebot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeacherManagementService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final LessonRepository lessonRepository;
    private final RoleRepository roleRepository;
    private final InlineKeyboardFactory keyboards;
    private final Clock clock;

    @Transactional(readOnly = true)
    public BotResponse groupsWithStudents(User teacher) {
        if (!teacher.isTeacher()) return forbidden(teacher);

        List<Group> groups = groupRepository.findAllByOrderByNameAsc();
        if (groups.isEmpty()) {
            return BotResponse.text(teacher.getChatId(), "Список групп пуст.");
        }

        return BotResponse.of(
                teacher.getChatId(),
                "Выберите группу:",
                keyboards.studentsGroups(groups));
    }

    @Transactional(readOnly = true)
    public BotResponse studentsInGroup(User teacher, Long groupId) {
        if (!teacher.isTeacher()) return forbidden(teacher);

        Optional<Group> group = groupRepository.findById(groupId);
        if (group.isEmpty()) {
            return BotResponse.text(teacher.getChatId(), "Группа не найдена.");
        }

        List<User> students = userRepository.findAllByGroupIdAndRoleCodeOrderByFullNameAsc(
                groupId, Role.STUDENT);

        if (students.isEmpty()) {
            return BotResponse.text(teacher.getChatId(),
                    "В группе «" + group.get().getName() + "» пока нет студентов.");
        }

        StringBuilder text = new StringBuilder()
                .append("👥 *Студенты группы ")
                .append(group.get().getName())
                .append("*\n\n");

        for (User student : students) {
            text.append("• ")
                    .append(student.getFullName() == null ? "Без имени" : student.getFullName());

            if (student.getUsername() != null && !student.getUsername().isBlank()) {
                text.append(" (@").append(student.getUsername()).append(")");
            }

            if (student.getRegistrationState() == RegistrationState.PENDING_APPROVAL) {
                text.append(" — ожидает подтверждения");
            } else if (student.isRegistered()) {
                text.append(" — подтверждён");
            }

            text.append('\n');
        }

        return BotResponse.text(teacher.getChatId(), text.toString());
    }

    @Transactional(readOnly = true)
    public BotResponse pendingStudents(User teacher) {
        if (!teacher.isTeacher()) return forbidden(teacher);

        List<User> students =
                userRepository.findAllByRegistrationStateAndRoleCodeOrderByFullNameAsc(
                        RegistrationState.PENDING_APPROVAL, Role.STUDENT);

        if (students.isEmpty()) {
            return BotResponse.text(teacher.getChatId(),
                    "✅ Нет регистраций, ожидающих подтверждения.");
        }

        StringBuilder text = new StringBuilder("Ожидают подтверждения:\n\n");
        for (User student : students) {
            text.append("• ")
                    .append(student.getFullName() == null ? "Без имени" : student.getFullName())
                    .append(" — группа: ")
                    .append(student.getGroup() == null ? "не указана" : student.getGroup().getName())
                    .append('\n');
        }

        return BotResponse.of(teacher.getChatId(), text.toString(),
                keyboards.pendingStudents(students));
    }

    @Transactional
    public BotResponse approveStudent(User teacher, Long studentId) {
        if (!teacher.isTeacher()) return forbidden(teacher);

        Optional<User> found = userRepository.findById(studentId);
        if (found.isEmpty()) {
            return BotResponse.text(teacher.getChatId(), "Пользователь не найден.");
        }

        User student = found.get();
        if (!student.isStudent()
                || student.getRegistrationState() != RegistrationState.PENDING_APPROVAL) {
            return BotResponse.text(teacher.getChatId(),
                    "Эта регистрация уже подтверждена или не ожидает подтверждения.");
        }

        student.setRegistrationState(RegistrationState.COMPLETED);
        student.setRegisteredAt(LocalDateTime.now(clock));
        userRepository.save(student);

        return BotResponse.text(teacher.getChatId(),
                "✅ Регистрация студента "
                        + (student.getFullName() == null ? "" : student.getFullName() + " ")
                        + "подтверждена.");
    }

    @Transactional
    public BotResponse createGroup(User teacher, String name) {
        if (!teacher.isTeacher()) return forbidden(teacher);

        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isBlank()) {
            return BotResponse.text(teacher.getChatId(),
                    "Введите название группы после команды /group_add.\n"
                            + "Например: /group_add ИВТ-21");
        }
        if (cleanName.length() > 64) {
            return BotResponse.text(teacher.getChatId(),
                    "Название группы не должно быть длиннее 64 символов.");
        }
        if (groupRepository.findByNameIgnoreCase(cleanName).isPresent()) {
            return BotResponse.text(teacher.getChatId(),
                    "Группа «" + cleanName + "» уже существует.");
        }

        teacher.setRegistrationState(RegistrationState.COMPLETED);
        userRepository.save(teacher);

        groupRepository.save(Group.builder().name(cleanName).build());
        return BotResponse.text(teacher.getChatId(),
                "✅ Группа «" + cleanName + "» создана.");
    }

    @Transactional
    public BotResponse promptCreateGroup(User teacher) {
        if (!teacher.isTeacher() || !teacher.isRegistered()) {
            return BotResponse.text(teacher.getChatId(),
                    "⛔ Это действие доступно только зарегистрированному преподавателю.");
        }

        teacher.setRegistrationState(RegistrationState.AWAITING_GROUP_NAME);
        userRepository.save(teacher);

        return BotResponse.text(teacher.getChatId(),
                "Введите название новой группы:");
    }

    @Transactional(readOnly = true)
    public BotResponse groupsForDeletion(User teacher) {
        if (!teacher.isTeacher()) return forbidden(teacher);

        List<Group> groups = groupRepository.findAllByOrderByNameAsc();
        if (groups.isEmpty()) {
            return BotResponse.text(teacher.getChatId(), "Удалять нечего: групп нет.");
        }

        return BotResponse.of(
                teacher.getChatId(),
                "Выберите группу для удаления. Группы со студентами или занятиями удалить нельзя.",
                keyboards.groupsToDelete(groups));
    }

    @Transactional
    public BotResponse deleteGroup(User teacher, Long groupId) {
        if (!teacher.isTeacher()) return forbidden(teacher);

        Optional<Group> found = groupRepository.findById(groupId);
        if (found.isEmpty()) {
            return BotResponse.text(teacher.getChatId(), "Группа не найдена.");
        }

        if (userRepository.existsByGroupId(groupId)
                || lessonRepository.existsByGroupId(groupId)) {
            return BotResponse.text(teacher.getChatId(),
                    "⛔ Нельзя удалить группу «" + found.get().getName()
                            + "»: к ней привязаны студенты или занятия.");
        }

        groupRepository.delete(found.get());
        return BotResponse.text(teacher.getChatId(),
                "🗑 Группа «" + found.get().getName() + "» удалена.");
    }

    private BotResponse forbidden(User teacher) {
        return BotResponse.text(teacher.getChatId(),
                "⛔ Это действие доступно только зарегистрированному преподавателю.");
    }
}