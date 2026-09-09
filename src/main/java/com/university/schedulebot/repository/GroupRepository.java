package com.university.schedulebot.repository;

import com.university.schedulebot.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByNameIgnoreCase(String name);
    List<Group> findAllByOrderByNameAsc();
}