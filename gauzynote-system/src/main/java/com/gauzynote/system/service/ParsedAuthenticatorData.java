package com.gauzynote.system.service;

/**
 * 认证器数据解析对象
 */
class ParsedAuthenticatorData {
    private byte[] rpIdHash;
    private Long signCount;
    private boolean userPresent;
    private boolean userVerified;
    private String aaguid;
    private String credentialId;
    private byte[] credentialPublicKeyCose;
    private Integer algorithm;

    /**
     * 获取 RP ID 哈希
     *
     * @return RP ID 哈希
     */
    public byte[] getRpIdHash() {
        return rpIdHash;
    }

    /**
     * 设置 RP ID 哈希
     *
     * @param rpIdHash RP ID 哈希
     */
    public void setRpIdHash(byte[] rpIdHash) {
        this.rpIdHash = rpIdHash;
    }

    /**
     * 获取签名计数器
     *
     * @return 签名计数器
     */
    public Long getSignCount() {
        return signCount;
    }

    /**
     * 设置签名计数器
     *
     * @param signCount 签名计数器
     */
    public void setSignCount(Long signCount) {
        this.signCount = signCount;
    }

    /**
     * 获取是否用户在场
     *
     * @return UP 标志
     */
    public boolean isUserPresent() {
        return userPresent;
    }

    /**
     * 设置是否用户在场
     *
     * @param userPresent UP 标志
     */
    public void setUserPresent(boolean userPresent) {
        this.userPresent = userPresent;
    }

    /**
     * 获取是否用户已验证
     *
     * @return UV 标志
     */
    public boolean isUserVerified() {
        return userVerified;
    }

    /**
     * 设置是否用户已验证
     *
     * @param userVerified UV 标志
     */
    public void setUserVerified(boolean userVerified) {
        this.userVerified = userVerified;
    }

    /**
     * 获取 AAGUID
     *
     * @return AAGUID
     */
    public String getAaguid() {
        return aaguid;
    }

    /**
     * 设置 AAGUID
     *
     * @param aaguid AAGUID
     */
    public void setAaguid(String aaguid) {
        this.aaguid = aaguid;
    }

    /**
     * 获取凭证ID
     *
     * @return 凭证ID
     */
    public String getCredentialId() {
        return credentialId;
    }

    /**
     * 设置凭证ID
     *
     * @param credentialId 凭证ID
     */
    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    /**
     * 获取 COSE 公钥
     *
     * @return COSE 公钥字节
     */
    public byte[] getCredentialPublicKeyCose() {
        return credentialPublicKeyCose;
    }

    /**
     * 设置 COSE 公钥
     *
     * @param credentialPublicKeyCose COSE 公钥字节
     */
    public void setCredentialPublicKeyCose(byte[] credentialPublicKeyCose) {
        this.credentialPublicKeyCose = credentialPublicKeyCose;
    }

    /**
     * 获取算法编号
     *
     * @return 算法编号
     */
    public Integer getAlgorithm() {
        return algorithm;
    }

    /**
     * 设置算法编号
     *
     * @param algorithm 算法编号
     */
    public void setAlgorithm(Integer algorithm) {
        this.algorithm = algorithm;
    }
}
