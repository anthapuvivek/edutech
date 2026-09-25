package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    List<QuizQuestion> findByQuizIdOrderBySequenceNumberAsc(UUID quizId);

    long countByQuizId(UUID quizId);
}
