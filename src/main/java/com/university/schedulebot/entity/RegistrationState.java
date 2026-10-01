package com.university.schedulebot.entity;

/** Конечный автомат регистрации. */
public enum RegistrationState {
    NONE,
    AWAITING_ROLE,
    AWAITING_GROUP,
    AWAITING_GROUP_NAME,
    AWAITING_DEPARTMENT,
    AWAITING_LOCATION,
    PENDING_APPROVAL,
    COMPLETED
}