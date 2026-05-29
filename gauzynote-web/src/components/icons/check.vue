<script setup>
/**
 * @copyright https://feathericons.com/
 */
import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconCheck'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <polyline points="20 6 9 17 4 12"></polyline>
  </icon-base>
</template>

<style scoped>

</style>