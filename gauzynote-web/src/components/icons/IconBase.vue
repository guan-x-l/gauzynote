<template>
  <svg
    xmlns="http://www.w3.org/2000/svg"
    :viewBox="viewBox"
    :class="svgClass"
    :style="innerStyle"
    :stroke-width="strokeWidth"
    :stroke-linecap="strokeLinecap"
    :stroke-linejoin="strokeLinejoin"
    :fill="fill"
  >
    <slot/>
  </svg>
</template>

<script setup>
import {computed} from 'vue'
import {iconBaseProps} from "@/components/icons/props.js";
import {camelToKebab} from "@/utils/index.js";

defineOptions({name: 'IconBase'})
const props = defineProps({
  ...iconBaseProps,
  viewBox: {
    type: String,
    default: '0 0 24 24'
  },
  iconName: {
    type: String,
    default: ''
  }
})
const innerStyle = computed(() => {
  const styles = {};
  if (props.size) {
    styles.fontSize = Number.isInteger(props.size) ? `${props.size}px` : props.size;
  }
  if (props.rotate) {
    styles.transform = `rotate(${props.rotate}deg)`;
  }
  return styles;
});

const svgClass = computed(() => [
  'svg-icon',
  {
    [`svg-icon-spin`]: props.spin,
  },
  camelToKebab(props.iconName)
])


</script>

<style scoped>

.svg-icon-spin{
  animation: rotate-center 1s infinite linear;
}

@keyframes rotate-center {
  100% {
    transform: rotate(1turn);
  }
}
</style>