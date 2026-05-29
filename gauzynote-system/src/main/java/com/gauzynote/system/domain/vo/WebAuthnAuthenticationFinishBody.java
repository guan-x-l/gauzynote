package com.gauzynote.system.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 完成 WebAuthn 认证请求体
 */
@Data
public class WebAuthnAuthenticationFinishBody {
    /**
     * 浏览器返回的凭证ID
     */
    private String id;
    /**
     * 凭证ID原始值(base64url)
     */
    private String rawId;
    /**
     * 凭证类型
     */
    private String type;
    /**
     * 用户句柄(base64url)
     */
    private String userHandle;
    /**
     * 客户端扩展结果
     */
    private Object clientExtensionResults;
    /**
     * 认证器返回数据
     */
    private Response response;

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
         * 认证器数据(base64url)
         */
        private String authenticatorData;
        /**
         * 签名(base64url)
         */
        private String signature;
        /**
         * 用户句柄(base64url)
         */
        private String userHandle;
        /**
         * 传输能力
         */
        private List<String> transports;
    }
}
