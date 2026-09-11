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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TimeZoneService timeZoneService;

    @Mock
    private InlineKeyboardFactory inlineKeyboards;

    @Mock
    private ReplyKeyboardFactory replyKeyboards;

    private Clock clock;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                Instant.parse("2024-01-01T12:00:00Z"),
                ZoneId.of("UTC")
        );

        registrationService = new RegistrationService(
                userService,
                roleRepository,
                groupRepository,
                departmentRepository,
                timeZoneService,
                inlineKeyboards,
                replyKeyboards,
                clock
        );
    }

    @Test
    void startShouldCreateRegistrationStateForNewUser() {
        UserRequest request = request();

        User user = new User();
        user.setRegistrationState(RegistrationState.NONE);

        when(userService.findOrCreate(request))
                .thenReturn(user);

        when(inlineKeyboards.roleSelection())
                .thenReturn(null);

        BotResponse response =
                registrationService.start(request);

        assertNotNull(response);
        assertEquals(
                RegistrationState.AWAITING_ROLE,
                user.getRegistrationState()
        );
        assertEquals("test_user", user.getUsername());
        assertEquals("Иван Иванов", user.getFullName());

        verify(userService).save(user);
        verify(inlineKeyboards).roleSelection();
    }

    @Test
    void startShouldReturnAlreadyRegisteredMessage() {
        UserRequest request = request();

        User user = new User();
        user.setRegistrationState(RegistrationState.COMPLETED);

        when(userService.findOrCreate(request))
                .thenReturn(user);

        when(replyKeyboards.studentMenu())
                .thenReturn(new ReplyKeyboardMarkup());

        BotResponse response =
                registrationService.start(request);

        assertNotNull(response);

        verify(userService, never()).save(any());
        verify(replyKeyboards).studentMenu();
    }

    @Test
    void handleRoleSelectionShouldReturnErrorWhenUserNotFound() {
        UserRequest request = request();

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        BotResponse response =
                registrationService.handleRoleSelection(
                        request,
                        Role.STUDENT
                );

        assertNotNull(response);
        verifyNoInteractions(roleRepository);
    }

    @Test
    void handleRoleSelectionShouldReturnErrorWhenWrongState() {
        UserRequest request = request();

        User user = new User();
        user.setRegistrationState(RegistrationState.COMPLETED);

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        BotResponse response =
                registrationService.handleRoleSelection(
                        request,
                        Role.STUDENT
                );

        assertNotNull(response);
        verifyNoInteractions(roleRepository);
    }

    @Test
    void handleRoleSelectionShouldReturnErrorForUnknownRole() {
        UserRequest request = request();

        User user = new User();
        user.setRegistrationState(RegistrationState.AWAITING_ROLE);

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByCode("unknown"))
                .thenReturn(Optional.empty());

        BotResponse response =
                registrationService.handleRoleSelection(
                        request,
                        "unknown"
                );

        assertNotNull(response);
        verify(userService, never()).save(any());
    }

    @Test
    void handleStudentRoleShouldReturnGroupKeyboard() {
        UserRequest request = request();

        User user = new User();
        user.setRegistrationState(RegistrationState.AWAITING_ROLE);

        Role role = studentRole();

        Group group = new Group();
        group.setName("ИС-21");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByCode(Role.STUDENT))
                .thenReturn(Optional.of(role));

        when(groupRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(group));

        when(inlineKeyboards.groups(anyList()))
                .thenReturn(null);

        BotResponse response =
                registrationService.handleRoleSelection(
                        request,
                        Role.STUDENT
                );

        assertNotNull(response);
        assertEquals(role, user.getRole());
        assertEquals(
                RegistrationState.AWAITING_GROUP,
                user.getRegistrationState()
        );

        verify(userService).save(user);
        verify(inlineKeyboards).groups(List.of(group));
    }

    @Test
    void handleStudentRoleShouldReturnErrorWhenGroupsAreEmpty() {
        UserRequest request = request();

        User user = new User();
        user.setRegistrationState(RegistrationState.AWAITING_ROLE);

        Role role = studentRole();

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByCode(Role.STUDENT))
                .thenReturn(Optional.of(role));

        when(groupRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of());

        BotResponse response =
                registrationService.handleRoleSelection(
                        request,
                        Role.STUDENT
                );

        assertNotNull(response);
        assertEquals(
                RegistrationState.AWAITING_GROUP,
                user.getRegistrationState()
        );

        verify(userService).save(user);
    }

    @Test
    void handleTeacherRoleShouldReturnDepartmentKeyboard() {
        UserRequest request = request();

        User user = new User();
        user.setRegistrationState(RegistrationState.AWAITING_ROLE);

        Role role = teacherRole();

        Department department = new Department();
        department.setName("Кафедра информатики");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByCode(Role.TEACHER))
                .thenReturn(Optional.of(role));

        when(departmentRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(department));

        when(inlineKeyboards.departments(anyList()))
                .thenReturn(null);

        BotResponse response =
                registrationService.handleRoleSelection(
                        request,
                        Role.TEACHER
                );

        assertNotNull(response);
        assertEquals(role, user.getRole());
        assertEquals(
                RegistrationState.AWAITING_DEPARTMENT,
                user.getRegistrationState()
        );

        verify(userService).save(user);
        verify(inlineKeyboards).departments(List.of(department));
    }

    @Test
    void handleTeacherRoleShouldReturnErrorWhenDepartmentsAreEmpty() {
        UserRequest request = request();

        User user = new User();
        user.setRegistrationState(RegistrationState.AWAITING_ROLE);

        Role role = teacherRole();

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByCode(Role.TEACHER))
                .thenReturn(Optional.of(role));

        when(departmentRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of());

        BotResponse response =
                registrationService.handleRoleSelection(
                        request,
                        Role.TEACHER
                );

        assertNotNull(response);
        assertEquals(
                RegistrationState.AWAITING_DEPARTMENT,
                user.getRegistrationState()
        );

        verify(userService).save(user);
    }

    @Test
    void handleGroupSelectionShouldAssignGroupAndDepartment() {
        UserRequest request = request();

        User user = new User();
        user.setRegistrationState(RegistrationState.AWAITING_GROUP);

        Department department = new Department();
        department.setName("Кафедра информатики");

        Group group = new Group();
        group.setName("ИС-21");
        group.setDepartment(department);

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(groupRepository.findById(1L))
                .thenReturn(Optional.of(group));

        BotResponse response =
                registrationService.handleGroupSelection(
                        request,
                        1L
                );

        assertNotNull(response);
        assertEquals(group, user.getGroup());
        assertEquals(department, user.getDepartment());
        assertEquals(
                RegistrationState.AWAITING_LOCATION,
                user.getRegistrationState()
        );

        verify(userService).save(user);
    }

    @Test
    void handleGroupSelectionShouldReturnErrorWhenUserNotFound() {
        UserRequest request = request();

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        BotResponse response =
                registrationService.handleGroupSelection(
                        request,
                        1L
                );

        assertNotNull(response);
        verifyNoInteractions(groupRepository);
    }

    @Test
    void handleGroupSelectionShouldReturnErrorForInvalidGroup() {
        UserRequest request = request();

        User user = new User();
        user.setRegistrationState(RegistrationState.AWAITING_GROUP);

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(groupRepository.findById(1L))
                .thenReturn(Optional.empty());

        BotResponse response =
                registrationService.handleGroupSelection(
                        request,
                        1L
                );

        assertNotNull(response);
        verify(userService, never()).save(any());
    }

    @Test
    void handleDepartmentSelectionShouldAssignDepartment() {
        UserRequest request = request();

        User user = new User();
        user.setRegistrationState(
                RegistrationState.AWAITING_DEPARTMENT
        );

        Department department = new Department();
        department.setName("Кафедра математики");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(departmentRepository.findById(10L))
                .thenReturn(Optional.of(department));

        BotResponse response =
                registrationService.handleDepartmentSelection(
                        request,
                        10L
                );

        assertNotNull(response);
        assertEquals(department, user.getDepartment());
        assertEquals(
                RegistrationState.AWAITING_LOCATION,
                user.getRegistrationState()
        );

        verify(userService).save(user);
    }

    @Test
    void handleDepartmentSelectionShouldReturnErrorWhenUserNotFound() {
        UserRequest request = request();

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        BotResponse response =
                registrationService.handleDepartmentSelection(
                        request,
                        10L
                );

        assertNotNull(response);
        verifyNoInteractions(departmentRepository);
    }

    @Test
    void handleDepartmentSelectionShouldReturnErrorForInvalidDepartment() {
        UserRequest request = request();

        User user = new User();
        user.setRegistrationState(
                RegistrationState.AWAITING_DEPARTMENT
        );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(departmentRepository.findById(10L))
                .thenReturn(Optional.empty());

        BotResponse response =
                registrationService.handleDepartmentSelection(
                        request,
                        10L
                );

        assertNotNull(response);
        verify(userService, never()).save(any());
    }

    @Test
    void handleLocationShouldReturnErrorWhenUserNotFound() {
        UserRequest request = request();

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.empty());

        BotResponse response =
                registrationService.handleLocation(request);

        assertNotNull(response);
        verifyNoInteractions(timeZoneService);
    }

    @Test
    void handleLocationShouldReturnErrorWhenWrongState() {
        UserRequest request = request();

        User user = new User();
        user.setRegistrationState(RegistrationState.COMPLETED);

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        BotResponse response =
                registrationService.handleLocation(request);

        assertNotNull(response);
        verifyNoInteractions(timeZoneService);
    }

    @Test
    void handleLocationShouldReturnErrorWhenTimezoneCannotBeResolved() {
        UserRequest request = request();
        request.setText("Неизвестный город");

        User user = new User();
        user.setRegistrationState(
                RegistrationState.AWAITING_LOCATION
        );

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(timeZoneService.resolve("Неизвестный город"))
                .thenReturn(Optional.empty());

        BotResponse response =
                registrationService.handleLocation(request);

        assertNotNull(response);
        assertEquals(
                RegistrationState.AWAITING_LOCATION,
                user.getRegistrationState()
        );

        verify(userService, never()).save(any());
    }

    @Test
    void handleLocationShouldCompleteStudentRegistration() {
        UserRequest request = request();
        request.setText("Москва");

        User user = new User();
        user.setRegistrationState(
                RegistrationState.AWAITING_LOCATION
        );
        user.setRole(studentRole());

        Group group = new Group();
        group.setName("ИС-21");
        user.setGroup(group);

        ZoneId moscow = ZoneId.of("Europe/Moscow");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(timeZoneService.resolve("Москва"))
                .thenReturn(Optional.of(moscow));

        when(timeZoneService.offsetLabel(
                "Europe/Moscow",
                clock
        )).thenReturn("UTC+03:00");

        when(replyKeyboards.studentMenu())
                .thenReturn(new ReplyKeyboardMarkup());

        BotResponse response =
                registrationService.handleLocation(request);

        assertNotNull(response);
        assertEquals("Москва", user.getLocation());
        assertEquals("Europe/Moscow", user.getZoneId());
        assertEquals(
                RegistrationState.COMPLETED,
                user.getRegistrationState()
        );
        assertEquals(
                LocalDateTime.of(2024, 1, 1, 12, 0),
                user.getRegisteredAt()
        );

        verify(userService).save(user);
        verify(replyKeyboards).studentMenu();
    }

    @Test
    void handleLocationShouldCompleteTeacherRegistration() {
        UserRequest request = request();
        request.setText("Москва");

        User user = new User();
        user.setRegistrationState(
                RegistrationState.AWAITING_LOCATION
        );
        user.setRole(teacherRole());

        Department department = new Department();
        department.setName("Кафедра информатики");
        user.setDepartment(department);

        ZoneId moscow = ZoneId.of("Europe/Moscow");

        when(userService.find(request.getChatId()))
                .thenReturn(Optional.of(user));

        when(timeZoneService.resolve("Москва"))
                .thenReturn(Optional.of(moscow));

        when(timeZoneService.offsetLabel(
                "Europe/Moscow",
                clock
        )).thenReturn("UTC+03:00");

        when(replyKeyboards.teacherMenu())
                .thenReturn(new ReplyKeyboardMarkup());

        BotResponse response =
                registrationService.handleLocation(request);

        assertNotNull(response);
        assertEquals("Москва", user.getLocation());
        assertEquals("Europe/Moscow", user.getZoneId());
        assertEquals(
                RegistrationState.COMPLETED,
                user.getRegistrationState()
        );
        assertEquals(
                LocalDateTime.of(2024, 1, 1, 12, 0),
                user.getRegisteredAt()
        );

        verify(userService).save(user);
        verify(replyKeyboards).teacherMenu();
    }

    @Test
    void isAwaitingInputShouldReturnTrueOnlyForLocationState() {
        User user = new User();

        user.setRegistrationState(
                RegistrationState.AWAITING_LOCATION
        );

        assertTrue(registrationService.isAwaitingInput(user));

        user.setRegistrationState(
                RegistrationState.COMPLETED
        );

        assertFalse(registrationService.isAwaitingInput(user));
        assertFalse(registrationService.isAwaitingInput(null));
    }

    private UserRequest request() {
        UserRequest request = new UserRequest();
        request.setChatId(100L);
        request.setUsername("test_user");
        request.setFullName("Иван Иванов");
        request.setText("Москва");
        return request;
    }

    private Role studentRole() {
        Role role = new Role();
        role.setCode(Role.STUDENT);
        return role;
    }

    private Role teacherRole() {
        Role role = new Role();
        role.setCode(Role.TEACHER);
        return role;
    }
}