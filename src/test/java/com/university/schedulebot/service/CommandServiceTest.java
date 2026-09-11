package com.university.schedulebot.service;

import com.university.schedulebot.dto.BotResponse;
import com.university.schedulebot.entity.User;
import com.university.schedulebot.keyboard.ReplyKeyboardFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandServiceTest {

    @Mock
    private ReplyKeyboardFactory replyKeyboardFactory;

    @Mock
    private ReplyKeyboardMarkup guestMenu;

    @Mock
    private ReplyKeyboardMarkup studentMenu;

    @Mock
    private ReplyKeyboardMarkup teacherMenu;

    private CommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new CommandService(replyKeyboardFactory);
    }

    @Test
    void help_shouldReturnHelpTextAndGuestMenuForEmptyUser() {
        Long chatId = 1L;

        when(replyKeyboardFactory.guestMenu()).thenReturn(guestMenu);

        BotResponse response = commandService.help(chatId, Optional.empty());

        assertNotNull(response);
        assertEquals(chatId, response.getChatId());
        assertEquals(CommandService.HELP_TEXT, response.getText());
        assertSame(guestMenu, response.getKeyboard());
        assertFalse(response.isEditMessage());

        verify(replyKeyboardFactory).guestMenu();
    }

    @Test
    void info_shouldReturnInfoTextAndStudentMenuForRegisteredStudent() {
        Long chatId = 2L;
        User user = mock(User.class);

        when(user.isRegistered()).thenReturn(true);
        when(user.isTeacher()).thenReturn(false);
        when(replyKeyboardFactory.studentMenu()).thenReturn(studentMenu);

        BotResponse response = commandService.info(chatId, Optional.of(user));

        assertNotNull(response);
        assertEquals(chatId, response.getChatId());
        assertEquals(CommandService.INFO_TEXT, response.getText());
        assertSame(studentMenu, response.getKeyboard());

        verify(replyKeyboardFactory).studentMenu();
    }

    @Test
    void start_shouldReturnWelcomeMessageForEmptyUser() {
        Long chatId = 3L;

        when(replyKeyboardFactory.guestMenu()).thenReturn(guestMenu);

        BotResponse response = commandService.start(chatId, Optional.empty());

        assertNotNull(response);
        assertEquals(chatId, response.getChatId());
        assertTrue(response.getText().contains("Добро пожаловать"));
        assertTrue(response.getText().contains("/register"));
        assertTrue(response.getText().contains("/help"));
        assertSame(guestMenu, response.getKeyboard());

        verify(replyKeyboardFactory).guestMenu();
    }

    @Test
    void start_shouldReturnWelcomeMessageForUnregisteredUser() {
        Long chatId = 4L;
        User user = mock(User.class);

        when(user.isRegistered()).thenReturn(false);
        when(replyKeyboardFactory.guestMenu()).thenReturn(guestMenu);

        BotResponse response = commandService.start(chatId, Optional.of(user));

        assertNotNull(response);
        assertEquals(chatId, response.getChatId());
        assertTrue(response.getText().contains("Добро пожаловать"));
        assertSame(guestMenu, response.getKeyboard());

        verify(replyKeyboardFactory).guestMenu();
    }

    @Test
    void start_shouldReturnReturningMessageWithFullNameForRegisteredStudent() {
        Long chatId = 5L;
        User user = mock(User.class);

        when(user.isRegistered()).thenReturn(true);
        when(user.getFullName()).thenReturn("Иван Иванов");
        when(user.isTeacher()).thenReturn(false);
        when(replyKeyboardFactory.studentMenu()).thenReturn(studentMenu);

        BotResponse response = commandService.start(chatId, Optional.of(user));

        assertNotNull(response);
        assertEquals(chatId, response.getChatId());
        assertEquals("👋 С возвращением, Иван Иванов!", response.getText());
        assertSame(studentMenu, response.getKeyboard());

        verify(replyKeyboardFactory).studentMenu();
    }

    @Test
    void start_shouldUseDefaultNameWhenFullNameIsNull() {
        Long chatId = 6L;
        User user = mock(User.class);

        when(user.isRegistered()).thenReturn(true);
        when(user.getFullName()).thenReturn(null);
        when(user.isTeacher()).thenReturn(false);
        when(replyKeyboardFactory.studentMenu()).thenReturn(studentMenu);

        BotResponse response = commandService.start(chatId, Optional.of(user));

        assertNotNull(response);
        assertEquals(chatId, response.getChatId());
        assertEquals("👋 С возвращением, пользователь!", response.getText());
        assertSame(studentMenu, response.getKeyboard());

        verify(replyKeyboardFactory).studentMenu();
    }

    @Test
    void start_shouldReturnReturningMessageAndTeacherMenuForRegisteredTeacher() {
        Long chatId = 7L;
        User user = mock(User.class);

        when(user.isRegistered()).thenReturn(true);
        when(user.getFullName()).thenReturn("Петров Пётр");
        when(user.isTeacher()).thenReturn(true);
        when(replyKeyboardFactory.teacherMenu()).thenReturn(teacherMenu);

        BotResponse response = commandService.start(chatId, Optional.of(user));

        assertNotNull(response);
        assertEquals(chatId, response.getChatId());
        assertEquals("👋 С возвращением, Петров Пётр!", response.getText());
        assertSame(teacherMenu, response.getKeyboard());

        verify(replyKeyboardFactory).teacherMenu();
    }

    @Test
    void unknown_shouldReturnUnknownCommandMessageForEmptyUser() {
        Long chatId = 8L;

        when(replyKeyboardFactory.guestMenu()).thenReturn(guestMenu);

        BotResponse response = commandService.unknown(chatId, Optional.empty());

        assertNotNull(response);
        assertEquals(chatId, response.getChatId());
        assertEquals(
                "Неизвестная команда. Используйте /help для списка доступных команд.",
                response.getText()
        );
        assertSame(guestMenu, response.getKeyboard());

        verify(replyKeyboardFactory).guestMenu();
    }

    @Test
    void unknown_shouldReturnUnknownCommandMessageForRegisteredStudent() {
        Long chatId = 9L;
        User user = mock(User.class);

        when(user.isRegistered()).thenReturn(true);
        when(user.isTeacher()).thenReturn(false);
        when(replyKeyboardFactory.studentMenu()).thenReturn(studentMenu);

        BotResponse response = commandService.unknown(chatId, Optional.of(user));

        assertNotNull(response);
        assertEquals(chatId, response.getChatId());
        assertEquals(
                "Неизвестная команда. Используйте /help для списка доступных команд.",
                response.getText()
        );
        assertSame(studentMenu, response.getKeyboard());

        verify(replyKeyboardFactory).studentMenu();
    }

    @Test
    void menuFor_shouldReturnGuestMenuWhenUserIsEmpty() {
        when(replyKeyboardFactory.guestMenu()).thenReturn(guestMenu);

        ReplyKeyboardMarkup result =
                commandService.menuFor(Optional.empty());

        assertSame(guestMenu, result);

        verify(replyKeyboardFactory).guestMenu();
        verifyNoMoreInteractions(replyKeyboardFactory);
    }

    @Test
    void menuFor_shouldReturnGuestMenuWhenUserIsNotRegistered() {
        User user = mock(User.class);

        when(user.isRegistered()).thenReturn(false);
        when(replyKeyboardFactory.guestMenu()).thenReturn(guestMenu);

        ReplyKeyboardMarkup result =
                commandService.menuFor(Optional.of(user));

        assertSame(guestMenu, result);

        verify(replyKeyboardFactory).guestMenu();
        verify(user, never()).isTeacher();
    }

    @Test
    void menuFor_shouldReturnStudentMenuForRegisteredStudent() {
        User user = mock(User.class);

        when(user.isRegistered()).thenReturn(true);
        when(user.isTeacher()).thenReturn(false);
        when(replyKeyboardFactory.studentMenu()).thenReturn(studentMenu);

        ReplyKeyboardMarkup result =
                commandService.menuFor(Optional.of(user));

        assertSame(studentMenu, result);

        verify(replyKeyboardFactory).studentMenu();
    }

    @Test
    void menuFor_shouldReturnTeacherMenuForRegisteredTeacher() {
        User user = mock(User.class);

        when(user.isRegistered()).thenReturn(true);
        when(user.isTeacher()).thenReturn(true);
        when(replyKeyboardFactory.teacherMenu()).thenReturn(teacherMenu);

        ReplyKeyboardMarkup result =
                commandService.menuFor(Optional.of(user));

        assertSame(teacherMenu, result);

        verify(replyKeyboardFactory).teacherMenu();
    }
}