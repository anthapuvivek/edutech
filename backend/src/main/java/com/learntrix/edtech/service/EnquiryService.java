package com.learntrix.edtech.service;

import com.learntrix.edtech.dto.crm.EnquiryRequest;
import com.learntrix.edtech.dto.crm.EnquiryResponse;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Enquiry;
import com.learntrix.edtech.entity.Lead;
import com.learntrix.edtech.entity.LeadNote;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnquiryRepository;
import com.learntrix.edtech.repository.LeadNoteRepository;
import com.learntrix.edtech.repository.LeadRepository;
import com.learntrix.edtech.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final LeadRepository leadRepository;
    private final CourseRepository courseRepository;
    private final LeadNoteRepository leadNoteRepository;
    private final UserRepository userRepository;

    public EnquiryService(EnquiryRepository enquiryRepository,
                          LeadRepository leadRepository,
                          CourseRepository courseRepository,
                          LeadNoteRepository leadNoteRepository,
                          UserRepository userRepository) {
        this.enquiryRepository = enquiryRepository;
        this.leadRepository = leadRepository;
        this.courseRepository = courseRepository;
        this.leadNoteRepository = leadNoteRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EnquiryResponse submitEnquiry(EnquiryRequest request) {
        // 1. Resolve Course if courseId provided
        Course course = null;
        String courseTitle = "General Enquiry";

        if (request.getCourseId() != null && !request.getCourseId().isBlank()) {
            String cId = request.getCourseId().trim();
            try {
                UUID courseUuid = UUID.fromString(cId);
                Optional<Course> courseOpt = courseRepository.findById(courseUuid);
                if (courseOpt.isPresent()) {
                    course = courseOpt.get();
                    courseTitle = course.getTitle();
                }
            } catch (IllegalArgumentException e) {
                // Not a valid UUID, try by slug
                Optional<Course> courseOpt = courseRepository.findBySlug(cId);
                if (courseOpt.isPresent()) {
                    course = courseOpt.get();
                    courseTitle = course.getTitle();
                } else {
                    courseTitle = cId;
                }
            }
        }

        // 2. Find or Create Lead
        List<Lead> existingLeads = leadRepository.findByEmailOrPhone(request.getEmail().trim(), request.getPhone().trim());
        Lead lead;

        if (!existingLeads.isEmpty()) {
            lead = existingLeads.get(0);
            lead.setLastContactAt(Instant.now());
            if (courseTitle != null && !courseTitle.equalsIgnoreCase("General Enquiry")) {
                lead.setCourseInterest(courseTitle);
            }
            leadRepository.save(lead);
        } else {
            // Find default counsellor
            User defaultCounsellor = userRepository.findAll().stream()
                    .filter(u -> u.getRoles().stream().anyMatch(r -> "COUNSELLOR".equalsIgnoreCase(r.getName()) || "ADMIN".equalsIgnoreCase(r.getName())))
                    .findFirst()
                    .orElse(null);

            lead = Lead.builder()
                    .name(request.getName().trim())
                    .email(request.getEmail().trim())
                    .phone(request.getPhone().trim())
                    .courseInterest(courseTitle)
                    .source("Course Enquiry")
                    .stage("New")
                    .assignedTo(defaultCounsellor)
                    .score(60)
                    .demoAttended(false)
                    .enrollmentStatus("Not Enrolled")
                    .lastContactAt(Instant.now())
                    .build();

            lead = leadRepository.save(lead);
        }

        // 3. Save Enquiry
        Enquiry enquiry = Enquiry.builder()
                .lead(lead)
                .course(course)
                .name(request.getName().trim())
                .email(request.getEmail().trim())
                .phone(request.getPhone().trim())
                .experienceLevel(request.getExperienceLevel())
                .learningMode(request.getLearningMode())
                .message(request.getMessage())
                .status("New")
                .build();

        enquiry = enquiryRepository.save(enquiry);

        // 4. If message is present, record a note
        if (request.getMessage() != null && !request.getMessage().isBlank()) {
            LeadNote note = LeadNote.builder()
                    .lead(lead)
                    .authorName("Visitor (Public Enquiry)")
                    .body("Enquiry message (" + (request.getExperienceLevel() != null ? request.getExperienceLevel() + ", " : "")
                            + (request.getLearningMode() != null ? request.getLearningMode() + ": " : "")
                            + "): " + request.getMessage())
                    .build();
            leadNoteRepository.save(note);
        }

        return EnquiryResponse.builder()
                .id(enquiry.getId())
                .status(enquiry.getStatus())
                .createdAt(enquiry.getCreatedAt() != null ? enquiry.getCreatedAt() : Instant.now())
                .build();
    }
}
