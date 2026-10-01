package com.university.schedulebot.keyboard;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReplyKeyboardFactory {

    public static final String BTN_SCHEDULE = "📅 Расписание";
    public static final String BTN_HELP     = "❓ Помощь";
    public static final String BTN_INFO     = "ℹ️ О боте";
    public static final String BTN_PROFILE  = "👤 Профиль";
    public static final String BTN_DELETE   = "🗑 Удалить данные";
    public static final String BTN_ADD_LESSON    = "➕ Добавить занятие";
    public static final String BTN_MANAGE_LESSON = "✏️ Мои занятия";
    public static final String BTN_STUDENTS = "👥 Студенты по группам";
    public static final String BTN_PENDING_STUDENTS = "✅ Подтвердить студентов";
    public static final String BTN_CREATE_GROUP = "➕ Создать группу";
    public static final String BTN_DELETE_GROUP = "🗑 Удалить группу";

    /** Главное меню для незарегистрированного пользователя. */
    public ReplyKeyboardMarkup guestMenu() {
        KeyboardRow r1 = new KeyboardRow();
        r1.add("/register");
        KeyboardRow r2 = new KeyboardRow();
        r2.add(BTN_HELP);
        r2.add(BTN_INFO);
        return build(List.of(r1, r2));
    }

    /** Меню студента. */
    public ReplyKeyboardMarkup studentMenu() {
        KeyboardRow r1 = new KeyboardRow();
        r1.add(BTN_SCHEDULE);
        r1.add(BTN_PROFILE);
        KeyboardRow r2 = new KeyboardRow();
        r2.add(BTN_HELP);
        r2.add(BTN_INFO);
        KeyboardRow r3 = new KeyboardRow();
        r3.add(BTN_DELETE);
        return build(List.of(r1, r2, r3));
    }

    /** Меню преподавателя. */
    public ReplyKeyboardMarkup teacherMenu() {
        KeyboardRow r1 = new KeyboardRow();
        r1.add(BTN_SCHEDULE);
        r1.add(BTN_ADD_LESSON);

        KeyboardRow r2 = new KeyboardRow();
        r2.add(BTN_MANAGE_LESSON);
        r2.add(BTN_STUDENTS);

        KeyboardRow r3 = new KeyboardRow();
        r3.add(BTN_PENDING_STUDENTS);

        KeyboardRow r4 = new KeyboardRow();
        r4.add(BTN_CREATE_GROUP);
        r4.add(BTN_DELETE_GROUP);

        KeyboardRow r5 = new KeyboardRow();
        r5.add(BTN_PROFILE);
        r5.add(BTN_HELP);
        r5.add(BTN_INFO);

        KeyboardRow r6 = new KeyboardRow();
        r6.add(BTN_DELETE);


        return build(List.of(r1, r2, r3, r4, r5, r6));
    }

    public ReplyKeyboardRemove remove() {
        return ReplyKeyboardRemove.builder().removeKeyboard(true).build();
    }

    private ReplyKeyboardMarkup build(List<KeyboardRow> rows) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(new ArrayList<>(rows));
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);
        markup.setSelective(true);
        return markup;
    }
}