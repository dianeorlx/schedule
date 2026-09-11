package com.university.schedulebot.service;

import com.university.schedulebot.dto.BotResponse;
import com.university.schedulebot.entity.Group;
import com.university.schedulebot.entity.Lesson;
import com.university.schedulebot.entity.LessonStatus;
import com.university.schedulebot.entity.User;
import com.university.schedulebot.keyboard.InlineKeyboardFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private LessonRepositoryFacade lessons;

    @Mock
    private TimeZoneService timeZoneService;

    @Mock
    private InlineKeyboardFactory inlineKeyboards;

    private Clock clock;
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                Instant.parse("2024-01-17T12:00:00Z"),
                ZoneOffset.UTC
        );

        scheduleService = new ScheduleService(
                lessons,
                timeZoneService,
                inlineKeyboards,
                clock
        );
    }

    @Test
    void lessonsFor_shouldFindLessonsByTeacher() {
        User teacher = mock(User.class);

        LocalDate from = LocalDate.of(2024, 1, 15);
        LocalDate to = LocalDate.of(2024, 1, 21);

        when(teacher.isTeacher()).thenReturn(true);
        when(teacher.getId()).thenReturn(10L);

        List<Lesson> expected = List.of(mock(Lesson.class));

        when(lessons.byTeacher(10L, from, to))
                .thenReturn(expected);

        List<Lesson> result =
                scheduleService.lessonsFor(teacher, from, to);

        assertThat(result).isEqualTo(expected);

        verify(lessons).byTeacher(10L, from, to);
        verify(lessons, never()).byGroup(anyLong(), any(), any());
    }

    @Test
    void lessonsFor_shouldFindLessonsByGroup() {
        User student = mock(User.class);
        Group group = mock(Group.class);

        LocalDate from = LocalDate.of(2024, 1, 15);
        LocalDate to = LocalDate.of(2024, 1, 21);

        when(student.isTeacher()).thenReturn(false);
        when(student.getGroup()).thenReturn(group);
        when(group.getId()).thenReturn(20L);

        List<Lesson> expected = List.of(mock(Lesson.class));

        when(lessons.byGroup(20L, from, to))
                .thenReturn(expected);

        List<Lesson> result =
                scheduleService.lessonsFor(student, from, to);

        assertThat(result).isEqualTo(expected);

        verify(lessons).byGroup(20L, from, to);
        verify(lessons, never()).byTeacher(anyLong(), any(), any());
    }

    @Test
    void lessonsFor_shouldUseMinusOneWhenStudentHasNoGroup() {
        User student = mock(User.class);

        LocalDate from = LocalDate.of(2024, 1, 15);
        LocalDate to = LocalDate.of(2024, 1, 21);

        when(student.isTeacher()).thenReturn(false);
        when(student.getGroup()).thenReturn(null);

        when(lessons.byGroup(-1L, from, to))
                .thenReturn(List.of());

        List<Lesson> result =
                scheduleService.lessonsFor(student, from, to);

        assertThat(result).isEmpty();

        verify(lessons).byGroup(-1L, from, to);
    }

    @Test
    void lessonsFor_shouldThrowExceptionWhenFromAfterTo() {
        User user = mock(User.class);

        LocalDate from = LocalDate.of(2024, 1, 20);
        LocalDate to = LocalDate.of(2024, 1, 15);

        assertThatThrownBy(() ->
                scheduleService.lessonsFor(user, from, to)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Дата начала не может быть позже даты окончания");

        verifyNoInteractions(lessons);
    }

    @Test
    void today_shouldReturnDateFromTimeZoneService() {
        User user = mock(User.class);

        when(user.getZoneId()).thenReturn("Europe/Moscow");
        when(timeZoneService.today("Europe/Moscow", clock))
                .thenReturn(LocalDate.of(2024, 1, 17));

        LocalDate result = scheduleService.today(user);

        assertThat(result).isEqualTo(LocalDate.of(2024, 1, 17));

        verify(timeZoneService)
                .today("Europe/Moscow", clock);
    }

    @Test
    void tomorrow_shouldReturnNextDay() {
        User user = mock(User.class);

        when(user.getZoneId()).thenReturn("Europe/Moscow");
        when(timeZoneService.today("Europe/Moscow", clock))
                .thenReturn(LocalDate.of(2024, 1, 17));

        LocalDate result = scheduleService.tomorrow(user);

        assertThat(result).isEqualTo(LocalDate.of(2024, 1, 18));
    }

    @Test
    void weekStart_shouldReturnMonday() {
        User user = mock(User.class);

        when(user.getZoneId()).thenReturn("Europe/Moscow");
        when(timeZoneService.today("Europe/Moscow", clock))
                .thenReturn(LocalDate.of(2024, 1, 17)); // среда

        LocalDate result = scheduleService.weekStart(user);

        assertThat(result).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    void weekEnd_shouldReturnSunday() {
        User user = mock(User.class);

        when(user.getZoneId()).thenReturn("Europe/Moscow");
        when(timeZoneService.today("Europe/Moscow", clock))
                .thenReturn(LocalDate.of(2024, 1, 17));

        LocalDate result = scheduleService.weekEnd(user);

        assertThat(result).isEqualTo(LocalDate.of(2024, 1, 21));
    }

    @Test
    void renderLesson_shouldRenderFullLesson() {
        Lesson lesson = mock(Lesson.class);
        Group group = mock(Group.class);
        User teacher = mock(User.class);

        when(lesson.getStatus()).thenReturn(LessonStatus.ACTIVE);
        when(lesson.getStartTime()).thenReturn(LocalTime.of(9, 0));
        when(lesson.getEndTime()).thenReturn(LocalTime.of(10, 30));
        when(lesson.getTitle()).thenReturn("Математика");
        when(lesson.getClassroom()).thenReturn("301");

        when(lesson.getGroup()).thenReturn(group);
        when(group.getName()).thenReturn("ИВТ-21");

        when(lesson.getTeacher()).thenReturn(teacher);
        when(teacher.getFullName()).thenReturn("Иван Иванов");

        String result = scheduleService.renderLesson(lesson);

        assertThat(result)
                .isEqualTo("• 09:00–10:30  Математика | ауд. 301 | ИВТ-21 | Иван Иванов");
    }

    @Test
    void renderLesson_shouldRenderCancelledLesson() {
        Lesson lesson = mock(Lesson.class);

        when(lesson.getStatus()).thenReturn(LessonStatus.CANCELLED);
        when(lesson.getStartTime()).thenReturn(LocalTime.of(11, 15));
        when(lesson.getEndTime()).thenReturn(LocalTime.of(12, 45));
        when(lesson.getTitle()).thenReturn("Физика");
        when(lesson.getClassroom()).thenReturn(null);
        when(lesson.getGroup()).thenReturn(null);
        when(lesson.getTeacher()).thenReturn(null);

        String result = scheduleService.renderLesson(lesson);

        assertThat(result)
                .isEqualTo("❌ 11:15–12:45  Физика  (ОТМЕНЕНО)");
    }

    @Test
    void renderLesson_shouldNotRenderBlankClassroom() {
        Lesson lesson = mock(Lesson.class);

        when(lesson.getStatus()).thenReturn(LessonStatus.ACTIVE);
        when(lesson.getStartTime()).thenReturn(LocalTime.of(8, 0));
        when(lesson.getEndTime()).thenReturn(LocalTime.of(9, 0));
        when(lesson.getTitle()).thenReturn("История");
        when(lesson.getClassroom()).thenReturn(" ");
        when(lesson.getGroup()).thenReturn(null);
        when(lesson.getTeacher()).thenReturn(null);

        String result = scheduleService.renderLesson(lesson);

        assertThat(result)
                .isEqualTo("• 08:00–09:00  История");
    }

    @Test
    void render_shouldReturnNoLessonsMessage() {
        User user = mock(User.class);

        LocalDate date = LocalDate.of(2024, 1, 17);

        when(user.isTeacher()).thenReturn(true);
        when(user.getId()).thenReturn(10L);

        when(lessons.byTeacher(10L, date, date))
                .thenReturn(List.of());

        String result = scheduleService.render(user, date, date);

        assertThat(result)
                .contains("📅 *Расписание на 17.01.2024")
                .contains("🎉 Занятий нет.");
    }

    @Test
    void render_shouldSortLessonsByStartTime() {
        User user = mock(User.class);

        LocalDate date = LocalDate.of(2024, 1, 17);

        Lesson lateLesson = mock(Lesson.class);
        when(lateLesson.getLessonDate()).thenReturn(date);
        when(lateLesson.getStartTime()).thenReturn(LocalTime.of(12, 0));
        when(lateLesson.getEndTime()).thenReturn(LocalTime.of(13, 30));
        when(lateLesson.getTitle()).thenReturn("Физика");
        when(lateLesson.getStatus()).thenReturn(LessonStatus.ACTIVE);

        Lesson earlyLesson = mock(Lesson.class);
        when(earlyLesson.getLessonDate()).thenReturn(date);
        when(earlyLesson.getStartTime()).thenReturn(LocalTime.of(9, 0));
        when(earlyLesson.getEndTime()).thenReturn(LocalTime.of(10, 30));
        when(earlyLesson.getTitle()).thenReturn("Математика");
        when(earlyLesson.getStatus()).thenReturn(LessonStatus.ACTIVE);

        when(user.isTeacher()).thenReturn(true);
        when(user.getId()).thenReturn(10L);
        when(user.getZoneId()).thenReturn("Europe/Moscow");

        when(lessons.byTeacher(10L, date, date))
                .thenReturn(List.of(lateLesson, earlyLesson));

        String result = scheduleService.render(user, date, date);

        assertThat(result)
                .contains("🕒 Время указано в поясе Europe/Moscow");

        assertThat(result.indexOf("Математика"))
                .isLessThan(result.indexOf("Физика"));
    }

    @Test
    void show_shouldReturnTodayScheduleAndNavigation() {
        User user = mock(User.class);

        LocalDate today = LocalDate.of(2024, 1, 17);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        when(user.getChatId()).thenReturn(100L);
        when(user.getZoneId()).thenReturn("Europe/Moscow");
        when(user.isTeacher()).thenReturn(true);
        when(user.getId()).thenReturn(10L);

        when(timeZoneService.today("Europe/Moscow", clock))
                .thenReturn(today);

        when(lessons.byTeacher(10L, today, today))
                .thenReturn(List.of());

        when(inlineKeyboards.showNavigation())
                .thenReturn(keyboard);

        BotResponse result = scheduleService.show(user);

        assertThat(result.getChatId()).isEqualTo(100L);
        assertThat(result.getText())
                .contains("Расписание на 17.01.2024");
    }

    @Test
    void handleNavigation_shouldHandleTodayCallback() {
        User user = mock(User.class);

        LocalDate today = LocalDate.of(2024, 1, 17);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        when(user.getChatId()).thenReturn(100L);
        when(user.getZoneId()).thenReturn("Europe/Moscow");
        when(user.isTeacher()).thenReturn(true);
        when(user.getId()).thenReturn(10L);

        when(timeZoneService.today("Europe/Moscow", clock))
                .thenReturn(today);

        when(lessons.byTeacher(10L, today, today))
                .thenReturn(List.of());

        when(inlineKeyboards.showNavigation())
                .thenReturn(keyboard);

        BotResponse result = scheduleService.handleNavigation(
                user,
                InlineKeyboardFactory.CB_SHOW_TODAY,
                55
        );

        assertThat(result.getChatId()).isEqualTo(100L);
        assertThat(result.getText())
                .contains("Расписание на 17.01.2024");
        assertThat(result.getCallbackAnswer())
                .isEqualTo("Сегодня");
    }

    @Test
    void handleNavigation_shouldReturnErrorForUnknownCallback() {
        User user = mock(User.class);

        when(user.getChatId()).thenReturn(100L);

        BotResponse result = scheduleService.handleNavigation(
                user,
                "SHOW_UNKNOWN",
                55
        );

        assertThat(result.getChatId()).isEqualTo(100L);
        assertThat(result.getText())
                .isEqualTo("⚠️ Неизвестное действие.");
        assertThat(result.getCallbackAnswer())
                .isEqualTo("Ошибка");

        verifyNoInteractions(lessons);
    }
}