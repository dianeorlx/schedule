package com.university.schedulebot.service;

import com.university.schedulebot.dto.BotResponse;
import com.university.schedulebot.dto.UserRequest;
import com.university.schedulebot.entity.RegistrationState;
import com.university.schedulebot.entity.User;
import com.university.schedulebot.keyboard.InlineKeyboardFactory;
import com.university.schedulebot.keyboard.ReplyKeyboardFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UpdateDispatcher {

    private final CommandService commandService;
    private final RegistrationService registrationService;
    private final UserService userService;
    private final ScheduleService scheduleService;
    private final LessonManagementService lessonService;
    private final DateParserService dateParser;
    private final InlineKeyboardFactory inlineKeyboards;
    private final TeacherManagementService teacherManagementService;

    public BotResponse dispatch(UserRequest request) {
        return request.isCallback() ? handleCallback(request) : handleText(request);
    }

    private BotResponse handleText(UserRequest request) {
        Long chatId = request.getChatId();
        String text = request.getText() == null ? "" : request.getText().trim();
        Optional<User> user = userService.find(chatId);

        // Незавершённая регистрация: ожидание города
        if (user.isPresent() && registrationService.isAwaitingInput(user.get())
                && !text.startsWith("/")) {
            return registrationService.handleLocation(request);
        }
        if (user.isPresent()
                && user.get().getRegistrationState() == RegistrationState.AWAITING_GROUP_NAME
                && !text.startsWith("/")) {
            return teacherManagementService.createGroup(user.get(), text);
        }

        String command = text.split("\\s+")[0].toLowerCase();
        String args = text.length() > command.length() ? text.substring(command.length()).trim() : "";

        return switch (normalize(command, text)) {
            case "/start"    -> commandService.start(chatId, user);
            case "/help"     -> commandService.help(chatId, user);
            case "/info"     -> commandService.info(chatId, user);
            case "/register" -> registrationService.start(request);
            case "/profile"  -> requireUser(chatId, user,
                    u -> BotResponse.of(chatId, userService.profileText(u), commandService.menuFor(user)));
            case "/show"     -> requireUser(chatId, user, scheduleService::show);
            case "/schedule" -> requireUser(chatId, user, u -> schedule(u, args));
            case "/addlesson"  -> requireUser(chatId, user, u -> lessonService.create(u, args));
            case "/mylessons"  -> requireUser(chatId, user, lessonService::myLessons);
            case "/editlesson" -> requireUser(chatId, user, u -> editLesson(u, args));
            case "/cancellesson" -> requireUser(chatId, user, u -> cancelLesson(u, args));
            case "/delete_me"  -> BotResponse.of(chatId,
                    "⚠️ Все ваши данные (профиль, занятия) будут удалены безвозвратно. Продолжить?",
                    inlineKeyboards.deleteConfirmation());
            case "/students" -> requireUser(chatId, user,
                    teacherManagementService::groupsWithStudents);

            case "/pending_students" -> requireUser(chatId, user,
                    teacherManagementService::pendingStudents);

            case "/group_add" -> requireTeacher(chatId, user,
                    teacherManagementService::promptCreateGroup);

            case "/group_delete" -> requireUser(chatId, user,
                    teacherManagementService::groupsForDeletion);

            default -> commandService.unknown(chatId, user);
        };
    }

    /** Reply-кнопки маппятся на команды. */
    private String normalize(String command, String fullText) {
        String normalizedText = fullText.trim();

        return switch (normalizedText) {
            case ReplyKeyboardFactory.BTN_HELP -> "/help";
            case ReplyKeyboardFactory.BTN_INFO -> "/info";
            case ReplyKeyboardFactory.BTN_SCHEDULE -> "/show";
            case ReplyKeyboardFactory.BTN_PROFILE -> "/profile";
            case ReplyKeyboardFactory.BTN_DELETE -> "/delete_me";
            case ReplyKeyboardFactory.BTN_ADD_LESSON -> "/addlesson";
            case ReplyKeyboardFactory.BTN_MANAGE_LESSON -> "/mylessons";
            case ReplyKeyboardFactory.BTN_STUDENTS -> "/students";
            case ReplyKeyboardFactory.BTN_PENDING_STUDENTS -> "/pending_students";
            case ReplyKeyboardFactory.BTN_CREATE_GROUP -> "/group_add";
            case ReplyKeyboardFactory.BTN_DELETE_GROUP -> "/group_delete";
            default -> command;
        };
    }

    private BotResponse handleCallback(UserRequest request) {
        String data = request.getCallbackData();
        Long chatId = request.getChatId();
        Optional<User> user = userService.find(chatId);

        if (data.startsWith("ROLE:")) {
            return registrationService.handleRoleSelection(request, data.substring(5));
        }
        if (data.startsWith(InlineKeyboardFactory.CB_GROUP_PREFIX)) {
            return registrationService.handleGroupSelection(request,
                    Long.parseLong(data.substring(InlineKeyboardFactory.CB_GROUP_PREFIX.length())));
        }
        if (data.startsWith(InlineKeyboardFactory.CB_DEPT_PREFIX)) {
            return registrationService.handleDepartmentSelection(request,
                    Long.parseLong(data.substring(InlineKeyboardFactory.CB_DEPT_PREFIX.length())));
        }
        if (data.startsWith("SHOW:")) {
            return requireUser(chatId, user,
                    u -> scheduleService.handleNavigation(u, data, request.getMessageId()));
        }
        if (data.startsWith(InlineKeyboardFactory.CB_CANCEL_PREFIX)) {
            return requireUser(chatId, user, u -> lessonService.cancel(u,
                    Long.parseLong(data.substring(InlineKeyboardFactory.CB_CANCEL_PREFIX.length()))));
        }
        if (data.startsWith(InlineKeyboardFactory.CB_EDIT_PREFIX)) {
            String id = data.substring(InlineKeyboardFactory.CB_EDIT_PREFIX.length());
            return BotResponse.text(chatId,
                    "Отправьте: /editlesson " + id + " | title=...; date=...; start=...; end=...; room=...");
        }
        if (InlineKeyboardFactory.CB_DELETE_CONFIRM.equals(data)) {
            boolean deleted = userService.deleteAllUserData(chatId);
            return BotResponse.of(chatId,
                    deleted ? "🗑 Все ваши данные удалены. Для повторной регистрации: /register"
                            : "ℹ️ Данные не найдены.",
                    commandService.menuFor(Optional.empty()));
        }
        if (InlineKeyboardFactory.CB_DELETE_ABORT.equals(data)) {
            return BotResponse.of(chatId, "✅ Удаление отменено.", commandService.menuFor(user));
        }
        if (data.startsWith(InlineKeyboardFactory.CB_STUDENTS_GROUP_PREFIX)) {
            Long groupId = Long.parseLong(
                    data.substring(InlineKeyboardFactory.CB_STUDENTS_GROUP_PREFIX.length()));
            return requireTeacher(chatId, user,
                    u -> teacherManagementService.studentsInGroup(u, groupId));
        }

        if (data.startsWith(InlineKeyboardFactory.CB_APPROVE_STUDENT_PREFIX)) {
            Long studentId = Long.parseLong(
                    data.substring(InlineKeyboardFactory.CB_APPROVE_STUDENT_PREFIX.length()));
            return requireTeacher(chatId, user,
                    u -> teacherManagementService.approveStudent(u, studentId));
        }

        if (data.startsWith("DELETE_GROUP:")) {
            Long groupId = Long.parseLong(data.substring("DELETE_GROUP:".length()));
            return requireTeacher(chatId, user,
                    u -> teacherManagementService.deleteGroup(u, groupId));
        }

        return BotResponse.text(chatId, "⚠️ Неизвестное действие.");
    }

    private BotResponse requireTeacher(
            Long chatId,
            Optional<User> user,
            java.util.function.Function<User, BotResponse> action) {
        if (user.isEmpty() || !user.get().isRegistered() || !user.get().isTeacher()) {
            return BotResponse.text(chatId,
                    "⛔ Это действие доступно только зарегистрированному преподавателю.");
        }
        return action.apply(user.get());
    }

    private BotResponse schedule(User user, String args) {
        LocalDate today = scheduleService.today(user);
        if (args.isBlank()) {
            return scheduleService.show(user);
        }
        return dateParser.parseRange(args, today)
                .map(r -> BotResponse.of(user.getChatId(),
                        scheduleService.render(user, r.getFrom(), r.getTo()),
                        inlineKeyboards.showNavigation()))
                .orElseGet(() -> BotResponse.text(user.getChatId(),
                        "❌ Некорректная дата. Примеры: `/schedule 13.05.2024`, "
                                + "`/schedule 13.05.2024-19.05.2024`, `/schedule завтра`"));
    }

    private BotResponse editLesson(User user, String args) {
        String[] parts = args.split("\\|", 2);
        try {
            Long id = Long.parseLong(parts[0].trim());
            return lessonService.edit(user, id, parts.length > 1 ? parts[1] : "");
        } catch (NumberFormatException e) {
            return BotResponse.text(user.getChatId(),
                    "❌ Формат: /editlesson <id> | title=...; start=...");
        }
    }

    private BotResponse cancelLesson(User user, String args) {
        try {
            return lessonService.cancel(user, Long.parseLong(args.trim()));
        } catch (NumberFormatException e) {
            return BotResponse.text(user.getChatId(), "❌ Формат: /cancellesson <id>");
        }
    }

    private BotResponse requireUser(Long chatId, Optional<User> user,
                                    java.util.function.Function<User, BotResponse> action) {
        if (user.isEmpty() || !user.get().isRegistered()) {
            return BotResponse.of(chatId,
                    "⚠️ Сначала пройдите регистрацию: /register",
                    commandService.menuFor(Optional.empty()));
        }
        return action.apply(user.get());
    }
}