package com.example.grademanager.identity.session;

import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 除白名单（ping / login）外，所有 /api/** 接口都要求已登录。 */
@Component
public class SessionAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getSession(false) == null
                || request.getSession(false).getAttribute(SessionUser.SESSION_KEY) == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return true;
    }
}
