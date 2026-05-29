<script setup>

import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconChevronUpDown'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <polyline points="6 9 12 3 18 9"></polyline>
    <polyline points="6 15 12 21 18 15"></polyline>
  </icon-base>
</template>

<style scoped>

</style>