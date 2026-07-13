<script setup>
import {useRoute, useRouter} from "vue-router";
import {nextTick, ref, watch} from "vue";
import ToolbarFile from "@/views/file/components/ToolbarFile.vue";
import ImageViewer from "@/views/file/components/ImageViewer.vue";
import TextViewer from "@/views/file/components/TextViewer.vue";
import {useAppStore, useTabsStore} from "@/store/index.js";
import {getFileById} from "@/biz/file.js";
import {NodeType} from "@/enum/index.js";
import {useI18n} from "vue-i18n";
import {formatFileSize} from "@/utils/index.js";


const {t} = useI18n()
const route = useRoute()
const router = useRouter()
const {assetLoadingStatus} = useAppStore()
const {activeTab, updateTabTitleByRelatedId, removeTab} = useTabsStore()
assetLoadingStatus.value = false
const id = route.params.id
const file = ref({})
const fileName = ref('')
const isPreviewable = ref(false)
const loading = ref(true)

const PREVIEWABLE_TYPES = ['image/', 'text/plain', 'application/json', 'application/pdf', 'video/', 'audio/']

function checkPreviewable(fileType) {
  if (!fileType) return false
  return PREVIEWABLE_TYPES.some(type => fileType.startsWith(type))
}


getFileById(id).then(res=>{
  if (!res) {
    removeTab(activeTab.value)
    router.replace('/newtab')
    return
  }
  file.value = res
  isPreviewable.value = checkPreviewable(res.fileType)
  if (activeTab.value.title !== res.fileName) {
    updateTabTitleByRelatedId(id, NodeType.FILE.getCode(), res.fileName)
  }
  loading.value = false
  nextTick(()=>{
    assetLoadingStatus.value = true
  })
})

function updateFileNameOfActiveTab() {
  if (file.value && activeTab.value && activeTab.value?.title !== file.value.fileName) {
    file.value.fileName = activeTab.value.title
    fileName.value = activeTab.value.title
  }
}


watch(activeTab, updateFileNameOfActiveTab, {deep: true})

function handleDownload() {
  const baseUrl = import.meta.env.VITE_FILE_BASE_URL
  const downloadUrl = baseUrl + '/asset/files/' + id + '/download'
  // 使用 <a> 标签触发下载，确保浏览器以附件形式保存、使用原始文件名
  const link = document.createElement('a')
  link.href = downloadUrl
  link.download = file.value.fileName || ''
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
</script>

<template>
  <div class="file asset-wrapper">
    <toolbar-file :asset="file"></toolbar-file>
    <!-- 可预览文件：使用类型组件展示 -->
    <template v-if="isPreviewable">
      <!-- 图片类型用 ImageViewer 预览 -->
      <image-viewer v-if="file.fileType && file.fileType.startsWith('image/')" :file="file" />
      <!-- 文本 / JSON 类型用 TextViewer 预览 -->
      <text-viewer v-else-if="file.fileType && (file.fileType === 'text/plain' || file.fileType === 'application/json')" :file="file" />
      <!-- 其他可预览类型暂显示基本信息 -->
      <div v-else class="file-preview-placeholder">
        <p>{{ t('file.previewNotSupported') }}</p>
      </div>
    </template>
    <!-- 不可预览文件：显示基本信息 + 下载 -->
    <div v-else-if="!loading" class="file-info-only">
      <div class="file-icon-large">
        <icon-file size="64"></icon-file>
      </div>
      <p>{{ t('file.noPreview') }}</p>
    </div>
    <!-- 基本信息（通用） -->
    <div class="file-meta">
      <div class="text-break">文件名: {{file.fileName}}</div>
      <div>上传于: {{file.uploadTime}}</div>
      <div>文件大小: {{ formatFileSize(file.fileSize) }}</div>
      <div>文件类型: {{file.fileType}}</div>
      <m-button type="primary" size="small" @click="handleDownload">{{ t('action.download') }}</m-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.file {
  text-align: center;

  .file-preview-placeholder, .file-info-only {
    padding: 40px 0;
    color: var(--color-text-3);
  }

  .file-meta {
    text-align: center;
    padding: 16px 24px;
    line-height: 2;
    color: var(--color-text-2);
    font-size: 14px;
  }
}
</style>
