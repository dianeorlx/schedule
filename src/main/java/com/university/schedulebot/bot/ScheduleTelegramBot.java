package com.university.schedulebot.bot;

import com.university.schedulebot.config.BotConfig;
import com.university.schedulebot.dto.BotResponse;
import com.university.schedulebot.dto.UserRequest;
import com.university.schedulebot.service.UpdateDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;
    private final UpdateDispatcher dispatcher;

    @Override
    public String getBotUsername() {
        return config.getName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        UserRequest request = toRequest(update);
        if (request == null) return;
        try {
            BotResponse response = dispatcher.dispatch(request);
            send(response, request);
        } catch (Exception e) {
            log.error("Ошибка обработки обновления", e);
        }
    }

    UserRequest toRequest(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message m = update.getMessage();
            return UserRequest.builder()
                    .chatId(m.getChatId())
                    .username(m.getFrom().getUserName())
                    .fullName(fullName(m.getFrom().getFirstName(), m.getFrom().getLastName()))
                    .text(m.getText())
                    .messageId(m.getMessageId())
                    .build();
        }
        if (update.hasCallbackQuery()) {
            CallbackQuery q = update.getCallbackQuery();
            return UserRequest.builder()
                    .chatId(q.getMessage().getChatId())
                    .username(q.getFrom().getUserName())
                    .fullName(fullName(q.getFrom().getFirstName(), q.getFrom().getLastName()))
                    .callbackData(q.getData())
                    .messageId(q.getMessage().getMessageId())
                    .callbackQueryId(q.getId())
                    .build();
        }
        return null;
    }

    private String fullName(String first, String last) {
        return last == null ? first : first + " " + last;
    }

    private void send(BotResponse response, UserRequest request) throws TelegramApiException {
        if (request.getCallbackQueryId() != null) {
            execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(request.getCallbackQueryId())
                    .text(response.getCallbackAnswer() == null ? "" : response.getCallbackAnswer())
                    .build());
        }
        if (response.isEditMessage() && response.getMessageId() != null) {
            execute(EditMessageText.builder()
                    .chatId(response.getChatId().toString())
                    .messageId(response.getMessageId())
                    .text(response.getText())
                    .parseMode(ParseMode.MARKDOWN)
                    .replyMarkup((InlineKeyboardMarkup) response.getKeyboard())
                    .build());
            return;
        }
        SendMessage message = SendMessage.builder()
                .chatId(response.getChatId().toString())
                .text(response.getText())
                .parseMode(ParseMode.MARKDOWN)
                .build();
        if (response.getKeyboard() != null) {
            message.setReplyMarkup(response.getKeyboard());
        }
        execute(message);
    }
}