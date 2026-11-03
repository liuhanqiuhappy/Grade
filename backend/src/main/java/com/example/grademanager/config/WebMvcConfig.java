package com.example.grademanager.config;

import com.example.grademanager.identity.session.SessionAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 只做登录拦截。按契约 §1.3，后端不放开 CORS（前端走 Vite 代理，浏览器视角同源）。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SessionAuthInterceptor())
                .addPathPatterns("/api/**")
                // 探活、登录、退出无需登录（退出设计为幂等）
                .excludePathPatterns(
                        "/api/ping",
                        "/api/identity/login",
                        "/api/identity/logout"
                );
    }
}
