<script>
import {computed, defineComponent} from "vue";

export default defineComponent({
  name: "MButton",
  props: {
    /**
     * 按钮类型 Button type
     * @type {import('vue').PropType.<'base' | 'secondary' | 'primary' | 'outline' | 'text'>}
     * @default secondary
     */
    type: {
      type: String,
      default: 'secondary'
    },
    /**
     * 按钮的尺寸 Button size
     * @type {import('vue').PropType.<'mini' | 'small' | 'medium' | 'large' | 'custom'>}
     * @default medium
     */
    size: {
      type: String,
      default: 'medium'
    },
    /**
     * 形状
     * @type {import('vue').PropType.<'rectangle'|'square'|'circle'>}
     */
    shape: {
      type: String,
      default: 'rectangle',
    },
    /**
     * @zh 按钮的状态
     * @en Button state
     * @type {import('vue').PropType.<'normal'|'warning'|'success'|'danger'>}
     * @default 'normal'
     */
    status: {
      type: String,
    },
    long: {
      type: Boolean,
      default: false,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
    },
    htmlType: {
      type: String,
      default: 'button',
    },
  },
  emits: ['click'],
  setup(props, {emit}) {
    const prefixCls = 'm-btn'
    const cls = computed(() => [
      prefixCls,
      `${prefixCls}--${props.type}`,
      `${prefixCls}-size--${props.size}`,
      `${prefixCls}-shape--${props.shape}`,
      `${prefixCls}-status--${props.status ?? 'normal'}`,
      {
        [`${prefixCls}--loading`]: props.loading,
        [`${prefixCls}--long`]: props.long,
        [`${prefixCls}--disabled`]: props.disabled,
      },
    ]);
    const handleClick = (ev) => {
      if (props.disabled || props.loading) {
        ev.preventDefault();
        return;
      }
      emit('click', ev);
    };
    return {
      prefixCls, cls, handleClick
    }
  }
})
</script>

<template>
  <button
      :class="[cls, { [`${prefixCls}-only-icon`]: $slots.icon && !$slots.default },]"
      :type="htmlType"
      :disabled="disabled"
      @click="handleClick"
  >
    <span v-if="loading || $slots.icon" class="m-btn-icon">
      <icon-loading v-if="loading"/>
      <slot v-else name="icon"/>
    </span>
    <slot/>
  </button>
</template>

<style scoped lang="scss">
$prefix: m-btn;

.#{$prefix} {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  white-space: nowrap;
  outline: none;
  line-height: 1.5715;
  border: 1px solid transparent;
  transition: box-shadow ease-out .1s, background-color ease-out .1s, border-color ease-out .1s, color ease-out .1s;
  user-select: none;
  cursor: pointer;

  &.#{$prefix}--long {
    display: flex;
    width: 100%;
  }

  &.#{$prefix}-only-icon .#{$prefix}-icon {
    display: flex;
  }

  &.#{$prefix}-only-icon {
    color: inherit;
    padding: 0;
  }


  &.#{$prefix}--loading {
    cursor: default;
  }

  &.#{$prefix}--loading:before {
    content: "";
    position: absolute;
    inset: -1px;
    z-index: 1;
    display: block;
    background: #fff;
    border-radius: inherit;
    opacity: .4;
    transition: opacity .1s cubic-bezier(0, 0, 1, 1);
    pointer-events: none;
  }
}

.#{$prefix}--base, .#{$prefix}--base[type=button], .#{$prefix}--base[type=submit] {
  color: var(--color-text-2);
  background-color: transparent;
  border: 0;

  &:hover {
    color: var(--color-text-2);
    background-color: var(--color-fill-2);
  }

  &:active {
    color: var(--color-text-2);
    background-color: var(--color-fill-3);
    border-color: transparent;
  }

  &:focus-visible {
    box-shadow: 0 0 0 2px var(--color-border-3);
  }

  &.#{$prefix}--loading {
    background-color: transparent;
  }

  &.#{$prefix}--disabled {
    color: var(--color-text-4);
    background-color: transparent;
    cursor: not-allowed;
  }
}


.#{$prefix}--secondary, .#{$prefix}--secondary[type=button], .#{$prefix}--secondary[type=submit] {
  color: var(--color-text-2);
  background-color: var(--color-secondary);

  &:hover {
    color: var(--color-text-2);
    background-color: var(--color-secondary-hover);
    border-color: transparent;
  }

  &:active {
    color: var(--color-text-2);
    background-color: var(--color-secondary-active);
    border-color: transparent;
  }

  &:focus-visible {
    box-shadow: 0 0 0 2px var(--color-border-3);
  }

  &.#{$prefix}--loading {
    background-color: var(--color-secondary);
  }

  &.#{$prefix}--disabled {
    color: var(--color-text-4);
    background-color: var(--color-secondary-disabled);
    cursor: not-allowed;
  }

  &.#{$prefix}-status--danger {
    color: rgb(var(--danger-6));
    background-color: var(--color-danger-light-1);

    &:hover {
      background-color: var(--color-danger-light-2);
    }

    &:active {
      background-color: var(--color-danger-light-3);
    }

    &:focus-visible {
      box-shadow: 0 0 0 2px rgb(var(--danger-3));
    }

    &.#{$prefix}--loading {
      background-color: var(--color-danger-light-1);
    }

    &.#{$prefix}--disabled {
      color: var(--color-danger-light-3);
      background-color: var(--color-danger-light-1);
    }
  }
}

