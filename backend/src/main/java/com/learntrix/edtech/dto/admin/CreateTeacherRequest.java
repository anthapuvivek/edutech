package com.learntrix.edtech.dto.admin;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public class CreateTeacherRequest {

    @JsonAlias({"name", "Full name", "fullName"})
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    @JsonAlias({"email", "Email"})
    private String email;

    @JsonAlias({"phone", "Phone"})
    private String phone;

    @JsonAlias({"headline", "Headline"})
    private String headline;

    @JsonAlias({"department", "Department"})
    private String department;

    @JsonAlias({"qualification", "Qualification"})
    private String qualification;

    @JsonAlias({"experienceYears", "experience", "Experience (years)"})
    private Integer experienceYears;

    @JsonAlias({"skills", "Skills"})
    private String skills;

    @JsonAlias({"bio", "Bio"})
    private String bio;

    @JsonAlias({"approvalStatus", "approval_status"})
    private String approvalStatus;

    public String resolveName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName.trim();
        }
        return "";
    }
}
