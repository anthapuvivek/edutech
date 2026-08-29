package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    List<Quiz> findByCourseId(UUID courseId);
    List<Quiz> findByCourseIdIn(List<UUID> courseIds);
    List<Quiz> findByTeacherId(UUID teacherId);
}
