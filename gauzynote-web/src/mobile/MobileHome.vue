<script setup>
import {computed, nextTick, onBeforeUnmount, onMounted, ref, useTemplateRef} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {getResourceNode} from '@/api/resourceNode.js'
import {logout} from '@/api/login.js'
import {initializePassword} from '@/api/user.js'
import {removeToken} from '@/utils/auth.js'
import {useThemeStore, useUserStore} from '@/store/index.js'
import localStorageUtil from '@/utils/lib/localStorageUtil.js'
import {commonKeys} from '@/constants/cacheKeys.js'
import dayjs from '@/utils/lib/dayjs.js'
import {Message} from '@/components/feedback/Message/index.js'
import {resolveMobileRedirect} from './mobile.js'

const homeState = {
    panel: 'directory',
    folderIds: [],
    scrollTop: 0
}

const router = useRouter()
const route = useRoute()
const {t, locale} = useI18n()
const {userinfo, isDefaultModifyPwd, updateUserinfo} = useUserStore()
const {currentTheme, loadTheme} = useThemeStore()
const directoryRef = useTemplateRef('directoryRef')
const panel = ref(homeState.panel)
const nodes = ref([])
const folderIds = ref([...homeState.folderIds])
const loading = ref(true)
const loadFailed = ref(false)
const initialPasswordOpen = ref(false)
const passwordSubmitting = ref(false)
const passwordForm = ref({newPassword: '', confirmPassword: ''})

const activeFolderId = computed(() => folderIds.value.at(-1) ?? null)
const activeFolder = computed(() => nodes.value.find(node => node.nodeId === activeFolderId.value))
const directoryPath = computed(() => [
    t('mobile.directory'),
    ...folderIds.value
        .map(id => nodes.value.find(node => node.nodeId === id)?.nodeName)
        .filter(Boolean)
].join(' / '))
const visibleNodes = computed(() => nodes.value
    .filter(node => (node.nodeType === '1' || node.nodeType === '2') && (activeFolderId.value == null ? node.parentId == null : node.parentId === activeFolderId.value))
    .sort((left, right) => Number(left.sort || 0) - Number(right.sort || 0) || left.nodeName.localeCompare(right.nodeName)))

function persistHomeState() {
    homeState.panel = panel.value
    homeState.folderIds = [...folderIds.value]
    homeState.scrollTop = directoryRef.value?.scrollTop || 0
}

async function loadNodes() {
    loading.value = true
    loadFailed.value = false
    try {
        const result = await getResourceNode()
        if (result?.code !== 200) {
            throw new Error('Unable to load resource nodes')
        }
        nodes.value = result.data || []
        folderIds.value = folderIds.value.filter(id => nodes.value.some(node => node.nodeId === id && node.nodeType === '1'))
        await nextTick()
        if (directoryRef.value) directoryRef.value.scrollTop = homeState.scrollTop
    } catch (error) {
        loadFailed.value = true
    } finally {
        loading.value = false
    }
}

function enterNode(node) {
    if (node.nodeType === '1') {
        persistHomeState()
        folderIds.value.push(node.nodeId)
        nextTick(() => {
            if (directoryRef.value) directoryRef.value.scrollTop = 0
        })
        return
    }
    if (node.nodeType === '2' && node.relatedId) {
        persistHomeState()
        router.push({path: `/m/note/${node.relatedId}`, query: {from: 'home'}})
    }
}

function goUp() {
    if (!folderIds.value.length) return
    folderIds.value.pop()
    nextTick(() => {
        if (directoryRef.value) directoryRef.value.scrollTop = 0
    })
}

function changeLanguage(event) {
    locale.value = event.target.value
    dayjs.locale(locale.value === 'zh-CN' ? 'zh-cn' : 'en')
    localStorageUtil.set(commonKeys.LANGUAGE, locale.value)
}

async function signOut() {
    if (!window.confirm(t('message.confirmLogout'))) return
    try {
        await logout()
    } catch (error) {
        // The local session must still be removed when the server session has expired.
    } finally {
        removeToken()
        userinfo.value = {}
        nodes.value = []
        folderIds.value = []
        homeState.folderIds = []
        homeState.scrollTop = 0
        await router.replace('/m/login')
    }
}

