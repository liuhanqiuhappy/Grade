package com.example.grademanager.identity.controller;

import com.example.grademanager.common.ApiResponse;
import com.example.grademanager.identity.dto.LoginRequest;
import com.example.grademanager.identity.dto.UserView;
import com.example.grademanager.identity.service.IdentityService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity")
public class IdentityController {

    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/login")
    public ApiResponse<UserView> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        return ApiResponse.ok(identityService.login(request, session));
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me(HttpSession session) {
        return ApiResponse.ok(identityService.me(session));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        identityService.logout(session);
        return ApiResponse.ok();
    }
}
