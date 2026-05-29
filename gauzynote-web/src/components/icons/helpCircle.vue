<script setup>
/**
 * @copyright https://feathericons.com/
 */


import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconHelpCircle'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <circle cx="12" cy="12" r="10"></circle>
    <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path>
    <line x1="12" y1="17" x2="12.01" y2="17"></line>
  </icon-base>
</template>

<style scoped>

</style>