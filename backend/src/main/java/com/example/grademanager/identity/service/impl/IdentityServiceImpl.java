package com.example.grademanager.identity.service.impl;

import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.identity.dto.ChangePasswordRequest;
import com.example.grademanager.identity.dto.LoginRequest;
import com.example.grademanager.identity.dto.UserView;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.identity.service.IdentityService;
import com.example.grademanager.identity.service.PasswordPolicy;
import com.example.grademanager.identity.session.SessionUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class IdentityServiceImpl implements IdentityService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public IdentityServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserView login(LoginRequest request, HttpSession session) {
        User user = userMapper.findByUsername(request.username());
        // 凭据错误统一提示，不区分账号不存在 / 密码错
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        session.setAttribute(SessionUser.SESSION_KEY, SessionUser.of(user));
        return toView(user);
    }

    @Override
    public UserView me(HttpSession session) {
        SessionUser current = SessionUser.require(session);
        return new UserView(current.getUserId(), current.getUsername(), current.getDisplayName(),
                current.getRole(), current.isMustChangePassword());
    }

    @Override
    public void logout(HttpSession session) {
        session.invalidate();
    }

    @Override
    public void changePassword(ChangePasswordRequest request, HttpSession session) {
        SessionUser current = SessionUser.require(session);
        User user = userMapper.selectById(current.getUserId());
        if (user == null) {
            // 会话指向的账号已被删除，按未登录处理，避免继续操作悬空身份
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 原口令不符：沿用 1001，与登录失败同口径，不透露账号存在性
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS, "原口令不正确");
        }
        PasswordPolicy.validate(request.newPassword(), "newPassword");
        if (request.newPassword().equals(request.oldPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "newPassword: 新口令不能与原口令相同");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setPasswordResetBy(null);
        userMapper.updateById(user);

        // 刷新会话身份：改密后立即放行被 1004 拦住的其他接口，不需要重新登录
        session.setAttribute(SessionUser.SESSION_KEY, SessionUser.of(user));
    }

    private UserView toView(User user) {
        return new UserView(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(),
                Boolean.TRUE.equals(user.getMustChangePassword()));
    }
}
