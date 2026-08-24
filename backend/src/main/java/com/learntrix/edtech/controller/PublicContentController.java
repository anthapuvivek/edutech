package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.dto.publiccontent.CategoryResponse;
import com.learntrix.edtech.dto.publiccontent.PlatformStatResponse;
import com.learntrix.edtech.dto.publiccontent.TestimonialResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PublicContentController {

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> getCategories() {
        return ApiResponse.success(List.of(
            CategoryResponse.builder().id("c1").name("Full Stack Development").slug("full-stack").courseCount(24).description("React, Node, APIs and modern web architecture.").build(),
            CategoryResponse.builder().id("c2").name("Data Science").slug("data-science").courseCount(18).description("Statistics, analytics and production data workflows.").build(),
            CategoryResponse.builder().id("c3").name("Artificial Intelligence").slug("ai").courseCount(15).description("LLMs, RAG systems and applied deep learning.").build(),
            CategoryResponse.builder().id("c4").name("Machine Learning").slug("machine-learning").courseCount(16).description("Model development from theory to deployment.").build(),
            CategoryResponse.builder().id("c5").name("Cloud Computing").slug("cloud").courseCount(12).description("Distributed systems and cloud-native design.").build(),
            CategoryResponse.builder().id("c6").name("AWS").slug("aws").courseCount(11).description("Certification tracks and hands-on AWS engineering.").build(),
            CategoryResponse.builder().id("c7").name("DevOps").slug("devops").courseCount(10).description("CI/CD, containers, observability and SRE practice.").build(),
            CategoryResponse.builder().id("c8").name("Cyber Security").slug("cyber-security").courseCount(9).description("Offensive and defensive security fundamentals.").build(),
            CategoryResponse.builder().id("c9").name("Java").slug("java").courseCount(14).description("Core Java, Spring Boot and enterprise backends.").build(),
            CategoryResponse.builder().id("c10").name("Python").slug("python").courseCount(21).description("From language foundations to FastAPI services.").build(),
            CategoryResponse.builder().id("c11").name("Data Analytics").slug("data-analytics").courseCount(13).description("SQL, BI tooling and decision analytics.").build(),
            CategoryResponse.builder().id("c12").name("Programming").slug("programming").courseCount(28).description("Problem solving, DSA and engineering craft.").build()
        ));
    }

    @GetMapping("/testimonials")
    public ApiResponse<List<TestimonialResponse>> getTestimonials() {
        return ApiResponse.success(List.of(
            TestimonialResponse.builder().id("t1").name("Priya Sharma").role("Software Engineer").company("Razorpay").rating(5).quote("The Full Stack track was the most structured program I have taken. The project reviews were as rigorous as a real code review at work.").build(),
            TestimonialResponse.builder().id("t2").name("Arjun Nair").role("Data Scientist").company("Swiggy").rating(5).quote("Learntrix combined theory with real datasets. I moved from support engineering into a data science role in nine months.").build(),
            TestimonialResponse.builder().id("t3").name("Fatima Khan").role("Cloud Engineer").company("Thoughtworks").rating(5).quote("The AWS labs and the coding practice platform together made certification feel like a by-product rather than the goal.").build(),
            TestimonialResponse.builder().id("t4").name("Rohit Deshmukh").role("SDE II").company("Atlassian").rating(5).quote("500+ curated problems with the AI coding coach explaining my failed submissions is what finally got me through interviews.").build()
        ));
    }

    @GetMapping("/stats")
    public ApiResponse<List<PlatformStatResponse>> getStats() {
        return ApiResponse.success(List.of(
            PlatformStatResponse.builder().id("s1").label("Learners enrolled").value("10K+").build(),
            PlatformStatResponse.builder().id("s2").label("Expert-led courses").value("100+").build(),
            PlatformStatResponse.builder().id("s3").label("Industry instructors").value("50+").build(),
            PlatformStatResponse.builder().id("s4").label("Problems solved").value("1M+").build()
        ));
    }
}
