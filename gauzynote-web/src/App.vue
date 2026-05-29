<script setup>
import {useThemeStore} from "@/store/index.js";
import GlobalLoading from "@/components/feedback/GlobalLoading/GlobalLoading.vue";
import localStorageUtil from "@/utils/lib/localStorageUtil.js";
import {STORAGE_VERSION, storageVersion} from "@/constants/cacheKeys.js";
import {onMounted} from "vue";

localStorageUtil.versionUpdate(STORAGE_VERSION, storageVersion)

const {initTheme} = useThemeStore()
initTheme()

onMounted(() => {
  // 全局监听所有error事件
  document.addEventListener('error', function(e) {
    const target = e.target
    // 监听img标签
    if (target.tagName === 'IMG') {
      if (target.alt==='') {
        target.removeAttribute('alt')
      }
      target.classList.add('img-error')
      target.onerror = null // 彻底关闭error事件，防止循环
    }
  }, true) // 第三个参数必须为true：捕获阶段，确保所有img都能监听到（包括markdown动态渲染的）
})
</script>

<template>
  <GlobalLoading/>
  <router-view></router-view>
</template>

<style>

</style>
