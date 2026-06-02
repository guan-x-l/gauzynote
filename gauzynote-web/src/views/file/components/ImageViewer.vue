<script setup>
import {ref} from "vue";
import {buildFilePath, extractRelativeFilePath} from "@/biz/file.js";
import {domUtils} from "@/utils/index.js";
import {useI18n} from "vue-i18n";

const {t} = useI18n()

const props = defineProps({
  file: {
    type: Object,
    required: true
  }
})

const imageSrc = ref('')
const loading = ref(true)

imageSrc.value = buildFilePath(props.file.filePath)

function handleError(e) {
  domUtils(e.target).setStyle({width: '100px', height: '100px'}).setAttribute('alt', t('message.loadingError'))
}

function handleLoad() {
  loading.value = false
}
</script>

<template>
  <div class="image-viewer">
    <img
      v-if="imageSrc"
      :src="imageSrc"
      :alt="file.fileName"
      loading="lazy"
      @error="handleError"
      @load="handleLoad"
    >
  </div>
</template>

<style scoped lang="scss">
.image-viewer {
  text-align: center;
  padding: 16px 0;

  img {
    display: inline-block;
    width: 70%;
    max-width: 100%;
  }
}
</style>
