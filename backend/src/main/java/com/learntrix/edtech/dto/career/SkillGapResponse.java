package com.learntrix.edtech.dto.career;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillGapResponse {
    private String targetRole;
    private List<String> strong;
    private List<NeedsImprovementSkill> needsImprovement;
    private List<String> missing;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NeedsImprovementSkill {
        private String skill;
        private int level;
        private String recommendedCourseSlug;
    }
}
