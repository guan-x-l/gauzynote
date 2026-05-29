<script setup>
/**
 * @copyright https://feathericons.com/
 */

import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconTest'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
    <circle cx="12" cy="12" r="10" fill="none" stroke-linecap="round" stroke-dashoffset="65"
            stroke-dasharray="117">
    </circle>
  </icon-base>
</template>

<style scoped>

</style>