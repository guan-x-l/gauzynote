package com.gauzynote.system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.gauzynote.common.constant.CacheConstants;
import com.gauzynote.common.domain.entity.SysUser;
import com.gauzynote.common.domain.model.LoginUser;
import com.gauzynote.common.enums.UserStatus;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.RedisUtil;
import com.gauzynote.system.domain.entity.SysWebAuthnCredential;
import com.gauzynote.system.domain.vo.WebAuthnAuthenticationFinishBody;
import com.gauzynote.system.domain.vo.WebAuthnCredentialVO;
import com.gauzynote.system.domain.vo.WebAuthnRegistrationFinishBody;
import com.gauzynote.system.mapper.SysWebAuthnCredentialMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WebAuthn 无密码认证服务
 */
@Service
public class SysWebAuthnService {
    private static final String FLOW_REGISTER = "register";
    private static final String FLOW_AUTH = "auth";
    private static final long AUTH_DATA_FIXED_LEN = 37L;
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper CBOR_MAPPER = new ObjectMapper(new CBORFactory());
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder B64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODER = Base64.getUrlDecoder();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private SysWebAuthnCredentialMapper webAuthnCredentialMapper;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private SysUserService sysUserService;

    @Value("${app.webauthn.rp-id}")
    private String rpId;
    @Value("${app.webauthn.rp-name}")
    private String rpName;
    @Value("${app.webauthn.origins}")
    private List<String> expectedOrigin;
    @Value("${app.webauthn.timeout-ms}")
    private long timeoutMs;
    @Value("${app.webauthn.challenge-ttl-seconds}")
    private long challengeTtlSeconds;
    @Value("${app.webauthn.require-user-verification}")
    private boolean requireUserVerification;

    /**
     * 生成注册选项并缓存挑战
     *
     * @param userId 用户ID
     * @return 注册参数
     */
    public Map<String, Object> createRegistrationOptions(Long userId) {
        SysUser user = requireUser(userId);
        List<SysWebAuthnCredential> credentials = webAuthnCredentialMapper.selectActiveByUserId(userId);
        String challenge = randomBase64Url(32);
        String challengeId = UUID.randomUUID().toString().replace("-", "");
        saveChallenge(challengeId, new WebAuthnChallengeSession(FLOW_REGISTER, userId, user.getUsername(), challenge));

        Map<String, Object> options = new HashMap<>();
        Map<String, Object> publicKey = new HashMap<>();
        publicKey.put("challenge", challenge);
        publicKey.put("rp", createRp());
        publicKey.put("user", createUser(user));
        publicKey.put("pubKeyCredParams", createPubKeyCredParams());
        publicKey.put("timeout", timeoutMs);
        publicKey.put("attestation", "none");
        publicKey.put("excludeCredentials", credentials.stream().map(this::toAllowCredential).collect(Collectors.toList()));
        publicKey.put("authenticatorSelection", createAuthenticatorSelection());
        options.put("challengeId", challengeId);
        options.put("publicKey", publicKey);
        return options;
    }

