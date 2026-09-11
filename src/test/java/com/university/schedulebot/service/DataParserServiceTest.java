package com.university.schedulebot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DateParserServiceTest {

    private DateParserService dateParserService;

    private final LocalDate today = LocalDate.of(2024, 5, 15);

    @BeforeEach
    void setUp() {
        dateParserService = new DateParserService();
    }

    // =========================
    // parseDate()
    // =========================

    @Test
    void parseDate_shouldReturnEmptyForNull() {
        Optional<LocalDate> result =
                dateParserService.parseDate(null, today);

        assertTrue(result.isEmpty());
    }

    @Test
    void parseDate_shouldReturnEmptyForBlankString() {
        Optional<LocalDate> result =
                dateParserService.parseDate("   ", today);

        assertTrue(result.isEmpty());
    }

    @Test
    void parseDate_shouldParseFullDateWithDots() {
        Optional<LocalDate> result =
                dateParserService.parseDate("13.05.2024", today);

        assertTrue(result.isPresent());
        assertEquals(LocalDate.of(2024, 5, 13), result.get());
    }

    @Test
    void parseDate_shouldParseIsoDate() {
        Optional<LocalDate> result =
                dateParserService.parseDate("2024-05-13", today);

        assertTrue(result.isPresent());
        assertEquals(LocalDate.of(2024, 5, 13), result.get());
    }

    @Test
    void parseDate_shouldParseShortDateUsingCurrentYear() {
        Optional<LocalDate> result =
                dateParserService.parseDate("13.05", today);

        assertTrue(result.isPresent());
        assertEquals(LocalDate.of(2024, 5, 13), result.get());
    }

    @Test
    void parseDate_shouldParseTodayInRussian() {
        Optional<LocalDate> result =
                dateParserService.parseDate("сегодня", today);

        assertTrue(result.isPresent());
        assertEquals(today, result.get());
    }

    @Test
    void parseDate_shouldParseTodayInEnglish() {
        Optional<LocalDate> result =
                dateParserService.parseDate("today", today);

        assertTrue(result.isPresent());
        assertEquals(today, result.get());
    }

    @Test
    void parseDate_shouldIgnoreCaseAndSpaces() {
        Optional<LocalDate> result =
                dateParserService.parseDate("  СЕГОДНЯ  ", today);

        assertTrue(result.isPresent());
        assertEquals(today, result.get());
    }

    @Test
    void parseDate_shouldParseTomorrow() {
        Optional<LocalDate> result =
                dateParserService.parseDate("завтра", today);

        assertTrue(result.isPresent());
        assertEquals(today.plusDays(1), result.get());
    }

    @Test
    void parseDate_shouldParseTomorrowInEnglish() {
        Optional<LocalDate> result =
                dateParserService.parseDate("tomorrow", today);

        assertTrue(result.isPresent());
        assertEquals(today.plusDays(1), result.get());
    }

    @Test
    void parseDate_shouldParseDayAfterTomorrow() {
        Optional<LocalDate> result =
                dateParserService.parseDate("послезавтра", today);

        assertTrue(result.isPresent());
        assertEquals(today.plusDays(2), result.get());
    }

    @Test
    void parseDate_shouldParseYesterday() {
        Optional<LocalDate> result =
                dateParserService.parseDate("вчера", today);

        assertTrue(result.isPresent());
        assertEquals(today.minusDays(1), result.get());
    }

    @Test
    void parseDate_shouldParseYesterdayInEnglish() {
        Optional<LocalDate> result =
                dateParserService.parseDate("yesterday", today);

        assertTrue(result.isPresent());
        assertEquals(today.minusDays(1), result.get());
    }

    @Test
    void parseDate_shouldNormalizeLetterYo() {
        Optional<LocalDate> result =
                dateParserService.parseDate("сегодня", today);

        assertTrue(result.isPresent());
        assertEquals(today, result.get());
    }

    @Test
    void parseDate_shouldReturnEmptyForUnsupportedFormat() {
        Optional<LocalDate> result =
                dateParserService.parseDate("15/05/2024", today);

        assertTrue(result.isEmpty());
    }

    // =========================
    // parseRange()
    // =========================

    @Test
    void parseRange_shouldReturnEmptyForNull() {
        Optional<DateParserService.DateRange> result =
                dateParserService.parseRange(null, today);

        assertTrue(result.isEmpty());
    }

    @Test
    void parseRange_shouldReturnEmptyForBlankString() {
        Optional<DateParserService.DateRange> result =
                dateParserService.parseRange("   ", today);

        assertTrue(result.isEmpty());
    }

    @Test
    void parseRange_shouldParseSingleDateAsOneDayRange() {
        Optional<DateParserService.DateRange> result =
                dateParserService.parseRange("13.05.2024", today);

        assertTrue(result.isPresent());

        DateParserService.DateRange range = result.get();

        assertEquals(LocalDate.of(2024, 5, 13), range.getFrom());
        assertEquals(LocalDate.of(2024, 5, 13), range.getTo());
        assertTrue(range.isSingleDay());
        assertEquals(1, range.days());
    }

    @Test
    void parseRange_shouldParseRangeWithHyphen() {
        Optional<DateParserService.DateRange> result =
                dateParserService.parseRange(
                        "13.05.2024-19.05.2024",
                        today
                );

        assertTrue(result.isPresent());

        DateParserService.DateRange range = result.get();

        assertEquals(LocalDate.of(2024, 5, 13), range.getFrom());
        assertEquals(LocalDate.of(2024, 5, 19), range.getTo());
        assertFalse(range.isSingleDay());
        assertEquals(7, range.days());
    }

    @Test
    void parseRange_shouldParseRangeWithSpacesAroundHyphen() {
        Optional<DateParserService.DateRange> result =
                dateParserService.parseRange(
                        "13.05.2024 - 19.05.2024",
                        today
                );

        assertTrue(result.isPresent());

        DateParserService.DateRange range = result.get();

        assertEquals(LocalDate.of(2024, 5, 13), range.getFrom());
        assertEquals(LocalDate.of(2024, 5, 19), range.getTo());
        assertEquals(7, range.days());
    }

    @Test
    void parseRange_shouldParseRangeWithEnDash() {
        Optional<DateParserService.DateRange> result =
                dateParserService.parseRange(
                        "13.05.2024–19.05.2024",
                        today
                );

        assertTrue(result.isPresent());

        DateParserService.DateRange range = result.get();

        assertEquals(LocalDate.of(2024, 5, 13), range.getFrom());
        assertEquals(LocalDate.of(2024, 5, 19), range.getTo());
    }

    @Test
    void parseRange_shouldParseRangeWithEmDash() {
        Optional<DateParserService.DateRange> result =
                dateParserService.parseRange(
                        "13.05.2024—19.05.2024",
                        today
                );

        assertTrue(result.isPresent());

        DateParserService.DateRange range = result.get();

        assertEquals(LocalDate.of(2024, 5, 13), range.getFrom());
        assertEquals(LocalDate.of(2024, 5, 19), range.getTo());
    }

    @Test
    void parseRange_shouldReturnEmptyWhenStartDateIsAfterEndDate() {
        Optional<DateParserService.DateRange> result =
                dateParserService.parseRange(
                        "19.05.2024-13.05.2024",
                        today
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void parseRange_shouldReturnEmptyWhenOneDateIsInvalid() {
        Optional<DateParserService.DateRange> result =
                dateParserService.parseRange(
                        "13.05.2024-31.02.2024",
                        today
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void parseRange_shouldParseCurrentWeekInRussian() {
        Optional<DateParserService.DateRange> result =
                dateParserService.parseRange("неделя", today);

        assertTrue(result.isPresent());

        DateParserService.DateRange range = result.get();

        // 15.05.2024 — среда
        assertEquals(LocalDate.of(2024, 5, 13), range.getFrom());
        assertEquals(LocalDate.of(2024, 5, 19), range.getTo());
        assertEquals(7, range.days());
    }

    @Test
    void parseRange_shouldParseCurrentWeekInEnglish() {
        Optional<DateParserService.DateRange> result =
                dateParserService.parseRange("week", today);

        assertTrue(result.isPresent());

        DateParserService.DateRange range = result.get();

        assertEquals(LocalDate.of(2024, 5, 13), range.getFrom());
        assertEquals(LocalDate.of(2024, 5, 19), range.getTo());
    }

    @Test
    void parseRange_shouldIgnoreCaseAndSpacesForWeek() {
        Optional<DateParserService.DateRange> result =
                dateParserService.parseRange("  НЕДЕЛЯ  ", today);

        assertTrue(result.isPresent());

        DateParserService.DateRange range = result.get();

        assertEquals(LocalDate.of(2024, 5, 13), range.getFrom());
        assertEquals(LocalDate.of(2024, 5, 19), range.getTo());
    }

    @Test
    void dateRange_shouldCalculateNumberOfDays() {
        DateParserService.DateRange range =
                new DateParserService.DateRange(
                        LocalDate.of(2024, 5, 1),
                        LocalDate.of(2024, 5, 10)
                );

        assertEquals(10, range.days());
        assertFalse(range.isSingleDay());
    }

    @Test
    void dateRange_shouldRecognizeSingleDay() {
        LocalDate date = LocalDate.of(2024, 5, 15);

        DateParserService.DateRange range =
                new DateParserService.DateRange(date, date);

        assertTrue(range.isSingleDay());
        assertEquals(1, range.days());
    }

    // =========================
    // parseTime()
    // =========================

    @Test
    void parseTime_shouldReturnEmptyForNull() {
        Optional<LocalTime> result =
                dateParserService.parseTime(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void parseTime_shouldReturnEmptyForBlankString() {
        Optional<LocalTime> result =
                dateParserService.parseTime("   ");

        assertTrue(result.isEmpty());
    }

    @Test
    void parseTime_shouldParseValidTime() {
        Optional<LocalTime> result =
                dateParserService.parseTime("09:30");

        assertTrue(result.isPresent());
        assertEquals(LocalTime.of(9, 30), result.get());
    }

    @Test
    void parseTime_shouldIgnoreSpaces() {
        Optional<LocalTime> result =
                dateParserService.parseTime(" 18:45 ");

        assertTrue(result.isPresent());
        assertEquals(LocalTime.of(18, 45), result.get());
    }

    @Test
    void parseTime_shouldParseMidnight() {
        Optional<LocalTime> result =
                dateParserService.parseTime("00:00");

        assertTrue(result.isPresent());
        assertEquals(LocalTime.MIDNIGHT, result.get());
    }

    @Test
    void parseTime_shouldParseLastValidTimeOfDay() {
        Optional<LocalTime> result =
                dateParserService.parseTime("23:59");

        assertTrue(result.isPresent());
        assertEquals(LocalTime.of(23, 59), result.get());
    }

    @Test
    void parseTime_shouldReturnEmptyForInvalidMinute() {
        Optional<LocalTime> result =
                dateParserService.parseTime("12:60");

        assertTrue(result.isEmpty());
    }

    @Test
    void parseTime_shouldReturnEmptyForUnsupportedFormat() {
        Optional<LocalTime> result =
                dateParserService.parseTime("9:30");

        assertTrue(result.isEmpty());
    }

    @Test
    void parseTime_shouldReturnEmptyForText() {
        Optional<LocalTime> result =
                dateParserService.parseTime("утро");

        assertTrue(result.isEmpty());
    }
}