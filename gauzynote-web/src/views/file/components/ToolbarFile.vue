<script setup>
import {computed} from "vue";
import {getFileNameWithoutExtension} from "@/utils/_utils/fileUtils.js";

const props = defineProps({
  asset: Object
})
const displayName = computed(()=>{
  if(props.asset && props.asset.fileName){
    return getFileNameWithoutExtension(props.asset.fileName)
  }
})
</script>

<template>
<div class="toolbar-container">
  <div>
  </div>
  <div class="toolbar-title hide-scrollbar">{{displayName}}</div>
  <div>
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
