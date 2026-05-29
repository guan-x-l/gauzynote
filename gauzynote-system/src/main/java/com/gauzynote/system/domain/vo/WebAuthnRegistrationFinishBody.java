package com.gauzynote.system.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 完成 WebAuthn 注册请求体
 */
@Data
public class WebAuthnRegistrationFinishBody {
    /**
     * 浏览器返回的凭证ID
     */
    private String id;
    /**
     * 凭证ID的原始值(base64url)
     */
    private String rawId;
    /**
     * 凭证类型
     */
    private String type;
    /**
     * 凭证显示名称
     */
    private String credentialName;
    /**
     * 客户端 User-Agent
     */
    private String userAgent;
    /**
     * 认证器返回数据
     */
    private Response response;
    /**
     * 客户端扩展结果
     */
    private Object clientExtensionResults;

    /**
     * 认证器返回数据结构
     */
    @Data
    public static class Response {
        /**
         * 客户端数据JSON(base64url)
         */
        private String clientDataJSON;
        /**
         * 认证器证明对象(base64url)
         */
        private String attestationObject;
        /**
         * 传输能力
         */
        private List<String> transports;
    }
}
