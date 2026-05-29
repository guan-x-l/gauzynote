package com.gauzynote.system.domain.vo;

import lombok.Data;

/**
 * 修改通行密钥名称请求体
 */
@Data
public class WebAuthnCredentialRenameBody {
    /**
     * 新的通行密钥名称
     */
    private String credentialName;
}
