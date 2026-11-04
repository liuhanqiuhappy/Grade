package com.example.grademanager.identity.session;

import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 受保护接口的统一登录校验：无有效会话直接抛 1002（HTTP 200 + code=1002）。
 * 身份只从会话判定，不读任何请求参数。
 */
public class SessionAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        Object attribute = session == null ? null : session.getAttribute(SessionKeys.CURRENT_USER);
        if (!(attribute instanceof SessionUser sessionUser)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        request.setAttribute(SessionKeys.CURRENT_USER_REQUEST_ATTR, sessionUser);
        return true;
    }
}
