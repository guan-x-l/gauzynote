<script setup>
import FileList from "@/layout/components/Sidebar/FileList.vue";
import FileAll from "@/layout/components/Sidebar/FileAll.vue";

defineOptions({name: 'Sidebar'})
const props = defineProps({
  currentMenu: String,
})
</script>

<template>
  <div class="layout__left-sidebar-container">
    <file-all v-if="currentMenu === 'fileAll'"></file-all>
    <file-list v-else></file-list>
  </div>
</template>

<style scoped lang="scss">
.layout__left-sidebar-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>
