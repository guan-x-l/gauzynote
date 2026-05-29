<script setup>
/**
 * @copyright https://feathericons.com/
 */
import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconAlertOctagon'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <polygon points="7.86 2 16.14 2 22 7.86 22 16.14 16.14 22 7.86 22 2 16.14 2 7.86 7.86 2"></polygon>
    <line x1="12" y1="8" x2="12" y2="12"></line>
    <line x1="12" y1="16" x2="12.01" y2="16"></line>
  </icon-base>
</template>

<style scoped>

</style>