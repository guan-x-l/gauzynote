<script setup>
import {ref, onMounted, computed} from "vue";
import {useI18n} from "vue-i18n";

const {t} = useI18n()

const props = defineProps({
  file: {
    type: Object,
    required: true
  }
})

const content = ref('')
const loading = ref(true)
const error = ref('')

const isJson = computed(() => props.file.fileType === 'application/json')

async function fetchContent() {
  loading.value = true
  error.value = ''
  try {
    const baseUrl = import.meta.env.VITE_FILE_BASE_URL
    const url = baseUrl + '/asset/files/' + props.file.fileId + '/download'
    const response = await fetch(url)
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    const text = await response.text()
    if (isJson.value) {
      try {
        const parsed = JSON.parse(text)
        content.value = JSON.stringify(parsed, null, 2)
      } catch {
        // JSON 解析失败，直接显示原始文本
        content.value = text
      }
    } else {
      content.value = text
    }
  } catch (e) {
    error.value = e.message || t('message.loadingError')
  } finally {
    loading.value = false
  }
}

onMounted(fetchContent)
</script>

<template>
  <div class="text-viewer">
    <div v-if="loading" class="text-viewer-loading">
      <span>{{ t('file.loading') }}</span>
    </div>
    <div v-else-if="error" class="text-viewer-error">
      <span>{{ error }}</span>
    </div>
    <pre v-else class="text-viewer-content" :class="{ 'text-viewer-json': isJson }">{{ content }}</pre>
  </div>
</template>

<style scoped lang="scss">
.text-viewer {
  padding: 16px;

  &-loading, &-error {
    text-align: center;
    padding: 40px 0;
    color: var(--color-text-3);
  }

  &-error {
    color: var(--color-error, #e74c3c);
  }

  &-content {
    margin: 0;
    padding: 16px;
    background: var(--color-fill-2);
    border-radius: 4px;
    font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, monospace;
    font-size: 14px;
    line-height: 1.6;
    color: var(--color-text-2);
    white-space: pre-wrap;
    word-break: break-all;
    overflow-x: auto;
    max-height: 70vh;
    overflow-y: auto;
    text-align: left;
  }
}
</style>
