<script setup>
import {computed, reactive, ref, toRefs, watch, provide} from "vue";
import {isFunction, isNull, isNumber, isString, isUndefined} from "@/utils/index.js";
import Radio from "@/components/base/Radio/radio.vue";

defineOptions({name: 'RadioGroup'})
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
   * @zh 默认值（非受控状态）
   * @en Default value (uncontrolled state)
   */
  defaultValue: {
    type: [String, Number, Boolean],
    default: '',
  },
  /**
   * @zh 单选框组的类型
   * @en Types of radio group
   * @values 'radio', 'button'
   */
  type: {
    type: String,
    default: 'radio',
  },
  /**
   * @zh 单选框组的尺寸
   * @en The size of the radio group
   * @values 'mini','small','medium','large'
   */
  size: {
    type: String,
    default: 'medium',
  },
  /**
   * @zh 选项
   * @en Options
   * @version 2.27.0
   */
  options: {
    type: Array,
  },
  /**
   * @zh 单选框组的方向
   * @en The direction of the radio group
   * @values 'horizontal', 'vertical'
   */
  direction: {
    type: String,
    default: 'horizontal',
  },
  /**
   * @zh 是否禁用
   * @en Whether to disable
   */
  disabled: {
    type: Boolean,
    default: false,
  },
})
const emit = defineEmits(['update:modelValue', 'change'])

const prefixCls = 'm-radio-group';
const { size: mergedSize, type, disabled, modelValue } = toRefs(props);
const mergedDisabled = computed(()=>props.disabled)


const _value = ref(props.defaultValue);

const computedValue = computed(() => props.modelValue ?? _value.value);

const options = computed(() => {
  return (props.options ?? []).map((option) => {
    if (isString(option) || isNumber(option)) {
      return {
        label: option,
        value: option,
      };
    }
    return option;
  });
});

const handleChange = (value, e ) => {
  _value.value = value;
  emit('update:modelValue', value);
  emit('change', value, e);
  // eventHandlers.value?.onChange?.(e);
};

provide(
    'RadioGroup',
    reactive({
      name: 'RadioGroup',
      value: computedValue,
      size: mergedSize,
      type,
      disabled: mergedDisabled,
      // slots,
      handleChange,
    })
);

watch(computedValue, (cur) => {
  if (_value.value !== cur) {
    _value.value = cur;
  }
});

watch(modelValue, (val) => {
  if (isUndefined(val) || isNull(val)) {
    _value.value = '';
  }
});

const cls = computed(() => [
  `${prefixCls}${props.type === 'button' ? '-button' : ''}`,
  `${prefixCls}-size-${mergedSize.value}`,
  `${prefixCls}-direction-${props.direction}`,
  {
    [`${prefixCls}-disabled`]: mergedDisabled.value,
  },
]);
</script>

<template>
<span :class="cls">
  <slot>
    <Radio
        v-for="option in options"
        :key="option.value"
        :value="option.value"
        :type="type"
        :disabled="option.disabled"
        :modelValue="computedValue === option.value"
        >
      <slot name="label" :data="option">
        {{isFunction(option.label) ? option.label() : option.label}}
      </slot>
    </Radio>
  </slot>
</span>
</template>

<style scoped>
.m-radio-group-button{
  display: inline-flex;
  padding: 2px;
  line-height: 26px;
  background-color: var(--color-fill-2);
  border-radius: var(--border-radius-small);
}
</style>
