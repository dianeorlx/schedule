package com.university.schedulebot.service;

import com.university.schedulebot.dto.BotResponse;
import com.university.schedulebot.dto.UserRequest;
import com.university.schedulebot.entity.Department;
import com.university.schedulebot.entity.Group;
import com.university.schedulebot.entity.RegistrationState;
import com.university.schedulebot.entity.Role;
import com.university.schedulebot.entity.User;
import com.university.schedulebot.keyboard.InlineKeyboardFactory;
import com.university.schedulebot.keyboard.ReplyKeyboardFactory;
import com.university.schedulebot.repository.DepartmentRepository;
import com.university.schedulebot.repository.GroupRepository;
import com.university.schedulebot.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateDispatcherTest {

    @Mock
    private CommandService commandService;

    @Mock
    private RegistrationService registrationService;

    @Mock
    private UserService userService;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private LessonManagementService lessonService;

    @Mock
    private DateParserService dateParser;

    @Mock
    private InlineKeyboardFactory inlineKeyboards;

    private UpdateDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new UpdateDispatcher(
                commandService,
                registrationService,
                userService,
                scheduleService,
                lessonService,
                dateParser,
                inlineKeyboards
        );
    }

    @Test
    void dispatchShouldHandleTextRequest() {
        UserRequest request = textRequest("/help");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        when(commandService.help(
                request.getChatId(),
                Optional.empty()
        )).thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(commandService).help(
                request.getChatId(),
                Optional.empty()
        );
    }

    @Test
    void dispatchShouldHandleCallbackRequest() {
        UserRequest request = callbackRequest("UNKNOWN");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(userService).find(request.getChatId());
    }

    @Test
    void startCommandShouldBeDispatched() {
        UserRequest request = textRequest("/start");
        Optional<User> user = Optional.empty();

        when(userService.find(request.getChatId()))
                .thenReturn(user);

        when(commandService.start(
                request.getChatId(),
                user
        )).thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(commandService).start(
                request.getChatId(),
                user
        );
    }

    @Test
    void helpCommandShouldBeDispatched() {
        UserRequest request = textRequest("/help");
        Optional<User> user = Optional.empty();

        when(userService.find(request.getChatId()))
                .thenReturn(user);

        when(commandService.help(
                request.getChatId(),
                user
        )).thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(commandService).help(
                request.getChatId(),
                user
        );
    }

    @Test
    void infoCommandShouldBeDispatched() {
        UserRequest request = textRequest("/info");
        Optional<User> user = Optional.empty();

        when(userService.find(request.getChatId()))
                .thenReturn(user);

        when(commandService.info(
                request.getChatId(),
                user
        )).thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(commandService).info(
                request.getChatId(),
                user
        );
    }

    @Test
    void registerCommandShouldBeDispatched() {
        UserRequest request = textRequest("/register");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        when(registrationService.start(request))
                .thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(registrationService).start(request);
    }

    @Test
    void profileButtonShouldBeNormalizedToProfileCommand() {
        User user = registeredUser();
        Optional<User> optionalUser = Optional.of(user);

        UserRequest request = textRequest(
                ReplyKeyboardFactory.BTN_PROFILE
        );

        when(userService.find(request.getChatId()))
                .thenReturn(optionalUser);

        when(userService.profileText(user))
                .thenReturn("Профиль");

        when(commandService.menuFor(optionalUser))
                .thenReturn(new ReplyKeyboardMarkup());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(userService).profileText(user);
        verify(commandService).menuFor(optionalUser);
    }

    @Test
    void scheduleButtonShouldBeNormalizedToShowCommand() {
        User user = registeredUser();
        UserRequest request = textRequest(
                ReplyKeyboardFactory.BTN_SCHEDULE
        );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(scheduleService.show(user))
                .thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(scheduleService).show(user);
    }

    @Test
    void showCommandShouldRequireRegisteredUser() {
        UserRequest request = textRequest("/show");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        when(commandService.menuFor(Optional.empty()))
                .thenReturn(new ReplyKeyboardMarkup());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(commandService).menuFor(Optional.empty());
        verifyNoInteractions(scheduleService);
    }

    @Test
    void showCommandShouldBeDispatchedForRegisteredUser() {
        User user = registeredUser();
        UserRequest request = textRequest("/show");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(scheduleService.show(user))
                .thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(scheduleService).show(user);
    }

    @Test
    void scheduleCommandWithoutArgumentsShouldShowSchedule() {
        User user = registeredUser();
        UserRequest request = textRequest("/schedule");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(scheduleService.show(user))
                .thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(scheduleService).show(user);
        verify(dateParser, never())
                .parseRange(anyString(), any(LocalDate.class));
    }

    @Test
    void scheduleCommandWithValidDateShouldRenderSchedule() {
        User user = registeredUser();
        UserRequest request = textRequest(
                "/schedule 13.05.2024"
        );

        LocalDate today = LocalDate.of(2024, 5, 1);

        DateParserService.DateRange range =
                new DateParserService.DateRange(
                        LocalDate.of(2024, 5, 13),
                        LocalDate.of(2024, 5, 13)
                );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(scheduleService.today(user))
                .thenReturn(today);

        when(dateParser.parseRange(
                "13.05.2024",
                today
        )).thenReturn(Optional.of(range));

        when(scheduleService.render(
                user,
                range.getFrom(),
                range.getTo()
        )).thenReturn("Расписание");

        when(inlineKeyboards.showNavigation())
                .thenReturn(null);

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(scheduleService).today(user);

        verify(dateParser).parseRange(
                "13.05.2024",
                today
        );

        verify(scheduleService).render(
                user,
                range.getFrom(),
                range.getTo()
        );

        verify(inlineKeyboards).showNavigation();
    }

    @Test
    void scheduleCommandWithInvalidDateShouldReturnError() {
        User user = registeredUser();
        UserRequest request = textRequest(
                "/schedule неправильная-дата"
        );

        LocalDate today = LocalDate.of(2024, 5, 1);

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(scheduleService.today(user))
                .thenReturn(today);

        when(dateParser.parseRange(
                "неправильная-дата",
                today
        )).thenReturn(Optional.empty());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(scheduleService).today(user);

        verify(dateParser).parseRange(
                "неправильная-дата",
                today
        );

        verify(scheduleService, never())
                .render(any(), any(), any());
    }

    @Test
    void addLessonCommandShouldBeDispatched() {
        User user = registeredUser();
        UserRequest request = textRequest(
                "/addlesson Математика"
        );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(lessonService.create(
                user,
                "Математика"
        )).thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(lessonService).create(
                user,
                "Математика"
        );
    }

    @Test
    void myLessonsCommandShouldBeDispatched() {
        User user = registeredUser();
        UserRequest request = textRequest("/mylessons");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(lessonService.myLessons(user))
                .thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(lessonService).myLessons(user);
    }

    @Test
    void editLessonCommandShouldBeDispatched() {
        User user = registeredUser();

        UserRequest request = textRequest(
                "/editlesson 15 | title=Математика"
        );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(lessonService.edit(
                user,
                15L,
                " title=Математика"
        )).thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(lessonService).edit(
                user,
                15L,
                " title=Математика"
        );
    }

    @Test
    void editLessonCommandShouldReturnErrorForInvalidId() {
        User user = registeredUser();
        UserRequest request = textRequest(
                "/editlesson abc | title=Математика"
        );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(lessonService, never())
                .edit(any(), anyLong(), anyString());
    }

    @Test
    void cancelLessonCommandShouldBeDispatched() {
        User user = registeredUser();
        UserRequest request = textRequest(
                "/cancellesson 25"
        );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(lessonService.cancel(user, 25L))
                .thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(lessonService).cancel(user, 25L);
    }

    @Test
    void cancelLessonCommandShouldReturnErrorForInvalidId() {
        User user = registeredUser();
        UserRequest request = textRequest(
                "/cancellesson abc"
        );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(lessonService, never())
                .cancel(any(User.class), anyLong());
    }

    @Test
    void deleteMeCommandShouldAskForConfirmation() {
        UserRequest request = textRequest("/delete_me");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        when(inlineKeyboards.deleteConfirmation())
                .thenReturn(null);

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(inlineKeyboards).deleteConfirmation();
    }

    @Test
    void roleCallbackShouldBeDispatched() {
        UserRequest request = callbackRequest("ROLE:STUDENT");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        when(registrationService.handleRoleSelection(
                request,
                "STUDENT"
        )).thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(registrationService).handleRoleSelection(
                request,
                "STUDENT"
        );
    }

    @Test
    void groupCallbackShouldBeDispatched() {
        UserRequest request = callbackRequest(
                InlineKeyboardFactory.CB_GROUP_PREFIX + "10"
        );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        when(registrationService.handleGroupSelection(
                request,
                10L
        )).thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(registrationService).handleGroupSelection(
                request,
                10L
        );
    }

    @Test
    void departmentCallbackShouldBeDispatched() {
        UserRequest request = callbackRequest(
                InlineKeyboardFactory.CB_DEPT_PREFIX + "20"
        );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        when(registrationService.handleDepartmentSelection(
                request,
                20L
        )).thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(registrationService).handleDepartmentSelection(
                request,
                20L
        );
    }

    @Test
    void showCallbackShouldBeDispatchedForRegisteredUser() {
        User user = registeredUser();

        UserRequest request = callbackRequest("SHOW:NEXT");
        request.setMessageId(50);

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(scheduleService.handleNavigation(
                user,
                "SHOW:NEXT",
                50
        )).thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(scheduleService).handleNavigation(
                user,
                "SHOW:NEXT",
                50
        );
    }

    @Test
    void cancelCallbackShouldBeDispatched() {
        User user = registeredUser();

        UserRequest request = callbackRequest(
                InlineKeyboardFactory.CB_CANCEL_PREFIX + "30"
        );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(lessonService.cancel(user, 30L))
                .thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(lessonService).cancel(user, 30L);
    }

    @Test
    void editCallbackShouldReturnEditInstruction() {
        UserRequest request = callbackRequest(
                InlineKeyboardFactory.CB_EDIT_PREFIX + "45"
        );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verifyNoInteractions(lessonService);
    }

    @Test
    void deleteConfirmationCallbackShouldDeleteUserData() {
        UserRequest request = callbackRequest(
                InlineKeyboardFactory.CB_DELETE_CONFIRM
        );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        when(userService.deleteAllUserData(
                request.getChatId()
        )).thenReturn(true);

        when(commandService.menuFor(Optional.empty()))
                .thenReturn(new ReplyKeyboardMarkup());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(userService).deleteAllUserData(
                request.getChatId()
        );

        verify(commandService).menuFor(Optional.empty());
    }

    @Test
    void deleteAbortCallbackShouldCancelDeletion() {
        User user = registeredUser();
        Optional<User> optionalUser = Optional.of(user);

        UserRequest request = callbackRequest(
                InlineKeyboardFactory.CB_DELETE_ABORT
        );

        when(userService.find(request.getChatId()))
                .thenReturn(optionalUser);

        when(commandService.menuFor(optionalUser))
                .thenReturn(new ReplyKeyboardMarkup());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(commandService).menuFor(optionalUser);

        verify(userService, never())
                .deleteAllUserData(anyLong());
    }

    @Test
    void unknownCommandShouldBeHandled() {
        UserRequest request = textRequest("/unknown");
        Optional<User> user = Optional.empty();

        when(userService.find(request.getChatId()))
                .thenReturn(user);

        when(commandService.unknown(
                request.getChatId(),
                user
        )).thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(commandService).unknown(
                request.getChatId(),
                user
        );
    }

    @Test
    void textDuringLocationRegistrationShouldBeHandledAsLocation() {
        User user = new User();
        user.setRegistrationState(
                RegistrationState.AWAITING_LOCATION
        );

        UserRequest request = textRequest("Москва");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(registrationService.isAwaitingInput(user))
                .thenReturn(true);

        when(registrationService.handleLocation(request))
                .thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(registrationService).handleLocation(request);
        verifyNoInteractions(commandService);
    }

    @Test
    void commandDuringLocationRegistrationShouldNotBeHandledAsLocation() {
        User user = new User();
        user.setRegistrationState(
                RegistrationState.AWAITING_LOCATION
        );

        UserRequest request = textRequest("/help");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(registrationService.isAwaitingInput(user))
                .thenReturn(true);

        when(commandService.help(
                request.getChatId(),
                Optional.of(user)
        )).thenReturn(response());

        BotResponse result = dispatcher.dispatch(request);

        assertNotNull(result);

        verify(registrationService, never())
                .handleLocation(request);

        verify(commandService).help(
                request.getChatId(),
                Optional.of(user)
        );
    }

    private User registeredUser() {
        User user = new User();
        user.setChatId(100L);
        user.setRegistrationState(
                RegistrationState.COMPLETED
        );
        return user;
    }

    private UserRequest textRequest(String text) {
        UserRequest request = new UserRequest();
        request.setChatId(100L);
        request.setText(text);
        return request;
    }

    private UserRequest callbackRequest(String callbackData) {
        UserRequest request = new UserRequest();
        request.setChatId(100L);
        request.setCallbackData(callbackData);
        request.setMessageId(1);
        return request;
    }

    private BotResponse response() {
        return BotResponse.text(100L, "OK");
    }
}