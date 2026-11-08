package com.example.grademanager.identity.service.impl;

import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.identity.dto.LoginRequest;
import com.example.grademanager.identity.dto.UserView;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.identity.service.IdentityService;
import com.example.grademanager.identity.session.SessionUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
        return new UserView(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }

    @Override
    public UserView me(HttpSession session) {
        SessionUser current = SessionUser.require(session);
        return new UserView(current.getUserId(), current.getUsername(), current.getDisplayName(), current.getRole());
    }

    @Override
    public void logout(HttpSession session) {
        session.invalidate();
    }
}
