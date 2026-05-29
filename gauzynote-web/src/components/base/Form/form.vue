<script setup>
import {computed, nextTick, onMounted, onUpdated, provide, reactive, ref, toRefs, useTemplateRef, watch} from 'vue';

const props = defineProps({
  autoLabelWidth: Boolean,
  /**
   * 标签的对齐方向
   * @type {import('vue').PropType.<'left' | 'right'>}
   */
  labelAlign: {
    type: String,
    default: 'right',
  },
  /**
   * 标签的对齐方向
   * @type {import('vue').PropType.<'horizontal' | 'vertical'>}
   */
  layout: {
    type: String,
    default: 'horizontal',
  }
})
const model = defineModel()

const emit = defineEmits(['submit', 'validate'])

const formData = reactive({...model.value})
const errors = reactive({})
const fields = ref([])
const formRef = useTemplateRef('formRef')
const autoLabelWidth = computed(
    () => props.layout === 'horizontal' && props.autoLabelWidth
);
const labelAlign = computed(()=>props.labelAlign)
const maxLabelWidth = ref();
const {layout} = toRefs(props)

// 提供表单上下文
provide('form', {
  formData,
  errors,
  autoLabelWidth,
  maxLabelWidth,
  labelAlign,
  layout,
  // 注册Field， 并返回卸载Field函数
  registerField: (field) => {
    fields.value.push(field)
    return () => {
      fields.value = fields.value.filter(f => f !== field)
    }
  }
})

// 校验指定字段
const validateField = async (prop) => {
  const field = fields.value.find(f => f.prop === prop)
  if (field) {
    return await field.validate()
  }
  return true
}

// 校验所有字段
const validate = async () => {
  const results = await Promise.all(fields.value.map(field => field.validate()))
  return results.every(result => result)
}

// 重置表单
const reset = () => {
  // 触发字段重置
  fields.value.forEach(field => {
    field.reset()
    // 重置数据
    model.value[field.prop] = formData[field.prop]
  })
  // 清除错误
  Object.keys(errors).forEach(key => {
    errors[key] = ''
  })
}

const handleSubmit = async () => {
  const isValid = await validate()
  if (isValid) {
    console.log('表单提交成功', formData)
    emit('submit', {...formData})
  } else {
    console.log('表单校验失败')
  }
}
// 监听外部数据变化
watch(() => model.value, (newVal) => {
  Object.assign(formData, newVal)
}, {deep: true, immediate: true})

watch(autoLabelWidth, updateLabelWidth)

// 自动计算最大 label 宽度
const calculateMaxLabelWidth = () => {
  if (!formRef.value) return;

  const labels = formRef.value.querySelectorAll('.form-item__label-wrapper');
  let maxWidth = 0;

  labels.forEach(label => {

    const width = label.clientWidth;
    if (width > maxWidth) {
      maxWidth = width;
    }
  });
  maxLabelWidth.value = maxWidth;
};

function updateLabelWidth() {
  maxLabelWidth.value = undefined
  if (autoLabelWidth.value) {
    nextTick(() => {
      calculateMaxLabelWidth();
    });
  }
}
const prefixCls = 'm-form'
const cls = computed(() => [
  `${prefixCls}-layout--${props.layout}`,
  {
    [`${prefixCls}-auto-label-width`]: props.autoLabelWidth,
  },
]);
onMounted(() => {
  updateLabelWidth()
});

onUpdated(() => {
  updateLabelWidth()
});
defineExpose({
  validate,
  validateField,
  reset
})
</script>

<template>
  <form @submit.prevent="handleSubmit" :class="cls" ref="formRef">
    <slot :labelAlign="labelAlign"></slot>
  </form>
</template>

<style scoped>

</style>