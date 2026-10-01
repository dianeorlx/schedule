package com.university.schedulebot.service;

import com.university.schedulebot.dto.BotResponse;
import com.university.schedulebot.entity.*;
import com.university.schedulebot.keyboard.InlineKeyboardFactory;
import com.university.schedulebot.repository.GroupRepository;
import com.university.schedulebot.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LessonManagementService {

    private final LessonRepository lessonRepository;
    private final GroupRepository groupRepository;
    private final DateParserService dateParser;
    private final ScheduleService scheduleService;
    private final InlineKeyboardFactory inlineKeyboards;
    private final Clock clock;

    /**
     * Формат: /addlesson Название | ГРУППА | дата | HH:mm-HH:mm | аудитория
     * Пример:  /addlesson Матанализ | ИВТ-21 | 13.05.2024 | 10:00-11:30 | 305
     */
    @Transactional
    public BotResponse create(User author, String payload) {
        if (!author.isTeacher()) {
            return BotResponse.text(author.getChatId(),
                    "⛔ Создавать занятия может только преподаватель.");
        }
        String[] parts = payload == null ? new String[0] : payload.split("\\|");
        if (parts.length < 4) {
            return BotResponse.text(author.getChatId(), usage());
        }

        String title = parts[0].trim();
        if (title.isEmpty()) {
            return BotResponse.text(author.getChatId(), "❌ Название занятия не может быть пустым.");
        }

        Optional<Group> group = groupRepository.findByNameIgnoreCase(parts[1].trim());
        if (group.isEmpty()) {
            return BotResponse.text(author.getChatId(),
                    "❌ Группа «" + parts[1].trim() + "» не найдена.");
        }

        LocalDate today = scheduleService.today(author);
        Optional<LocalDate> date = dateParser.parseDate(parts[2].trim(), today);
        if (date.isEmpty()) {
            return BotResponse.text(author.getChatId(),
                    "❌ Некорректная дата. Формат: 13.05.2024 или «завтра».");
        }

        String[] times = parts[3].split("\\s*-\\s*");
        if (times.length != 2) {
            return BotResponse.text(author.getChatId(), "❌ Некорректное время. Формат: 10:00-11:30");
        }
        Optional<LocalTime> start = dateParser.parseTime(times[0]);
        Optional<LocalTime> end = dateParser.parseTime(times[1]);
        if (start.isEmpty() || end.isEmpty()) {
            return BotResponse.text(author.getChatId(), "❌ Некорректное время. Формат: 10:00-11:30");
        }
        if (!end.get().isAfter(start.get())) {
            return BotResponse.text(author.getChatId(),
                    "❌ Время окончания должно быть позже времени начала.");
        }

        String classroom = parts.length > 4 ? parts[4].trim() : null;

        boolean conflict = lessonRepository
                .findByTeacherAndPeriod(author.getId(), date.get(), date.get())
                .stream()
                .filter(l -> l.getStatus() == LessonStatus.ACTIVE)
                .anyMatch(l -> overlaps(l.getStartTime(), l.getEndTime(), start.get(), end.get()));
        if (conflict) {
            return BotResponse.text(author.getChatId(),
                    "⚠️ У вас уже есть занятие в это время.");
        }

        Lesson lesson = lessonRepository.save(Lesson.builder()
                .title(title)
                .group(group.get())
                .department(author.getDepartment())
                .teacher(author)
                .createdBy(author)
                .lessonDate(date.get())
                .startTime(start.get())
                .endTime(end.get())
                .classroom(classroom)
                .status(LessonStatus.ACTIVE)
                .build());

        return BotResponse.text(author.getChatId(),
                "✅ Занятие создано:\n" + scheduleService.renderLesson(lesson));
    }

    /** Формат: /editlesson <id> | поле=значение; ... (title, date, start, end, room) */
    @Transactional
    public BotResponse edit(User author, Long lessonId, String changes) {
        Optional<Lesson> found = lessonRepository.findById(lessonId);
        if (found.isEmpty()) {
            return BotResponse.text(author.getChatId(), "❌ Занятие не найдено.");
        }
        Lesson lesson = found.get();
        if (!isOwner(author, lesson)) {
            return BotResponse.text(author.getChatId(),
                    "⛔ Вы можете изменять только свои занятия.");
        }
        if (changes == null || changes.isBlank()) {
            return BotResponse.text(author.getChatId(),
                    "❌ Укажите изменения: title=..., date=..., start=..., end=..., room=...");
        }

        LocalDate today = scheduleService.today(author);
        LocalTime newStart = lesson.getStartTime();
        LocalTime newEnd = lesson.getEndTime();

        for (String token : changes.split(";")) {
            String[] kv = token.split("=", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim().toLowerCase();
            String value = kv[1].trim();
            switch (key) {
                case "title" -> {
                    if (value.isEmpty()) {
                        return BotResponse.text(author.getChatId(), "❌ Название не может быть пустым.");
                    }
                    lesson.setTitle(value);
                }
                case "date" -> {
                    Optional<LocalDate> d = dateParser.parseDate(value, today);
                    if (d.isEmpty()) {
                        return BotResponse.text(author.getChatId(), "❌ Некорректная дата: " + value);
                    }
                    lesson.setLessonDate(d.get());
                }
                case "start" -> {
                    Optional<LocalTime> t = dateParser.parseTime(value);
                    if (t.isEmpty()) {
                        return BotResponse.text(author.getChatId(), "❌ Некорректное время: " + value);
                    }
                    newStart = t.get();
                }
                case "end" -> {
                    Optional<LocalTime> t = dateParser.parseTime(value);
                    if (t.isEmpty()) {
                        return BotResponse.text(author.getChatId(), "❌ Некорректное время: " + value);
                    }
                    newEnd = t.get();
                }
                case "room" -> lesson.setClassroom(value);
                default -> { /* игнорируем неизвестные поля */ }
            }
        }

        if (!newEnd.isAfter(newStart)) {
            return BotResponse.text(author.getChatId(),
                    "❌ Время окончания должно быть позже времени начала.");
        }
        lesson.setStartTime(newStart);
        lesson.setEndTime(newEnd);
        lessonRepository.save(lesson);

        return BotResponse.text(author.getChatId(),
                "✏️ Занятие обновлено:\n" + scheduleService.renderLesson(lesson));
    }

    /** Отмена занятия (мягкая — статус CANCELLED). */
    @Transactional
    public BotResponse cancel(User author, Long lessonId) {
        Optional<Lesson> found = lessonRepository.findById(lessonId);
        if (found.isEmpty()) {
            return BotResponse.text(author.getChatId(), "❌ Занятие не найдено.");
        }
        Lesson lesson = found.get();
        if (!isOwner(author, lesson)) {
            return BotResponse.text(author.getChatId(),
                    "⛔ Вы можете отменять только свои занятия.");
        }
        if (lesson.getStatus() == LessonStatus.CANCELLED) {
            return BotResponse.text(author.getChatId(), "ℹ️ Занятие уже отменено.");
        }
        lesson.setStatus(LessonStatus.CANCELLED);
        lessonRepository.save(lesson);

        return BotResponse.text(author.getChatId(),
                "❌ Занятие отменено:\n" + scheduleService.renderLesson(lesson));
    }

    /** Список ближайших занятий преподавателя с inline-кнопками ✏️/❌. */
    @Transactional(readOnly = true)
    public BotResponse myLessons(User author) {
        if (!author.isTeacher()) {
            return BotResponse.text(author.getChatId(),
                    "⛔ Управление занятиями доступно только преподавателю.");
        }
        LocalDate from = scheduleService.today(author);
        List<Lesson> list = lessonRepository.findByTeacherAndPeriod(author.getId(), from, from.plusDays(30));
        if (list.isEmpty()) {
            return BotResponse.text(author.getChatId(), "У вас нет занятий на ближайший месяц.");
        }
        StringBuilder sb = new StringBuilder("✏️ *Ваши занятия (30 дней)*\n\n");
        list.forEach(l -> sb.append(l.getLessonDate().format(ScheduleService.DATE_FMT))
                .append(' ')
                .append(scheduleService.renderLesson(l))
                .append('\n'));

        return BotResponse.of(author.getChatId(), sb.toString(), inlineKeyboards.lessonActions(list));
    }

    boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private boolean isOwner(User user, Lesson lesson) {
        return lesson.getTeacher() != null
                && user.getId() != null
                && user.getId().equals(lesson.getTeacher().getId());
    }

    private String usage() {
        return """
               ❌ Неверный формат.

               Используйте:
               `/addlesson Название | Группа | Дата | Начало-Конец | Аудитория`

               Пример:
               `/addlesson Матанализ | ИВТ-21 | 13.05.2024 | 10:00-11:30 | 305`""";
    }
}