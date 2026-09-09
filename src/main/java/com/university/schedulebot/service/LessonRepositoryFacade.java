package com.university.schedulebot.service;

import com.university.schedulebot.entity.Lesson;
import com.university.schedulebot.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LessonRepositoryFacade {

    private final LessonRepository lessonRepository;

    public List<Lesson> byGroup(Long groupId, LocalDate from, LocalDate to) {
        return lessonRepository.findByGroupAndPeriod(groupId, from, to);
    }

    public List<Lesson> byTeacher(Long teacherId, LocalDate from, LocalDate to) {
        return lessonRepository.findByTeacherAndPeriod(teacherId, from, to);
    }
}