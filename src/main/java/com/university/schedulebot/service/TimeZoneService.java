package com.university.schedulebot.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class TimeZoneService {

    public static final String DEFAULT_ZONE = "Europe/Moscow";

    private static final Map<String, String> CITY_TO_ZONE = new LinkedHashMap<>();

    static {
        CITY_TO_ZONE.put("москва", "Europe/Moscow");
        CITY_TO_ZONE.put("санкт-петербург", "Europe/Moscow");
        CITY_TO_ZONE.put("спб", "Europe/Moscow");
        CITY_TO_ZONE.put("калининград", "Europe/Kaliningrad");
        CITY_TO_ZONE.put("самара", "Europe/Samara");
        CITY_TO_ZONE.put("екатеринбург", "Asia/Yekaterinburg");
        CITY_TO_ZONE.put("челябинск", "Asia/Yekaterinburg");
        CITY_TO_ZONE.put("уфа", "Asia/Yekaterinburg");
        CITY_TO_ZONE.put("омск", "Asia/Omsk");
        CITY_TO_ZONE.put("новосибирск", "Asia/Novosibirsk");
        CITY_TO_ZONE.put("красноярск", "Asia/Krasnoyarsk");
        CITY_TO_ZONE.put("иркутск", "Asia/Irkutsk");
        CITY_TO_ZONE.put("якутск", "Asia/Yakutsk");
        CITY_TO_ZONE.put("владивосток", "Asia/Vladivostok");
        CITY_TO_ZONE.put("магадан", "Asia/Magadan");
        CITY_TO_ZONE.put("камчатка", "Asia/Kamchatka");
        CITY_TO_ZONE.put("петропавловск-камчатский", "Asia/Kamchatka");
        CITY_TO_ZONE.put("минск", "Europe/Minsk");
        CITY_TO_ZONE.put("астана", "Asia/Almaty");
        CITY_TO_ZONE.put("алматы", "Asia/Almaty");
    }

    /**
     * Определяет часовой пояс по названию города или по прямому ZoneId.
     * @return Optional.empty(), если распознать не удалось.
     */
    public Optional<ZoneId> resolve(String location) {
        if (location == null || location.isBlank()) return Optional.empty();

        String normalized = location.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');

        String zone = CITY_TO_ZONE.get(normalized);
        if (zone != null) return Optional.of(ZoneId.of(zone));

        // Пользователь мог ввести сам ZoneId, например "Asia/Tokyo"
        String raw = location.trim();
        if (ZoneId.getAvailableZoneIds().contains(raw)) {
            return Optional.of(ZoneId.of(raw));
        }
        return Optional.empty();
    }

    public ZoneId resolveOrDefault(String location) {
        return resolve(location).orElse(ZoneId.of(DEFAULT_ZONE));
    }

    public ZoneId zoneOf(String zoneId) {
        try {
            return ZoneId.of(zoneId);
        } catch (Exception e) {
            return ZoneId.of(DEFAULT_ZONE);
        }
    }

    /** «Сегодня» в часовом поясе пользователя. */
    public LocalDate today(String zoneId, java.time.Clock clock) {
        return LocalDate.now(clock.withZone(zoneOf(zoneId)));
    }

    public LocalDateTime now(String zoneId, java.time.Clock clock) {
        return LocalDateTime.now(clock.withZone(zoneOf(zoneId)));
    }

    /** Смещение в формате UTC+3 для отображения. */
    public String offsetLabel(String zoneId, java.time.Clock clock) {
        ZoneId zone = zoneOf(zoneId);
        String off = zone.getRules().getOffset(clock.instant()).getId();
        if ("Z".equals(off)) return "UTC+00:00";
        return "UTC" + off;
    }
}