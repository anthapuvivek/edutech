package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.dto.crm.EnquiryRequest;
import com.learntrix.edtech.dto.crm.EnquiryResponse;
import com.learntrix.edtech.service.EnquiryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {

    private final EnquiryService enquiryService;

    public EnquiryController(EnquiryService enquiryService) {
        this.enquiryService = enquiryService;
    }

    @PostMapping
    public ApiResponse<EnquiryResponse> submitEnquiry(@Valid @RequestBody EnquiryRequest request) {
        EnquiryResponse response = enquiryService.submitEnquiry(request);
        return ApiResponse.success(response);
    }
}
