<script setup>
/**
 * @copyright https://feathericons.com/
 */

import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconClose'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <line x1="18" y1="6" x2="6" y2="18"></line>
    <line x1="6" y1="6" x2="18" y2="18"></line>
  </icon-base>
</template>

<style scoped>

</style>