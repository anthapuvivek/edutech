package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.QuizOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizOptionRepository extends JpaRepository<QuizOption, UUID> {

    List<QuizOption> findByQuestionIdOrderBySequenceNumberAsc(UUID questionId);

    /** Used when marking an attempt; the correct flag never leaves the service layer. */
    List<QuizOption> findByQuestionIdAndCorrectTrue(UUID questionId);
}
