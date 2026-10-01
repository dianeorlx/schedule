package com.university.schedulebot.repository;

import com.university.schedulebot.entity.Lesson;
import com.university.schedulebot.entity.RegistrationState;
import com.university.schedulebot.entity.User;
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
    @Query("""
       SELECT u FROM User u
       WHERE u.group.id = :groupId AND u.role.code = :roleCode
       ORDER BY u.fullName ASC
       """)
    List<User> findStudentsByGroupId(@Param("groupId") Long groupId,
                                     @Param("roleCode") String roleCode);

    @Query("""
       SELECT u FROM User u
       WHERE u.registrationState = :state AND u.role.code = :roleCode
       ORDER BY u.fullName ASC
       """)
    List<User> findStudentsByStateAndRole(@Param("state") RegistrationState state,
                                          @Param("roleCode") String roleCode);

    void deleteAllByTeacherId(Long teacherId);
    void deleteAllByCreatedById(Long userId);
    boolean existsByGroupId(Long groupId);
}