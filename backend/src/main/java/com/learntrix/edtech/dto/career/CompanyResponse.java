package com.learntrix.edtech.dto.career;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponse {
    private UUID id;
    private String name;
    private String logoUrl;
    private String industry;
    private List<String> locations;
    private boolean officialPartner;
    private boolean preparationTrack;
    private List<String> typicalRoles;
    private List<String> requiredSkills;
    private String difficulty;
    private int averagePreparationWeeks;
}
