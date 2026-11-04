package com.example.grademanager.identity.session;

import java.io.Serializable;

/**
 * 存放于 HttpSession 的登录身份快照。
 * 身份只从会话读取，任何请求体/参数里的角色自述一律不采信。
 */
public record SessionUser(Long userId, String username, String displayName, String role) implements Serializable {
}
