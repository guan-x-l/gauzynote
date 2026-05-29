import request from "@/utils/request/request.js";

/**
 * 获取 WebAuthn 认证参数
 * @param {{username?: string}} data 请求参数
 * @returns {Promise<any>}
 */
export function getWebAuthnAuthOptions(data = {}) {
  return request({
    url: "/webauthn/auth/options",
    method: "POST",
    data,
    custom: {
      requireToken: false,
      showLoading: false
    }
  });
}

/**
 * 提交 WebAuthn 认证结果
 * @param {string} challengeId 挑战ID
 * @param {object} credential WebAuthn 凭证
 * @returns {Promise<any>}
 */
export function verifyWebAuthnAuth(challengeId, credential) {
  return request({
    url: `/webauthn/auth/verify?challengeId=${encodeURIComponent(challengeId)}`,
    method: "POST",
    data: credential,
    custom: {
      requireToken: false,
      showLoading: false
    }
  });
}

/**
 * 获取 WebAuthn 注册参数
 * @returns {Promise<any>}
 */
export function   getWebAuthnRegisterOptions() {
  return request({
    url: "/webauthn/register/options",
    method: "POST"
  });
}

/**
 * 提交 WebAuthn 注册结果
 * @param {string} challengeId 挑战ID
 * @param {object} credential WebAuthn 凭证
 * @returns {Promise<any>}
 */
export function verifyWebAuthnRegister(challengeId, credential) {
  return request({
    url: `/webauthn/register/verify?challengeId=${encodeURIComponent(challengeId)}`,
    method: "POST",
    data: credential
  });
}

/**
 * 修改指定 Passkey 名称
 * @param {string} credentialId 凭证ID
 * @param {string} credentialName 凭证名称
 * @returns {Promise<any>}
 */
export function renameWebAuthnCredential(credentialId, credentialName) {
  return request({
    url: `/webauthn/credentials/${encodeURIComponent(credentialId)}/name`,
    method: "PUT",
    data: {credentialName}
  });
}

/**
 * 查询当前账号已绑定的 Passkey 列表
 * @returns {Promise<any>}
 */
export function listWebAuthnCredentials() {
  return request({
    url: "/webauthn/credentials",
    method: "GET"
  });
}

/**
 * 撤销指定 Passkey
 * @param {string} credentialId 凭证ID
 * @returns {Promise<any>}
 */
export function revokeWebAuthnCredential(credentialId) {
  return request({
    url: `/webauthn/credentials/${encodeURIComponent(credentialId)}`,
    method: "DELETE"
  });
}
