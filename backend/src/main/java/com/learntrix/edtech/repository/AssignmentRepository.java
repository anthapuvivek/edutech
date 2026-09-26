package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByCourseId(UUID courseId);
    List<Assignment> findByCourseIdIn(List<UUID> courseIds);
    List<Assignment> findByTeacherId(UUID teacherId);
    /**
     * Published assignments a student may see: course-wide, or their own batch.
     *
     * <p>Copied deliberately from QuizRepository.findAccessiblePublishedQuizzes so both
     * features obey one rule. Enforcing it in the query means no caller can skip it.</p>
     */
    @Query("SELECT DISTINCT a FROM Assignment a LEFT JOIN a.batch b LEFT JOIN b.students s "
         + "WHERE a.status = 'PUBLISHED' AND a.course.id IN :courseIds "
         + "AND (a.batch IS NULL OR s.id = :studentId)")
    List<Assignment> findAccessiblePublished(
            @Param("studentId") UUID studentId,
            @Param("courseIds") List<UUID> courseIds);
}
