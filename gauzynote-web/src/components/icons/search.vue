<script setup>
/**
 * @copyright https://feathericons.com/
 */

import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconSearch'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <circle cx="11" cy="11" r="8"></circle>
    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
  </icon-base>
</template>

<style scoped>

</style>