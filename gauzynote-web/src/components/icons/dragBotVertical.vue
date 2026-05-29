<script setup>
/**
 * @copyright https://feathericons.com/
 */
import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconDragBotVertical'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <circle cx="9" cy="5" r=".6"></circle>
    <circle cx="15" cy="5" r=".6"></circle>
    <circle cx="9" cy="12" r=".6"></circle>
    <circle cx="15" cy="12" r=".6"></circle>
    <circle cx="9" cy="19" r=".6"></circle>
    <circle cx="15" cy="19" r=".6"></circle>
  </icon-base>
</template>

<style scoped>

</style>