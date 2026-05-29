/**
 * 检测当前浏览器是否支持 WebAuthn
 * @returns {boolean}
 */
export function isWebAuthnSupported() {
  return !!(window.PublicKeyCredential && navigator.credentials);
}

/**
 * 检测平台认证器可用性
 * @returns {Promise<boolean>}
 */
export async function isPlatformAuthenticatorAvailable() {
  if (!isWebAuthnSupported() || !window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable) {
    return false;
  }
  try {
    return await window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
  } catch (e) {
    return false;
  }
}

/**
 * 将 base64url 转为 Uint8Array
 * @param {string} value base64url 字符串
 * @returns {Uint8Array}
 */
export function base64UrlToUint8Array(value) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padding = "=".repeat((4 - (normalized.length % 4)) % 4);
  const base64 = normalized + padding;
  const raw = atob(base64);
  const bytes = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i += 1) {
    bytes[i] = raw.charCodeAt(i);
  }
  return bytes;
}

/**
 * 将 ArrayBuffer 转为 base64url
 * @param {ArrayBuffer} value 二进制数据
 * @returns {string}
 */
export function arrayBufferToBase64Url(value) {
  const bytes = new Uint8Array(value);
  let binary = "";
  bytes.forEach((item) => {
    binary += String.fromCharCode(item);
  });
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/**
 * 将服务端注册参数转换为浏览器可识别格式
 * @param {object} payload 服务端参数
 * @returns {PublicKeyCredentialCreationOptions}
 */
export function toRegistrationOptions(payload) {
  const publicKey = {...payload};
  publicKey.challenge = base64UrlToUint8Array(publicKey.challenge);
  if (publicKey.user?.id) {
    publicKey.user = {...publicKey.user, id: base64UrlToUint8Array(publicKey.user.id)};
  }
  if (Array.isArray(publicKey.excludeCredentials)) {
    publicKey.excludeCredentials = publicKey.excludeCredentials.map((item) => ({
      ...item,
      id: base64UrlToUint8Array(item.id)
    }));
  }
  return publicKey;
}

/**
 * 将服务端认证参数转换为浏览器可识别格式
 * @param {object} payload 服务端参数
 * @returns {PublicKeyCredentialRequestOptions}
 */
export function toAuthenticationOptions(payload) {
  const publicKey = {...payload};
  publicKey.challenge = base64UrlToUint8Array(publicKey.challenge);
  if (Array.isArray(publicKey.allowCredentials)) {
    publicKey.allowCredentials = publicKey.allowCredentials.map((item) => ({
      ...item,
      id: base64UrlToUint8Array(item.id)
    }));
  }
  return publicKey;
}

/**
 * 将浏览器凭证对象序列化为后端可消费结构
 * @param {PublicKeyCredential} credential 浏览器凭证对象
 * @returns {object}
 */
export function credentialToJSON(credential) {
  if (!credential) return {};
  const response = credential.response || {};
  return {
    id: credential.id,
    rawId: credential.rawId ? arrayBufferToBase64Url(credential.rawId) : "",
    type: credential.type,
    response: {
      clientDataJSON: response.clientDataJSON ? arrayBufferToBase64Url(response.clientDataJSON) : "",
      attestationObject: response.attestationObject ? arrayBufferToBase64Url(response.attestationObject) : "",
      authenticatorData: response.authenticatorData ? arrayBufferToBase64Url(response.authenticatorData) : "",
      signature: response.signature ? arrayBufferToBase64Url(response.signature) : "",
      userHandle: response.userHandle ? arrayBufferToBase64Url(response.userHandle) : "",
      transports: typeof response.getTransports === "function" ? response.getTransports() : []
    },
    clientExtensionResults: typeof credential.getClientExtensionResults === "function"
      ? credential.getClientExtensionResults()
      : {}
  };
}

/**
 * 标准化 WebAuthn 错误信息
 * @param {unknown} error 原始错误
 * @returns {{code: string, message: string}}
 */
export function normalizeWebAuthnError(error) {
  const name = error?.name || "UnknownError";
  const errorMessage = error?.message || "WebAuthn 调用失败";
  if (name === "NotAllowedError") {
    return {code: name, message: "操作已取消或超时，请重试。"};
  }
  if (name === "InvalidStateError") {
    return {code: name, message: "当前设备已存在该账号的 Passkey，请直接使用 Passkey 登录。"};
  }
  if (name === "NotSupportedError") {
    return {code: name, message: "当前浏览器或设备不支持 Passkey。"};
  }
  if (name === "SecurityError") {
    return {code: name, message: "当前页面安全上下文异常，请确认使用 HTTPS 或 localhost。"};
  }
  return {code: name, message: errorMessage};
}
