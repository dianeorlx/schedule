package com.university.schedulebot.dto;

import lombok.*;

/** Нормализованное входящее сообщение (message или callback). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRequest {
    private Long chatId;
    private String username;
    private String fullName;
    private String text;
    private String callbackData;
    private Integer messageId;
    private String callbackQueryId;

    public boolean isCallback() {
        return callbackData != null;
    }
}