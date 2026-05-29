<script setup>
import {login} from "@/api/login.js";
import {
  getWebAuthnAuthOptions,
  verifyWebAuthnAuth,
} from "@/api/webauthn.js";
import {computed, onMounted, reactive, ref} from "vue";
import {setToken} from "@/utils/auth.js";
import {useRoute, useRouter} from "vue-router";
import localStorageUtil from "@/utils/lib/localStorageUtil.js";
import {userKeys} from "@/constants/cacheKeys.js";
import {useI18n} from "vue-i18n";
import {Message} from "@/components/feedback/Message/index.js";
import {
  credentialToJSON,
  isWebAuthnSupported,
  normalizeWebAuthnError,
  toAuthenticationOptions,
} from "@/utils/webauthn.js";

const router = useRouter()
const route = useRoute()
const {t} = useI18n()

const loginStatus = ref(false)
const passkeyLoading = ref(false)
const passkeySupported = ref(false)
const pendingRedirectPath = ref("/")
const form = reactive({
  username: '',
  password:''
})

/**
 * 处理账号密码登录
 */
async function handleSubmit() {
  if (loginStatus.value) return
  loginStatus.value = true
  const loginPayload = {...form}
  try {
    const res = await login(loginPayload)
    if (!(res.code === 200 && res.token)) {
      return
    }
    localStorageUtil.set(userKeys.username, loginPayload.username)
    setToken(res.token)
    pendingRedirectPath.value = route.query.redirect || "/"
    await router.replace({path: pendingRedirectPath.value})
  } finally {
    loginStatus.value = false
  }
}

/**
 * 初始化 WebAuthn 兼容性状态
 */
async function setupPasskeyCapability() {
  passkeySupported.value = isWebAuthnSupported()
}

/**
 * 发起 Passkey 登录流程
 */
async function handlePasskeyLogin() {
  if (passkeyLoading.value) return
  if (!passkeySupported.value) {
    Message.warning(t("login.passkeyNotSupported"))
    return
  }
  passkeyLoading.value = true
  try {
    await executePasskeyLoginOnce()
  } catch (error) {
    const normalized = normalizeWebAuthnError(error)
    Message.error(normalized.message || t("login.passkeyFailed"))
    throw error
  } finally {
    passkeyLoading.value = false
  }
}

/**
 * 执行完整 Passkey 登录
 */
async function executePasskeyLoginOnce() {
  const optionsRes = await getWebAuthnAuthOptions({username: form.username || undefined})
  const challengeId = optionsRes?.data?.challengeId
  const publicKeyOptions = optionsRes?.data?.publicKey
  if (!challengeId || !publicKeyOptions) {
    return
  }

  const assertion = await navigator.credentials.get({
    publicKey: toAuthenticationOptions(publicKeyOptions),
    mediation: "optional"
  })

  if (!assertion) {
    throw new Error(t("login.passkeyNoAssertion"))
  }

  const verifyRes = await verifyWebAuthnAuth(challengeId, credentialToJSON(assertion))
  if (verifyRes?.code !== 200 || !verifyRes?.token) {
    throw new Error(verifyRes?.msg || t("login.passkeyFailed"))
  }

  if (form.username) {
    localStorageUtil.set(userKeys.username, form.username)
  }
  setToken(verifyRes.token)
  Message.success(t("login.passkeySuccess"))
  await router.replace({path: route.query.redirect || "/"})
}

const dasharray = 314.2035217285156
const logoStrokeDashoffset = computed(()=>{
  return !(!!form.username + !!form.password)
    ? dasharray
    : dasharray - +(!!form.username + !!form.password + !!loginStatus.value) * (dasharray/3)
})

onMounted(() => {
  setupPasskeyCapability()
})
</script>

<template>
  <div class="login-container flex items-center justify-center flex-col">
    <div>
      <svg class="login-logo" :aria-label="t('text.textList', [t('SignIn'), t('text.progress')])" viewBox="-60 -60 120 120" fill="none" stroke-width="10"
           stroke-linecap="round">
        <g class="rotate-vert-center">
          <circle v-if="loginStatus" class="ping" cx="0" cy="0" r="80" stroke="rgb(var(--arcoblue-3))" stroke-width="10">
          </circle>
          <path
            d="M0 -50 A 50 50 0 1 1 0,50 A50 50 0 1 1 0,-50"
              stroke="rgb(var(--arcoblue-2))"></path>
          <path
            :class="{'pulsate-fwd': loginStatus}"
            d="M0 -50 A 50 50 0 1 1 0,50 A50 50 0 1 1 0,-50"
            :stroke-dasharray="dasharray"
            :stroke-dashoffset="logoStrokeDashoffset"
            stroke="rgb(var(--arcoblue-5))">
          </path>
        </g>
      </svg>
    </div>
    <h1 class="login-container-title">
      {{t('SignIn')}}
    </h1>
    <m-form class="login-form" v-model="form"  @submit="handleSubmit">
      <m-form-item prop="username" required hide-label>
        <m-input
            v-model="form.username"
            size="large"
            :placeholder="t('form.pleaseEnter', {field: t('user.username')})"
            :aria-label="t('form.pleaseEnter', {field: t('user.username')})"
        ></m-input>
      </m-form-item>
      <m-form-item prop="password" required hide-label>
        <m-input
            v-model="form.password"
            type="password"
            autocomplete="new-password"
            size="large"
            :placeholder="t('form.pleaseEnter', {field: t('user.password')})"
            :aria-label="t('form.pleaseEnter', {field: t('user.password')})"
        ></m-input>
      </m-form-item>
      <m-form-item hide-label>
        <m-button type="primary" size="large" html-type="submit" long :loading="loginStatus">{{t('SignIn')}}</m-button>
      </m-form-item>
      <m-form-item hide-label v-if="passkeySupported">
        <m-button type="outline" size="large" long :loading="passkeyLoading" @click="handlePasskeyLogin">
          {{ t('login.passkeyLogin') }}
        </m-button>
      </m-form-item>
    </m-form>
  </div>
</template>

<style lang="scss" scoped>
@keyframes ping {
  0% {
    transform: scale(0.6);
    opacity: 0.8;
  }
  80% {
    transform: scale(1.6);
    opacity: 0;
  }
  100% {
    transform: scale(2.6);
    opacity: 0;
  }
}

@keyframes pulsate-fwd {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes rotate-vert-center {
  0% {
    transform: rotateY(0)
  }
  100% {
    transform: rotateY(360deg)
  }
}

.login-container {
  min-height: 100vh;
  padding-bottom: 80px;
  background-color: var(--color-bg-1);
}

.login-logo {
  width: 60px;
  height: 60px;
  overflow: visible;
  filter: drop-shadow(10px 10px 20px rgb(var(--arcoblue-4)));
  
  path {
    transition: all .3s;
  }
}

.ping {
  animation: ping 1.2s ease-in-out infinite both;
}

.pulsate-fwd {
  animation: pulsate-fwd 1.2s ease-in-out infinite both;
}

.rotate-vert-center {
  animation: rotate-vert-center .5s cubic-bezier(.455, .03, .515, .955)
}

.login-container-title {
  background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: var(--spacing-14) 0;
  background-image: linear-gradient(120deg, rgb(var(--arcoblue-6)) 0%, rgb(var(--arcoblue-2)) 100%);
}

.login-form{
  width: 260px;
}
.login-form .m-btn[type="submit"]{
  margin-top: var(--spacing-4);
}
</style>
