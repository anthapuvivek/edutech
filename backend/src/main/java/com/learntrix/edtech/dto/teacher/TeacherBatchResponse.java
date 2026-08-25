package com.learntrix.edtech.dto.teacher;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherBatchResponse {
    private UUID id;
    private String name;
    private UUID courseId;
    private String courseTitle;
    private UUID teacherId;
    private String teacherName;
    private int capacity;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private int studentCount;
}
