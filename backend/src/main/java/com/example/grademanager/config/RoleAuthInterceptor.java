package com.example.grademanager.config;

import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.identity.session.SessionKeys;
import com.example.grademanager.identity.session.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 角色校验拦截器（契约 §7.4）：读取 Handler 上的 {@link RequireRole} 注解，
 * 与 Session 中的角色比对，不符抛 1003。必须在 {@code SessionAuthInterceptor} 之后注册，
 * 保证未登录请求先得到 1002 而不是 1003。
 */
public class RoleAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireRole annotation = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequireRole.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(), RequireRole.class);
        }
        if (annotation == null) {
            return true;
        }
        Object attribute = request.getAttribute(SessionKeys.CURRENT_USER_REQUEST_ATTR);
        if (!(attribute instanceof SessionUser sessionUser) || !annotation.value().equals(sessionUser.role())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return true;
    }
}
