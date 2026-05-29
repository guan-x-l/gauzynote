<script setup>
import {computed, inject, nextTick, ref, toRef, toRefs, useTemplateRef, watch} from "vue";
import {isNull, isUndefined} from "@/utils/index.js";

defineOptions({name: 'Radio'})
const props = defineProps({
  /**
   * @zh 绑定值
   * @en Value
   * @vModel
   */
  modelValue: {
    type: [String, Number, Boolean],
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
   * @zh 单选的类型
   * @en Radio type
   * @values 'radio', 'button'
   */
  type: {
    type: String,
    default: 'radio',
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
   * @private
   */
  uninjectGroupContext: {
    type: Boolean,
    default: false,
  },
})
const emit = defineEmits(['update:modelValue', 'change'])


const prefixCls = 'm-radio';
const {modelValue} = toRefs(props);
const radioGroupCtx = !props.uninjectGroupContext
    ? inject('RadioGroup', undefined)
    : undefined;
// const { mergedDisabled: _mergedDisabled, eventHandlers } = useFormItem({
//   disabled: toRef(props, 'disabled'),
// });

const inputRef = useTemplateRef('inputRef');
const _checked = ref(props.defaultChecked);

const isGroup = computed(() => radioGroupCtx?.name === 'RadioGroup');
const mergedType = computed(() => radioGroupCtx?.type ?? props.type);
const mergedDisabled = computed(() => radioGroupCtx?.disabled || props.disabled);

const computedChecked = computed(() => {
  if (isGroup.value) {
    return radioGroupCtx?.value === (props.value ?? true);
  }

  if (!isUndefined(props.modelValue)) {
    return props.modelValue === (props.value ?? true);
  }
  return _checked.value;
});

watch(modelValue, (value) => {
  if (isUndefined(value) || isNull(value)) {
    _checked.value = false;
  }
});

watch(computedChecked, (curValue, preValue) => {
  if (curValue !== preValue) {
    _checked.value = curValue;
    if (inputRef.value) {
      inputRef.value.checked = curValue;
    }
  }
});


const handleFocus = (ev) => {
  // eventHandlers.value?.onFocus?.(ev);
};

const handleBlur = (ev) => {
  // eventHandlers.value?.onBlur?.(ev);
};

const handleClick = (ev) => {
  ev.stopPropagation();
};

const handleChange = (e) => {
  if (mergedDisabled.value) return
  _checked.value = true;
  if (isGroup.value) {
    radioGroupCtx?.handleChange(props.value ?? true, e);
  } else {
    emit('update:modelValue', props.value ?? true);
    emit('change', props.value ?? true, e);
    // eventHandlers.value?.onChange?.(e);
  }

  nextTick(() => {
    if (
        inputRef.value &&
        inputRef.value.checked !== computedChecked.value
    ) {
      inputRef.value.checked = computedChecked.value;
    }
  });
};

const cls = computed(() => [
  `${mergedType.value === 'button' ? `${prefixCls}-button` : prefixCls}`,
  {
    [`${prefixCls}--checked`]: computedChecked.value,
    [`${prefixCls}--disabled`]: mergedDisabled.value,
  },
]);
</script>

<template>
  <label :class="cls" @click="handleChange">
    <input
        ref="inputRef"
        type="radio"
        :checked="computedChecked"
        :value="value"
        :class="`${prefixCls}__target`"
        :disabled="mergedDisabled"
        @onClick="handleClick"
        @onChange="handleChange"
        @onFocus="handleFocus"
        @onBlur="handleBlur"
    />
    <template v-if="mergedType === 'radio'">
      <slot name="radio" :checked="computedChecked" :disabled="mergedDisabled">
        <span
            :class="[
              `${prefixCls}__icon-hover`,
              {
                [`${prefixCls}--disabled`]: mergedDisabled || computedChecked,
              },
            ]"
        >
          <span class="m-radio__icon-mask"></span>
          <span class="m-radio__icon-effect"></span>
        </span>
        <span :class="`${prefixCls}__label`">
           <slot/>
          </span>
      </slot>
    </template>
    <span v-else :class="`${prefixCls}-button-content`" @click.stop>
      <slot/>
    </span>
  </label>
</template>

<style scoped>
.m-radio {
  position: relative;
  display: inline-flex;
  align-items: center;
  font-size: 14px;
  line-height: unset;
  cursor: pointer;
  padding-left: 6px;
}

.m-radio > input[type=radio], .m-radio-button > input[type=radio] {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  opacity: 0;
}

.m-radio__label {
  margin-left: 8px;
  color: var(--color-text-1);
}

.m-radio__icon-hover {
  position: relative;
  display: inline-block;
  cursor: pointer;
  width: 1em;
  height: 1em;
  line-height: 1em;
  border-radius: var(--border-radius-circle);
}

.m-radio__icon-mask {
  position: absolute;
  width: 100%;
  height: 100%;
  left: 0;
  top: 0;
  border-radius: inherit;
  background-color: var(--color-white);
  border: 5px solid rgb(var(--primary-6));
  box-shadow: 0 0 8px var(--color-primary-light-2), 0 0 67px var(--color-primary-light-1);
  z-index: 1;
  transform: scale(0);
  opacity: 0;
  transition: transform .3s cubic-bezier(.25, .1, .53, 1.3), opacity .15s ease;
}

.m-radio__icon-effect {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: inherit;
  left: 0;
  top: 0;
  border: 2px solid var(--color-neutral-3);
  transition: all .2s ease;
  will-change: border;
}

.m-radio:hover .m-radio__icon-effect {
  box-shadow: 0 0 0 4px var(--color-fill-2);
}

.m-radio--checked.m-radio:hover .m-radio__icon-effect {
  box-shadow: none;
}

.m-radio--checked .m-radio__icon-mask {
  transform: scale(1);
  opacity: 1;
}

.m-radio__target:focus-visible + .m-radio__icon-hover .m-radio__icon-effect {
  box-shadow: 0 0 0 4px var(--color-fill-2);
}


.m-radio-button{
  position: relative;
  display: inline-block;
  margin: 2px;
  color: var(--color-text-2);
  font-size: 14px;
  line-height: 26px;
  background-color: transparent;
  border-radius: var(--border-radius-small);
  cursor: pointer;
  transition: all .1s cubic-bezier(0, 0, 1, 1);
}

.m-radio-button-content{
  position: relative;
  display: block;
  padding: 0 12px;
}
.m-radio-button:hover{
  color: var(--color-text-1);
  background-color: var(--color-bg-5);
}
.m-radio-button.m-radio--checked{
  color: rgb(var(--primary-6));
  background-color: var(--color-bg-5);
}
.m-radio__target:focus-visible + .m-radio-button-content{
  box-shadow: 0 0 0 3px var(--color-fill-3);
}
.m-radio-group-direction-vertical .m-radio{
  display: flex;
}
.m-radio--disabled, .m-radio--disabled .m-radio-icon-hover{
  cursor: not-allowed;
}
.m-radio--disabled.m-radio .m-radio__icon-effect {
  box-shadow: none;
}
.m-radio--disabled.m-radio.m-radio--checked .m-radio__icon-mask {
  background-color: var(--color-white);
  border: 5px solid var(--color-primary-light-4);
  box-shadow: none;
}
.m-radio--disabled.m-radio .m-radio__label{
  color: var(--color-text-4);
}
.m-radio--disabled.m-radio-button{
  color: var(--color-text-4);
}
.m-radio--disabled.m-radio-button:hover{
  background-color: var(--color-fill-2);
}
.m-radio--disabled.m-radio-button.m-radio--checked{
  color: var(--color-primary-light-4);
  background-color: var(--color-bg-1);
}
</style>
