package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
    Optional<Course> findBySlug(String slug);
    java.util.List<Course> findByInstructorId(UUID instructorId);
}
