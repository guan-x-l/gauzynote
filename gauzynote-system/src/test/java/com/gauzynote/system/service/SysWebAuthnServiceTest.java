package com.gauzynote.system.service;

import com.gauzynote.common.domain.entity.SysUser;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.RedisUtil;
import com.gauzynote.system.domain.entity.SysWebAuthnCredential;
import com.gauzynote.system.domain.vo.WebAuthnCredentialVO;
import com.gauzynote.system.mapper.SysWebAuthnCredentialMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebAuthn 服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class SysWebAuthnServiceTest {
    @Mock
    private SysWebAuthnCredentialMapper webAuthnCredentialMapper;
    @Mock
    private RedisUtil redisUtil;
    @Mock
    private SysUserService sysUserService;

    @InjectMocks
    private SysWebAuthnService webAuthnService;

    /**
     * 注册参数应包含 ES256 和 RS256，且携带排除凭证
     */
    @Test
    void shouldBuildRegistrationOptionsWithAlgorithmsAndExcludeCredentials() {
        ReflectionTestUtils.setField(webAuthnService, "rpId", "localhost");
        ReflectionTestUtils.setField(webAuthnService, "rpName", "GauzyNote");
        ReflectionTestUtils.setField(webAuthnService, "timeoutMs", 60000L);
        ReflectionTestUtils.setField(webAuthnService, "challengeTtlSeconds", 300L);

        SysUser user = new SysUser();
        user.setUserId(2L);
        user.setUsername("u2");
        user.setNickname("u2");
        when(sysUserService.selectUserById(2L)).thenReturn(user);

        SysWebAuthnCredential credential = new SysWebAuthnCredential();
        credential.setCredentialId("credential-1");
        credential.setTransports("internal");
        when(webAuthnCredentialMapper.selectActiveByUserId(2L)).thenReturn(Arrays.asList(credential));

        Map<String, Object> options = webAuthnService.createRegistrationOptions(2L);
        Map<String, Object> publicKey = (Map<String, Object>) options.get("publicKey");
        List<Map<String, Object>> algs = (List<Map<String, Object>>) publicKey.get("pubKeyCredParams");
        List<Map<String, Object>> exclude = (List<Map<String, Object>>) publicKey.get("excludeCredentials");

        Assertions.assertNotNull(options.get("challengeId"));
        Assertions.assertEquals(2, algs.size());
        Assertions.assertTrue(algs.stream().anyMatch(item -> Integer.valueOf(-7).equals(item.get("alg"))));
        Assertions.assertTrue(algs.stream().anyMatch(item -> Integer.valueOf(-257).equals(item.get("alg"))));
        Assertions.assertEquals(1, exclude.size());
        verify(redisUtil).set(any(String.class), any(Object.class), eq(300L));
    }

    /**
     * 指定用户名但没有绑定凭证时应提示错误
     */
    @Test
    void shouldRejectAuthenticationWhenUserHasNoCredential() {
        SysUser user = new SysUser();
        user.setUserId(3L);
        user.setUsername("u3");
        when(sysUserService.selectUserByUsername("u3")).thenReturn(user);
        when(webAuthnCredentialMapper.selectActiveByUserId(3L)).thenReturn(Arrays.asList());

        Assertions.assertThrows(ServiceException.class, () -> webAuthnService.createAuthenticationOptions("u3"));
    }

    /**
     * 凭证列表应输出可展示字段
     */
    @Test
    void shouldListCredentialsAsVO() {
        SysWebAuthnCredential credential = new SysWebAuthnCredential();
        credential.setCredentialId("credential-2");
        credential.setAlgorithm("ES256");
        credential.setTransports("internal,hybrid");
        when(webAuthnCredentialMapper.selectActiveByUserId(9L)).thenReturn(Arrays.asList(credential));

        List<WebAuthnCredentialVO> list = webAuthnService.listCredentials(9L);

        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("credential-2", list.get(0).getCredentialId());
        Assertions.assertEquals("ES256", list.get(0).getAlgorithm());
        Assertions.assertEquals(Boolean.TRUE, list.get(0).getCanOperate());
    }

    /**
     * 撤销时凭证ID为空应报错
     */
    @Test
    void shouldRejectRevokeWhenCredentialIdBlank() {
        Assertions.assertThrows(ServiceException.class, () -> webAuthnService.revokeCredential(1L, ""));
    }

    /**
     * 修改通行密钥名称成功时应落库
     */
    @Test
    void shouldRenameCredential() {
        when(webAuthnCredentialMapper.updateCredentialName(1L, "credential-1", "My Passkey")).thenReturn(1);
        int rows = webAuthnService.renameCredential(1L, "credential-1", "My Passkey");
        Assertions.assertEquals(1, rows);
        verify(webAuthnCredentialMapper).updateCredentialName(1L, "credential-1", "My Passkey");
    }

    /**
     * 签名计数器不递增时应判定为风险
     */
    @Test
    void shouldRejectWhenSignCountNotIncreased() {
        Assertions.assertThrows(ServiceException.class, () ->
                ReflectionTestUtils.invokeMethod(webAuthnService, "validateSignCount", 10L, 10L));
    }

    /**
     * 计数器为0时应允许通过（兼容部分认证器）
     */
    @Test
    void shouldAllowZeroSignCount() {
        Assertions.assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(webAuthnService, "validateSignCount", 0L, 0L));
    }
}
