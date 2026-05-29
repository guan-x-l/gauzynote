<script setup>
import {computed} from 'vue';

const props = defineProps({
  /**
   * 分割线方向
   * @type {import('vue').PropType.<'horizontal' | 'vertical'>}
   */
  direction: {
    type: String,
    default: 'horizontal',
    validator: (value) => ['horizontal', 'vertical'].includes(value)
  },
  /**
   * 是否虚线
   */
  dashed: {
    type: Boolean,
    default: false
  },
  /**
   * 分割线颜色
   */
  color: {
    type: String,
    default: 'var(--color-border-2)'
  },
  /**
   * 文本位置（仅水平方向有效）
   * @type {import('vue').PropType.<'left' | 'center' | 'right'>}
   */
  orientation: {
    type: String,
    default: 'center',
    validator: (value) => ['left', 'center', 'right'].includes(value)
  },
  /**
   * 分割线厚度
   */
  thickness: {
    type: String,
    default: '1px'
  },
  /**
   * 分割线长度（仅垂直方向有效）
   */
  size: {
    type: String,
    default: '1em'
  },
  /**
   * 分割线边距
   */
  margin: {
    type: String,
    // default: '8px 0'
  },
});

const dividerStyle = computed(() => {
  return {
    margin: props.margin
  };
});

const prefixCls = 'divider'
const cls = computed(() => [
  prefixCls,
  `text-position--${props.orientation}`,
  {
    [`${prefixCls}-direction--${props.direction}`]: props.direction,
    [`${prefixCls}--dashed`]: props.dashed,
    [`${prefixCls}--disabled`]: props.disabled,
  },
]);
</script>

<template>
  <div
    :class="[...cls, {'with-text': !!$slots.default}]"
    :style="dividerStyle"
  >
    <template v-if="direction === 'horizontal'">
      <template v-if="$slots.default">
        <div class="divider__line is-left"></div>
        <span class="divider__text"><slot></slot></span>
        <div class="divider__line is-right"></div>
      </template>
      <div v-else class="divider__line"></div>
    </template>
    
    <template v-if="direction === 'vertical'">
      <div class="divider-direction--vertical__line"></div>
    </template>
  </div>
</template>

<style scoped>
.divider {
  display: flex;
  align-items: center;
  box-sizing: border-box;
}

/* 水平分割线 */
.divider-direction--horizontal {
  width: 100%;
  margin: 8px 0;
}

/* 垂直分割线 */
.divider-direction--vertical {
  display: inline-flex;
  height: 1em;
  margin: 0 12px;
  vertical-align: middle
}

.divider-direction--vertical__line {
  width: v-bind(thickness);
  background-color: v-bind(color);
  height: 100%;
}

/* 线条样式 */
.divider__line {
  flex: 1;
  height: v-bind(thickness);
  background-color: v-bind(color);
}

/* 虚线样式 */
.divider--dashed .divider__line {
  background-color: transparent;
  border-top: v-bind(thickness) dashed v-bind(color);
}

.divider--dashed .divider-direction--vertical__line {
  background-color: transparent;
  border-left: v-bind(thickness) dashed v-bind(color);
}

/* 文本样式 */
.divider__text {
  padding: 0 1em;
  white-space: nowrap;
  color: var(--color-text-3);
  font-size: 0.875rem;
}

/* 带文本的分割线样式 */
.with-text.text-position--left {
  justify-content: flex-start;
}

.with-text.text-position--left .is-right {
  flex: 3;
}

.with-text.text-position--left .is-left {
  flex: 1;
}

.with-text.text-position--right {
  justify-content: flex-end;
}

.with-text.text-position--right .is-left {
  flex: 3;
}

.with-text.text-position--right .is-right {
  flex: 1;
}
</style>
