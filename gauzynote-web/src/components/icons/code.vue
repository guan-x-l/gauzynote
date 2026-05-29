<script setup>
/**
 * @copyright https://feathericons.com/
 */


import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconCode'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <polyline points="16 18 22 12 16 6"></polyline>
    <polyline points="8 6 2 12 8 18"></polyline>
  </icon-base>
</template>

<style scoped>

</style>