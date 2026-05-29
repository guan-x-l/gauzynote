<script setup>
import {useRoute, useRouter} from "vue-router";
import {nextTick, ref, watch} from "vue";
import ToolbarImage from "@/views/image/components/ToolbarImage.vue";
import {useAppStore, useTabsStore} from "@/store/index.js";
import {extractRelativeImagePath, getImageById} from "@/biz/image.js";
import {domUtils} from "@/utils/index.js";
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
const image = ref({})
const imageName = ref('')
const imageSrc = ref('')



getImageById(id).then(res=>{
  if (!res) {
    removeTab(activeTab.value)
    router.replace('/newtab')
    return
  }
  image.value = res
  if (activeTab.value.title !== res.imageName) {
    updateTabTitleByRelatedId(id, NodeType.IMAGE.getCode(), res.imageName)
  }
  const relativePath = extractRelativeImagePath(res.imagePath, import.meta.env.VITE_IMAGE_BASE_DIR);
  imageSrc.value = import.meta.env.VITE_IMAGE_BASE_URL + '/image/' + relativePath
  nextTick(()=>{
    assetLoadingStatus.value = true
    console.log(assetLoadingStatus.value)
  })
})

function updateNoteNameOfActiveTab() {
  if (image.value && activeTab.value && activeTab.value?.title !== image.value.imageName) {
    image.value.imageName = activeTab.value.title
    imageName.value = activeTab.value.title
  }
}


watch(activeTab, updateNoteNameOfActiveTab, {deep: true})

function handleError(e) {
  domUtils(e.target).setStyle({width: '100px', height: '100px'}).setAttribute('alt', t('message.loadingError'))
}
</script>

<template>
  <div class="image asset-wrapper">
    <toolbar-image :asset="image"></toolbar-image>
    <img :src="imageSrc" v-if="imageSrc" :alt="imageSrc.imageName" loading="lazy" @error="handleError">
    <div>
      <div>文件名: {{image.imageName}}</div>
      <div>图片上传于: {{image.uploadTime}}</div>
      <div>图片大小: {{ formatFileSize(image.imageSize) }}</div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.image {
  text-align: center;
  
  img {
    display: inline-block;
    width: 70%;
    max-width: 100%;
  }
}
</style>
