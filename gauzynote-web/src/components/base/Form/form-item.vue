<script setup>
import {computed, inject, nextTick, onMounted, onUnmounted, onUpdated, provide, ref, toRefs} from "vue";
import {useI18n} from "vue-i18n";


const props = defineProps({
  prop: {
    type: String,
  },
  label: {
    type: String,
    default: ''
  },
  required: {
    type: Boolean,
    default: false
  },
  rules: {
    type: Array,
    default: () => []
  },
  validateTrigger: {
    type: String,
    default: null
  },
  hideLabel: Boolean
})
const {t} = useI18n()
const formCtx = inject('form', null)
const errorMessage = computed(() => formCtx.errors[props.prop] || '')
const labelWrapperStyle = computed(() => {
  return {
    flex: formCtx.autoLabelWidth.value ? `0 0 ${formCtx.maxLabelWidth.value || 0}px` : undefined
  }
})
const prefixCls = 'form-item'
const labelWrapperCls = computed(() => {
  return {
    'form-item__label-wrapper-default-width': !formCtx.autoLabelWidth.value,
    [`form-item__label-wrapper-align-${formCtx.labelAlign.value}`]: true,
  }
})
const cls = computed(() => [
  prefixCls,
  `${prefixCls}-layout--${formCtx.layout.value}`,
  {
    'form-item--error': !!errorMessage.value
  },
]);

// 初始值
const initialValue = formCtx?.formData[props.prop]

// 校验逻辑
const validateRule = (rule, value) => {
  if (rule.required && (value === undefined || value === null || value === '')) {
    return rule.message || t('form.validateMessages.required', {field: props.prop});
  }

  // if (rule.pattern && value && !rule.pattern.test(value)) {
  //   return rule.message || `${props.prop}格式不正确`;
  // }
  //
  // if (rule.min !== undefined && value && value.length < rule.min) {
  //   return rule.message || `${props.prop}长度不能小于${rule.min}`;
  // }
  //
  // if (rule.max !== undefined && value && value.length > rule.max) {
  //   return rule.message || `${props.prop}长度不能大于${rule.max}`;
  // }

  return null
}


// 校验函数
const validateField = async () => {
  if (!props.rules || !props.prop) return true

  let error = ''
  const value = formCtx.formData[props.prop]

  // 合并规则，若 required 为 true 则添加必填规则
  let rules = [...props.rules]
  if (props.required) {
    rules.unshift({required: true})
  }

  for (const rule of rules) {
    const ruleError = validateRule(rule, value)
    if (ruleError) {
      error = ruleError
      break
    }

    // 处理函数校验
    if (typeof rule.validator === 'function') {
      let result = rule.validator(value)
      // 处理异步校验
      if (result instanceof Promise) {
        try {
          result = await result
        } catch (e) {
          result = e.message || e || t('form.validateMessages.validateError')
        }
      }
      if (result !== true) {
        error = typeof result === 'string' ? result : t('form.validateMessages.validateError')
        break
      }
    }
  }

  // 更新错误信息
  formCtx.errors[props.prop] = error
  return !error
}

// 重置字段
const resetField = () => {
  formCtx.formData[props.prop] = initialValue
}

function eventHandlers(event) {
  const trigger = props.validateTrigger === event.type ? props.validateTrigger : ''
  switch (trigger) {
    case 'change':
    case 'focus':
    case 'blur':
      validateField();
      break
    case 'input':
      nextTick(() => {
        validateField();
      });
      break
    default:
      break
  }
  return event;
}

let unregister
// 注册表单项
onMounted(() => {
  if (props.prop && formCtx) {
    // 初始化表单数据
    if (formCtx.formData[props.prop] === undefined) {
      formCtx.formData[props.prop] = ''
    }

    // 注册到表单
    unregister = formCtx.registerField({
      prop: props.prop,
      validate: validateField,
      reset: resetField
    })

  }
})

// 卸载表单项
onUnmounted(() => {
  if (unregister) unregister()
})
provide('formItemContext', {
  errorMessage,
  eventHandlers
})
// 暴露方法
defineExpose({
  validate: validateField,
  reset: resetField,
})
</script>

<template>
  <div :class="cls" class="flex flex-row items-baseline flex-wrap">
    <div class="form-item__label-wrapper" v-if="!hideLabel" :style="labelWrapperStyle" :class="labelWrapperCls">
      <label class="form-item__label">
        <strong v-if="required" class="form-item__label-required-symbol">*</strong>
        {{ label }}
      </label>
    </div>
    <div class="flex-1">
      <div class="form-item__content-wrapper">
        <div class="form-item__content">
          <slot/>
        </div>
      </div>
      <div role="alert" v-if="errorMessage" class="blink-1 form-item__message" style="width: 100%">
        {{ errorMessage }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.form-item {
  margin-bottom: var(--spacing-12);
}

.form-item.form-item-layout--vertical {
  display: block;
}

.form-item .form-item__label-wrapper {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 16px;
  line-height: 1.62;
}

.form-item .form-item__label-wrapper .form-item__label {
  max-width: 100%;
  color: var(--color-text-2);
  font-size: 14px;
}

.form-item .form-item__label-wrapper-default-width {
  flex: 0 0 22%;
}

.form-item .form-item__label-wrapper-align-left {
  justify-content: flex-start;
}

.form-item.form-item-layout--vertical > .form-item__label-wrapper {
  margin-bottom: 8px;
}

.form-item .form-item__content-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  width: 100%;
}

.form-item .form-item__content {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex: 1;
}

.form-item__label-required-symbol {
  width: 12px;
  color: rgb(var(--danger-6));
  font-size: 12px;
  line-height: 1;
}

.form-item.form-item--error {
  margin-bottom: 0;
}

.form-item__message {
  min-height: var(--spacing-12);
  color: rgb(var(--danger-6));
  font-size: 12px;
  line-height: var(--spacing-12);
}

@keyframes blink-1 {
  0%, 50%, 100% {
    opacity: 1
  }
  25%, 75% {
    opacity: 0
  }
}

.blink-1 {
  animation: blink-1 .6s both
}
</style>