package com.gauzynote.common.domain.model;


import com.alibaba.fastjson2.annotation.JSONField;
import com.gauzynote.common.domain.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class LoginUser implements UserDetails {
    private static final long serialVersionUID = 1L;
    /**
     * 账号密码登录方式
     */
    public static final String LOGIN_METHOD_PASSWORD = "password";
    /**
     * WebAuthn 登录方式
     */
    public static final String LOGIN_METHOD_WEBAUTHN = "webauthn";

    private Long userId;
    private String username;
    private String password;
    /**
     * 登录方式
     */
    private String loginMethod;
    /**
     * 用户唯一标识
     */
    private String token;

    /**
     * 过期时间
     */
    private Long expireTime;

    private SysUser sysUser;

    public LoginUser() {
    }

    public LoginUser(Long userId, String username, String password, String loginMethod, SysUser sysUser) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.loginMethod = loginMethod;
        this.sysUser = sysUser;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoginMethod() {
        return loginMethod;
    }

    public void setLoginMethod(String loginMethod) {
        this.loginMethod = loginMethod;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    public SysUser getSysUser() {
        return sysUser;
    }

    public void setSysUser(SysUser sysUser) {
        this.sysUser = sysUser;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
//        return Collections.emptyList();
    }

    @JSONField(serialize=false)
    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @JSONField(serialize = false)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JSONField(serialize = false)
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JSONField(serialize = false)
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JSONField(serialize = false)
    @Override
    public boolean isEnabled() {
        return true;
    }
}
