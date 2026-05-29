<script setup>
import Outline from "@/layout/components/SidebarRight/Outline.vue";

defineOptions({name: 'SidebarRight'})
</script>

<template>
  <section class="layout__right-sidebar flex flex-col">
    <div class="layout__right-sidebar-header flex-shrink-0 flex items-center"></div>
    <outline></outline>
  </section>
</template>

<style scoped lang="scss">
.layout__right-sidebar {
  position: relative;
  height: 100vh;
}

.layout__right-sidebar {
  background-color: var(--color-bg-1);
  transition: width .2s ease-out;
  overflow: hidden;
  user-select: none;
  height: 100vh;
  min-width: 180px;
}

.layout__right-sidebar .layout__right-sidebar-header {
  height: 40px;
  padding: 0 8px;
  border-bottom: 1px solid var(--color-fill-4);
  background-color: var(--color-bg-3);
  text-align: center;
}

</style>
