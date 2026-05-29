package com.gauzynote.application.controller.system;

import com.gauzynote.common.annotation.Log;
import com.gauzynote.common.annotation.RequestLimiter;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.domain.model.LoginUser;
import com.gauzynote.common.enums.BusinessType;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.framework.service.TokenService;
import com.gauzynote.system.domain.vo.WebAuthnAuthenticationFinishBody;
import com.gauzynote.system.domain.vo.WebAuthnCredentialRenameBody;
import com.gauzynote.system.domain.vo.WebAuthnRegistrationFinishBody;
import com.gauzynote.system.service.SysWebAuthnService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static com.gauzynote.common.constant.Constants.TOKEN;

/**
 * WebAuthn 无密码登录控制器
 */
@RestController
@RequestMapping("/webauthn")
public class SysWebAuthnController {
    @Resource
    private SysWebAuthnService webAuthnService;
    @Resource
    private TokenService tokenService;

    /**
     * 生成注册参数
     *
     * @return 注册选项
     */
    @RequestLimiter
    @PostMapping("/register/options")
    public AjaxResult registerOptions() {
        Long userId = SecurityUtils.getUserId();
        return AjaxResult.success(webAuthnService.createRegistrationOptions(userId));
    }

    /**
     * 校验注册结果并落库凭证
     *
     * @param challengeId 挑战ID
     * @param body 注册响应
     * @return 注册结果
     */
    @Log(operationName = "registerPasskey", businessType = BusinessType.ADD, isSaveResponseParams = true)
    @RequestLimiter
    @PostMapping("/register/verify")
    public AjaxResult registerVerify(@RequestParam("challengeId") String challengeId, @RequestBody WebAuthnRegistrationFinishBody body) {
        Long userId = SecurityUtils.getUserId();
        return AjaxResult.success(webAuthnService.verifyRegistration(userId, challengeId, body));
    }

    /**
     * 生成认证参数
     *
     * @param body 认证请求体
     * @return 认证选项
     */
    @RequestLimiter
    @PostMapping("/auth/options")
    public AjaxResult authOptions(@RequestBody(required = false) Map<String, Object> body) {
        String username = body == null ? null : Objects.toString(body.get("username"), null);
        return AjaxResult.success(webAuthnService.createAuthenticationOptions(username));
    }

    /**
     * 校验认证结果并签发会话
     *
     * @param challengeId 挑战ID
     * @param body 认证响应
     * @return 登录结果
     */
    @Log(operationName = "loginByPasskey", businessType = BusinessType.LOGIN, isSaveResponseParams = true)
    @RequestLimiter
    @PostMapping("/auth/verify")
    public AjaxResult authVerify(@RequestParam("challengeId") String challengeId, @RequestBody WebAuthnAuthenticationFinishBody body) {
        LoginUser loginUser = webAuthnService.verifyAuthentication(challengeId, body);
        String token = tokenService.createToken(loginUser);
        return AjaxResult.success().put(TOKEN, token).put("authMethod", "passkey");
    }

    /**
     * 查询当前用户已绑定凭证
     *
     * @return 凭证列表
     */
    @RequestLimiter
    @GetMapping("/credentials")
    public AjaxResult credentials() {
        Long userId = SecurityUtils.getUserId();
        return AjaxResult.success(webAuthnService.listCredentials(userId));
    }

    /**
     * 撤销当前用户凭证
     *
     * @param credentialId 凭证ID
     * @return 撤销结果
     */
    @Log(operationName = "revokePasskey", businessType = BusinessType.REMOVE)
    @RequestLimiter
    @DeleteMapping("/credentials/{credentialId}")
    public AjaxResult revoke(@PathVariable("credentialId") String credentialId) {
        Long userId = SecurityUtils.getUserId();
        int rows = webAuthnService.revokeCredential(userId, credentialId);
        if (rows <= 0) {
            return AjaxResult.error("凭证不存在或已撤销").put("data", Collections.singletonMap("credentialId", credentialId));
        }
        return AjaxResult.success(Collections.singletonMap("credentialId", credentialId));
    }

    /**
     * 修改当前用户凭证名称
     *
     * @param credentialId 凭证ID
     * @param body 名称请求体
     * @return 修改结果
     */
    @Log(operationName = "renamePasskey", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PutMapping("/credentials/{credentialId}/name")
    public AjaxResult renameCredential(@PathVariable("credentialId") String credentialId, @RequestBody WebAuthnCredentialRenameBody body) {
        Long userId = SecurityUtils.getUserId();
        String credentialName = body == null ? null : body.getCredentialName();
        int rows = webAuthnService.renameCredential(userId, credentialId, credentialName);
        if (rows <= 0) {
            return AjaxResult.error("凭证不存在或已撤销").put("data", Collections.singletonMap("credentialId", credentialId));
        }
        return AjaxResult.success(Collections.singletonMap("credentialId", credentialId));
    }
}
