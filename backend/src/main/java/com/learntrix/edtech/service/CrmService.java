package com.learntrix.edtech.service;

import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.dto.crm.*;
import com.learntrix.edtech.dto.rbac.StaffMemberResponse;
import com.learntrix.edtech.entity.*;
import com.learntrix.edtech.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CrmService {

    private static final List<String> STAFF_ROLES = List.of(
            "SUPER_ADMIN", "ADMIN", "TEACHER", "MENTOR", "PLACEMENT_OFFICER", "SUPPORT_AGENT", "COUNSELLOR");

    private final LeadRepository leadRepository;
    private final EnquiryRepository enquiryRepository;
    private final LeadNoteRepository leadNoteRepository;
    private final LeadCommunicationRepository leadCommunicationRepository;
    private final LeadFollowUpRepository leadFollowUpRepository;
    private final UserRepository userRepository;

    public CrmService(LeadRepository leadRepository,
                      EnquiryRepository enquiryRepository,
                      LeadNoteRepository leadNoteRepository,
                      LeadCommunicationRepository leadCommunicationRepository,
                      LeadFollowUpRepository leadFollowUpRepository,
                      UserRepository userRepository) {
        this.leadRepository = leadRepository;
        this.enquiryRepository = enquiryRepository;
        this.leadNoteRepository = leadNoteRepository;
        this.leadCommunicationRepository = leadCommunicationRepository;
        this.leadFollowUpRepository = leadFollowUpRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public LeadOverviewResponse getOverview() {
        long totalLeads = leadRepository.count();
        long newLeads = leadRepository.countByStage("New");
        long demoRegistrations = leadRepository.countByStage("Demo Scheduled");
        long demoAttendees = leadRepository.countByStage("Demo Attended");
        long paymentPending = leadRepository.countByStage("Payment Pending");
        long convertedLeads = leadRepository.countByStage("Converted");

        String today = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE);
        long followUpsToday = leadFollowUpRepository.findByFollowUpDateAndStatus(today, "Pending").size();
        if (followUpsToday == 0) {
            // Also check leads with nextFollowUpAt date today
            Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant endOfDay = startOfDay.plusSeconds(86400);
            followUpsToday = leadRepository.countFollowUpsBetween(startOfDay, endOfDay);
        }

        double conversionRate = totalLeads > 0
                ? Math.round(((double) convertedLeads / totalLeads) * 1000.0) / 10.0
                : 0.0;

        return LeadOverviewResponse.builder()
                .totalLeads(totalLeads)
                .newLeads(newLeads)
                .followUpsToday(followUpsToday)
                .demoRegistrations(demoRegistrations)
                .demoAttendees(demoAttendees)
                .paymentPending(paymentPending)
                .convertedLeads(convertedLeads)
                .conversionRate(conversionRate)
                .build();
    }

    @Transactional(readOnly = true)
    public List<LeadResponse> getLeads(String stage, String source, String assignedToId, String search) {
        boolean filterStage = stage != null && !stage.isBlank() && !"all".equalsIgnoreCase(stage);
        boolean filterSource = source != null && !source.isBlank() && !"all".equalsIgnoreCase(source);
        boolean filterOwner = assignedToId != null && !assignedToId.isBlank() && !"all".equalsIgnoreCase(assignedToId);
        String term = search != null ? search.trim().toLowerCase() : "";

        return leadRepository.findAll().stream()
                .filter(l -> !filterStage || l.getStage().equalsIgnoreCase(stage.trim()))
                .filter(l -> !filterSource || l.getSource().equalsIgnoreCase(source.trim()))
                .filter(l -> !filterOwner || (l.getAssignedTo() != null && l.getAssignedTo().getId().toString().equalsIgnoreCase(assignedToId.trim())))
                .filter(l -> term.isEmpty() || matchesSearch(l, term))
                .sorted(Comparator.comparing(Lead::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToLeadResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LeadDetailResponse getLeadDetail(UUID id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", id));

        List<LeadCommunicationDto> comms = leadCommunicationRepository.findByLeadIdOrderByCreatedAtDesc(id).stream()
                .map(c -> LeadCommunicationDto.builder()
                        .id(c.getId())
                        .leadId(c.getLead().getId())
                        .channel(c.getChannel())
                        .summary(c.getSummary())
                        .by(c.getAuthorName())
                        .at(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<LeadNoteDto> notes = leadNoteRepository.findByLeadIdOrderByCreatedAtDesc(id).stream()
                .map(n -> LeadNoteDto.builder()
                        .id(n.getId())
                        .leadId(n.getLead().getId())
                        .body(n.getBody())
                        .by(n.getAuthorName())
                        .at(n.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<LeadFollowUpDto> followUps = leadFollowUpRepository.findByLeadIdOrderByCreatedAtDesc(id).stream()
                .map(f -> LeadFollowUpDto.builder()
                        .id(f.getId())
                        .leadId(f.getLead().getId())
                        .date(f.getFollowUpDate())
                        .time(f.getFollowUpTime())
                        .notes(f.getNotes())
                        .nextAction(f.getNextAction())
                        .assignedTo(f.getAssignedToName() != null ? f.getAssignedToName() : (lead.getAssignedTo() != null ? lead.getAssignedTo().getName() : "Unassigned"))
                        .status(f.getStatus())
                        .build())
                .collect(Collectors.toList());

        List<EnquiryHistoryDto> enquiries = enquiryRepository.findByEmailOrderByCreatedAtDesc(lead.getEmail()).stream()
                .map(e -> EnquiryHistoryDto.builder()
                        .id(e.getId())
                        .subject(e.getCourse() != null ? "Enquiry about " + e.getCourse().getTitle() : "Course enquiry (" + lead.getCourseInterest() + ")")
                        .at(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        if (enquiries.isEmpty()) {
            enquiries = List.of(
                    EnquiryHistoryDto.builder()
                            .id(UUID.randomUUID())
                            .subject("Enquiry about " + lead.getCourseInterest())
                            .at(lead.getCreatedAt())
                            .build()
            );
        }

        List<PaymentHistoryDto> paymentHistory = new ArrayList<>();
        if ("Converted".equalsIgnoreCase(lead.getStage())) {
            paymentHistory.add(PaymentHistoryDto.builder()
                    .id(UUID.randomUUID())
                    .amount(lead.getBudget() != null ? lead.getBudget() : 35000)
                    .status("Paid")
                    .at(lead.getUpdatedAt() != null ? lead.getUpdatedAt() : lead.getCreatedAt())
                    .build());
        } else if ("Payment Pending".equalsIgnoreCase(lead.getStage())) {
            paymentHistory.add(PaymentHistoryDto.builder()
                    .id(UUID.randomUUID())
                    .amount(5000)
                    .status("Partial")
                    .at(lead.getUpdatedAt() != null ? lead.getUpdatedAt() : lead.getCreatedAt())
                    .build());
        }

        return LeadDetailResponse.builder()
                .id(lead.getId())
                .name(lead.getName())
                .phone(lead.getPhone())
                .email(lead.getEmail())
                .courseInterest(lead.getCourseInterest())
                .source(lead.getSource())
                .assignedTo(lead.getAssignedTo() != null ? lead.getAssignedTo().getName() : "Unassigned")
                .assignedToId(lead.getAssignedTo() != null ? lead.getAssignedTo().getId().toString() : "")
                .stage(lead.getStage())
                .lastContactAt(lead.getLastContactAt())
                .nextFollowUpAt(lead.getNextFollowUpAt())
                .createdAt(lead.getCreatedAt())
                .city(lead.getCity())
                .budget(lead.getBudget())
                .score(lead.getScore())
                .demoAttended(lead.getDemoAttended())
                .enrollmentStatus(lead.getEnrollmentStatus())
                .communications(comms)
                .notes(notes)
                .followUps(followUps)
                .enquiryHistory(enquiries)
                .paymentHistory(paymentHistory)
                .build();
    }

    @Transactional
    public LeadResponse moveStage(UUID id, String nextStage) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", id));

        lead.setStage(nextStage);
        lead.setLastContactAt(Instant.now());

        if ("Converted".equalsIgnoreCase(nextStage)) {
            lead.setEnrollmentStatus("Enrolled");
            lead.setScore(Math.max(lead.getScore(), 95));
        } else if ("Payment Pending".equalsIgnoreCase(nextStage)) {
            lead.setEnrollmentStatus("Payment Pending");
            lead.setScore(Math.max(lead.getScore(), 90));
        } else if ("Demo Attended".equalsIgnoreCase(nextStage)) {
            lead.setDemoAttended(true);
            lead.setScore(Math.max(lead.getScore(), 80));
        }

        lead = leadRepository.save(lead);
        return mapToLeadResponse(lead);
    }

    @Transactional
    public LeadResponse assignLead(UUID id, UUID staffId) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", id));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user", "id", staffId));

        lead.setAssignedTo(staff);
        lead = leadRepository.save(lead);
        return mapToLeadResponse(lead);
    }

    @Transactional
    public void addNote(UUID id, String body, String username) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", id));

        User author = findUserByEmailOrName(username);
        String authorName = author != null ? author.getName() : (username != null ? username : "Staff Member");

        LeadNote note = LeadNote.builder()
                .lead(lead)
                .author(author)
                .authorName(authorName)
                .body(body)
                .build();

        leadNoteRepository.save(note);
    }

    @Transactional
    public void logCommunication(UUID id, String channel, String summary, String username) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", id));

        User author = findUserByEmailOrName(username);
        String authorName = author != null ? author.getName() : (username != null ? username : "Staff Member");

        LeadCommunication comm = LeadCommunication.builder()
                .lead(lead)
                .author(author)
                .authorName(authorName)
                .channel(channel)
                .summary(summary)
                .build();

        leadCommunicationRepository.save(comm);
        lead.setLastContactAt(Instant.now());
        leadRepository.save(lead);
    }

    @Transactional
    public void scheduleFollowUp(UUID id, ScheduleFollowUpRequest request, String username) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", id));

        User staff = lead.getAssignedTo() != null ? lead.getAssignedTo() : findUserByEmailOrName(username);
        String staffName = staff != null ? staff.getName() : (lead.getAssignedTo() != null ? lead.getAssignedTo().getName() : "Staff");

        LeadFollowUp followUp = LeadFollowUp.builder()
                .lead(lead)
                .assignedTo(staff)
                .assignedToName(staffName)
                .followUpDate(request.getDate())
                .followUpTime(request.getTime())
                .notes(request.getNotes())
                .nextAction(request.getNextAction() != null && !request.getNextAction().isBlank() ? request.getNextAction() : "Call")
                .status("Pending")
                .build();

        leadFollowUpRepository.save(followUp);

        // Update lead's nextFollowUpAt
        try {
            String isoString = request.getDate() + "T" + (request.getTime().length() == 5 ? request.getTime() + ":00" : request.getTime()) + "Z";
            lead.setNextFollowUpAt(Instant.parse(isoString));
        } catch (Exception e) {
            lead.setNextFollowUpAt(Instant.now().plusSeconds(86400));
        }

        leadRepository.save(lead);
    }

    @Transactional(readOnly = true)
    public List<StaffMemberResponse> getStaff(String role) {
        boolean allRoles = role == null || role.isBlank() || "all".equalsIgnoreCase(role);
        String wanted = allRoles ? null : role.trim().toUpperCase();

        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> STAFF_ROLES.contains(r.getName().toUpperCase())))
                .filter(u -> allRoles || u.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(wanted)))
                .map(u -> {
                    String roleKey = u.getRoles().stream()
                            .map(Role::getName)
                            .filter(name -> STAFF_ROLES.contains(name.toUpperCase()))
                            .min(Comparator.comparingInt((String name) -> STAFF_ROLES.indexOf(name.toUpperCase())))
                            .map(String::toLowerCase)
                            .orElse("student");

                    long assignedCount = leadRepository.countByAssignedToId(u.getId());

                    return StaffMemberResponse.builder()
                            .id(u.getId())
                            .name(u.getName())
                            .email(u.getEmail())
                            .role(roleKey)
                            .status(mapStatus(u.getStatus()))
                            .assignedCount((int) assignedCount)
                            .createdAt(u.getCreatedAt())
                            .build();
                })
                .sorted(Comparator.comparing(StaffMemberResponse::getName))
                .collect(Collectors.toList());
    }

    private boolean matchesSearch(Lead lead, String term) {
        return (lead.getName() != null && lead.getName().toLowerCase().contains(term)) ||
                (lead.getEmail() != null && lead.getEmail().toLowerCase().contains(term)) ||
                (lead.getPhone() != null && lead.getPhone().toLowerCase().contains(term)) ||
                (lead.getCourseInterest() != null && lead.getCourseInterest().toLowerCase().contains(term));
    }

    private LeadResponse mapToLeadResponse(Lead lead) {
        return LeadResponse.builder()
                .id(lead.getId())
                .name(lead.getName())
                .phone(lead.getPhone())
                .email(lead.getEmail())
                .courseInterest(lead.getCourseInterest())
                .source(lead.getSource())
                .assignedTo(lead.getAssignedTo() != null ? lead.getAssignedTo().getName() : "Unassigned")
                .assignedToId(lead.getAssignedTo() != null ? lead.getAssignedTo().getId().toString() : "")
                .stage(lead.getStage())
                .lastContactAt(lead.getLastContactAt())
                .nextFollowUpAt(lead.getNextFollowUpAt())
                .createdAt(lead.getCreatedAt())
                .city(lead.getCity())
                .budget(lead.getBudget())
                .score(lead.getScore())
                .build();
    }

    private User findUserByEmailOrName(String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findAll().stream().filter(u -> identifier.equalsIgnoreCase(u.getName())).findFirst())
                .orElse(null);
    }

    private String mapStatus(String status) {
        if (status == null) return "inactive";
        return switch (status.toUpperCase()) {
            case "ACTIVE" -> "active";
            case "SUSPENDED" -> "suspended";
            default -> "inactive";
        };
    }
}
