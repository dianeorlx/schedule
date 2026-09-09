package com.university.schedulebot.service;

import com.university.schedulebot.dto.BotResponse;
import com.university.schedulebot.entity.User;
import com.university.schedulebot.keyboard.ReplyKeyboardFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommandService {

    private final ReplyKeyboardFactory replyKeyboardFactory;

    public static final String HELP_TEXT = """
        📖 *Доступные команды*

        /start — начать работу с ботом
        /register — регистрация
        /help — это сообщение
        /info — информация о боте
        /show — расписание с навигацией
        /schedule дата  — расписание на дату или диапазон
        /profile — мой профиль
        /addlesson — создать занятие
        /mylessons — изменить или отменить занятие
        /delete\\_me — полностью удалить мои данные
        """;

    public static final String INFO_TEXT = """
            ℹ️ *Университетское расписание*

            Назначение: хранение и просмотр расписания занятий \
            для студентов и преподавателей.""";

    public BotResponse help(Long chatId, Optional<User> user) {
        return BotResponse.of(chatId, HELP_TEXT, menuFor(user));
    }

    public BotResponse info(Long chatId, Optional<User> user) {
        return BotResponse.of(chatId, INFO_TEXT, menuFor(user));
    }

    public BotResponse start(Long chatId, Optional<User> user) {
        String text = user.filter(User::isRegistered)
                .map(u -> "👋 С возвращением, " +
                        (u.getFullName() == null ? "пользователь" : u.getFullName()) + "!")
                .orElse("""
                        Добро пожаловать в бот «Schdule»!

                        Чтобы начать, пройдите регистрацию: /register
                        Список команд: /help""");
        return BotResponse.of(chatId, text, menuFor(user));
    }

    public BotResponse unknown(Long chatId, Optional<User> user) {
        return BotResponse.of(chatId,
                "Неизвестная команда. Используйте /help для списка доступных команд.",
                menuFor(user));
    }

    /** Меню зависит от того, зарегистрирован ли пользователь и от его роли. */
    public ReplyKeyboardMarkup menuFor(Optional<User> user) {
        if (user.isEmpty() || !user.get().isRegistered()) {
            return replyKeyboardFactory.guestMenu();
        }
        return user.get().isTeacher()
                ? replyKeyboardFactory.teacherMenu()
                : replyKeyboardFactory.studentMenu();
    }
}