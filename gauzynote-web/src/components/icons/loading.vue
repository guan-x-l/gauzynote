<script setup>

import {iconBaseProps} from "@/components/icons/props.js";

defineOptions({name: 'IconLoading'})
const props = defineProps({...iconBaseProps})
</script>

<template>
  <icon-base v-bind="props" spin :iconName="$options.name">
    <circle class="circle" cx="12" cy="12" r="10" fill="none" stroke-linecap="round" stroke-dashoffset="20"
            stroke-dasharray="63">
    </circle>
  </icon-base>
</template>

<style scoped>
.svg-icon-spin .circle{
  animation: loading-dash 1.8s ease-in-out infinite;
}
@keyframes loading-dash {
  0% {
    stroke-dasharray: 1, 100;
    stroke-dashoffset: 0;
  }
  50% {
    stroke-dasharray: 50, 100;
    stroke-dashoffset: -20;
  }
  100% {
    stroke-dasharray: 50, 100;
    stroke-dashoffset: -60;
  }
}
</style>