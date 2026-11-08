package com.example.grademanager.identity.session;

import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.identity.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** 会话中的当前登录身份。身份只从 Session 判定，不采信请求参数中的角色自述。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionUser implements Serializable {

    public static final String SESSION_KEY = "SESSION_USER";

    private Long userId;
    private String username;
    private String displayName;
    private String role;

    public static SessionUser of(User user) {
        return new SessionUser(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }

    public static SessionUser require(HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute(SESSION_KEY);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return user;
    }

    public static SessionUser requireTeacher(HttpSession session) {
        SessionUser user = require(session);
        if (!User.ROLE_TEACHER.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return user;
    }
}
