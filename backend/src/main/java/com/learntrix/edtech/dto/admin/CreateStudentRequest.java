package com.learntrix.edtech.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentRequest {

    private String fullName;
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    private String password;
    private String phone;
    private String dateOfBirth;
    private String gender;
    private String location;
    private String education;
    private String college;
    private Integer graduationYear;
    private String qualification;
    private String skills;
    private String github;
    private String linkedin;
    private String leetcode;
    private String hackerrank;
    private String accountStatus;

    // Optional immediate allocation fields
    private java.util.UUID courseId;
    private java.util.UUID teacherId;
    private java.util.UUID batchId;

    public String resolveName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName.trim();
        }
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        return "";
    }
}
