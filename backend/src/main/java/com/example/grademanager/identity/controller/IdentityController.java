package com.example.grademanager.identity.controller;

import com.example.grademanager.common.ApiResponse;
import com.example.grademanager.identity.dto.LoginRequest;
import com.example.grademanager.identity.dto.RegisterRequest;
import com.example.grademanager.identity.dto.UserInfoResponse;
import com.example.grademanager.identity.service.IdentityService;
import com.example.grademanager.identity.session.SessionKeys;
import com.example.grademanager.identity.session.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 身份模块：登录 / 当前身份 / 退出 / 注册。
 * 所有方法均返回 HTTP 200，业务结果看 code。
 */
@RestController
@RequestMapping("/api/identity")
public class IdentityController {

    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    /** 契约 §3.2 登录：校验凭据 + 建会话（先销毁旧会话，防会话固定） */
    @PostMapping("/login")
    public ApiResponse<UserInfoResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        UserInfoResponse user = identityService.login(request);

        HttpSession oldSession = httpRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SessionKeys.CURRENT_USER,
                new SessionUser(user.userId(), user.username(), user.displayName(), user.role()));

        return ApiResponse.ok(user);
    }

    /** 契约 §3.3 查询当前身份：由拦截器保证会话有效 */
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> me(HttpServletRequest httpRequest) {
        SessionUser currentUser = (SessionUser) httpRequest.getAttribute(SessionKeys.CURRENT_USER_REQUEST_ATTR);
        return ApiResponse.ok(identityService.currentUser(currentUser));
    }

    /** 契约 §3.4 退出：幂等，会话已失效也返回 code=0 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ApiResponse.ok();
    }

    /** 契约 §3.5（草案）注册：仅创建学生账号，不自动登录 */
    @PostMapping("/register")
    public ApiResponse<UserInfoResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(identityService.register(request));
    }
}
