package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.dto.crm.*;
import com.learntrix.edtech.dto.rbac.StaffMemberResponse;
import com.learntrix.edtech.service.CrmService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COUNSELLOR')")
public class CrmController {

    private final CrmService crmService;

    public CrmController(CrmService crmService) {
        this.crmService = crmService;
    }

    @GetMapping("/crm/overview")
    public ApiResponse<LeadOverviewResponse> getOverview() {
        return ApiResponse.success(crmService.getOverview());
    }

    @GetMapping("/crm/leads")
    public ApiResponse<List<LeadResponse>> getLeads(
            @RequestParam(value = "stage", required = false) String stage,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "assignedToId", required = false) String assignedToId,
            @RequestParam(value = "search", required = false) String search) {
        return ApiResponse.success(crmService.getLeads(stage, source, assignedToId, search));
    }

    @GetMapping("/crm/leads/{id}")
    public ApiResponse<LeadDetailResponse> getLeadDetail(@PathVariable("id") UUID id) {
        return ApiResponse.success(crmService.getLeadDetail(id));
    }

    @PatchMapping("/crm/leads/{id}/stage")
    public ApiResponse<LeadResponse> moveStage(
            @PathVariable("id") UUID id,
            @Valid @RequestBody MoveStageRequest request) {
        return ApiResponse.success(crmService.moveStage(id, request.getStage()));
    }

    @PatchMapping("/crm/leads/{id}/assign")
    public ApiResponse<LeadResponse> assignLead(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AssignLeadRequest request) {
        return ApiResponse.success(crmService.assignLead(id, request.getAssignedToId()));
    }

    @PostMapping("/crm/leads/{id}/notes")
    public ApiResponse<Map<String, String>> addNote(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AddNoteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "Staff Member";
        crmService.addNote(id, request.getBody(), username);
        return ApiResponse.success(Map.of("message", "Note added successfully"));
    }

    @PostMapping("/crm/leads/{id}/communications")
    public ApiResponse<Map<String, String>> logCommunication(
            @PathVariable("id") UUID id,
            @Valid @RequestBody LogCommunicationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "Staff Member";
        crmService.logCommunication(id, request.getChannel(), request.getSummary(), username);
        return ApiResponse.success(Map.of("message", "Communication logged successfully"));
    }

    @PostMapping("/crm/leads/{id}/follow-ups")
    public ApiResponse<Map<String, String>> scheduleFollowUp(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ScheduleFollowUpRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "Staff Member";
        crmService.scheduleFollowUp(id, request, username);
        return ApiResponse.success(Map.of("message", "Follow-up scheduled successfully"));
    }

    @GetMapping("/staff")
    public ApiResponse<List<StaffMemberResponse>> getStaff(
            @RequestParam(value = "role", required = false) String role) {
        return ApiResponse.success(crmService.getStaff(role));
    }
}
