<template>
  <transition name="fade-out">
    <div
      v-if="isAnyLoading"
      class="global-loading__mask"
      :style="{backgroundColor: currentConfig.background}"
    >
      <div class="global-loading__spinner">
        <icon-loading spin size="2em"/>
        <p v-if="currentConfig.text">{{ currentConfig.text }}</p>
      </div>
    </div>
  </transition>
</template>

<script setup>
import {computed, ref} from 'vue';
import {useLoading} from "@/components/feedback/GlobalLoading/useLoading.js";

// 获取加载状态管理
const { loadingInstances } = useLoading();

// 当前显示的配置（取最后一个添加的实例配置）
const currentConfig = computed(() => {
  if (loadingInstances.value.size === 0) return {};
  
  // 获取最后一个添加的实例
  const lastInstanceId = Array.from(loadingInstances.value.keys()).pop();
  return loadingInstances.value.get(lastInstanceId);
});
const isAnyLoading = ref(false)
// 判断是否有任何加载实例
// const isAnyLoading = computed(() => loadingInstances.value.size > 0);
</script>

<style scoped lang="scss">
@import '@/assets/style/animation.css';
.global-loading__mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.global-loading__spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: rgb(var(--primary-6));
}
</style>
