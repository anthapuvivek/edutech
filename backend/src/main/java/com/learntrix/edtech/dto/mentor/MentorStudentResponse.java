package com.learntrix.edtech.dto.mentor;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorStudentResponse {
    private UUID id;
    private String name;
    private String courseTitle;
    private String batchName;
    private List<String> skills;
    private double progressPercent;
    private double attendancePercent;
    private int careerReadiness;
    private double quizScore;
    private double codingScore;
    private String lastSessionAt;
    private String nextSessionAt;
    private String status;
}
