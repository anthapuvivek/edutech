package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.LiveClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LiveClassRepository extends JpaRepository<LiveClass, UUID> {

    @Query("SELECT lc FROM LiveClass lc LEFT JOIN FETCH lc.course c " +
           "WHERE lc.published = true AND (lc.visibility = 'public' OR (c IS NOT NULL AND c.id IN :courseIds)) " +
           "ORDER BY lc.classDate ASC, lc.startTime ASC")
    List<LiveClass> findAvailableLiveClasses(@Param("courseIds") List<UUID> courseIds);

    @Query("SELECT lc FROM LiveClass lc LEFT JOIN FETCH lc.course c " +
           "WHERE lc.published = true AND lc.visibility = 'public' " +
           "ORDER BY lc.classDate ASC, lc.startTime ASC")
    List<LiveClass> findPublicLiveClasses();
}
