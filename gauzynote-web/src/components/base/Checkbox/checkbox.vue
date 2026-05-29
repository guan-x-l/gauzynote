<script setup>
import {computed, inject, ref, toRefs, unref, useTemplateRef, watch} from "vue";
import {isArray, isNull, isUndefined} from "@/utils/index.js";

defineOptions({name: 'Checkbox'})
const props = defineProps({
  /**
   * @zh 绑定值
   * @en Value
   * @vModel
   */
  modelValue: {
    type: [Boolean, Array],
    default: undefined,
  },
  /**
   * @zh 默认是否选中（非受控状态）
   * @en Whether checked by default (uncontrolled state)
   */
  defaultChecked: {
    type: Boolean,
    default: false,
  },
  /**
   * @zh 选项的 `value`
   * @en The `value` of the option
   */
  value: {
    type: [String, Number, Boolean],
    default: true,
  },
  /**
   * @zh 是否禁用
   * @en Whether to disable
   */
  disabled: {
    type: Boolean,
    default: false,
  },
  /**
   * @zh 是否为半选状态
   * @en Whether indeterminate
   */
  indeterminate: {
    type: Boolean,
    default: false,
  },
  /**
   * @private
   */
  uninjectGroupContext: {
    type: Boolean,
    default: false,
  },
})
const emit = defineEmits(['update:modelValue', 'change'])

const prefixCls = 'm-checkbox';
const {modelValue} = toRefs(props);
const checkboxGroupCtx = !props.uninjectGroupContext
    ? inject('CheckboxGroup', undefined)
    : undefined;
const inputRef = useTemplateRef('inputRef');
const _checked = ref(props.defaultChecked);

const isGroup = computed(() => checkboxGroupCtx?.name === 'CheckboxGroup');
const groupValue = computed(() => {
  const value = unref(checkboxGroupCtx?.value);
  return isArray(value) ? value : [];
});
const groupDisabled = computed(() => Boolean(unref(checkboxGroupCtx?.disabled)));
const groupMax = computed(() => unref(checkboxGroupCtx?.max));

const computedChecked = computed(() => {
  if (isGroup.value) {
    return groupValue.value.includes(props.value);
  }
  if (!isUndefined(props.modelValue)) {
    if (isArray(props.modelValue)) {
      return props.modelValue.includes(props.value);
    }
    return Boolean(props.modelValue);
  }
  return _checked.value;
});

const mergedDisabled = computed(() => {
  if (props.disabled || groupDisabled.value) {
    return true;
  }
  if (isGroup.value && groupMax.value && !computedChecked.value) {
    return groupValue.value.length >= groupMax.value;
  }
  return false;
});

watch(modelValue, (value) => {
  if (isUndefined(value) || isNull(value)) {
    _checked.value = false;
  }
});

watch(computedChecked, (curValue, preValue) => {
  if (curValue !== preValue) {
    _checked.value = curValue;
  }
});

watch([computedChecked, () => props.indeterminate], ([checked, indeterminate]) => {
  if (inputRef.value) {
    inputRef.value.checked = checked;
    inputRef.value.indeterminate = indeterminate && !checked;
  }
}, {immediate: true});

const handleChange = (ev) => {
  if (mergedDisabled.value) return
  const checked = Boolean(ev?.target?.checked);
  _checked.value = checked;
  if (isGroup.value) {
    checkboxGroupCtx?.handleChange(props.value, checked, ev);
  } else {
    if (isArray(props.modelValue)) {
      const nextValue = [...props.modelValue];
      const index = nextValue.indexOf(props.value);
      if (checked && index < 0) {
        nextValue.push(props.value);
      }
      if (!checked && index > -1) {
        nextValue.splice(index, 1);
      }
      emit('update:modelValue', nextValue);
      emit('change', nextValue, ev);
      return;
    }
    emit('update:modelValue', checked);
    emit('change', checked, ev);
  }
};

const cls = computed(() => [
  prefixCls,
  {
    [`${prefixCls}--checked`]: computedChecked.value,
    [`${prefixCls}--disabled`]: mergedDisabled.value,
    [`${prefixCls}--indeterminate`]: props.indeterminate && !computedChecked.value,
  },
]);
</script>

<template>
  <label :class="cls">
    <input
        ref="inputRef"
        type="checkbox"
        :checked="computedChecked"
        :value="value"
        :class="`${prefixCls}__target`"
        :disabled="mergedDisabled"
        @change="handleChange"
    />
    <span :class="`${prefixCls}__icon`">
      <icon-check :class="`${prefixCls}__icon-check`" :size="10" :stroke-width="3"/>
    </span>
    <span :class="`${prefixCls}__label`">
      <slot/>
    </span>
  </label>
</template>

<style scoped lang="scss">
.m-checkbox {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding-left: 6px;
  font-size: 14px;
  line-height: unset;
  cursor: pointer;
}

.m-checkbox > input[type=checkbox] {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  opacity: 0;
}

.m-checkbox__icon {
  position: relative;
  width: 14px;
  height: 14px;
  border-radius: var(--border-radius-small);
  border: 2px solid var(--color-neutral-3);
  background-color: var(--color-white);
  transition: all .2s ease;
}

.m-checkbox:hover .m-checkbox__icon {
  border-color: rgb(var(--primary-6));
  box-shadow: 0 0 0 4px var(--color-fill-2);
}

.m-checkbox__icon-check {
  position: absolute;
  left: 0;
  top: 0;
  color: var(--color-white);
  opacity: 0;
  transform: scale(0.6);
  transition: all .15s ease;
}

.m-checkbox--checked .m-checkbox__icon, .m-checkbox--checked:hover .m-checkbox__icon {
  background-color: rgb(var(--primary-6));
  border-color: rgb(var(--primary-6));
  box-shadow: 0 0 8px var(--color-primary-light-2), 0 0 67px var(--color-primary-light-1);
}

.m-checkbox--checked .m-checkbox__icon-check {
  opacity: 1;
  transform: scale(1);
}

.m-checkbox--indeterminate .m-checkbox__icon {
  background-color: rgb(var(--primary-6));
  border-color: rgb(var(--primary-6));
}

.m-checkbox--indeterminate .m-checkbox__icon::after {
  content: '';
  position: absolute;
  left: 2px;
  top: 4px;
  width: 6px;
  height: 2px;
  background-color: var(--color-white);
}

.m-checkbox__target:focus-visible + .m-checkbox__icon {
  box-shadow: 0 0 0 4px var(--color-fill-2);
}

.m-checkbox__label {
  margin-left: 8px;
  color: var(--color-text-1);
}

.m-checkbox--disabled {
  cursor: not-allowed;
}

.m-checkbox--disabled .m-checkbox__icon {
  border-color: var(--color-neutral-4);
  background-color: var(--color-fill-2);
  box-shadow: none;
}

.m-checkbox--disabled.m-checkbox:hover .m-checkbox__icon {
  box-shadow: none;
  border-color: var(--color-neutral-4);
}

.m-checkbox--disabled .m-checkbox__label {
  color: var(--color-text-4);
}

.m-checkbox--disabled.m-checkbox--checked .m-checkbox__icon,
.m-checkbox--disabled.m-checkbox--checked:hover .m-checkbox__icon,
.m-checkbox--disabled.m-checkbox--indeterminate .m-checkbox__icon {
  border-color: var(--color-primary-light-4);
  background-color: var(--color-primary-light-4);
}
</style>
