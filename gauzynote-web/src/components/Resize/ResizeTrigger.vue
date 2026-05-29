<template>
  <ResizeObserver @resize="onResize">
    <div :class="classNames">
      <!-- @slot 自定义内容 -->
      <slot/>
    </div>
  </ResizeObserver>
</template>
<script>
import { computed, defineComponent, toRefs } from 'vue';
import ResizeObserver from './ResizeObserver.vue';

// https://arco.design/vue
export default defineComponent({
  name: 'ResizeTrigger',
  components: {
    ResizeObserver,
  },
  props: {
    prefixCls: {
      type: String,
      required: true,
    },
    /**
     * @type {import('vue').PropType.<'horizontal' | 'vertical'>}
     * @default horizontal
     */
    direction: {
      type: String,
      default: 'horizontal',
    },
  },
  emits: ['resize'],
  setup(props, { emit }) {
    const { direction, prefixCls } = toRefs(props);
    const isHorizontal = computed(() => direction?.value === 'horizontal');
    const classNames = computed(() => [
      prefixCls.value,
      {
        [`${prefixCls.value}-horizontal`]: isHorizontal.value,
        [`${prefixCls.value}-vertical`]: !isHorizontal.value,
      },
    ]);
    /**
     * @param {ResizeObserverEntry} entry
     */
    const onResize = (entry) => {
      emit('resize', entry);
    };

    return {
      classNames,
      onResize,
      isHorizontal,
    };
  },
});
</script>