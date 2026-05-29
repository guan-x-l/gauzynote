<script setup>
import {computed} from "vue";
import {getFileNameWithoutExtension} from "@/utils/_utils/fileUtils.js";

const props = defineProps({
  asset: Object
})
const imageName = computed(()=>{
  if(props.asset && props.asset.imageName){
    return getFileNameWithoutExtension(props.asset.imageName)
  }
})
</script>

<template>
<div class="toolbar-container">
  <div>
  </div>
  <div class="toolbar-title hide-scrollbar">{{imageName}}</div>
  <div>
<!--    <m-button type="base" size="small" shape="square">
      <template #icon>
        <icon-more-vertical size="20"></icon-more-vertical>
      </template>
    </m-button>-->
  </div>
</div>
</template>

<style scoped>
.toolbar-container{
  width: 100%;
  height: 48px;
  position: sticky;
  top: 0;
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
}
</style>