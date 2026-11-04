package com.example.grademanager.identity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.identity.dto.LoginRequest;
import com.example.grademanager.identity.dto.UserInfoResponse;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.identity.service.IdentityService;
import com.example.grademanager.identity.session.SessionUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class IdentityServiceImpl implements IdentityService {

    private static final int STATUS_ENABLED = 1;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    /** 账号不存在时也走一次哈希比对，避免用响应时间区分「账号不存在」和「密码错误」 */
    private final String dummyHash;

    public IdentityServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing-only");
    }

    @Override
    public UserInfoResponse login(LoginRequest request) {
        String username = request.username().trim();
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));

        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        // 停用账号与凭据错误给同一提示，避免账号枚举
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())
                || user.getStatus() == null
                || user.getStatus() != STATUS_ENABLED) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        return UserInfoResponse.from(user);
    }

    @Override
    public UserInfoResponse currentUser(SessionUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return new UserInfoResponse(currentUser.userId(), currentUser.username(),
                currentUser.displayName(), currentUser.role());
    }
}
