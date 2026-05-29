<script setup>
import {ref} from "vue";

const value1 = ref('1')
</script>

<template>
<div class="file-all__nav">
  <m-radio-group v-model="value1" type="button" size="large" class="flex flex-col" style="width: 100%">
    <m-radio value="1">全部文件</m-radio>
    <m-radio value="2">收藏</m-radio>
    <m-radio value="3">回收站</m-radio>
  </m-radio-group>
</div>
</template>

<style scoped>

</style>