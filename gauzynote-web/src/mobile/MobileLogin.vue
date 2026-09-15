<script setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {login} from '@/api/login.js'
import {getWebAuthnAuthOptions, verifyWebAuthnAuth} from '@/api/webauthn.js'
import {setToken} from '@/utils/auth.js'
import localStorageUtil from '@/utils/lib/localStorageUtil.js'
import {userKeys} from '@/constants/cacheKeys.js'
import {credentialToJSON, isWebAuthnSupported, normalizeWebAuthnError, toAuthenticationOptions} from '@/utils/webauthn.js'
import {Message} from '@/components/feedback/Message/index.js'
import {useUserStore} from '@/store/index.js'
import {resolveMobileRedirect} from './mobile.js'

const router = useRouter()
const route = useRoute()
const {t} = useI18n()
let form = reactive({username: '', password: ''})
const submitting = ref(false)
const passkeyLoading = ref(false)
const passkeySupported = ref(false)
const redirectPath = computed(() => resolveMobileRedirect(route.query.redirect))
const {isDefaultModifyPwd, updateUserinfo} = useUserStore()

async function completeLogin(token, username) {
    if (username) {
        localStorageUtil.set(userKeys.username, username)
    }
    setToken(token)
    const infoResult = await updateUserinfo()
    if (!infoResult?.data) return
    if (isDefaultModifyPwd.value) {
        await router.replace({path: '/m', query: {redirect: redirectPath.value}})
        return
    }
    await router.replace(redirectPath.value)
}

async function submitPassword() {
    if (submitting.value) return
    submitting.value = true
    try {
        const result = await login({...form})
        if (result?.code === 200 && result.token) {
            await completeLogin(result.token, form.username)
        }
    } finally {
        submitting.value = false
    }
}

async function submitPasskey() {
    if (passkeyLoading.value) return
    if (!passkeySupported.value) {
        Message.warning(t('mobile.passkeyNotSupported'))
        return
    }
    passkeyLoading.value = true
    try {
        const optionsResult = await getWebAuthnAuthOptions({username: form.username || undefined})
        const challengeId = optionsResult?.data?.challengeId
        const publicKey = optionsResult?.data?.publicKey
        if (!challengeId || !publicKey) return

        const assertion = await navigator.credentials.get({
            publicKey: toAuthenticationOptions(publicKey),
            mediation: 'optional'
        })
        if (!assertion) {
            throw new Error(t('login.passkeyNoAssertion'))
        }

        const result = await verifyWebAuthnAuth(challengeId, credentialToJSON(assertion))
        if (result?.code !== 200 || !result?.token) {
            throw new Error(result?.msg || t('login.passkeyFailed'))
        }
        await completeLogin(result.token, form.username)
        Message.success(t('login.passkeySuccess'))
    } catch (error) {
        Message.error(normalizeWebAuthnError(error).message || t('login.passkeyFailed'))
    } finally {
        passkeyLoading.value = false
    }
}

onMounted(() => {
    passkeySupported.value = isWebAuthnSupported()
})
</script>

<template>
  <main class="mobile-login">
    <form class="mobile-login__form" @submit.prevent="submitPassword">
      <p class="mobile-login__eyebrow">Gauzy Note</p>
      <h1>{{ t('SignIn') }}</h1>
      <label>
        <span>{{ t('user.username') }}</span>
        <input v-model="form.username" autocomplete="username" required />
      </label>
      <label>
        <span>{{ t('user.password') }}</span>
        <input v-model="form.password" type="password" autocomplete="current-password" required />
      </label>
      <button class="mobile-button mobile-button--primary" type="submit" :disabled="submitting">
        {{ submitting ? t('mobile.signingIn') : t('SignIn') }}
      </button>
      <button v-if="passkeySupported" class="mobile-button" type="button" :disabled="passkeyLoading" @click="submitPasskey">
        {{ passkeyLoading ? t('mobile.signingIn') : t('login.passkeyLogin') }}
      </button>
    </form>
  </main>
</template>

<style scoped>
.mobile-login {
  display: grid;
  min-height: 100dvh;
  padding: 24px;
  place-items: center;
  color: var(--color-text-1);
  background: var(--color-bg-1);
}

.mobile-login__form {
  display: grid;
  width: min(100%, 380px);
  gap: 16px;
}

.mobile-login__eyebrow {
  margin: 0;
  color: rgb(var(--arcoblue-6));
  font-weight: 600;
  letter-spacing: .08em;
  text-transform: uppercase;
}

h1 { margin: 0 0 8px; }

label { display: grid; gap: 8px; font-size: 14px; }

input {
  box-sizing: border-box;
  width: 100%;
  min-height: 44px;
  padding: 10px 12px;
  color: inherit;
  font: inherit;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
}

.mobile-button {
  min-height: 44px;
  padding: 10px 14px;
  color: var(--color-text-1);
  font: inherit;
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
}

.mobile-button--primary {
  color: #fff;
  background: rgb(var(--arcoblue-6));
  border-color: rgb(var(--arcoblue-6));
}

button:disabled { opacity: .65; }
</style>
