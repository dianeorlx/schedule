package com.university.schedulebot.keyboard;

import com.university.schedulebot.entity.Department;
import com.university.schedulebot.entity.Group;
import com.university.schedulebot.entity.Lesson;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import com.university.schedulebot.entity.User;

import java.util.ArrayList;
import java.util.List;

@Component
public class InlineKeyboardFactory {

    public static final String CB_ROLE_STUDENT = "ROLE:STUDENT";
    public static final String CB_ROLE_TEACHER = "ROLE:TEACHER";
    public static final String CB_GROUP_PREFIX = "GROUP:";
    public static final String CB_DEPT_PREFIX  = "DEPT:";

    public static final String CB_SHOW_TODAY    = "SHOW:TODAY";
    public static final String CB_SHOW_TOMORROW = "SHOW:TOMORROW";
    public static final String CB_SHOW_WEEK     = "SHOW:WEEK";

    public static final String CB_CANCEL_PREFIX = "CANCEL:";
    public static final String CB_EDIT_PREFIX   = "EDIT:";
    public static final String CB_DELETE_CONFIRM = "DELETE:CONFIRM";
    public static final String CB_DELETE_ABORT   = "DELETE:ABORT";
    public static final String CB_STUDENTS_GROUP_PREFIX = "STUDENTS_GROUP:";
    public static final String CB_APPROVE_STUDENT_PREFIX = "APPROVE_STUDENT:";


    public InlineKeyboardMarkup studentsGroups(List<Group> groups) {
        List<InlineKeyboardButton> buttons = groups.stream()
                .map(g -> btn(g.getName(), CB_STUDENTS_GROUP_PREFIX + g.getId()))
                .toList();
        return grid(buttons, 2);
    }

    public InlineKeyboardMarkup pendingStudents(List<User> students) {
        List<InlineKeyboardButton> buttons = students.stream()
                .map(user -> btn(
                        "✅ " + (user.getFullName() == null
                                ? "Студент #" + user.getId()
                                : user.getFullName()),
                        CB_APPROVE_STUDENT_PREFIX + user.getId()))
                .toList();
        return grid(buttons, 1);
    }

    public InlineKeyboardMarkup groupsToDelete(List<Group> groups) {
        List<InlineKeyboardButton> buttons = groups.stream()
                .map(g -> btn("🗑 " + g.getName(), "DELETE_GROUP:" + g.getId()))
                .toList();
        return grid(buttons, 1);
    }

    public InlineKeyboardMarkup roleSelection() {
        return oneColumn(List.of(
                btn("🎓 Студент", CB_ROLE_STUDENT),
                btn("👨‍🏫 Преподаватель", CB_ROLE_TEACHER)
        ));
    }

    public InlineKeyboardMarkup groups(List<Group> groups) {
        List<InlineKeyboardButton> buttons = groups.stream()
                .map(g -> btn(g.getName(), CB_GROUP_PREFIX + g.getId()))
                .toList();
        return grid(buttons, 2);
    }

    public InlineKeyboardMarkup departments(List<Department> departments) {
        List<InlineKeyboardButton> buttons = departments.stream()
                .map(d -> btn(d.getName(), CB_DEPT_PREFIX + d.getId()))
                .toList();
        return grid(buttons, 1);
    }

    /** Навигация /show: Сегодня / Завтра / Неделя. */
    public InlineKeyboardMarkup showNavigation() {
        List<InlineKeyboardButton> row = List.of(
                btn("Сегодня", CB_SHOW_TODAY),
                btn("Завтра", CB_SHOW_TOMORROW),
                btn("Неделя", CB_SHOW_WEEK)
        );
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(new ArrayList<>(row)));
        return markup;
    }

    /** Список занятий для отмены/редактирования. */
    public InlineKeyboardMarkup lessonActions(List<Lesson> lessons) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Lesson l : lessons) {
            rows.add(List.of(
                    btn("✏️ " + l.getTitle(), CB_EDIT_PREFIX + l.getId()),
                    btn("❌", CB_CANCEL_PREFIX + l.getId())
            ));
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup deleteConfirmation() {
        List<InlineKeyboardButton> row = List.of(
                btn("✅ Да, удалить", CB_DELETE_CONFIRM),
                btn("↩️ Отмена", CB_DELETE_ABORT)
        );
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(new ArrayList<>(row)));
        return markup;
    }

    private InlineKeyboardButton btn(String text, String data) {
        InlineKeyboardButton b = new InlineKeyboardButton();
        b.setText(text);
        b.setCallbackData(data);
        return b;
    }

    private InlineKeyboardMarkup oneColumn(List<InlineKeyboardButton> buttons) {
        return grid(buttons, 1);
    }

    private InlineKeyboardMarkup grid(List<InlineKeyboardButton> buttons, int perRow) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> current = new ArrayList<>();
        for (InlineKeyboardButton b : buttons) {
            current.add(b);
            if (current.size() == perRow) {
                rows.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) rows.add(current);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
}