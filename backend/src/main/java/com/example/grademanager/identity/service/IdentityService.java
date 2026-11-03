package com.example.grademanager.identity.service;

import com.example.grademanager.identity.dto.LoginRequest;
import com.example.grademanager.identity.dto.UserInfoResponse;
import com.example.grademanager.identity.session.SessionUser;

public interface IdentityService {

    /** 校验凭据，成功返回身份；失败抛 1001。会话的建立由控制器负责。 */
    UserInfoResponse login(LoginRequest request);

    /** 从会话身份生成响应体（不回查数据库，避免会话与库不一致时互相打脸）。 */
    UserInfoResponse currentUser(SessionUser currentUser);
}
