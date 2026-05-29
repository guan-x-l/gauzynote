<script setup>
/**
 * @copyright https://feathericons.com/
 */


import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconUnlock'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
    <path d="M7 11V7a5 5 0 0 1 9.9-1"></path>
  </icon-base>
</template>

<style scoped>

</style>