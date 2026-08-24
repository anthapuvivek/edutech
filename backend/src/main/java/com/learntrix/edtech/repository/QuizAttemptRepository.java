package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    List<QuizAttempt> findByStudentId(UUID studentId);
    List<QuizAttempt> findByQuizId(UUID quizId);
}
