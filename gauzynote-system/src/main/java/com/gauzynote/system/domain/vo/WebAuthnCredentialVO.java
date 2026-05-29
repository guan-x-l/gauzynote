package com.gauzynote.system.domain.vo;

import lombok.Data;

/**
 * WebAuthn 凭证展示对象
 */
@Data
public class WebAuthnCredentialVO {
    /**
     * 凭证ID
     */
    private String credentialId;
    /**
     * 凭证名称
     */
    private String credentialName;
    /**
     * 算法
     */
    private String algorithm;
    /**
     * 传输方式
     */
    private String transports;
    /**
     * 浏览器信息
     */
    private String browserName;
    /**
     * 最后使用时间
     */
    private String lastUsedTime;
    /**
     * 创建时间
     */
    private String createTime;
    /**
     * 是否可操作
     */
    private Boolean canOperate;
}
