package com.university.schedulebot.dto;

import lombok.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

/** Результат работы сервиса — то, что бот должен отправить пользователю. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BotResponse {

    private Long chatId;
    private String text;
    private ReplyKeyboard keyboard;
    /** true — нужно отредактировать сообщение (для inline-навигации). */
    @Builder.Default
    private boolean editMessage = false;
    private Integer messageId;
    /** Текст всплывающего answerCallbackQuery. */
    private String callbackAnswer;

    public static BotResponse text(Long chatId, String text) {
        return BotResponse.builder().chatId(chatId).text(text).build();
    }

    public static BotResponse of(Long chatId, String text, ReplyKeyboard kb) {
        return BotResponse.builder().chatId(chatId).text(text).keyboard(kb).build();
    }

    public static BotResponse edit(Long chatId, Integer messageId, String text,
                                   InlineKeyboardMarkup kb) {
        return BotResponse.builder()
                .chatId(chatId).messageId(messageId).text(text)
                .keyboard(kb).editMessage(true).build();
    }

    public boolean hasReplyKeyboard() {
        return keyboard instanceof ReplyKeyboardMarkup;
    }

    public boolean hasInlineKeyboard() {
        return keyboard instanceof InlineKeyboardMarkup;
    }
}