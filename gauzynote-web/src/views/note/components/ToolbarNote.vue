<script setup>
import {ref} from "vue";
import Tooltip from "@/components/base/Tooltip/Tooltip.vue";
import {useI18n} from "vue-i18n";
const viewModeList = [
  {
    label: 'model.readonly',
    key: 'readonly'
  },
  {
    label: 'model.livePreview',
    key: 'livePreview'
  },
  {
    label: 'model.sourcecode',
    key: 'sourcecode'
  },
]
const props = defineProps({
  note: Object
})
const {t} = useI18n()
const emit = defineEmits(['viewModeClick'])
const viewMode = defineModel('viewMode')


const delta = ref(0)

function back() {
  delta.value++
}
function forward() {
  delta.value--
}
function handleViewModeClick() {
  const index = viewModeList.findIndex(item=>item.key === viewMode.value)
  let value = 'readonly'
  if (viewModeList[index+1]){
    value = viewModeList[index+1].key
  } else {
    value = viewModeList[0].key
  }
  emit('viewModeClick', value)
}
// 下载为md文件
function handleDownload() {
  const content = props.note?.content || ''
  const fileName = (props.note?.noteName || 'untitled') + '.md'
  const blob = new Blob([content], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
</script>

<template>
<div class="toolbar-container">
  <div>
<!--    <m-button type="base" size="small" shape="square" @click="back">
      <template #icon>
        <icon-arrow-left size="20"></icon-arrow-left>
      </template>
    </m-button>-->
<!--    <m-button type="base" size="small" shape="square" @click="forward">
      <template #icon>
        <icon-arrow-left size="20" rotate="180"></icon-arrow-left>
      </template>
    </m-button>-->
  </div>
  <div class="toolbar-title hide-scrollbar text-ellipsis">
    {{note && note.noteName}}
<!--    <input type="text" class="text-ellipsis" style="width: 200px" v-if="note" :value="note.noteName">-->
  </div>
  <div>
    <Tooltip :content="t(`model.${viewMode}`)" position="bottom" :mouseEnterDelay="300" mini>
      <m-button type="base" size="small" shape="square" @click="handleViewModeClick" :aria-label="t(`model.${viewMode}`)">
        <template #icon>
          <icon-book-open size="20" v-if="viewMode==='readonly'"/>
          <icon-edit size="20" v-if="viewMode==='livePreview'"/>
          <icon-code size="20" v-if="viewMode==='sourcecode'"/>
        </template>
      </m-button>
    </Tooltip>
    <dropdown position="bl">
      <m-button type="base" size="small" shape="square" style="margin-left: 4px">
        <template #icon>
          <icon-more-vertical size="20"></icon-more-vertical>
        </template>
      </m-button>
      <template #content>
        <dropdown-option @click="handleDownload">{{t('action.download')}}</dropdown-option>
      </template>
    </dropdown>
  </div>
</div>
</template>

<style scoped lang="scss">
.toolbar-container{
  width: 100%;
  height: 48px;
  //position: sticky;
  //top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  background-color: var(--color-bg-1);
  z-index: 1;
}
.toolbar-title{
  overflow: hidden;
  overflow-x: auto;
  white-space: nowrap;
  input{
    text-align: center;
    border: none;
    outline: none;
    color: var(--color-text-1);
    background-color: transparent;
  }
}
</style>
