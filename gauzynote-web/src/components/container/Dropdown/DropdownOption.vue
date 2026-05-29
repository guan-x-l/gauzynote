<script setup>
import {computed, inject, useTemplateRef} from "vue";
import {isFunction} from "@/utils/index.js";

const props = defineProps({
  value: [String, Number, Object],
  label: [String, Function],
  disabled: Boolean,
  // 无交互
  noBehavior: Boolean,
  /**
   * @type {import('vue').PropType.<"center" | "end">}
   */
  align: {
    type: String
  }
})
const emit = defineEmits(['click'])

const liRef = useTemplateRef('liRef')

const computedValue = computed(()=>{
  return props.value ? props.value : liRef.value ? liRef.value.textContent : null
})

const dropdownCtx = inject('mDropdown', undefined)
function handleClick(event) {
  if (props.disabled || props.noBehavior) {
    return
  }
  emit("click", event);
  dropdownCtx?.onOptionClick && dropdownCtx.onOptionClick(computedValue.value, event);
}
const prefixCls = 'm-dropdown__option'
const cls = computed(() => [
  prefixCls,
  {
    [`${prefixCls}--${props.align}`]: props.align,
    [`${prefixCls}--disabled`]: props.disabled,
    [`${prefixCls}-no-interaction`]: props.noBehavior,
  },
]);
</script>

<template>
  <li :class="cls"
      ref="liRef"
      @click.stop="handleClick">
    <div v-if="$slots.icon" class="m-dropdown__option__prefix">
      <slot name="icon"></slot>
    </div>
    <slot>{{isFunction(label) ? label() : label}}</slot>
  </li>
</template>

<style lang="scss" scoped>
.m-dropdown__option{
  display: flex;
  align-items: center;
  padding: 4px 16px;
  list-style-type: none;
  user-select: none;
}
.m-dropdown__option--center{
  justify-content: center;
}
.m-dropdown__option--end{
  justify-content: flex-end;
}
.m-dropdown__option:not(.m-dropdown__option--disabled,.m-dropdown__option-no-interaction):hover{
  background-color: var(--color-fill-3);
  border-radius: 4px;
}
.m-dropdown__option--disabled {
  color: var(--color-text-4);
  background-color: transparent;
  cursor: not-allowed;
}
.m-dropdown__option__prefix{
  display: inline-flex;
  margin-right: 8px;
}
</style>