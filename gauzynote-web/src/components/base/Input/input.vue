<script setup>
import {computed, inject, nextTick, onMounted, ref, useTemplateRef, watch} from "vue";
import {useCursor} from "@/hooks/index.js";

defineOptions({name: 'Input'})
const props = defineProps({
  defaultValue: {
    type: String,
  },
  type: {
    type: String,
    default: 'text'
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  readonly: {
    type: Boolean,
    default: false,
  },
  placeholder: String,
  error: {
    type: Boolean,
    default: false,
  },
  /**
   * size
   * @type {import('vue').PropType.<'fit' | 'small' | 'medium' | 'large'>}
   */
  size: {
    type: String,
    validator(value, props) {
      return ['fit', 'small', 'medium', 'large'].includes(value)
    },
    default: 'medium'
  },
  autocomplete: String,
  autocapitalize: String,
  autofocus: Boolean,
  showWordLimit: Boolean,
  maxLength: Number,
})

const modelValue = defineModel()

const _value = ref(props.defaultValue)

const emit = defineEmits(['input', 'change', 'pressEnter', 'focus', 'blur'])

const formItemContext = inject('formItemContext', null)

const mergedError = computed(() => props.error || !!formItemContext?.errorMessage.value);

const wrapperCls = computed(() => [
  `input__wrapper`,
  {
    [`input--error`]: mergedError.value,
    [`input--disabled`]: props.disabled,
    [`input--focus`]: focused.value,
    [`input-size--${props.size}`]: true
  },
]);

const isComposition = ref(false)

const focused = ref(false)
const inputRef = useTemplateRef('inputRef')
const [recordCursor, setCursor] = useCursor(inputRef);

let preValue = _value.value
const computedValue = computed(() => modelValue.value ?? _value.value);
watch(() => _value.value, (value, oldValue) => {
  preValue = oldValue
});
const handleMousedown = (e) => {
  if (inputRef.value && e.target !== inputRef.value) {
    e.preventDefault();
    inputRef.value.focus();
  }
};
const emitChange = (value, ev) => {
  if (value !== preValue) {
    preValue = value;
    emit('change', value, ev);
    formItemContext?.eventHandlers({type: 'change'})
  }
};

const updateValue = (value) => {
  if (
      props.maxLength &&
      value.length > props.maxLength
  ) {
    value = value.slice(0, props.maxLength);
  }
  _value.value = value;
  modelValue.value = value
};

function handleComposition(e) {
  const {value, selectionStart, selectionEnd} = e.target
  if (e.type === 'compositionend') {
    isComposition.value = false;
    if (
        props.maxLength &&
        value.length > props.maxLength
    ) {
      keepControl();
      return
    }
    updateValue(value)
    emit('input', value, e);
    formItemContext?.eventHandlers({type: 'input'})
  } else {
    isComposition.value = true;
  }
}

const keepControl = () => {
  recordCursor();
  nextTick(() => {
    if (inputRef.value && computedValue.value !== inputRef.value.value) {
      inputRef.value.value = computedValue.value;
      setCursor();
    }
  });
};
function handleInput(e) {
  const {value} = e.target;
  if (!isComposition.value) {
    if (
        props.maxLength &&
        value.length > props.maxLength
    ) {
      keepControl();
      return
    }
    // _value.value = value;
    // modelValue.value = value
    updateValue(value)
    emit('input', value, e);
    formItemContext?.eventHandlers(e)
  }
}

const handleFocus = (ev) => {
  focused.value = true;
  emit('focus', ev);
  formItemContext?.eventHandlers(ev);
};

const handleBlur = (ev) => {
  focused.value = false;
  emitChange(_value.value, ev);
  emit('blur', _value.value, ev);
  formItemContext?.eventHandlers(ev)
};
const handleKeyDown = (e) => {
  const keyCode = e.key || e.code;
  if (!isComposition.value && keyCode === 'Enter') {
    emitChange(_value.value, e);
    emit('pressEnter', _value.value, e);
  }
};

function focus() {
  inputRef.value.focus()
}

function blur() {
  inputRef.value.blur()
}

function select() {
  inputRef.value.select()
}

defineExpose({
  focus,
  blur,
  select,
})
onMounted(() => {
  if(props.autofocus){
    inputRef.value.focus()
  }
})
</script>

<template>
  <span :class="wrapperCls" @mousedown="handleMousedown">
    <span class="input__prefix" v-if="$slots.prefix">
      <slot name="prefix"></slot>
    </span>
    <input
        ref="inputRef"
        :value="computedValue"
        :disabled="disabled"
        :readonly="readonly"
        :type="type"
        :placeholder="placeholder"
        :autocomplete="autocomplete"
        :autocapitalize="autocapitalize"
        :max-length="maxLength"
        @input="handleInput"
        @keydown="handleKeyDown"
        @focus="handleFocus"
        @blur="handleBlur"
        @compositionstart="handleComposition"
        @compositionupdate="handleComposition"
        @compositionend="handleComposition"
    >
    <span class="input__suffix" v-if="$slots.suffix || (showWordLimit && maxLength)">
      <slot name="suffix">
        <span class="input-word-limit" v-if="showWordLimit && maxLength">
          {{computedValue.length}}/{{maxLength}}
        </span>
      </slot>
    </span>
  </span>
</template>

<style scoped lang="scss">
.input__wrapper{
  display: inline-flex;
  width: 100%;
  padding-right: var(--spacing-6);
  padding-left: var(--spacing-6);
  color: var(--color-text-1);
  font-size: inherit;
  background-color: var(--color-fill-2);
  border: 1px solid transparent;
  border-radius: var(--border-radius-medium);
  cursor: text;
  transition: border-color ease-out .2s, background-color ease-out .2s, box-shadow ease-out .2s;
}

.input__wrapper input {
  width: 100%;
  border: 0;
  padding: 0;
  font-size: inherit;
  background: none;
  border-radius: 0;
  outline: 0;
  color: inherit;
  cursor: inherit;
  line-height: 1.5715;
}
.input__wrapper input:-webkit-autofill, input:-webkit-autofill:active, input:-webkit-autofill:focus, input:-webkit-autofill:hover{
  -webkit-box-shadow: none !important;
  -webkit-background-clip: text !important;
}
.input__wrapper:hover {
  background-color: var(--color-fill-3);
}
.input__wrapper:focus-within, .input__wrapper.input--focus {
  border-color: rgb(var(--primary-6));
  background-color: var(--color-bg-1);
  box-shadow: 0 0 2px 0 rgb(var(--primary-2));
}

.input__wrapper.input--error{
  background-color: var(--color-danger-light-1);
  border-color: transparent;
}
.input__wrapper.input--error:hover {
  background-color: var(--color-danger-light-2);
  border-color: transparent;
}
.input__wrapper.input--error:focus-within, .input__wrapper.input--error.input--focus{
  background-color: var(--color-bg-2);
  border-color: rgb(var(--danger-6));
  box-shadow: 0 0 2px 0 var(--color-danger-light-2);
}

.input__wrapper.input--disabled{
  color: var(--color-text-4);
  background-color: var(--color-fill-2);
  border-color: transparent;
  cursor: not-allowed;
}

.input__wrapper input::placeholder {
  color: var(--color-text-3);
}
.input__wrapper.input--disabled input::placeholder{
  color: var(--color-text-4);
}

.input__wrapper.input-size--small {
  border-radius: var(--border-radius-small);
  font-size: 14px;
}
.input__wrapper.input-size--medium {
  border-radius: var(--border-radius-small);
  font-size: 14px;
}
.input__wrapper.input-size--fit {
  padding: 0;
}
.input__wrapper.input-size--small input {
  padding-top: var(--spacing-1);
  padding-bottom: var(--spacing-1);
}
.input__wrapper.input-size--medium input {
  padding-top: var(--spacing-2);
  padding-bottom: var(--spacing-2);
}
.input__wrapper.input-size--large input {
  padding-top: var(--spacing-3);
  padding-bottom: var(--spacing-3);
}

.input__wrapper .input__prefix, .input__wrapper .input__suffix{
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  white-space: nowrap;
  user-select: none;
}
.input__wrapper .input__prefix{
  padding-right: 12px;
  color: var(--color-text-2);
}
.input__wrapper .input__suffix{
  padding-left: 12px;
  color: var(--color-text-2);
}
.input__wrapper .input-word-limit{
  color: var(--color-text-3);
  font-size: 12px;
}
</style>