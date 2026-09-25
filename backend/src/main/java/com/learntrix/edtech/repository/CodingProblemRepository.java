package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.CodingProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CodingProblemRepository extends JpaRepository<CodingProblem, UUID> {

    List<CodingProblem> findByCourseIdIn(List<UUID> courseIds);

    /**
     * Published problems a student may see, using the same rule as live classes and
     * quizzes: no batch means course-wide, a batch means members of that batch only.
     * Enforced in the query so a caller cannot forget it.
     */
    @Query("SELECT DISTINCT p FROM CodingProblem p LEFT JOIN p.batch b LEFT JOIN b.students s "
         + "WHERE p.status = 'PUBLISHED' AND p.course.id IN :courseIds "
         + "AND (p.batch IS NULL OR s.id = :studentId)")
    List<CodingProblem> findAccessiblePublished(
            @Param("studentId") UUID studentId,
            @Param("courseIds") List<UUID> courseIds);
}
