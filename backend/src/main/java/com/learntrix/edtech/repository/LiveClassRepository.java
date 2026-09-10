package com.learntrix.edtech.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.learntrix.edtech.entity.LiveClass;

@Repository
public interface LiveClassRepository extends JpaRepository<LiveClass, UUID> {

    @Query("SELECT DISTINCT lc FROM LiveClass lc LEFT JOIN FETCH lc.course c LEFT JOIN FETCH lc.batch b LEFT JOIN b.students s " +
           "WHERE lc.published = true AND (lc.visibility = 'public' OR (b IS NOT NULL AND s.id = :studentId) OR (b IS NULL AND c.id IN :courseIds)) " +
           "ORDER BY lc.classDate ASC, lc.startTime ASC")
    List<LiveClass> findAvailableLiveClasses(@Param("studentId") UUID studentId, @Param("courseIds") List<UUID> courseIds);

    @Query("SELECT DISTINCT lc FROM LiveClass lc LEFT JOIN FETCH lc.course c LEFT JOIN FETCH lc.batch b LEFT JOIN b.students s " +
           "WHERE lc.published = true AND lc.visibility = 'public' " +
           "ORDER BY lc.classDate ASC, lc.startTime ASC")
    List<LiveClass> findPublicLiveClasses();

    @Query("SELECT lc FROM LiveClass lc LEFT JOIN FETCH lc.course c LEFT JOIN FETCH lc.batch b " +
           "WHERE lc.published = true AND c.id IN :courseIds " +
           "ORDER BY lc.classDate ASC, lc.startTime ASC")
    List<LiveClass> findCourseLiveClasses(@Param("courseIds") List<UUID> courseIds);

    List<LiveClass> findByTeacherIdOrderByClassDateAscStartTimeAsc(UUID teacherId);

    /** Classes pinned to a batch. Used to detach them before the batch is deleted. */
    List<LiveClass> findByBatchId(UUID batchId);

    @Query("SELECT DISTINCT lc FROM LiveClass lc LEFT JOIN FETCH lc.course c LEFT JOIN FETCH lc.batch b LEFT JOIN FETCH b.students s " +
           "WHERE lc.teacher.id = :teacherId OR b.teacher.id = :teacherId " +
           "ORDER BY lc.classDate ASC, lc.startTime ASC")
    List<LiveClass> findByTeacherOrBatchTeacher(@Param("teacherId") UUID teacherId);
}
