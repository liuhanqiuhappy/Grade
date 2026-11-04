package com.example.grademanager.identity.session;

public final class SessionKeys {

    /** 会话属性名：登录身份 */
    public static final String CURRENT_USER = "identity.currentUser";

    /** 请求属性名：由拦截器解析出的登录身份，供控制器直接取用 */
    public static final String CURRENT_USER_REQUEST_ATTR = "identity.currentUser";

    private SessionKeys() {
    }
}
