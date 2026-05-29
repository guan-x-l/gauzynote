<script setup>
/**
 * @copyright https://feathericons.com/
 */
import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconCalendar2'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <path d="M10.5,14 L 12,13 L12,19"></path>
    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
    <line x1="16" y1="2" x2="16" y2="6"></line>
    <line x1="8" y1="2" x2="8" y2="6"></line>
    <line x1="3" y1="10" x2="21" y2="10"></line>
  </icon-base>
</template>

<style scoped>

</style>