async function submitInitialPassword() {
    if (passwordSubmitting.value || !passwordForm.value.newPassword || passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
        if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
            Message.error(t('mobile.passwordMismatch'))
        }
        return
    }
    passwordSubmitting.value = true
    try {
        const result = await initializePassword({...passwordForm.value})
        if (result?.code === 200) {
            isDefaultModifyPwd.value = false
            initialPasswordOpen.value = false
            passwordForm.value = {newPassword: '', confirmPassword: ''}
            Message.success(t('message.updateSuccessPwd'))
            const redirectPath = resolveMobileRedirect(route.query.redirect)
            if (redirectPath !== '/m') await router.replace(redirectPath)
        }
    } finally {
        passwordSubmitting.value = false
    }
}

onMounted(async () => {
    const result = await updateUserinfo()
    if (result?.data) {
        initialPasswordOpen.value = Boolean(isDefaultModifyPwd.value)
        if (!initialPasswordOpen.value && route.query.redirect) {
            await router.replace(resolveMobileRedirect(route.query.redirect))
            return
        }
    }
    await loadNodes()
})

onBeforeUnmount(persistHomeState)
</script>

<template>
  <main class="mobile-home">
    <header class="mobile-home__header">
      <strong>{{ panel === 'directory' ? t('mobile.directory') : t('mobile.mine') }}</strong>
      <button v-if="panel === 'directory'" class="mobile-icon-button" type="button" :aria-label="t('action.refresh')" @click="loadNodes">↻</button>
    </header>

    <section v-if="panel === 'directory'" ref="directoryRef" class="mobile-home__content" @scroll="persistHomeState">
      <button v-if="activeFolder" class="mobile-row mobile-row--back" type="button" @click="goUp">
        <icon-folder class="mobile-row__icon" size="20" aria-hidden="true" />
        <span class="mobile-row__name">{{ directoryPath }}</span>
        <icon-arrow-left size="18" aria-hidden="true" />
      </button>
      <p v-if="loading" class="mobile-muted">{{ t('mobile.loading') }}</p>
      <div v-else-if="loadFailed" class="mobile-state">
        <p>{{ t('mobile.loadFailed') }}</p>
        <button class="mobile-button" type="button" @click="loadNodes">{{ t('action.refresh') }}</button>
      </div>
      <p v-else-if="!visibleNodes.length" class="mobile-muted">{{ t('mobile.emptyFolder') }}</p>
      <button v-else v-for="node in visibleNodes" :key="node.nodeId" class="mobile-row" type="button" @click="enterNode(node)">
        <icon-folder v-if="node.nodeType === '1'" class="mobile-row__icon" size="20" aria-hidden="true" />
        <icon-file-text v-else-if="node.nodeType === '2'" class="mobile-row__icon" size="20" aria-hidden="true" />
        <icon-file v-else class="mobile-row__icon" size="20" aria-hidden="true" />
        <span class="mobile-row__name">{{ node.nodeName || t('empty.untitled') }}</span>
        <icon-chevron-right v-if="node.nodeType === '1'" size="18" aria-hidden="true" />
      </button>
    </section>

    <section v-else class="mobile-home__content mobile-mine">
      <div class="mobile-setting"><span>{{ t('setting.account') }}</span><strong>{{ userinfo.username || '—' }}</strong></div>
      <div class="mobile-setting"><span>{{ t('setting.nickname') }}</span><strong>{{ userinfo.nickname || '—' }}</strong></div>
      <label class="mobile-setting">
        <span>{{ t('setting.themeMode') }}</span>
        <select :value="currentTheme" @change="loadTheme($event.target.value)">
          <option value="system">{{ t('setting.themes.system') }}</option>
          <option value="light">{{ t('setting.themes.light') }}</option>
          <option value="dark">{{ t('setting.themes.dark') }}</option>
        </select>
      </label>
      <label class="mobile-setting">
        <span>{{ t('setting.displayLanguage') }}</span>
        <select :value="locale" @change="changeLanguage">
          <option value="zh-CN">简体中文</option>
          <option value="en-US">English</option>
        </select>
      </label>
      <button class="mobile-button mobile-button--danger" type="button" @click="signOut">{{ t('layout.nav.SignOut') }}</button>
    </section>

    <nav class="mobile-home__tabbar" :aria-label="t('mobile.navigation')">
      <button type="button" :class="{active: panel === 'directory'}" @click="panel = 'directory'"><icon-folder size="24" aria-hidden="true" /><span>{{ t('mobile.directory') }}</span></button>
      <button type="button" :class="{active: panel === 'mine'}" @click="panel = 'mine'"><icon-user size="24" aria-hidden="true" /><span>{{ t('mobile.mine') }}</span></button>
    </nav>

    <div v-if="initialPasswordOpen" class="mobile-dialog-mask" role="dialog" aria-modal="true" :aria-label="t('layout.nav.modifyPwd')">
      <form class="mobile-dialog" @submit.prevent="submitInitialPassword">
        <h2>{{ t('layout.nav.modifyPwd') }}</h2>
        <p>{{ t('mobile.initialPasswordTip') }}</p>
        <label>{{ t('updatePwd.newPassword') }}<input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" required /></label>
        <label>{{ t('updatePwd.confirmPassword') }}<input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" required /></label>
        <button class="mobile-button mobile-button--primary" type="submit" :disabled="passwordSubmitting">{{ t('okText') }}</button>
      </form>
    </div>
  </main>
