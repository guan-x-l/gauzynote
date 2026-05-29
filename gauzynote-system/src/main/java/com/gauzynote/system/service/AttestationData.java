package com.gauzynote.system.service;

import java.security.PublicKey;

/**
 * 保存 attestation 解析结果
 */
class AttestationData {
    private final ParsedAuthenticatorData authenticatorData;
    private final PublicKey publicKey;
    private final String algorithm;

    /**
     * 初始化 attestation 数据对象
     *
     * @param authenticatorData 认证器数据
     * @param publicKey 公钥
     * @param algorithm 算法
     */
    AttestationData(ParsedAuthenticatorData authenticatorData, PublicKey publicKey, String algorithm) {
        this.authenticatorData = authenticatorData;
        this.publicKey = publicKey;
        this.algorithm = algorithm;
    }

    /**
     * 获取认证器数据
     *
     * @return 认证器数据
     */
    public ParsedAuthenticatorData getAuthenticatorData() {
        return authenticatorData;
    }

    /**
     * 获取公钥
     *
     * @return 公钥
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * 获取算法
     *
     * @return 算法名
     */
    public String getAlgorithm() {
        return algorithm;
    }
}
