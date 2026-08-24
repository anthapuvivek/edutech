package com.learntrix.edtech.dto.publiccontent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestimonialResponse {
    private String id;
    private String name;
    private String role;
    private String company;
    private Integer rating;
    private String quote;
}
