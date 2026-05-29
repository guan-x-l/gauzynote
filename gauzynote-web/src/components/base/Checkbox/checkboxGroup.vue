<script setup>
import {computed, provide, ref, toRefs, watch} from "vue";
import {isArray, isNumber, isString, isUndefined, isNull, isFunction} from "@/utils/index.js";
import Checkbox from "@/components/base/Checkbox/checkbox.vue";

defineOptions({name: 'CheckboxGroup'})
const props = defineProps({
  /**
   * @zh 绑定值
   * @en Value
   * @vModel
   */
  modelValue: {
    type: Array,
    default: undefined,
  },
  /**
   * @zh 默认值（非受控状态）
   * @en Default value (uncontrolled state)
   */
  defaultValue: {
    type: Array,
    default: () => [],
  },
  /**
   * @zh 选项
   * @en Options
   */
  options: {
    type: Array,
  },
  /**
   * @zh 复选框组的方向
   * @en The direction of the checkbox group
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
  /**
   * @zh 最多可被勾选的项目数
   * @en The max number of checked options
   */
  max: {
    type: Number,
    default: undefined,
  },
})
const emit = defineEmits(['update:modelValue', 'change'])

const prefixCls = 'm-checkbox-group';
const {modelValue} = toRefs(props);
const _value = ref(isArray(props.defaultValue) ? [...props.defaultValue] : []);

const computedValue = computed(() => {
  if (isArray(props.modelValue)) {
    return props.modelValue;
  }
  return _value.value;
});

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

const handleChange = (value, checked, e) => {
  const nextValue = [...computedValue.value];
  const index = nextValue.indexOf(value);

  if (checked) {
    if (index > -1) return;
    if (props.max && nextValue.length >= props.max) return;
    nextValue.push(value);
  } else if (index > -1) {
    nextValue.splice(index, 1);
  }

  _value.value = nextValue;
  emit('update:modelValue', nextValue);
  emit('change', nextValue, e);
};

provide(
    'CheckboxGroup',
    {
      name: 'CheckboxGroup',
      value: computedValue,
      disabled: computed(() => props.disabled),
      max: computed(() => props.max),
      handleChange,
    }
);

watch(computedValue, (cur) => {
  if (_value.value !== cur) {
    _value.value = isArray(cur) ? [...cur] : [];
  }
});

watch(modelValue, (val) => {
  if (isUndefined(val) || isNull(val)) {
    _value.value = [];
  }
});

const cls = computed(() => [
  prefixCls,
  `${prefixCls}-direction-${props.direction}`,
  {
    [`${prefixCls}-disabled`]: props.disabled,
  },
]);
</script>

<template>
  <span :class="cls">
    <slot>
      <Checkbox
          v-for="option in options"
          :key="option.value"
          :value="option.value"
          :disabled="option.disabled"
      >
        <slot name="label" :data="option">
          {{ isFunction(option.label) ? option.label() : option.label }}
        </slot>
      </Checkbox>
    </slot>
  </span>
</template>

<style scoped lang="scss">
.m-checkbox-group {
  display: inline-flex;
  align-items: center;
  column-gap: 12px;
}

.m-checkbox-group-direction-vertical {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;

  :deep(.m-checkbox){
    line-height: 32px;
  }
}
</style>
