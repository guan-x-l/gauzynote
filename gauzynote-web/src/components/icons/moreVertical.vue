<script setup>
/**
 * @copyright https://feathericons.com/
 */


import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconMoreVertical'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <circle cx="12" cy="12" r="1"></circle>
    <circle cx="12" cy="5" r="1"></circle>
    <circle cx="12" cy="19" r="1"></circle>
  </icon-base>
</template>

<style scoped>

</style>