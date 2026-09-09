package com.university.schedulebot.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

@Service
public class DateParserService {

    private static final DateTimeFormatter DOT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DOT_SHORT = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** Диапазон дат [from; to]. */
    @Getter
    @RequiredArgsConstructor
    public static class DateRange {
        private final LocalDate from;
        private final LocalDate to;

        public boolean isSingleDay() {
            return from.equals(to);
        }

        public long days() {
            return java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        }
    }

    /**
     * Разбирает дату: dd.MM.yyyy, dd.MM (текущий год), yyyy-MM-dd,
     * а также «сегодня», «завтра», «вчера», «послезавтра».
     */
    public Optional<LocalDate> parseDate(String raw, LocalDate today) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String s = raw.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');

        switch (s) {
            case "сегодня", "today" -> { return Optional.of(today); }
            case "завтра", "tomorrow" -> { return Optional.of(today.plusDays(1)); }
            case "послезавтра" -> { return Optional.of(today.plusDays(2)); }
            case "вчера", "yesterday" -> { return Optional.of(today.minusDays(1)); }
            default -> { /* дальше форматы */ }
        }

        for (DateTimeFormatter f : new DateTimeFormatter[]{DOT, ISO}) {
            try {
                return Optional.of(LocalDate.parse(raw.trim(), f));
            } catch (DateTimeParseException ignored) { }
        }
        try {
            java.time.MonthDay md = java.time.MonthDay.parse(raw.trim(), DOT_SHORT);
            return Optional.of(md.atYear(today.getYear()));
        } catch (DateTimeParseException ignored) { }

        return Optional.empty();
    }

    /**
     * Разбирает диапазон: "13.05.2024-19.05.2024", "13.05.2024 - 19.05.2024",
     * одиночная дата трактуется как диапазон из одного дня.
     * "неделя" — текущая календарная неделя.
     */
    public Optional<DateRange> parseRange(String raw, LocalDate today) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String s = raw.trim().toLowerCase(Locale.ROOT);

        if (s.equals("неделя") || s.equals("week")) {
            LocalDate start = today.minusDays(today.getDayOfWeek().getValue() - 1L);
            return Optional.of(new DateRange(start, start.plusDays(6)));
        }

        String[] parts = raw.split("\\s*(?:—|–|-{1,2}|\\.{2})\\s*(?=\\d)");
        if (parts.length == 2) {
            Optional<LocalDate> from = parseDate(parts[0], today);
            Optional<LocalDate> to = parseDate(parts[1], today);
            if (from.isEmpty() || to.isEmpty()) return Optional.empty();
            if (from.get().isAfter(to.get())) return Optional.empty();
            return Optional.of(new DateRange(from.get(), to.get()));
        }

        return parseDate(raw, today).map(d -> new DateRange(d, d));
    }

    public Optional<LocalTime> parseTime(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        try {
            return Optional.of(LocalTime.parse(raw.trim(), TIME));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}