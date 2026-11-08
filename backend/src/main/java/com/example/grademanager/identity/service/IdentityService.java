package com.example.grademanager.identity.service;

import com.example.grademanager.identity.dto.LoginRequest;
import com.example.grademanager.identity.dto.UserView;

import jakarta.servlet.http.HttpSession;

public interface IdentityService {

    /** 凭据校验 + 建会话；成功返回当前身份。 */
    UserView login(LoginRequest request, HttpSession session);

    /** 从会话恢复当前身份。 */
    UserView me(HttpSession session);

    /** 使会话失效；幂等：会话已失效也视为成功。 */
    void logout(HttpSession session);
}
