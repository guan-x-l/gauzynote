<template>
  <component :is="component" ref="wrapperRef" :class="classNames">
    <div
        :class="[`${prefixCls}-pane`, `${prefixCls}-pane-first`]"
        style="flex: 1"
        :style="resizeSecond ? 'flex: 1' : firstPaneStyles"
        ref="firstPaneRef"
    >
      <slot name="first"/>
    </div>
    <ResizeTrigger
        v-if="!disabled"
        :prefix-cls="`${prefixCls}-trigger`"
        :direction="isHorizontal ? 'vertical' : 'horizontal'"
        :class="{'split-trigger-is-dragging': isDragging}"
        @mousedown="onMoveStart"
        @resize="onTriggerResize"
    >
      <!--      <template #default>
              <slot name="resize-trigger"/>
            </template>-->
    </ResizeTrigger>
    <div
        :class="[`${prefixCls}-pane`, `${prefixCls}-pane-second`]"
        :style="resizeSecond ? firstPaneStyles : ''"
        ref="secondPaneRef"
    >
      <slot name="second"/>
    </div>
  </component>
</template>
<script>
import {
  computed,
  defineComponent,
  reactive,
  ref,
  toRefs,
  onMounted,
  nextTick,
} from 'vue';
import ResizeTrigger from '../../Resize/ResizeTrigger.vue';
import {useMergeState} from '@/hooks/index.js';
import {off, on, getSizeConfig} from '@/utils/index.js';


/**
 * @param {Object} employee
 * @param {number | string | undefined} employee.size
 * @param {string} [employee.defaultSize]
 * @param {number} employee.containerSize
 */
function getPxSize({size, defaultSize, containerSize}) {
  const config = getSizeConfig(size ?? defaultSize);
  if (config.isPx) {
    return config.size;
  }
  return config.size * containerSize;
}

/**
 * @param {number | string} numerator
 * @param {number | string} denominator
 * @returns {number}
 */
function px2percent(numerator, denominator) {
  return parseFloat(numerator) / parseFloat(denominator);
}

