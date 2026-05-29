package com.gauzynote.system.service;

import java.io.Serializable;

/**
 * 挑战会话数据
 */
public class WebAuthnChallengeSession implements Serializable {
    private static final long serialVersionUID = 3967939959381940124L;
    /**
     * 流程类型
     */
    private String flowType;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 用户名
     */
    private String username;
    /**
     * 挑战值
     */
    private String challenge;
    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 无参构造，用于 Redis 反序列化挑战会话。
     */
    public WebAuthnChallengeSession() {
    }

    /**
     * 初始化挑战会话
     *
     * @param flowType 流程类型
     * @param userId 用户ID
     * @param username 用户名
     * @param challenge 挑战值
     */
    public WebAuthnChallengeSession(String flowType, Long userId, String username, String challenge) {
        this.flowType = flowType;
        this.userId = userId;
        this.username = username;
        this.challenge = challenge;
        this.createTime = System.currentTimeMillis();
    }

    /**
     * 获取流程类型
     *
     * @return 流程类型
     */
    public String getFlowType() {
        return flowType;
    }

    /**
     * 设置流程类型。
     *
     * @param flowType 流程类型
     */
    public void setFlowType(String flowType) {
        this.flowType = flowType;
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户ID。
     *
     * @param userId 用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取挑战值
     *
     * @return 挑战值
     */
    public String getChallenge() {
        return challenge;
    }

    /**
     * 设置挑战值。
     *
     * @param challenge 挑战值
     */
    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    public Long getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}
