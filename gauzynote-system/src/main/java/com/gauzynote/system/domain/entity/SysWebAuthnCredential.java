package com.gauzynote.system.domain.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * WebAuthn 凭证实体
 */
@Data
public class SysWebAuthnCredential implements Serializable {
    private static final long serialVersionUID = 1029794283122054138L;

    /**
     * 凭证记录主键
     */
    private Long credentialRecordId;
    /**
     * 关联用户ID
     */
    private Long userId;
    /**
     * 凭证ID(base64url)
     */
    private String credentialId;
    /**
     * 凭证名称
     */
    private String credentialName;
    /**
     * 公钥(SPKI base64)
     */
    private String publicKey;
    /**
     * 签名算法
     */
    private String algorithm;
    /**
     * 计数器
     */
    private Long signCount;
    /**
     * AAGUID
     */
    private String aaguid;
    /**
     * 传输能力
     */
    private String transports;
    /**
     * 用户句柄
     */
    private String userHandle;
    /**
     * 浏览器信息
     */
    private String browserName;
    /**
     * 状态(0启用 1禁用)
     */
    private String status;
    /**
     * 最后使用时间
     */
    private Date lastUsedTime;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
}
