package com.example.grademanager.identity.session;

import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 除白名单（ping / login）外，所有 /api/** 接口都要求已登录。
 * 未完成强制改密（must_change_password=1）的账号只放行改密、查身份、退出三类接口，其余一律 1004，
 * 判定只读会话里的标记，不额外查库。
 */
@Component
public class SessionAuthInterceptor implements HandlerInterceptor {

    /** 强制改密期间仍可访问的接口：改密本身、恢复身份、退出。 */
    private static final Set<String> ALLOWED_BEFORE_PASSWORD_CHANGE = Set.of(
            "POST /api/identity/password",
            "GET /api/identity/me",
            "POST /api/identity/logout");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SessionUser.SESSION_KEY) == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        SessionUser current = (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
        if (current.isMustChangePassword()) {
            String currentApi = request.getMethod() + " " + request.getRequestURI();
            if (!ALLOWED_BEFORE_PASSWORD_CHANGE.contains(currentApi)) {
                throw new BusinessException(ErrorCode.PASSWORD_CHANGE_REQUIRED);
            }
        }
        return true;
    }
}
