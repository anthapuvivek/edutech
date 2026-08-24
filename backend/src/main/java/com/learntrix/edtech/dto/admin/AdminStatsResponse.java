package com.learntrix.edtech.dto.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsResponse {
    private int students;
    private int teachers;
    private int courses;
    private int batches;
    private int liveEvents;
    private int openEnquiries;
    private double revenueThisMonth;
    private int activeBatches;
}