</template>

<style scoped>
.mobile-home { min-height: 100dvh; padding-bottom: calc(64px + env(safe-area-inset-bottom)); color: var(--color-text-1); background: var(--color-bg-1); }
.mobile-home__header { position: sticky; top: 0; z-index: 2; display: flex; align-items: center; justify-content: space-between; min-height: 52px; padding: 0 16px; background: var(--color-bg-1); border-bottom: 1px solid var(--color-border-1); }
.mobile-home__content { min-height: 0; max-height: calc(100dvh - 116px - env(safe-area-inset-bottom)); overflow: auto; }
.mobile-row { display: flex; align-items: center; width: 100%; min-height: 56px; gap: 12px; padding: 10px 16px; color: inherit; font: inherit; text-align: left; background: transparent; border: 0; border-bottom: 1px solid var(--color-border-1); }
.mobile-row--back { color: rgb(var(--arcoblue-6)); }
.mobile-row__icon { color: var(--color-text-3); font-size: 20px; }
.mobile-row__name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mobile-muted, .mobile-state { padding: 32px 16px; color: var(--color-text-3); text-align: center; }
.mobile-mine { display: grid; align-content: start; gap: 1px; padding: 16px; background: var(--color-fill-1); }
.mobile-setting { display: flex; align-items: center; justify-content: space-between; min-height: 52px; gap: 16px; padding: 0 12px; background: var(--color-bg-1); }
.mobile-setting span { color: var(--color-text-2); }
select, input { box-sizing: border-box; min-height: 36px; padding: 6px 8px; color: inherit; font: inherit; background: var(--color-bg-1); border: 1px solid var(--color-border-2); border-radius: 6px; }
.mobile-home__tabbar { position: fixed; right: 0; bottom: 0; left: 0; z-index: 3; display: grid; grid-template-columns: repeat(2, 1fr); min-height: calc(64px + env(safe-area-inset-bottom)); padding-bottom: env(safe-area-inset-bottom); background: var(--color-bg-1); border-top: 1px solid var(--color-border-1); }
.mobile-home__tabbar button { display: grid; place-content: center; gap: 3px; color: var(--color-text-3); font: inherit; font-size: 20px; background: transparent; border: 0; }
.mobile-home__tabbar span { font-size: 12px; }
.mobile-home__tabbar button.active { color: rgb(var(--arcoblue-6)); }
.mobile-icon-button { width: 36px; height: 36px; color: inherit; font-size: 24px; background: transparent; border: 0; }
.mobile-button { min-height: 42px; padding: 9px 14px; color: var(--color-text-1); font: inherit; background: var(--color-fill-1); border: 1px solid var(--color-border-2); border-radius: 8px; }
.mobile-button--primary { color: #fff; background: rgb(var(--arcoblue-6)); border-color: rgb(var(--arcoblue-6)); }
.mobile-button--danger { width: 100%; margin-top: 16px; color: rgb(var(--danger-6)); }
.mobile-dialog-mask { position: fixed; inset: 0; z-index: 10; display: grid; padding: 24px; place-items: center; background: rgba(0, 0, 0, .45); }
.mobile-dialog { display: grid; width: min(100%, 400px); gap: 16px; padding: 20px; background: var(--color-bg-1); border-radius: 12px; }
.mobile-dialog h2, .mobile-dialog p { margin: 0; }
.mobile-dialog p { color: var(--color-text-2); }
.mobile-dialog label { display: grid; gap: 8px; }
.mobile-dialog input { width: 100%; }
</style>
