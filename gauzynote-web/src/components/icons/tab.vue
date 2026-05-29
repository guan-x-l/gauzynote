<script setup>
/**
 * @copyright https://feathericons.com/
 */


import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconTab'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <rect x="2" y="5" width="20" height="15" rx="2" ry="2"></rect>
    <rect x="15" y="6" width="6" height="2" rx="0" ry="0"></rect>
  </icon-base>
</template>
<style scoped>

</style>