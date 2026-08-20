package com.learntrix.edtech.common.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiStatusController {

    @GetMapping({"/", "/api"})
    public ApiResponse<Map<String, String>> status() {
        return ApiResponse.success(Map.of(
                "name", "LearntriX EdTech API",
                "status", "UP",
                "documentation", "/swagger-ui.html"
        ));
    }
}
