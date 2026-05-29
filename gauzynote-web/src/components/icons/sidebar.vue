<script setup>
/**
 * @copyright https://feathericons.com/
 */

import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconSidebar',})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
    <line x1="9" y1="3" x2="9" y2="21"></line>
  </icon-base>
</template>

<style scoped>

</style>