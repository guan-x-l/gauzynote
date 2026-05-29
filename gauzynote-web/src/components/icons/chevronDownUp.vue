<script setup>


import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconChevronDownUp'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <polyline points="6 3 12 9 18 3"></polyline>
    <polyline points="6 21 12 15  18 21"></polyline>
  </icon-base>
</template>

<style scoped>

</style>