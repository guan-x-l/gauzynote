<script>
import {computed, defineComponent, ref} from "vue";

export default defineComponent({
  name: 'Tag',
  props: {
    color: String,
    /**
     * 标签的大小
     * @defaultValue 'medium'
     * @type {import('vue').PropType.<'mini' | 'small' | 'medium' | 'large'>}
     * @default 'medium'
     */
    size: {
      type: String,
      default: 'medium'
    },
    /**
     * 是否显示边框
     */
    bordered: {
      type: Boolean,
      default: false,
    },
    checkable: {
      type: Boolean,
      default: false,
    },
    checked: {
      type: Boolean,
      default: undefined,
    },
    defaultChecked: {
      type: Boolean,
      default: true,
    }
  },
  emits: {
    'update:checked': (visible) => true,
    /**
     * @zh 用户选中时触发（仅在可选中模式下触发）
     * @param {boolean} checked
     * @param {MouseEvent} ev
     */
    'check': (checked, ev) => true,
  },
  setup(props, {emit}) {
    const prefixCls = 'm-tag'
    const _checked = ref(props.defaultChecked)
    const tagColors = ['arcoblue']
    const isBuiltInColor = computed(() => props.color && tagColors.includes(props.color));
    const isCustomColor = computed(() => props.color && !tagColors.includes(props.color));
    const computedChecked = computed(() => props.checkable ? props.checked ?? _checked.value : true);
    const cls = computed(() => [
      prefixCls,
      `${prefixCls}-size--${props.size}`,
      {
        [`${prefixCls}--${props.color}`]: isBuiltInColor.value,
        [`${prefixCls}--checkable`]: props.checkable,
        [`${prefixCls}--bordered`]: props.bordered,
        [`${prefixCls}--checked`]: computedChecked.value,
        [`${prefixCls}--custom-color`]: isCustomColor.value,
      },
    ]);
    const style = computed(() => {
      if (isCustomColor.value && computedChecked.value) {
        return {backgroundColor: props.color};
      }
      return undefined;
    });

    function handleTagClick(e) {
      if (!props.checkable) return
      _checked.value = !computedChecked.value;
      emit('update:checked', _checked.value);
      emit('check', _checked.value, e);
    }

    return {
      cls,
      style,
      handleTagClick,
      computedChecked,
    }
  }
})
</script>

<template>
 <span :class="cls" :style="style" @click="handleTagClick">
    <slot/>
 </span>
</template>

<style scoped lang="scss">
.m-tag {
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  color: var(--color-text-1);
  font-weight: 500;
  vertical-align: middle;
  border: 1px solid transparent;
  border-radius: var(--border-radius-small);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.m-tag--checkable {
  cursor: pointer;
  transition: all .1s cubic-bezier(0, 0, 1, 1);

  &:hover {
    background-color: var(--color-fill-2);
  }
}

.m-tag--checked {
  background-color: var(--color-fill-2);
  border-color: transparent;

  &.m-tag--arcoblue {
    color: rgb(var(--arcoblue-6));
    background-color: rgb(var(--arcoblue-1));
    border: 1px solid transparent;
  }
}

.m-tag--bordered,
.m-tag--checkable.m-tag-checked.m-tag--bordered:hover {
  border-color: var(--color-border-2);
}

.m-tag--checkable.m-tag-checked.m-tag--bordered {
  &.m-tag-arcoblue {
    border-color: rgb(var(--arcoblue-6));
  }
}

.m-tag--checkable.m-tag--checked:hover {
  background-color: var(--color-fill-3);
  border-color: transparent;

  &.m-tag-arcoblue {
    background-color: rgb(var(--arcoblue-2));
  }
}
//.m-tag.m-tag--custom-color:not(.m-tag--checkable), .m-tag.m-tag--checkable.m-tag--checked.m-tag--custom-color {
//  color: var(--color-white);
//}

.m-tag-size--mini {
  height: 16px;
  line-height: 14px;
  font-size: 12px;
  padding: 0 4px;
}

.m-tag-size--small {
  height: 20px;
  font-size: 12px;
  line-height: 18px;
}

.m-tag-size--medium {
  height: 24px;
  font-size: 12px;
  line-height: 22px;
}

.m-tag-size--large {
  height: 32px;
  font-size: 14px;
  line-height: 30px;
}
</style>
