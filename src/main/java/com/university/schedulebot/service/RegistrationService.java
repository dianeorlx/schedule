package com.university.schedulebot.service;

import com.university.schedulebot.dto.BotResponse;
import com.university.schedulebot.dto.UserRequest;
import com.university.schedulebot.entity.*;
import com.university.schedulebot.keyboard.InlineKeyboardFactory;
import com.university.schedulebot.keyboard.ReplyKeyboardFactory;
import com.university.schedulebot.repository.DepartmentRepository;
import com.university.schedulebot.repository.GroupRepository;
import com.university.schedulebot.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final DepartmentRepository departmentRepository;
    private final TimeZoneService timeZoneService;
    private final InlineKeyboardFactory inlineKeyboards;
    private final ReplyKeyboardFactory replyKeyboards;
    private final Clock clock;

    /** Шаг 0 — /register. */
    @Transactional
    public BotResponse start(UserRequest request) {
        User user = userService.findOrCreate(request);
        if (user.isRegistered()) {
            return BotResponse.of(request.getChatId(),
                    "✅ Вы уже зарегистрированы. Чтобы изменить данные, используйте /delete_me и зарегистрируйтесь заново.",
                    menuFor(user));
        }
        if (user.getRegistrationState() == RegistrationState.PENDING_APPROVAL) {
            return BotResponse.of(request.getChatId(),
                    "🕓 Ваша регистрация уже ожидает подтверждения преподавателем.",
                    replyKeyboards.guestMenu());
        }
        user.setRegistrationState(RegistrationState.AWAITING_ROLE);
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        userService.save(user);

        return BotResponse.of(request.getChatId(),
                "👤 Выберите вашу роль:",
                inlineKeyboards.roleSelection());
    }

    /** Шаг 1 — выбор роли через inline-кнопку. */
    @Transactional
    public BotResponse handleRoleSelection(UserRequest request, String roleCode) {
        Optional<User> found = userService.find(request.getChatId());
        if (found.isEmpty()) {
            return BotResponse.text(request.getChatId(), "⚠️ Сначала выполните /register");
        }
        User user = found.get();
        if (user.getRegistrationState() != RegistrationState.AWAITING_ROLE) {
            return BotResponse.text(request.getChatId(),
                    "⚠️ Этот шаг уже пройден. Используйте /register.");
        }
        Optional<Role> role = roleRepository.findByCode(roleCode);
        if (role.isEmpty()) {
            return BotResponse.text(request.getChatId(), "⚠️ Неизвестная роль.");
        }

        user.setRole(role.get());

        if (Role.STUDENT.equals(roleCode)) {
            user.setRegistrationState(RegistrationState.AWAITING_GROUP);
            userService.save(user);

            List<Group> groups = groupRepository.findAllByOrderByNameAsc();
            if (groups.isEmpty()) {
                return BotResponse.text(request.getChatId(),
                        "⚠️ Список групп пуст. Обратитесь к администратору.");
            }
            return BotResponse.of(request.getChatId(),
                    "🎓 Выберите вашу группу:", inlineKeyboards.groups(groups));
        }

        user.setRegistrationState(RegistrationState.AWAITING_DEPARTMENT);
        userService.save(user);

        List<Department> departments = departmentRepository.findAllByOrderByNameAsc();
        if (departments.isEmpty()) {
            return BotResponse.text(request.getChatId(),
                    "⚠️ Список кафедр пуст. Обратитесь к администратору.");
        }
        return BotResponse.of(request.getChatId(),
                "👨‍🏫 Выберите вашу кафедру:", inlineKeyboards.departments(departments));
    }

    /** Шаг 2а — выбор группы. */
    @Transactional
    public BotResponse handleGroupSelection(UserRequest request, Long groupId) {
        Optional<User> found = userService.find(request.getChatId());
        if (found.isEmpty() || found.get().getRegistrationState() != RegistrationState.AWAITING_GROUP) {
            return BotResponse.text(request.getChatId(), "⚠️ Некорректный шаг регистрации.");
        }
        Optional<Group> group = groupRepository.findById(groupId);
        if (group.isEmpty()) {
            return BotResponse.text(request.getChatId(), "⚠️ Группа не найдена.");
        }
        User user = found.get();
        user.setGroup(group.get());
        user.setDepartment(group.get().getDepartment());
        user.setRegistrationState(RegistrationState.AWAITING_LOCATION);
        userService.save(user);

        return BotResponse.text(request.getChatId(), locationPrompt());
    }

    /** Шаг 2б — выбор кафедры. */
    @Transactional
    public BotResponse handleDepartmentSelection(UserRequest request, Long departmentId) {
        Optional<User> found = userService.find(request.getChatId());
        if (found.isEmpty() || found.get().getRegistrationState() != RegistrationState.AWAITING_DEPARTMENT) {
            return BotResponse.text(request.getChatId(), "⚠️ Некорректный шаг регистрации.");
        }
        Optional<Department> dept = departmentRepository.findById(departmentId);
        if (dept.isEmpty()) {
            return BotResponse.text(request.getChatId(), "⚠️ Кафедра не найдена.");
        }
        User user = found.get();
        user.setDepartment(dept.get());
        user.setRegistrationState(RegistrationState.AWAITING_LOCATION);
        userService.save(user);

        return BotResponse.text(request.getChatId(), locationPrompt());
    }

    /** Шаг 3 — ввод местоположения → часовой пояс (Задача 3). */
    @Transactional
    public BotResponse handleLocation(UserRequest request) {
        Optional<User> found = userService.find(request.getChatId());
        if (found.isEmpty() || found.get().getRegistrationState() != RegistrationState.AWAITING_LOCATION) {
            return BotResponse.text(request.getChatId(), "⚠️ Некорректный шаг регистрации.");
        }
        Optional<ZoneId> zone = timeZoneService.resolve(request.getText());
        if (zone.isEmpty()) {
            return BotResponse.text(request.getChatId(),
                    "❌ Не удалось определить часовой пояс для «" + request.getText() + "».\n"
                            + "Попробуйте другой город (например: Москва, Новосибирск) "
                            + "или введите ZoneId вида Asia/Tokyo.");
        }
        User user = found.get();
        user.setLocation(request.getText().trim());
        user.setZoneId(zone.get().getId());
        if (user.isStudent()) {
            user.setRegistrationState(RegistrationState.PENDING_APPROVAL);
            userService.save(user);

            return BotResponse.of(request.getChatId(),
                    "🕓 Данные отправлены преподавателю на подтверждение. "
                            + "После подтверждения вам станет доступно расписание.",
                    replyKeyboards.guestMenu());
        }

        user.setRegistrationState(RegistrationState.COMPLETED);
        user.setRegisteredAt(LocalDateTime.now(clock));
        userService.save(user);

        String where = user.isTeacher()
                ? "Кафедра: " + user.getDepartment().getName()
                : "Группа: " + user.getGroup().getName();

        return BotResponse.of(request.getChatId(), """
                ✅ Регистрация завершена!

                Роль: %s
                %s
                Город: %s
                Часовой пояс: %s (%s)

                Посмотреть расписание — /show"""
                        .formatted(user.isTeacher() ? "Преподаватель" : "Студент",
                                where,
                                user.getLocation(),
                                user.getZoneId(),
                                timeZoneService.offsetLabel(user.getZoneId(), clock)),
                menuFor(user));
    }

    public boolean isAwaitingInput(User user) {
        return user != null && user.getRegistrationState() == RegistrationState.AWAITING_LOCATION;
    }

    private String locationPrompt() {
        return """
               🌍 Введите ваш город — он нужен для определения часового пояса.

               Например: Москва, Екатеринбург, Новосибирск, Владивосток.""";
    }

    private org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup menuFor(User user) {
        if (!user.isRegistered()) return replyKeyboards.guestMenu();
        return user.isTeacher() ? replyKeyboards.teacherMenu() : replyKeyboards.studentMenu();
    }
}