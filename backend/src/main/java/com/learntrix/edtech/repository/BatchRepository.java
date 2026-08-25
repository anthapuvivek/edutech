package com.learntrix.edtech.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.learntrix.edtech.entity.Batch;

@Repository
public interface BatchRepository extends JpaRepository<Batch, UUID> {
    List<Batch> findByTeacherIdOrderByStartDateAsc(UUID teacherId);
    List<Batch> findByCourseIdOrderByStartDateAsc(UUID courseId);
}