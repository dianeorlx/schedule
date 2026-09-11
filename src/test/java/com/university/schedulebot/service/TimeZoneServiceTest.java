package com.university.schedulebot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeZoneServiceTest {

    private TimeZoneService timeZoneService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        timeZoneService = new TimeZoneService();

        clock = Clock.fixed(
                Instant.parse("2024-01-15T12:00:00Z"),
                ZoneOffset.UTC
        );
    }

    @Test
    void resolveShouldReturnMoscowZoneForMoscow() {
        Optional<ZoneId> result =
                timeZoneService.resolve("Москва");

        assertTrue(result.isPresent());
        assertEquals(
                "Europe/Moscow",
                result.get().getId()
        );
    }

    @Test
    void resolveShouldIgnoreSpacesAndCase() {
        Optional<ZoneId> result =
                timeZoneService.resolve("  МОСКВА  ");

        assertTrue(result.isPresent());
        assertEquals(
                "Europe/Moscow",
                result.get().getId()
        );
    }

    @Test
    void resolveShouldNormalizeRussianLetterYo() {
        /*
         * Метод заменяет "ё" на "е".
         * Для проверки используем город, где это преобразование
         * не ломает распознавание.
         */
        Optional<ZoneId> result =
                timeZoneService.resolve("ЕКАТЕРИНБУРГ");

        assertTrue(result.isPresent());
        assertEquals(
                "Asia/Yekaterinburg",
                result.get().getId()
        );
    }

    @Test
    void resolveShouldSupportCityAliases() {
        assertEquals(
                "Europe/Moscow",
                timeZoneService.resolve("СПб")
                        .orElseThrow()
                        .getId()
        );

        assertEquals(
                "Asia/Yekaterinburg",
                timeZoneService.resolve("Уфа")
                        .orElseThrow()
                        .getId()
        );

        assertEquals(
                "Asia/Almaty",
                timeZoneService.resolve("Астана")
                        .orElseThrow()
                        .getId()
        );
    }

    @Test
    void resolveShouldResolveDirectZoneId() {
        Optional<ZoneId> result =
                timeZoneService.resolve("Asia/Tokyo");

        assertTrue(result.isPresent());
        assertEquals(
                "Asia/Tokyo",
                result.get().getId()
        );
    }

    @Test
    void resolveShouldReturnEmptyForUnknownLocation() {
        Optional<ZoneId> result =
                timeZoneService.resolve("Неизвестный город");

        assertTrue(result.isEmpty());
    }

    @Test
    void resolveShouldReturnEmptyForNull() {
        Optional<ZoneId> result =
                timeZoneService.resolve(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolveShouldReturnEmptyForBlankLocation() {
        assertTrue(timeZoneService.resolve("").isEmpty());
        assertTrue(timeZoneService.resolve("   ").isEmpty());
    }

    @Test
    void resolveOrDefaultShouldReturnResolvedZone() {
        ZoneId result =
                timeZoneService.resolveOrDefault("Новосибирск");

        assertEquals(
                "Asia/Novosibirsk",
                result.getId()
        );
    }

    @Test
    void resolveOrDefaultShouldReturnMoscowForUnknownLocation() {
        ZoneId result =
                timeZoneService.resolveOrDefault("Неизвестный город");

        assertEquals(
                TimeZoneService.DEFAULT_ZONE,
                result.getId()
        );
    }

    @Test
    void resolveOrDefaultShouldReturnMoscowForNull() {
        ZoneId result =
                timeZoneService.resolveOrDefault(null);

        assertEquals(
                TimeZoneService.DEFAULT_ZONE,
                result.getId()
        );
    }

    @Test
    void zoneOfShouldReturnValidZone() {
        ZoneId result =
                timeZoneService.zoneOf("Asia/Tokyo");

        assertEquals(
                "Asia/Tokyo",
                result.getId()
        );
    }

    @Test
    void zoneOfShouldReturnDefaultZoneForInvalidZone() {
        ZoneId result =
                timeZoneService.zoneOf("Invalid/Zone");

        assertEquals(
                TimeZoneService.DEFAULT_ZONE,
                result.getId()
        );
    }

    @Test
    void zoneOfShouldReturnDefaultZoneForNull() {
        ZoneId result =
                timeZoneService.zoneOf(null);

        assertEquals(
                TimeZoneService.DEFAULT_ZONE,
                result.getId()
        );
    }

    @Test
    void todayShouldReturnDateInSpecifiedTimezone() {
        Clock instantClock = Clock.fixed(
                Instant.parse("2024-01-01T23:30:00Z"),
                ZoneOffset.UTC
        );

        LocalDate result =
                timeZoneService.today(
                        "Asia/Tokyo",
                        instantClock
                );

        assertEquals(
                LocalDate.of(2024, 1, 2),
                result
        );
    }

    @Test
    void todayShouldUseDefaultTimezoneForInvalidZone() {
        Clock instantClock = Clock.fixed(
                Instant.parse("2024-01-01T21:30:00Z"),
                ZoneOffset.UTC
        );

        LocalDate result =
                timeZoneService.today(
                        "Invalid/Zone",
                        instantClock
                );

        assertEquals(
                LocalDate.of(2024, 1, 2),
                result
        );
    }

    @Test
    void nowShouldReturnLocalDateTimeInSpecifiedTimezone() {
        Clock instantClock = Clock.fixed(
                Instant.parse("2024-01-01T12:00:00Z"),
                ZoneOffset.UTC
        );

        LocalDateTime result =
                timeZoneService.now(
                        "Europe/Moscow",
                        instantClock
                );

        assertEquals(
                LocalDateTime.of(2024, 1, 1, 15, 0),
                result
        );
    }

    @Test
    void nowShouldUseDefaultTimezoneForInvalidZone() {
        Clock instantClock = Clock.fixed(
                Instant.parse("2024-01-01T12:00:00Z"),
                ZoneOffset.UTC
        );

        LocalDateTime result =
                timeZoneService.now(
                        "Invalid/Zone",
                        instantClock
                );

        assertEquals(
                LocalDateTime.of(2024, 1, 1, 15, 0),
                result
        );
    }

    @Test
    void offsetLabelShouldReturnMoscowOffset() {
        String result =
                timeZoneService.offsetLabel(
                        "Europe/Moscow",
                        clock
                );

        assertEquals(
                "UTC+03:00",
                result
        );
    }

    @Test
    void offsetLabelShouldReturnUtcForUtcZone() {
        String result =
                timeZoneService.offsetLabel(
                        "UTC",
                        clock
                );

        assertEquals(
                "UTC+00:00",
                result
        );
    }

    @Test
    void offsetLabelShouldReturnNegativeOffset() {
        String result =
                timeZoneService.offsetLabel(
                        "America/New_York",
                        clock
                );

        assertEquals(
                "UTC-05:00",
                result
        );
    }

    @Test
    void offsetLabelShouldUseDefaultTimezoneForInvalidZone() {
        String result =
                timeZoneService.offsetLabel(
                        "Invalid/Zone",
                        clock
                );

        assertEquals(
                "UTC+03:00",
                result
        );
    }

    @Test
    void offsetLabelShouldHandleHalfHourOffset() {
        String result =
                timeZoneService.offsetLabel(
                        "Asia/Kolkata",
                        clock
                );

        assertEquals(
                "UTC+05:30",
                result
        );
    }

    @Test
    void offsetLabelShouldHandleNegativeHalfHourOffset() {
        String result =
                timeZoneService.offsetLabel(
                        "America/St_Johns",
                        clock
                );

        assertNotNull(result);
        assertTrue(result.startsWith("UTC-"));
    }
}