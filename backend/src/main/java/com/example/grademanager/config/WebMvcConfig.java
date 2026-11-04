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

        // 模块二（契约 §7.4）：角色校验拦截器，注册在登录拦截器之后（先判登录 1002，再判角色 1003）
        registry.addInterceptor(new RoleAuthInterceptor())
                .addPathPatterns("/api/**");
    }
}
