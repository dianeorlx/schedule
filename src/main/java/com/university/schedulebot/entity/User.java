package com.university.schedulebot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    private String username;

    @Column(name = "full_name")
    private String fullName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    private String location;

    @Builder.Default
    @Column(name = "zone_id", nullable = false)
    private String zoneId = "Europe/Moscow";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_state", nullable = false)
    private RegistrationState registrationState = RegistrationState.NONE;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    public boolean isRegistered() {
        return registrationState == RegistrationState.COMPLETED;
    }

    public boolean isTeacher() {
        return role != null && Role.TEACHER.equals(role.getCode());
    }

    public boolean isStudent() {
        return role != null && Role.STUDENT.equals(role.getCode());
    }
}