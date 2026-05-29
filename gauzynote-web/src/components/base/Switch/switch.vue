<script setup>
import {computed, ref} from "vue";

defineOptions({name: 'Switch'})
const modelValue = defineModel()
const emit = defineEmits(['change', 'beforeChange'])
const props = defineProps({
  /**
   * @zh 默认选中状态
   * @en Default selected state (uncontrolled state)
   */
  defaultChecked: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  /**
   * 选中时的值
   */
  checkedValue: {
    type: [Boolean, String, Number],
    default: true
  },
  /**
   * 未选中时的值
   */
  uncheckedValue: {
    type: [Boolean, String, Number],
    default: false
  },
  /**
   * 选中时的开关颜色
   */
  checkedColor: {
    type: [String]
  },
  /**
   * 未选中时的开关颜色
   */
  uncheckedColor: {
    type: [String]
  },
  /**
   * @zh 打开状态时的文案（`type='line'`和`size='small'`时不生效）
   * @en Copywriting when opened (not effective when `type='line'` and `size='small'`)
   */
  checkedText: {
    type: String,
  },
  /**
   * @zh 关闭状态时的文案（`type='line'`和`size='small'`时不生效）
   * @en Copywriting when closed (not effective when `type='line'` and `size='small'`)
   */
  uncheckedText: {
    type: String,
  },
  /**
   * 状态切换前的拦截钩子。返回 false 或 Promise reject 时终止切换。
   */
  beforeChange: {
    type: Function,
    default: undefined
  }
})
const _checked = ref(
    props.defaultChecked ? props.checkedValue : props.uncheckedValue
);
const beforeChangeLoading = ref(false);
const computedCheck = computed(
    () => (modelValue.value ?? _checked.value) === props.checkedValue
);
const switchLoading = computed(() => props.loading || beforeChangeLoading.value);
const handleChange = (checked, ev) => {
  _checked.value = checked ? props.checkedValue : props.uncheckedValue;
  modelValue.value = _checked.value
  emit('change', _checked.value, ev);
};

/**
 * 执行切换前拦截。返回 false 或 Promise reject 时终止切换。
 */
async function runBeforeChange(checkedValue, ev) {
  emit('beforeChange', checkedValue, ev)
  if (typeof props.beforeChange !== 'function') {
    return true;
  }
  beforeChangeLoading.value = true;
  try {
    const result = await props.beforeChange(checkedValue);
    return result !== false;
  } catch (error) {
    return false;
  } finally {
    beforeChangeLoading.value = false;
  }
}

/**
 * 处理开关切换行为，支持切换前拦截。
 */
async function switchChange(ev) {
  if (props.disabled || switchLoading.value) {
    return;
  }
  const checked = !computedCheck.value;
  const checkedValue = checked ? props.checkedValue : props.uncheckedValue;
  const canChange = await runBeforeChange(checkedValue, ev);
  if (!canChange) {
    return;
  }
  handleChange(checked, ev);
}

const prefixCls = 'm-switch'
const cls = computed(() => [
  prefixCls,
  {
    [`${prefixCls}--loading`]: switchLoading.value,
    [`${prefixCls}--checked`]: computedCheck.value,
    [`${prefixCls}--disabled`]: props.disabled,
  },
]);
</script>

<template>
  <button
      tabindex="0"
      :class="cls"
      :style="{backgroundColor: props.uncheckedColor || undefined}"
      :disabled="disabled || switchLoading"
       @keydown.enter="switchChange" @click="switchChange" @keydown.space.prevent="switchChange">
    <span class="m-switch__circle">
      <icon-loading v-if="switchLoading" :stroke-width="3" spin size="14"/>
    </span>
    <span class="m-switch__background" :style="{backgroundColor: props.checkedColor || undefined}"></span>
    <span :class="`${prefixCls}__inner`">
        <slot v-if="computedCheck" name="checked">{{ checkedText }}</slot>
        <slot v-else name="unchecked">{{ uncheckedText }}</slot>
    </span>
  </button>
</template>

<style scoped>
.m-switch {
  position: relative;
  min-width: 40px;
  height: 24px;
  line-height: 24px;
  padding: 0;
  border-radius: 12px;
  border: none;
  vertical-align: middle;
  background-color: var(--color-fill-4);
  cursor: pointer;
  overflow: hidden;
  outline: none;
  transition: all ease .25s;
}
.m-switch:focus-visible{
  box-shadow: 0 0 0 2px var(--color-border-4);
}

.m-switch .m-switch__circle {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 16px;
  height: 16px;
  border-radius: 16px;
  background-color: var(--color-bg-white);
  transition: all ease .25s, transform-origin ease .25s;
  z-index: 2;
  will-change: transform;
  transform-origin: left center;
}

.m-switch .m-switch__background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 24px;
  border-radius: 12px;
  transform: translateX(-101%);
  background-color: rgb(var(--primary-6));
  transition: transform ease .25s;
  will-change: transform;
}

.m-switch:active .m-switch__circle {
  transform: scaleX(1.25);
}

.m-switch--checked .m-switch__circle {
  left: calc(100% - 20px);
  transform-origin: right center;
}

.m-switch--checked .m-switch__background {
  transform: translateX(0);
}

.m-switch--checked:active .m-switch__circle {
  transform: scaleX(1.25);
}

.m-switch--loading {
  background-color: var(--color-fill-2);
  cursor: not-allowed;
}

.m-switch.m-switch--loading .m-switch__circle {
  display:inline-flex;
  align-items: center;
  justify-content: center;
  color: rgb(var(--primary-6));
  /*transform: translateX(0);*/
}

/*.m-switch.m-switch-checked.m-switch--loading .m-switch__circle {
  transform: translateX(100%);
}*/

.m-switch--disabled {
  background-color: var(--color-fill-2);
  cursor: not-allowed;
}

.m-switch--disabled .m-switch__circle {
  background-color: var(--color-bg-white);
}
.m-switch__inner{
  position: relative;
  top: -1px;
  margin: 0 8px 0 26px;
  color: var(--color-white);
  font-size: 12px;
  transition: all .25s ease;
}
.m-switch--checked .m-switch__inner{
  margin: 0 26px 0 8px;
}
</style>
