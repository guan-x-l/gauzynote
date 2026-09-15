<script setup>
import {computed, nextTick, onBeforeUnmount, ref, useTemplateRef, watch} from 'vue'
import {onBeforeRouteLeave, useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {getAssetNote, updateAssetNote} from '@/api/assetNote.js'
import {updateResourceNodeNameByNote} from '@/api/resourceNode.js'
import markdownIt from '@/utils/lib/markdownIt/markdownIt.js'
import {Message} from '@/components/feedback/Message/index.js'
import {useUserStore} from '@/store/index.js'
import {isCurrentSnapshot} from './mobile.js'

const route = useRoute()
const router = useRouter()
const {t} = useI18n()
const {isDefaultModifyPwd, updateUserinfo} = useUserStore()
const editorRef = useTemplateRef('editorRef')
const noteId = computed(() => String(route.params.id || ''))
const loading = ref(true)
const loadFailed = ref(false)
const noteName = ref('')
const content = ref('')
const savedNoteName = ref('')
const savedContent = ref('')
const mode = ref('read')
const status = ref('saved')
const saving = ref(false)
const leaveOpen = ref(false)
const authExpired = ref(false)
let loadVersion = 0
let autoSaveTimer
let allowRouteLeave = false

const currentSnapshot = computed(() => ({noteId: noteId.value, noteName: noteName.value, content: content.value}))
const isDirty = computed(() => noteName.value !== savedNoteName.value || content.value !== savedContent.value)
const renderedContent = computed(() => markdownIt.render(content.value || ''))
const statusText = computed(() => t(`mobile.saveStatus.${status.value}`))

function dirtyKey() {
    return `mobile-note-${noteId.value}`
}

function syncDirtyState() {
    if (isDirty.value) {
        window.isFormDirty.add(dirtyKey())
    } else {
        window.isFormDirty.delete(dirtyKey())
    }
}

function stopAutoSave() {
    clearTimeout(autoSaveTimer)
    autoSaveTimer = undefined
}

function scheduleSave() {
    stopAutoSave()
    if (!isDirty.value || authExpired.value) return
    status.value = 'unsaved'
    autoSaveTimer = setTimeout(() => save(), 1200)
}

function handleChange() {
    syncDirtyState()
    scheduleSave()
}

async function loadNote() {
    const version = ++loadVersion
    stopAutoSave()
    loading.value = true
    loadFailed.value = false
    authExpired.value = false
    leaveOpen.value = false
    try {
        const userResult = await updateUserinfo()
        if (version !== loadVersion || !userResult?.data) return
        if (isDefaultModifyPwd.value) {
            await router.replace({path: '/m', query: {redirect: route.fullPath}})
            return
        }
        const result = await getAssetNote(noteId.value)
        if (version !== loadVersion) return
        if (result?.code !== 200 || !result.data) {
            loadFailed.value = true
            return
        }
        noteName.value = result.data.noteName || ''
        content.value = result.data.content || ''
        savedNoteName.value = noteName.value
        savedContent.value = content.value
        status.value = 'saved'
        syncDirtyState()
    } catch (error) {
        if (version === loadVersion) loadFailed.value = true
    } finally {
        if (version === loadVersion) loading.value = false
    }
}

async function save() {
    stopAutoSave()
    if (saving.value || authExpired.value || !isDirty.value) return !isDirty.value
    const snapshot = {...currentSnapshot.value}
    const saveName = snapshot.noteName !== savedNoteName.value
    const saveContent = snapshot.content !== savedContent.value
    let nameSaved = !saveName
    let contentSaved = !saveContent
    saving.value = true
    status.value = 'saving'
    try {
        if (saveName) {
            const result = await updateResourceNodeNameByNote({noteId: snapshot.noteId, noteName: snapshot.noteName})
            nameSaved = result?.code === 200
            if (nameSaved) savedNoteName.value = snapshot.noteName
        }
        if (saveContent) {
            const result = await updateAssetNote({noteId: snapshot.noteId, content: snapshot.content})
            contentSaved = result?.code === 200
            if (contentSaved) savedContent.value = snapshot.content
        }
    } catch (error) {
        // The request layer has already shown the network or authentication error.
    } finally {
        saving.value = false
    }

    const snapshotStillCurrent = isCurrentSnapshot(snapshot, currentSnapshot.value)
    const saveSucceeded = nameSaved && contentSaved
    syncDirtyState()
    if (!saveSucceeded) {
        status.value = 'failed'
    } else if (isDirty.value) {
        status.value = 'unsaved'
        scheduleSave()
    } else {
        status.value = 'saved'
        if (snapshotStillCurrent) Message.success(t('message.saveSuccess', {msg: ''}))
    }
    return saveSucceeded && !isDirty.value
}

function insertMarkdown(prefix, suffix = '', placeholder = '') {
    const editor = editorRef.value
    if (!editor) return
    const start = editor.selectionStart
    const end = editor.selectionEnd
    const selected = content.value.slice(start, end) || placeholder
    content.value = `${content.value.slice(0, start)}${prefix}${selected}${suffix}${content.value.slice(end)}`
    handleChange()
    nextTick(() => {
        editor.focus()
        const selectionStart = start + prefix.length
        editor.setSelectionRange(selectionStart, selectionStart + selected.length)
    })
}

function normalizeName() {
    const trimmed = noteName.value.trim()
    noteName.value = trimmed || t('empty.untitled')
    handleChange()
}

async function attemptLeave() {
    if (!isDirty.value || authExpired.value) {
        return finishLeave()
    }
    const saved = await save()
    if (saved) return finishLeave()
    leaveOpen.value = true
}

function finishLeave() {
    allowRouteLeave = true
    syncDirtyState()
    if (route.query.from === 'home') {
        router.back()
    } else {
        router.replace('/m')
    }
}

function discardAndLeave() {
    savedNoteName.value = noteName.value
    savedContent.value = content.value
    leaveOpen.value = false
    finishLeave()
}

function continueEditing() {
    leaveOpen.value = false
}

function handleUnauthorized(event) {
    authExpired.value = true
    stopAutoSave()
    if (isDirty.value) {
        event.preventDefault()
        status.value = 'failed'
    }
}

function goToLogin() {
    allowRouteLeave = true
    savedNoteName.value = noteName.value
    savedContent.value = content.value
    syncDirtyState()
    router.replace({path: '/m/login', query: {redirect: route.fullPath}})
}

watch(noteId, loadNote, {immediate: true})

onBeforeRouteLeave(async () => {
    if (allowRouteLeave || !isDirty.value || authExpired.value) return true
    const saved = await save()
    if (saved) return true
    leaveOpen.value = true
    return false
})

window.addEventListener('gauzynote:unauthorized', handleUnauthorized)
onBeforeUnmount(() => {
    stopAutoSave()
    window.isFormDirty.delete(dirtyKey())
    window.removeEventListener('gauzynote:unauthorized', handleUnauthorized)
})
</script>

<template>
  <main class="mobile-note">
    <header class="mobile-note__header">
      <button class="mobile-icon-button" type="button" :aria-label="t('mobile.back')" @click="attemptLeave">‹</button>
      <span class="mobile-note__status" :class="`mobile-note__status--${status}`">{{ statusText }}</span>
      <button v-if="mode === 'read'" class="mobile-header-button" type="button" @click="mode = 'edit'">{{ t('mobile.edit') }}</button>
      <button v-else class="mobile-header-button" type="button" @click="mode = 'read'">{{ t('mobile.read') }}</button>
    </header>

    <div v-if="loading" class="mobile-note__state">{{ t('mobile.loading') }}</div>
    <div v-else-if="loadFailed" class="mobile-note__state">
      <p>{{ t('mobile.noteUnavailable') }}</p>
      <button class="mobile-button" type="button" @click="loadNote">{{ t('action.refresh') }}</button>
    </div>
    <template v-else>
      <section class="mobile-note__body">
        <template v-if="mode === 'read'">
          <h1>{{ noteName || t('empty.untitled') }}</h1>
          <article class="markdown-body mobile-markdown-body" v-html="renderedContent"></article>
        </template>
        <template v-else>
          <input v-model="noteName" class="mobile-note__title" :aria-label="t('mobile.noteTitle')" @blur="normalizeName" @input="handleChange" />
          <div class="mobile-note__toolbar" :aria-label="t('mobile.editorToolbar')">
            <button type="button" @click="insertMarkdown('# ', '', t('mobile.heading'))">H</button>
            <button type="button" @click="insertMarkdown('**', '**', t('mobile.bold'))"><strong>B</strong></button>
            <button type="button" @click="insertMarkdown('- ', '', t('mobile.listItem'))">•</button>
            <button type="button" @click="insertMarkdown('- [ ] ', '', t('mobile.todoItem'))">☑</button>
            <button type="button" @click="insertMarkdown('[', '](url)', t('mobile.linkText'))">↗</button>
            <button type="button" @click="insertMarkdown('`', '`', t('mobile.code'))">&lt;/&gt;</button>
            <button type="button" :disabled="saving || authExpired" @click="save">{{ t('mobile.save') }}</button>
          </div>
          <textarea ref="editorRef" v-model="content" class="mobile-note__editor" :aria-label="t('mobile.noteContent')" @input="handleChange"></textarea>
        </template>
      </section>
      <section v-if="authExpired" class="mobile-auth-warning">
        <strong>{{ t('mobile.sessionExpired') }}</strong>
        <p>{{ t('mobile.sessionExpiredTip') }}</p>
        <button class="mobile-button mobile-button--primary" type="button" @click="goToLogin">{{ t('mobile.goToLogin') }}</button>
      </section>
    </template>

    <div v-if="leaveOpen" class="mobile-dialog-mask" role="dialog" aria-modal="true" :aria-label="t('mobile.unsavedTitle')">
      <section class="mobile-dialog">
        <h2>{{ t('mobile.unsavedTitle') }}</h2>
        <p>{{ t('mobile.unsavedTip') }}</p>
        <button class="mobile-button mobile-button--primary" type="button" @click="save">{{ t('mobile.retrySave') }}</button>
        <button class="mobile-button" type="button" @click="continueEditing">{{ t('mobile.continueEditing') }}</button>
        <button class="mobile-button mobile-button--danger" type="button" @click="discardAndLeave">{{ t('mobile.discardChanges') }}</button>
      </section>
    </div>
  </main>
</template>

<style lang="scss">
@use "sass:meta";

.theme-light .mobile-markdown-body { @include meta.load-css("github-markdown-css/github-markdown-light.css"); }
.theme-dark .mobile-markdown-body { @include meta.load-css("github-markdown-css/github-markdown-dark.css"); }
</style>

<style scoped>
.mobile-note { display: flex; flex-direction: column; height: 100dvh; min-height: 0; overflow: hidden; color: var(--color-text-1); background: var(--color-bg-1); }
.mobile-note__header { position: sticky; top: 0; z-index: 2; display: grid; grid-template-columns: 44px 1fr auto; align-items: center; min-height: 52px; padding: 0 8px; background: var(--color-bg-1); border-bottom: 1px solid var(--color-border-1); }
.mobile-icon-button, .mobile-header-button { min-width: 36px; min-height: 36px; color: inherit; font: inherit; background: transparent; border: 0; }
.mobile-icon-button { font-size: 32px; line-height: 1; }
.mobile-header-button { color: rgb(var(--arcoblue-6)); }
.mobile-note__status { color: var(--color-text-3); font-size: 13px; text-align: center; }
.mobile-note__status--failed { color: rgb(var(--danger-6)); }
.mobile-note__status--saving { color: rgb(var(--arcoblue-6)); }
.mobile-note__body { flex: 1; min-height: 0; padding: 18px 16px calc(40px + env(safe-area-inset-bottom)); overflow-y: auto; overscroll-behavior: contain; -webkit-overflow-scrolling: touch; }
.mobile-note__body h1 { margin: 0 0 16px; overflow-wrap: anywhere; }
.mobile-note__title { box-sizing: border-box; width: 100%; margin-bottom: 12px; padding: 4px 0; color: inherit; font-size: 28px; font-weight: 700; line-height: 1.3; background: transparent; border: 0; border-bottom: 1px solid var(--color-border-2); outline: 0; }
.mobile-note__toolbar { display: flex; gap: 4px; margin-bottom: 8px; overflow-x: auto; }
.mobile-note__toolbar button { flex: 0 0 auto; min-width: 36px; min-height: 36px; padding: 5px 8px; color: inherit; font: inherit; background: var(--color-fill-1); border: 1px solid var(--color-border-2); border-radius: 6px; }
.mobile-note__toolbar button.active { color: #fff; background: rgb(var(--arcoblue-6)); }
.mobile-note__editor { box-sizing: border-box; width: 100%; min-height: calc(100dvh - 210px); padding: 10px; color: inherit; font: 15px/1.6 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; background: var(--color-bg-1); border: 1px solid var(--color-border-2); border-radius: 8px; outline: 0; resize: vertical; }
.mobile-markdown-body { max-width: 100%; padding: 0; color: inherit; background: transparent; overflow-wrap: anywhere; }
.mobile-markdown-body :deep(img) { max-width: 100%; height: auto; }
.mobile-markdown-body :deep(pre) { max-width: 100%; overflow-x: auto; }
.mobile-markdown-body :deep(table) { display: block; max-width: 100%; overflow-x: auto; }
.mobile-note__state { padding: 48px 16px; color: var(--color-text-3); text-align: center; }
.mobile-auth-warning { position: fixed; right: 16px; bottom: 16px; left: 16px; z-index: 3; padding: 16px; background: var(--color-bg-1); border: 1px solid rgb(var(--warning-6)); border-radius: 10px; box-shadow: 0 8px 28px rgba(0, 0, 0, .2); }
.mobile-auth-warning p { color: var(--color-text-2); }
.mobile-button { min-height: 42px; padding: 9px 14px; color: var(--color-text-1); font: inherit; background: var(--color-fill-1); border: 1px solid var(--color-border-2); border-radius: 8px; }
.mobile-button--primary { color: #fff; background: rgb(var(--arcoblue-6)); border-color: rgb(var(--arcoblue-6)); }
.mobile-button--danger { color: rgb(var(--danger-6)); }
.mobile-dialog-mask { position: fixed; inset: 0; z-index: 10; display: grid; padding: 24px; place-items: center; background: rgba(0, 0, 0, .45); }
.mobile-dialog { display: grid; width: min(100%, 400px); gap: 12px; padding: 20px; background: var(--color-bg-1); border-radius: 12px; }
.mobile-dialog h2, .mobile-dialog p { margin: 0; }
.mobile-dialog p { color: var(--color-text-2); }
</style>
