package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.QuizAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, UUID> {

    List<QuizAttemptAnswer> findByAttemptId(UUID attemptId);
}
