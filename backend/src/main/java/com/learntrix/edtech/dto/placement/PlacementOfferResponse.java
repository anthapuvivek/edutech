package com.learntrix.edtech.dto.placement;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementOfferResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String companyName;
    private String role;
    private String offerDate;
    private String joiningDate;
    private Double annualSalary;
    private String location;
    private String status;
    private String verifiedBy;
}
