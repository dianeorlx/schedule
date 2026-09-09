package com.university.schedulebot.service;

import com.university.schedulebot.dto.BotResponse;
import com.university.schedulebot.entity.Lesson;
import com.university.schedulebot.entity.LessonStatus;
import com.university.schedulebot.entity.User;
import com.university.schedulebot.keyboard.InlineKeyboardFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final LessonRepositoryFacade lessons;
    private final TimeZoneService timeZoneService;
    private final InlineKeyboardFactory inlineKeyboards;
    private final Clock clock;

    /** Расписание пользователя за период (включительно). */
    @Transactional(readOnly = true)
    public List<Lesson> lessonsFor(User user, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }
        return user.isTeacher()
                ? lessons.byTeacher(user.getId(), from, to)
                : lessons.byGroup(user.getGroup() == null ? -1L : user.getGroup().getId(), from, to);
    }

    public LocalDate today(User user) {
        return timeZoneService.today(user.getZoneId(), clock);
    }

    public LocalDate tomorrow(User user) {
        return today(user).plusDays(1);
    }

    /** Начало недели (понедельник) относительно «сегодня» пользователя. */
    public LocalDate weekStart(User user) {
        LocalDate today = today(user);
        return today.minusDays(today.getDayOfWeek().getValue() - 1L);
    }

    public LocalDate weekEnd(User user) {
        return weekStart(user).plusDays(6);
    }

    @Transactional(readOnly = true)
    public String render(User user, LocalDate from, LocalDate to) {
        List<Lesson> found = lessonsFor(user, from, to);

        String header = from.equals(to)
                ? "📅 *Расписание на " + from.format(DATE_FMT) + " (" + dayName(from) + ")*"
                : "📅 *Расписание " + from.format(DATE_FMT) + " — " + to.format(DATE_FMT) + "*";

        if (found.isEmpty()) {
            return header + "\n\n🎉 Занятий нет.";
        }

        Map<LocalDate, List<Lesson>> byDate = found.stream()
                .collect(Collectors.groupingBy(Lesson::getLessonDate,
                        java.util.TreeMap::new, Collectors.toList()));

        StringBuilder sb = new StringBuilder(header).append("\n");
        byDate.forEach((date, dayLessons) -> {
            sb.append("\n*").append(date.format(DATE_FMT))
                    .append(", ").append(dayName(date)).append("*\n");
            dayLessons.stream()
                    .sorted(java.util.Comparator.comparing(Lesson::getStartTime))
                    .forEach(l -> sb.append(renderLesson(l)).append('\n'));
        });
        sb.append("\n🕒 Время указано в поясе ").append(user.getZoneId());
        return sb.toString();
    }

    public String renderLesson(Lesson l) {
        StringBuilder sb = new StringBuilder();
        sb.append(l.getStatus() == LessonStatus.CANCELLED ? "❌ " : "• ");
        sb.append(l.getStartTime().format(TIME_FMT)).append('–')
                .append(l.getEndTime().format(TIME_FMT)).append("  ")
                .append(l.getTitle());
        if (l.getClassroom() != null && !l.getClassroom().isBlank()) {
            sb.append(" | ауд. ").append(l.getClassroom());
        }
        if (l.getGroup() != null) {
            sb.append(" | ").append(l.getGroup().getName());
        }
        if (l.getTeacher() != null && l.getTeacher().getFullName() != null) {
            sb.append(" | ").append(l.getTeacher().getFullName());
        }
        if (l.getStatus() == LessonStatus.CANCELLED) {
            sb.append("  (ОТМЕНЕНО)");
        }
        return sb.toString();
    }

    /** /show — расписание на сегодня + inline-навигация. */
    @Transactional(readOnly = true)
    public BotResponse show(User user) {
        LocalDate today = today(user);
        return BotResponse.of(user.getChatId(), render(user, today, today),
                inlineKeyboards.showNavigation());
    }

    /** Обработка inline-кнопок SHOW:* — редактирование существующего сообщения. */
    @Transactional(readOnly = true)
    public BotResponse handleNavigation(User user, String callbackData, Integer messageId) {
        LocalDate from;
        LocalDate to;
        String answer;
        switch (callbackData) {
            case InlineKeyboardFactory.CB_SHOW_TODAY -> {
                from = today(user); to = from; answer = "Сегодня";
            }
            case InlineKeyboardFactory.CB_SHOW_TOMORROW -> {
                from = tomorrow(user); to = from; answer = "Завтра";
            }
            case InlineKeyboardFactory.CB_SHOW_WEEK -> {
                from = weekStart(user); to = weekEnd(user); answer = "Неделя";
            }
            default -> {
                return BotResponse.builder()
                        .chatId(user.getChatId())
                        .text("⚠️ Неизвестное действие.")
                        .callbackAnswer("Ошибка")
                        .build();
            }
        }
        BotResponse response = BotResponse.edit(user.getChatId(), messageId,
                render(user, from, to), inlineKeyboards.showNavigation());
        response.setCallbackAnswer(answer);
        return response;
    }

    private String dayName(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow.getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru"));
    }
}