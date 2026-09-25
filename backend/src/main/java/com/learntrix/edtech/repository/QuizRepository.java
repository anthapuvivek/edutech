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

    /**
     * Published quizzes a student may see, using the same rule live classes already use:
     * a quiz with no batch is course-wide, a quiz with a batch is visible only to members
     * of that batch. Enforced here in the query, not in the caller.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT q FROM Quiz q LEFT JOIN q.batch b LEFT JOIN b.students s "
      + "WHERE q.status = 'PUBLISHED' AND q.course.id IN :courseIds "
      + "AND (q.batch IS NULL OR s.id = :studentId)")
    List<Quiz> findAccessiblePublishedQuizzes(
            @org.springframework.data.repository.query.Param("studentId") UUID studentId,
            @org.springframework.data.repository.query.Param("courseIds") List<UUID> courseIds);
}
