<script setup>
/**
 * @copyright https://feathericons.com/
 */

import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconCopy'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
  </icon-base>
</template>

<style scoped>

</style>