    /**
     * 校验注册响应并保存凭证
     *
     * @param userId 用户ID
     * @param challengeId 挑战ID
     * @param body 注册响应
     * @return 注册结果
     */
    public Map<String, Object> verifyRegistration(Long userId, String challengeId, WebAuthnRegistrationFinishBody body) {
        validateRegistrationBody(body);
        WebAuthnChallengeSession session = consumeChallenge(challengeId, FLOW_REGISTER);
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "WebAuthn 注册挑战与当前用户不匹配");
        }
        byte[] clientDataBytes = decodeB64Url(body.getResponse().getClientDataJSON());
        Map<String, Object> clientData = parseClientData(clientDataBytes, "webauthn.create", session.getChallenge());

        byte[] attestationObject = decodeB64Url(body.getResponse().getAttestationObject());
        AttestationData attestationData = parseAttestation(attestationObject);
        ensureRpIdHash(attestationData.getAuthenticatorData().getRpIdHash());
        ensureUserPresence(attestationData.getAuthenticatorData().isUserPresent());
        if (requireUserVerification) {
            ensureUserVerification(attestationData.getAuthenticatorData().isUserVerified());
        }
        ensureCredentialIdMatches(body.getId(), attestationData.getAuthenticatorData().getCredentialId());
        ensureOrigin(clientData);
        ensureUniqueCredential(body.getId());

        SysWebAuthnCredential credential = new SysWebAuthnCredential();
        credential.setUserId(userId);
        credential.setCredentialId(body.getId());
        credential.setCredentialName(resolveCredentialName(body));
        credential.setPublicKey(encodeB64Url(attestationData.getPublicKey().getEncoded()));
        credential.setAlgorithm(attestationData.getAlgorithm());
        credential.setSignCount(attestationData.getAuthenticatorData().getSignCount());
        credential.setAaguid(attestationData.getAuthenticatorData().getAaguid());
        credential.setTransports(joinTransports(body.getResponse().getTransports()));
        credential.setUserHandle(encodeB64Url(String.valueOf(userId).getBytes(StandardCharsets.UTF_8)));
        credential.setBrowserName(resolveBrowserName(body.getUserAgent()));
        int rows = webAuthnCredentialMapper.insertCredential(credential);
        if (rows <= 0) {
            throw new ServiceException("保存 WebAuthn 凭证失败");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("credentialId", credential.getCredentialId());
        result.put("algorithm", credential.getAlgorithm());
        result.put("userPresent", attestationData.getAuthenticatorData().isUserPresent());
        result.put("userVerified", attestationData.getAuthenticatorData().isUserVerified());
        return result;
    }

    /**
     * 生成认证选项并缓存挑战
     *
     * @param username 用户名（为空时走 discoverable 流程）
     * @return 认证参数
     */
    public Map<String, Object> createAuthenticationOptions(String username) {
        Long userId = null;
        List<SysWebAuthnCredential> credentials = Collections.emptyList();
        if (StringUtils.hasText(username)) {
            SysUser user = sysUserService.selectUserByUsername(username);
            if (ObjectUtils.isEmpty(user)) {
                throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("user.not.exist"));
            } else if (!UserStatus.NORMAL.getCode().equals(user.getStatus())) {
                throw new ServiceException(MessageUtils.message("user.suspended"));
            }
            userId = user.getUserId();
            credentials = webAuthnCredentialMapper.selectActiveByUserId(userId);
            if (credentials.isEmpty()) {
                throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "该账号尚未绑定 Passkey");
            }
        }

        String challenge = randomBase64Url(32);
        String challengeId = UUID.randomUUID().toString().replace("-", "");
        saveChallenge(challengeId, new WebAuthnChallengeSession(FLOW_AUTH, userId, username, challenge));

        Map<String, Object> publicKey = new HashMap<>();
        publicKey.put("challenge", challenge);
        publicKey.put("rpId", rpId);
        publicKey.put("timeout", timeoutMs);
        publicKey.put("userVerification", requireUserVerification ? "required" : "preferred");
        if (!credentials.isEmpty()) {
            publicKey.put("allowCredentials", credentials.stream().map(this::toAllowCredential).collect(Collectors.toList()));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("challengeId", challengeId);
        result.put("publicKey", publicKey);
        return result;
    }

    /**
     * 校验认证响应并返回登录用户
     *
     * @param challengeId 挑战ID
     * @param body 认证响应
     * @return 登录用户
     */
    public LoginUser verifyAuthentication(String challengeId, WebAuthnAuthenticationFinishBody body) {
        validateAuthenticationBody(body);
        WebAuthnChallengeSession session = consumeChallenge(challengeId, FLOW_AUTH);
        byte[] clientDataBytes = decodeB64Url(body.getResponse().getClientDataJSON());
        Map<String, Object> clientData = parseClientData(clientDataBytes, "webauthn.get", session.getChallenge());
        ensureOrigin(clientData);

        byte[] authenticatorDataBytes = decodeB64Url(body.getResponse().getAuthenticatorData());
        ParsedAuthenticatorData authenticatorData = parseAuthenticatorData(authenticatorDataBytes, false);
        ensureRpIdHash(authenticatorData.getRpIdHash());
        ensureUserPresence(authenticatorData.isUserPresent());
        if (requireUserVerification) {
            ensureUserVerification(authenticatorData.isUserVerified());
        }

        SysWebAuthnCredential credential = webAuthnCredentialMapper.selectActiveByCredentialId(body.getId());
        if (ObjectUtils.isEmpty(credential)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "Passkey 不存在或已被禁用");
        }
        if (!ObjectUtils.isEmpty(session.getUserId()) && !Objects.equals(session.getUserId(), credential.getUserId())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "Passkey 与挑战中的账号不匹配");
        }

        verifyAssertionSignature(credential, body, authenticatorDataBytes, clientDataBytes);
        validateSignCount(credential.getSignCount(), authenticatorData.getSignCount());
        webAuthnCredentialMapper.updateUsage(credential.getCredentialRecordId(), authenticatorData.getSignCount());

        SysUser user = requireUser(credential.getUserId());
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getUsername(), user.getPassword(), LoginUser.LOGIN_METHOD_WEBAUTHN, user);
        return loginUser;
    }

    /**
     * 查询用户的凭证列表
     *
     * @param userId 用户ID
     * @return 凭证展示列表
     */
    public List<WebAuthnCredentialVO> listCredentials(Long userId) {
        List<SysWebAuthnCredential> credentials = webAuthnCredentialMapper.selectActiveByUserId(userId);
        return credentials.stream().map(item -> {
            WebAuthnCredentialVO vo = new WebAuthnCredentialVO();
            vo.setCredentialId(item.getCredentialId());
            vo.setCredentialName(item.getCredentialName());
            vo.setAlgorithm(item.getAlgorithm());
            vo.setTransports(item.getTransports());
            vo.setBrowserName(item.getBrowserName());
            vo.setLastUsedTime(formatDate(item.getLastUsedTime()));
            vo.setCreateTime(formatDate(item.getCreateTime()));
            vo.setCanOperate(Boolean.TRUE);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 撤销当前用户的凭证
     *
     * @param userId 用户ID
     * @param credentialId 凭证ID
     * @return 影响行数
     */
    public int revokeCredential(Long userId, String credentialId) {
        if (!StringUtils.hasText(credentialId)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "凭证ID不能为空");
        }
        return webAuthnCredentialMapper.revokeCredential(userId, credentialId);
    }

    /**
     * 修改当前用户凭证名称
     *
     * @param userId 用户ID
     * @param credentialId 凭证ID
     * @param credentialName 凭证名称
     * @return 影响行数
     */
    public int renameCredential(Long userId, String credentialId, String credentialName) {
        if (!StringUtils.hasText(credentialId)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "凭证ID不能为空");
        }
        String normalizedName = normalizeCredentialName(credentialName);
        if (!StringUtils.hasText(normalizedName)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "通行密钥名称不能为空");
        }
        return webAuthnCredentialMapper.updateCredentialName(userId, credentialId, normalizedName);
    }

    /**
     * 校验注册请求体的完整性
     *
     * @param body 注册请求
     */
    private void validateRegistrationBody(WebAuthnRegistrationFinishBody body) {
        if (ObjectUtils.isEmpty(body) || ObjectUtils.isEmpty(body.getResponse())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "无效的注册参数");
        }
        if (!StringUtils.hasText(body.getId()) || !StringUtils.hasText(body.getResponse().getClientDataJSON())
                || !StringUtils.hasText(body.getResponse().getAttestationObject())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "注册参数缺失");
        }
    }

    /**
     * 校验认证请求体的完整性
     *
     * @param body 认证请求
     */
    private void validateAuthenticationBody(WebAuthnAuthenticationFinishBody body) {
        if (ObjectUtils.isEmpty(body) || ObjectUtils.isEmpty(body.getResponse())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "无效的认证参数");
        }
        if (!StringUtils.hasText(body.getId())
                || !StringUtils.hasText(body.getResponse().getClientDataJSON())
                || !StringUtils.hasText(body.getResponse().getAuthenticatorData())
                || !StringUtils.hasText(body.getResponse().getSignature())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "认证参数缺失");
        }
    }

    /**
     * 根据用户ID获取用户，不存在则抛错
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    private SysUser requireUser(Long userId) {
        SysUser user = sysUserService.selectUserById(userId);
        if (ObjectUtils.isEmpty(user)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "账号不存在");
        }
        return user;
    }

    /**
     * 构建 RP 参数
     *
     * @return RP 参数
     */
    private Map<String, Object> createRp() {
        Map<String, Object> rp = new HashMap<>();
        rp.put("id", rpId);
        rp.put("name", rpName);
        return rp;
    }

    /**
     * 构建 user 参数
     *
     * @param user 用户
     * @return user 参数
     */
    private Map<String, Object> createUser(SysUser user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", encodeB64Url(String.valueOf(user.getUserId()).getBytes(StandardCharsets.UTF_8)));
        userMap.put("name", user.getUsername());
        userMap.put("displayName", user.getNickname());
        return userMap;
    }

    /**
     * 构建支持的公钥算法列表
     *
     * @return 算法参数
     */
    private List<Map<String, Object>> createPubKeyCredParams() {
        List<Map<String, Object>> params = new ArrayList<>();
        Map<String, Object> es256 = new HashMap<>();
        es256.put("type", "public-key");
        es256.put("alg", -7);
        params.add(es256);

        Map<String, Object> rs256 = new HashMap<>();
        rs256.put("type", "public-key");
        rs256.put("alg", -257);
        params.add(rs256);
        return params;
    }

    /**
     * 构建认证器选择策略
     *
     * @return 认证器选择参数
     */
    private Map<String, Object> createAuthenticatorSelection() {
        Map<String, Object> selection = new HashMap<>();
        selection.put("residentKey", "preferred");
        selection.put("userVerification", requireUserVerification ? "required" : "preferred");
        return selection;
    }

    /**
     * 构建 allow/exclude 结构
     *
     * @param credential 凭证信息
     * @return 结构体
     */
    private Map<String, Object> toAllowCredential(SysWebAuthnCredential credential) {
        Map<String, Object> item = new HashMap<>();
        item.put("type", "public-key");
        item.put("id", credential.getCredentialId());
        if (StringUtils.hasText(credential.getTransports())) {
            item.put("transports", Arrays.asList(credential.getTransports().split(",")));
        }
        return item;
    }

    /**
     * 保存挑战到 Redis
     *
     * @param challengeId 挑战ID
     * @param session 会话数据
     */
    private void saveChallenge(String challengeId, WebAuthnChallengeSession session) {
        redisUtil.set(challengeKey(challengeId), session, challengeTtlSeconds);
    }

    /**
     * 消费挑战，确保仅可使用一次
     *
     * @param challengeId 挑战ID
     * @param expectedFlow 期望流程类型
     * @return 挑战会话
     */
    private WebAuthnChallengeSession consumeChallenge(String challengeId, String expectedFlow) {
        if (!StringUtils.hasText(challengeId)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "挑战ID不能为空");
        }
        String key = challengeKey(challengeId);
        Object value = redisUtil.get(key);
        redisUtil.deleteObject(key);
        if (ObjectUtils.isEmpty(value)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "挑战不存在或已过期");
        }
        if (!(value instanceof WebAuthnChallengeSession)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "挑战数据格式无效");
        }
        WebAuthnChallengeSession session = (WebAuthnChallengeSession) value;
        if (!Objects.equals(expectedFlow, session.getFlowType())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "挑战流程类型不匹配");
        }
        return session;
    }

    /**
     * 解析并校验 clientDataJSON
     *
     * @param clientDataBytes clientDataJSON 原始字节
     * @param expectedType 期望类型
     * @param expectedChallenge 期望挑战值
     * @return 解析后的 map
     */
    private Map<String, Object> parseClientData(byte[] clientDataBytes, String expectedType, String expectedChallenge) {
        try {
            Map<String, Object> clientData = JSON_MAPPER.readValue(clientDataBytes, new TypeReference<Map<String, Object>>() {});
            String type = Objects.toString(clientData.get("type"), "");
            String challenge = Objects.toString(clientData.get("challenge"), "");
            if (!Objects.equals(expectedType, type)) {
                throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "WebAuthn 类型不匹配");
            }
            if (!Objects.equals(expectedChallenge, challenge)) {
                throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "WebAuthn 挑战校验失败");
            }
            return clientData;
        } catch (IOException e) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "解析 clientDataJSON 失败");
        }
    }

    /**
     * 校验 Origin 是否符合配置
     *
     * @param clientData clientData
     */
    private void ensureOrigin(Map<String, Object> clientData) {
        String origin = Objects.toString(clientData.get("origin"), "");
        if (!expectedOrigin.contains(origin)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "WebAuthn Origin 校验失败");
        }
    }

    /**
     * 解析 attestationObject 并提取公钥
     *
     * @param attestationObject attestationObject
     * @return 解析结果
     */
    private AttestationData parseAttestation(byte[] attestationObject) {
        try {
            Map<String, Object> attestationMap = CBOR_MAPPER.readValue(attestationObject, new TypeReference<Map<String, Object>>() {});
            byte[] authDataBytes = (byte[]) attestationMap.get("authData");
            if (ObjectUtils.isEmpty(authDataBytes)) {
                throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "attestationObject 缺少 authData");
            }
            ParsedAuthenticatorData authData = parseAuthenticatorData(authDataBytes, true);
            if (ObjectUtils.isEmpty(authData.getCredentialPublicKeyCose())) {
                throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "缺少 credentialPublicKey");
            }
            PublicKey publicKey = parsePublicKeyFromCose(authData.getCredentialPublicKeyCose());
            return new AttestationData(authData, publicKey, mapAlg(authData.getAlgorithm()));
        } catch (IOException e) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "解析 attestationObject 失败");
        }
    }

    /**
     * 解析 authenticatorData
     *
     * @param authData authenticatorData 字节
     * @param expectAttestedData 是否要求包含 attested credential data
     * @return 解析结构
     */
    private ParsedAuthenticatorData parseAuthenticatorData(byte[] authData, boolean expectAttestedData) {
        if (ObjectUtils.isEmpty(authData) || authData.length < AUTH_DATA_FIXED_LEN) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "authenticatorData 长度无效");
        }
        ByteBuffer buffer = ByteBuffer.wrap(authData);
        byte[] rpIdHash = new byte[32];
        buffer.get(rpIdHash);
        byte flags = buffer.get();
        long signCount = Integer.toUnsignedLong(buffer.getInt());
        boolean userPresent = (flags & 0x01) != 0;
        boolean userVerified = (flags & 0x04) != 0;
        boolean hasAttestedData = (flags & 0x40) != 0;

        ParsedAuthenticatorData parsed = new ParsedAuthenticatorData();
        parsed.setRpIdHash(rpIdHash);
        parsed.setSignCount(signCount);
        parsed.setUserPresent(userPresent);
        parsed.setUserVerified(userVerified);
        if (!expectAttestedData) {
            return parsed;
        }
        if (!hasAttestedData) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "注册数据缺少 attested credential data");
        }
        if (buffer.remaining() < 18) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "attested credential data 长度无效");
        }

        byte[] aaguid = new byte[16];
        buffer.get(aaguid);
        int credentialIdLength = Short.toUnsignedInt(buffer.getShort());
        if (buffer.remaining() < credentialIdLength) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "credentialId 长度无效");
        }
        byte[] credentialIdBytes = new byte[credentialIdLength];
        buffer.get(credentialIdBytes);
        byte[] credentialPublicKeyCose = new byte[buffer.remaining()];
        buffer.get(credentialPublicKeyCose);

        parsed.setAaguid(toHex(aaguid));
        parsed.setCredentialId(encodeB64Url(credentialIdBytes));
        parsed.setCredentialPublicKeyCose(credentialPublicKeyCose);
        parsed.setAlgorithm(readAlgorithmFromCose(credentialPublicKeyCose));
        return parsed;
    }

    /**
     * 从 COSE_Key 解析算法编号
     *
     * @param coseKey COSE_Key
     * @return 算法编号
     */
    private Integer readAlgorithmFromCose(byte[] coseKey) {
        try {
            Map<Object, Object> map = CBOR_MAPPER.readValue(coseKey, new TypeReference<Map<Object, Object>>() {});
            Object alg = getCoseMapValue(map, 3);
            if (alg instanceof Number) {
                return ((Number) alg).intValue();
            }
            if (alg instanceof String && StringUtils.hasText((String) alg)) {
                String normalized = ((String) alg).trim();
                if ("ES256".equalsIgnoreCase(normalized)) {
                    return -7;
                }
                if ("RS256".equalsIgnoreCase(normalized)) {
                    return -257;
                }
                try {
                    return Integer.parseInt(normalized);
                } catch (NumberFormatException ignored) {
                    // ignore and fallback to key-type inference
                }
            }
            Integer inferred = inferAlgorithmFromCoseKeyType(map);
            if (inferred != null) {
                return inferred;
            }
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "不支持的 COSE 算法");
        } catch (IOException e) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "解析 COSE 算法失败");
        }
    }

    /**
     * 按 COSE kty 推断算法编号，作为缺少 alg 字段时的兜底逻辑。
     *
     * @param map COSE 键值对
     * @return 推断出的算法编号
     */
    private Integer inferAlgorithmFromCoseKeyType(Map<Object, Object> map) {
        Object kty = getCoseMapValue(map, 1);
        if (!(kty instanceof Number)) {
            return null;
        }
        int value = ((Number) kty).intValue();
        if (value == 2) {
            return -7;
        }
        if (value == 3) {
            return -257;
        }
        return null;
    }

    /**
     * 从 COSE_Key 解析 Java PublicKey
     *
     * @param coseKey COSE_Key
     * @return 公钥
     */
    private PublicKey parsePublicKeyFromCose(byte[] coseKey) {
        try {
            Map<Object, Object> map = CBOR_MAPPER.readValue(new ByteArrayInputStream(coseKey), new TypeReference<Map<Object, Object>>() {});
            int kty = numberValue(getCoseMapValue(map, 1));
            if (kty == 2) {
                return parseEcPublicKey(map);
            }
            if (kty == 3) {
                return parseRsaPublicKey(map);
            }
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "暂不支持的公钥类型");
        } catch (IOException e) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "解析 COSE_Key 失败");
        }
    }

    /**
     * 解析 EC2 公钥
     *
     * @param map COSE map
     * @return EC 公钥
     */
    private PublicKey parseEcPublicKey(Map<Object, Object> map) {
        byte[] x = byteValue(getCoseMapValue(map, -2));
        byte[] y = byteValue(getCoseMapValue(map, -3));
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);
            ECPoint point = new ECPoint(new BigInteger(1, x), new BigInteger(1, y));
            ECPublicKeySpec keySpec = new ECPublicKeySpec(point, ecParameterSpec);
            return KeyFactory.getInstance("EC").generatePublic(keySpec);
        } catch (Exception e) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "解析 EC 公钥失败");
        }
    }

    /**
     * 解析 RSA 公钥
     *
     * @param map COSE map
     * @return RSA 公钥
     */
    private PublicKey parseRsaPublicKey(Map<Object, Object> map) {
        byte[] modulus = byteValue(getCoseMapValue(map, -1));
        byte[] exponent = byteValue(getCoseMapValue(map, -2));
        try {
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent));
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception e) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "解析 RSA 公钥失败");
        }
    }

    /**
     * 校验 assertion 签名
     *
     * @param credential 凭证
     * @param body 认证响应
     * @param authenticatorData 认证器数据
     * @param clientDataJSON 客户端数据
     */
    private void verifyAssertionSignature(SysWebAuthnCredential credential, WebAuthnAuthenticationFinishBody body, byte[] authenticatorData, byte[] clientDataJSON) {
        try {
            PublicKey publicKey = parseStoredPublicKey(credential);
            byte[] signature = decodeB64Url(body.getResponse().getSignature());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] clientHash = digest.digest(clientDataJSON);
            byte[] signedData = concat(authenticatorData, clientHash);
            Signature verifier = Signature.getInstance(signatureAlgorithm(credential.getAlgorithm(), publicKey));
            verifier.initVerify(publicKey);
            verifier.update(signedData);
            if (!verifier.verify(signature)) {
                throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "Passkey 签名校验失败");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "Passkey 签名校验异常");
        }
    }

    /**
     * 解析存储的公钥
     *
     * @param credential 凭证
     * @return 公钥
     */
    private PublicKey parseStoredPublicKey(SysWebAuthnCredential credential) {
        try {
            byte[] keyBytes = decodeB64Url(credential.getPublicKey());
            String algorithm = credential.getAlgorithm();
            if ("ES256".equals(algorithm)) {
                return KeyFactory.getInstance("EC").generatePublic(new java.security.spec.X509EncodedKeySpec(keyBytes));
            }
            return KeyFactory.getInstance("RSA").generatePublic(new java.security.spec.X509EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "解析存储公钥失败");
        }
    }

    /**
     * 校验签名计数器，检测克隆风险
     *
     * @param oldCount 历史计数
     * @param newCount 新计数
     */
    private void validateSignCount(Long oldCount, Long newCount) {
        if (ObjectUtils.isEmpty(oldCount) || ObjectUtils.isEmpty(newCount)) {
            return;
        }
        if (newCount == 0L || oldCount == 0L) {
            return;
        }
        if (newCount <= oldCount) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "检测到可疑设备克隆，计数器未递增");
        }
    }

    /**
     * 确保凭证ID唯一
     *
     * @param credentialId 凭证ID
     */
    private void ensureUniqueCredential(String credentialId) {
        SysWebAuthnCredential existed = webAuthnCredentialMapper.selectActiveByCredentialId(credentialId);
        if (!ObjectUtils.isEmpty(existed)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "当前 Passkey 已绑定");
        }
    }

    /**
     * 校验 RP ID 哈希
     *
     * @param rpIdHash RP ID 哈希
     */
    private void ensureRpIdHash(byte[] rpIdHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] expectedHash = digest.digest(rpId.getBytes(StandardCharsets.UTF_8));
            if (!Arrays.equals(expectedHash, rpIdHash)) {
                throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "RP ID 校验失败");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "RP ID 校验异常");
        }
    }

    /**
     * 校验用户在场标志
     *
     * @param userPresent UP 标志
     */
    private void ensureUserPresence(boolean userPresent) {
        if (!userPresent) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "未检测到用户在场(UP)");
        }
    }

    /**
     * 校验用户验证标志
     *
     * @param userVerified UV 标志
     */
    private void ensureUserVerification(boolean userVerified) {
        if (!userVerified) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "未完成用户验证(UV)");
        }
    }

    /**
     * 生成挑战缓存键
     *
     * @param challengeId 挑战ID
     * @return 缓存键
     */
    private String challengeKey(String challengeId) {
        return CacheConstants.WEBAUTHN_CHALLENGE_KEY + challengeId;
    }

    /**
     * 生成 base64url 随机串
     *
     * @param byteLength 字节长度
     * @return 随机串
     */
    private String randomBase64Url(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return encodeB64Url(bytes);
    }

    /**
     * base64url 编码
     *
     * @param value 字节数组
     * @return 编码结果
     */
    private String encodeB64Url(byte[] value) {
        return B64_ENCODER.encodeToString(value);
    }

    /**
     * base64url 解码
     *
     * @param value 编码串
     * @return 字节数组
     */
    private byte[] decodeB64Url(String value) {
        try {
            return B64_DECODER.decode(value);
        } catch (Exception e) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "base64url 解码失败");
        }
    }

    /**
     * 合并数组
     *
     * @param first 第一段
     * @param second 第二段
     * @return 合并结果
     */
    private byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    /**
     * 数值对象转 int
     *
     * @param value 数值对象
     * @return int 值
     */
    private int numberValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "参数类型不正确");
    }

    /**
     * 字节对象转 byte[]
     *
     * @param value 原始对象
     * @return byte[]
     */
    private byte[] byteValue(Object value) {
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "字节参数格式不正确");
    }

    /**
     * 校验请求中的凭证ID与认证器解析出的凭证ID一致，防止参数篡改。
     *
     * @param requestCredentialId 请求携带的凭证ID
     * @param parsedCredentialId 认证器数据中的凭证ID
     */
    private void ensureCredentialIdMatches(String requestCredentialId, String parsedCredentialId) {
        if (!StringUtils.hasText(requestCredentialId) || !StringUtils.hasText(parsedCredentialId)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "凭证ID缺失");
        }
        if (!Objects.equals(requestCredentialId, parsedCredentialId)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "凭证ID校验失败");
        }
    }

    /**
     * 将时间格式化为展示文本，空值返回空字符串。
     *
     * @param date 时间对象
     * @return 格式化结果
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return DATE_FORMATTER.format(Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()));
    }

    /**
     * 从 COSE Map 中读取整数键对应的值，兼容不同 Number 键类型。
     *
     * @param map COSE 键值对
     * @param key 目标键
     * @return 对应值
     */
    private Object getCoseMapValue(Map<Object, Object> map, int key) {
        Object direct = map.get(key);
        if (direct != null) {
            return direct;
        }
        Object directByString = map.get(String.valueOf(key));
        if (directByString != null) {
            return directByString;
        }
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Object mapKey = entry.getKey();
            if (mapKey instanceof Number && ((Number) mapKey).intValue() == key) {
                return entry.getValue();
            }
            if (mapKey instanceof String && StringUtils.hasText((String) mapKey)) {
                try {
                    if (Integer.parseInt(((String) mapKey).trim()) == key) {
                        return entry.getValue();
                    }
                } catch (NumberFormatException ignored) {
                    // ignore non-numeric string keys
                }
            }
        }
        return null;
    }

    /**
     * 算法编号映射为可读字符串
     *
     * @param alg 算法编号
     * @return 可读算法名
     */
    private String mapAlg(Integer alg) {
        if (Objects.equals(alg, -7)) {
            return "ES256";
        }
        if (Objects.equals(alg, -257)) {
            return "RS256";
        }
        throw new ServiceException(HttpStatus.BAD_REQUEST.value(), "不支持的签名算法");
    }

    /**
     * 获取 Java Signature 算法名
     *
     * @param alg WebAuthn 算法
     * @param publicKey 公钥对象
     * @return Signature 算法名
     */
    private String signatureAlgorithm(String alg, PublicKey publicKey) {
        if ("ES256".equals(alg) || publicKey instanceof ECPublicKey) {
            return "SHA256withECDSA";
        }
        return "SHA256withRSA";
    }

    /**
     * 将 AAGUID 字节转十六进制
     *
     * @param bytes AAGUID 字节
     * @return 十六进制文本
     */
    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte aByte : bytes) {
            builder.append(String.format("%02x", aByte));
        }
        return builder.toString();
    }

    /**
     * 组装 transports 字段
     *
     * @param transports 浏览器返回 transports
     * @return 逗号分隔字符串
     */
    private String joinTransports(List<String> transports) {
        if (ObjectUtils.isEmpty(transports)) {
            return "";
        }
        return transports.stream().filter(StringUtils::hasText).collect(Collectors.joining(","));
    }

    /**
     * 解析并生成凭证名称
     *
     * @param body 注册请求
     * @return 凭证名称
     */
    private String resolveCredentialName(WebAuthnRegistrationFinishBody body) {
        String fromRequest = normalizeCredentialName(body.getCredentialName());
        if (StringUtils.hasText(fromRequest)) {
            return fromRequest;
        }
        return "Passkey-" + resolveBrowserName(body.getUserAgent());
    }

    /**
     * 解析浏览器展示名称
     *
     * @param userAgent user-agent
     * @return 浏览器名称
     */
    private String resolveBrowserName(String userAgent) {
        String text = Objects.toString(userAgent, "");
        if (!StringUtils.hasText(text)) {
            return "Unknown Browser";
        }
        String normalized = text.toLowerCase();
        String os = "Unknown OS";
        if (normalized.contains("windows")) {
            os = "Windows";
        } else if (normalized.contains("mac os x") || normalized.contains("macintosh")) {
            os = "macOS";
        } else if (normalized.contains("android")) {
            os = "Android";
        } else if (normalized.contains("iphone") || normalized.contains("ipad") || normalized.contains("ios")) {
            os = "iOS";
        } else if (normalized.contains("linux")) {
            os = "Linux";
        }

        String browser = "Unknown";
        if (normalized.contains("edg/")) {
            browser = "Edge";
        } else if (normalized.contains("chrome/") && !normalized.contains("edg/")) {
            browser = "Chrome";
        } else if (normalized.contains("firefox/")) {
            browser = "Firefox";
        } else if (normalized.contains("safari/") && !normalized.contains("chrome/")) {
            browser = "Safari";
        }
        return browser + " on " + os;
    }

    /**
     * 规范化通行密钥名称
     *
     * @param credentialName 原始名称
     * @return 规范化后的名称
     */
    private String normalizeCredentialName(String credentialName) {
        if (!StringUtils.hasText(credentialName)) {
            return "";
        }
        String normalized = credentialName.trim();
        if (normalized.length() > 64) {
            return normalized.substring(0, 64);
        }
        return normalized;
    }

}
