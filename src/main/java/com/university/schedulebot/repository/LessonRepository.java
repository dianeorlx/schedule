package com.university.schedulebot.repository;

import com.university.schedulebot.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("""
           SELECT l FROM Lesson l
           WHERE l.group.id = :groupId
             AND l.lessonDate BETWEEN :from AND :to
           ORDER BY l.lessonDate ASC, l.startTime ASC
           """)
    List<Lesson> findByGroupAndPeriod(@Param("groupId") Long groupId,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);

    @Query("""
           SELECT l FROM Lesson l
           WHERE l.teacher.id = :teacherId
             AND l.lessonDate BETWEEN :from AND :to
           ORDER BY l.lessonDate ASC, l.startTime ASC
           """)
    List<Lesson> findByTeacherAndPeriod(@Param("teacherId") Long teacherId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);

    void deleteAllByTeacherId(Long teacherId);
    void deleteAllByCreatedById(Long userId);
}