.#{$prefix}--primary, .#{$prefix}--primary[type=button], .#{$prefix}--primary[type=submit] {
  color: #fff;
  background-color: rgb(var(--primary-6));
  border: 1px solid transparent;

  &:hover {
    color: #fff;
    background-color: rgb(var(--primary-5));
    border-color: transparent;
  }

  &:active {
    color: #fff;
    background-color: rgb(var(--primary-7));
    border-color: transparent;
  }

  &:focus-visible {
    box-shadow: 0 0 0 2px rgb(var(--primary-3));
  }

  &.#{$prefix}--loading {
    background-color: rgb(var(--primary-6));
  }

  &.#{$prefix}--disabled {
    background-color: var(--color-primary-light-3);
    cursor: not-allowed;
  }

  &.#{$prefix}-status--danger {
    background-color: rgb(var(--danger-6));

    &:hover {
      background-color: rgb(var(--danger-5));
    }

    &:active {
      background-color: rgb(var(--danger-7));
    }

    &:focus-visible {
      box-shadow: 0 0 0 2px rgb(var(--danger-3));
    }

    &.#{$prefix}--loading {
      background-color: rgb(var(--danger-6));
    }

    &.#{$prefix}--disabled {
      background-color: var(--color-danger-light-3);
    }
  }
}

.#{$prefix}--outline, .#{$prefix}--outline[type=button], .#{$prefix}--outline[type=submit] {
  color: var(--color-text-2);
  border: 1px solid var(--color-border-2);
  background-color: transparent;

  &:hover {
    border-color: var(--color-border-3);
  }

  &:active {
    border-color: var(--color-border-4);
    background-color: var(--color-fill-2);
  }

  &:focus-visible {
    box-shadow: 0 0 0 2px var(--color-neutral-4);
  }

  &.#{$prefix}--loading {
    border-color: var(--color-border-2);
    background-color: transparent;
  }

  &.#{$prefix}--disabled {
    color: var(--color-text-4);
    border-color: var(--color-border-1);
    background-color: transparent;
    cursor: not-allowed;
  }
}


.#{$prefix}--text, .#{$prefix}--text[type=button], .#{$prefix}--text[type=submit] {
  color: rgb(var(--primary-6));
  background-color: transparent;

  &:hover {
    background-color: var(--color-fill-2);
  }

  &:active {
    background-color: var(--color-fill-3);
  }

  &:focus-visible {
    box-shadow: 0 0 0 2px var(--color-neutral-4);
  }

  &.#{$prefix}--loading {
    background-color: transparent;
  }

  &.#{$prefix}--disabled {
    color: var(--color-primary-light-3);
    background-color: transparent;
    cursor: not-allowed;
  }

  &.#{$prefix}-status--danger {
    color: rgb(var(--danger-6));

    &:focus-visible {
      box-shadow: 0 0 0 2px rgb(var(--danger-3));
    }

    &.#{$prefix}--disabled {
      color: var(--color-danger-light-3);
      background-color: transparent;
      cursor: not-allowed;
    }
  }
}

.#{$prefix}-size--mini {
  height: 24px;
  font-size: 12px;
  padding: 0 var(--spacing-6);
  border-radius: var(--border-radius-medium);

  &.#{$prefix}-only-icon {
    width: 38px;
  }

  &.#{$prefix}-shape--square {
    width: 24px;
  }

  &.#{$prefix}-shape--circle {
    width: 24px;
  }

  &:not(.#{$prefix}-only-icon) .#{$prefix}-icon {
    margin-right: 2px;
  }
}

.#{$prefix}-size--small {
  height: 28px;
  font-size: 14px;
  padding: 0 var(--spacing-8);
  border-radius: var(--border-radius-medium);

  &.#{$prefix}-only-icon {
    width: 46px;
  }

  &.#{$prefix}-shape--square {
    width: 28px;
  }

  &.#{$prefix}-shape--circle {
    width: 28px;
  }

  &:not(.#{$prefix}-only-icon) .#{$prefix}-icon {
    margin-right: 4px;
  }
}

.#{$prefix}-size--medium {
  height: 32px;
  font-size: 14px;
  padding: 0 var(--spacing-8);
  border-radius: var(--border-radius-medium);

  &.#{$prefix}-only-icon {
    width: 52px;
  }

  &.#{$prefix}-shape--square {
    width: 32px;
  }

  &.#{$prefix}-shape--circle {
    width: 32px;
  }

  &:not(.#{$prefix}-only-icon) .#{$prefix}-icon {
    margin-right: 8px;
  }
}

.#{$prefix}-size--large {
  height: 36px;
  font-size: 16px;
  padding: 0 var(--spacing-8);
  border-radius: var(--border-radius-medium);

  &.#{$prefix}-only-icon {
    width: 58px;
  }

  &.#{$prefix}-shape--square {
    width: 36px;
  }

  &.#{$prefix}-shape--circle {
    width: 36px;
  }

  &:not(.#{$prefix}-only-icon) .#{$prefix}-icon {
    margin-right: 8px;
  }
}

.#{$prefix}-size--custom {
  margin: 0;
  padding: 0;
  border-radius: var(--border-radius-none);
}

.#{$prefix}-shape--circle {
  border-radius: var(--border-radius-circle);
  padding: 0;
}
</style>