// https://arco.design/vue
export default defineComponent({
  name: 'Split',
  components: {
    ResizeTrigger,
  },
  props: {
    /**
     * @zh 分割框的 html 标签
     * @en The html tag of the split box
     */
    component: {
      type: String,
      default: 'div',
    },
    /**
     * @zh 分割的方向
     * @en Direction of division
     * @type {import('vue').PropType.<'horizontal' | 'vertical'>}
     * @default horizontal
     */
    direction: {
      type: String,
      default: 'horizontal',
    },
    /**
     * @zh 分割的大小，可以是 0~1 代表百分比，或具体数值的像素，如 300px
     * @en The size of the segmentation, it can be 0~1 representing a percentage, or a specific number of pixels, such as 300px
     * @vModel
     */
    size: {
      type: [Number, String],
      default: undefined,
    },
    /**
     * @zh 默认分割的大小，可以是 0~1 代表百分比，或具体数值的像素，如 300px
     * @en Default split size, it can be 0~1 representing a percentage, or a specific number of pixels, such as 300px
     */
    defaultSize: {
      type: [Number, String],
      default: 0.5,
    },
    /**
     * @zh 最小阈值，可以是 0~1 代表百分比，或具体数值的像素，如 300px
     * @en Minimum threshold, it can be 0~1 representing a percentage, or a specific number of pixels, such as 300px
     */
    min: {
      type: [Number, String],
    },
    /**
     * @zh 最大阈值，可以是 0~1 代表百分比，或具体数值的像素，如 300px
     * @en Maximum threshold, it can be 0~1 representing a percentage, or a specific number of pixels, such as 300px
     * */
    max: {
      type: [Number, String],
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
     * 调整大小的元素改为第二
     */
    resizeSecond: Boolean,
  },
  emits: ['moveStart', 'moving', 'moveEnd', 'update:size'],
  // emits: {
  //   /**
  //    * @zh 开始拖拽之前触发
  //    * @en Triggered before dragging
  //    * @param {MouseEvent} ev
  //    */
  //   moveStart: (ev) => true,
  //   /**
  //    * @zh 拖拽时触发
  //    * @en Triggered when dragging
  //    * @param {MouseEvent} ev
  //    */
  //   moving: (ev) => true,
  //   /**
  //    * @zh 拖拽结束之后触发
  //    * @en Triggered after dragging ends
  //    * @param {MouseEvent} ev
  //    */
  //   moveEnd: (ev) => true,
  //   /**
  //    * @param {number | string} size
  //    */
  //   'update:size': (size) => true,
  // },
  /**
   * @zh 第一个面板的内容
   * @en The contents of the first panel
   * @slot first
   */
  /**
   * @zh 第二个面板的内容
   * @en The contents of the second panel
   * @slot second
   */
  /**
   * @zh 伸缩杆的内容
   * @en The contents of the resize pole
   * @slot resize-trigger
   */
  /**
   * @zh 伸缩杆的图标
   * @en Resize pole icon
   * @slot resize-trigger-icon
   */
  setup(props, {emit}) {
    const {direction, size: propSize, defaultSize, min, max, resizeSecond} = toRefs(props);
    const triggerSize = ref(0);
    const isDragging = ref(false);
    /**
     *
     * @type {import('vue').Ref<HTMLDivElement>}
     */
    const wrapperRef = ref();
    const firstPaneRef = ref();
    const secondPaneRef = ref();
    const prefixCls = 'split';
    const [size, setSize] = useMergeState(
        defaultSize.value,
        reactive({
          value: propSize,
        })
    );
    const sizeConfig = computed(() => getSizeConfig(size.value));
    const isHorizontal = computed(() => direction.value === 'horizontal');
    const classNames = computed(() => [
      prefixCls,
      {
        [`${prefixCls}-horizontal`]: isHorizontal.value,
        [`${prefixCls}-vertical`]: !isHorizontal.value,
      },
    ]);
    const firstPaneStyles = computed(() => {
      const {size: numberSize, unit, isPx} = sizeConfig.value;
      const baseVal = isPx ? numberSize : numberSize * 100;
      // if (props.disabled){
        return {
          flex: `0 0 calc(${baseVal}${unit})`,
        };
      // }
      // return {
        // flex: `0 0 calc(${baseVal}${unit} - ${triggerSize.value / 2}px)`,
      // };
    });
    /**
     * @type {{startContainerSize: number, startSize: number | string, startPageY: number, startPageX: number}}
     */
    const record = {
      startPageX: 0,
      startPageY: 0,
      startContainerSize: 0,
      startSize: 0,
    };

    async function getContainerSize() {
      const getSize = () => {
        return isHorizontal.value
            ? wrapperRef.value?.clientWidth
            : wrapperRef.value?.clientHeight || 0;
      };

      if (!wrapperRef.value || getSize()) {
        await nextTick();
      }

      return getSize();
    }

    /**
     *
     * @param {number} newPxSize
     * @param {number} containerSize
     */
    function updateSize(newPxSize, containerSize) {
      if (!containerSize) {
        return;
      }

      const newSize = sizeConfig.value.isPx
          ? `${newPxSize}px`
          : px2percent(newPxSize, containerSize);

      if (size.value === newSize) return;
      setSize(newSize);
      emit('update:size', newSize);
    }

    /**
     *
     * @param {number | string} size
     * @param {number} containerSize
     * @returns {number}
     */
    function getLegalPxSize(size, containerSize) {
      const pxSize = getPxSize({
        size,
        containerSize,
      });
      const minPxSize = getPxSize({
        size: min.value,
        defaultSize: '0px',
        containerSize,
      });
      const maxPxSize = getPxSize({
        size: max.value,
        defaultSize: `${containerSize}px`,
        containerSize,
      });

      let legalPxSize = pxSize;
      legalPxSize = Math.max(legalPxSize, minPxSize);
      legalPxSize = Math.min(legalPxSize, maxPxSize);

      return legalPxSize;
    }

    /**
     * @param {Object} employee
     * @param {number} employee.startContainerSize
     * @param {number | string} employee.startSize
     * @param {number} employee.startPosition
     * @param {number} employee.endPosition
     */
    function getNewPxSize({startContainerSize, startSize, startPosition, endPosition}) {
      const startPxSize = getPxSize({
        size: startSize,
        containerSize: startContainerSize,
      });
      if (resizeSecond.value) {
        return getLegalPxSize(
            `${startPxSize - (endPosition - startPosition)}px`,
            startContainerSize
        );
      }
      return getLegalPxSize(
          `${startPxSize + (endPosition - startPosition)}px`,
          startContainerSize
      );
    }

    /**
     *  移动中，更新 firstPane 的占位大小
     * @param {MouseEvent} e
     */
    function onMoving(e) {
      emit('moving', e);

      const newPxSize = isHorizontal.value
          ? getNewPxSize({
            startContainerSize: record.startContainerSize,
            startSize: record.startSize,
            startPosition: record.startPageX,
            endPosition: e.pageX,
          })
          : getNewPxSize({
            startContainerSize: record.startContainerSize,
            startSize: record.startSize,
            startPosition: record.startPageY,
            endPosition: e.pageY,
          });
      updateSize(newPxSize, record.startContainerSize);
    }

    /**
     * 移动结束，解除事件绑定
     * @param {MouseEvent} e
     */
    function onMovingEnd(e) {
      off(window, 'mousemove', onMoving);
      off(window, 'mouseup', onMovingEnd);
      off(window, 'contextmenu', onMovingEnd);

      document.body.style.cursor = 'default';

      isDragging.value = false

      emit('moveEnd', e);
    }

    /**
     * 移动开始，记录初始值，绑定移动事件
     * @param {MouseEvent} e
     */
    async function onMoveStart(e) {
      emit('moveStart', e);

      isDragging.value = true

      record.startPageX = e.pageX;
      record.startPageY = e.pageY;
      record.startContainerSize = await getContainerSize();
      record.startSize = size.value;

      on(window, 'mousemove', onMoving);
      on(window, 'mouseup', onMovingEnd);
      on(window, 'contextmenu', onMovingEnd);

      document.body.style.cursor = isHorizontal.value
          ? 'col-resize'
          : 'row-resize';
    }

    /**
     * @param {ResizeObserverEntry} entry
     */
    function onTriggerResize(entry) {
      const {width, height} = entry.contentRect;
      triggerSize.value = isHorizontal.value ? width : height;
    }

    onMounted(async () => {
      const containerSize = await getContainerSize();
      const fixedPxSize = getLegalPxSize(size.value, containerSize);
      updateSize(fixedPxSize, containerSize);
    });

    return {
      prefixCls,
      isDragging,
      record,
      classNames,
      isHorizontal,
      wrapperRef,
      firstPaneRef,
      secondPaneRef,
      onMoveStart,
      onTriggerResize,
      firstPaneStyles,
      resizeSecond,
    };
  },
});
</script>
<style lang="scss" scoped>

.split {
  display: flex
}

.split-pane {
  overflow: auto;
}

.split-pane-second {
  flex: 1;
}

.split-horizontal {
  flex-direction: row
}

.split-vertical {
  flex-direction: column
}

.split-trigger {
  background-color: var(--color-fill-3);
  pointer-events: auto;
  transition: background-color .2s ease-out, transform .2s ease-out;
  z-index: 99;
}

.split-trigger:hover {
  background-color: #3B82F6;
}

.split-trigger.split-trigger-is-dragging {
  background-color: #3B82F6;
}

.split-trigger-vertical {
  width: 2px;
  height: 100%;
  cursor: col-resize
}

.split-trigger.split-trigger-vertical:hover {
  transform: scaleX(1.6);
}

.split-trigger.split-trigger-is-dragging.split-trigger-vertical {
  transform: scaleX(2.6);
}

.split-trigger-horizontal {
  width: 100%;
  height: 2px;
  cursor: row-resize
}

.split-trigger.split-trigger-horizontal:hover {
  transform: scaleY(1.6);
}

.split-trigger.split-trigger-is-dragging.split-trigger-horizontal {
  transform: scaleY(2.6);
}
</style>
