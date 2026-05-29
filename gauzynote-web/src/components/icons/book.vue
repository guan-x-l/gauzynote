<script setup>
/**
 * @copyright https://feathericons.com/
 */

import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconBook'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" :iconName="$options.name">
      <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
      <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
  </icon-base>
</template>

<style scoped>

</style>