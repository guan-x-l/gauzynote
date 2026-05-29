<script setup>
import {onMounted, shallowRef} from "vue";


const fileList = shallowRef([])
onMounted(() => {
  // 获取src/assets目录下的所有jpg文件
  const modules = import.meta.glob('@/components/icons/*.vue', { eager: true })
  const arr = []
  for (const path in modules) {
    if (modules[path].default.name) {
      arr.push({ name: modules[path].default.name, path, component: modules[path].default })
    }
  }
  fileList.value = arr
})
</script>

<template>
<div class="box">
  <div class="icon-item" v-for="item in fileList">
    <component :is="item.component"></component>
    <span>{{item.name}}</span>
  </div>
</div>
</template>

<style scoped>
.box{
  flex: 1;
  min-height: 0;
  overflow: hidden;
  overflow-y: auto;
  padding: 100px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
  font-size: 36px;
  .icon-item{
    flex-basis: 140px;
    display: flex;
    align-items: center;
    flex-direction: column;
    span{
      margin-top: 4px;
      font-size: 14px;
    }
  }
  svg{
    outline: 1px solid #c3c3c3;
  }
}
</style>