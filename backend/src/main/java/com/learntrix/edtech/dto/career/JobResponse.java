package com.learntrix.edtech.dto.career;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {
    private UUID id;
    private String title;
    private UUID companyId;
    private String companyName;
    private String category;
    private String type;
    private String location;
    private String workMode;
    private String experience;
    private String salaryRange;
    private List<String> skills;
    private String postedAt;
    private String deadline;
    private int matchPercent;
    private boolean saved;
}
