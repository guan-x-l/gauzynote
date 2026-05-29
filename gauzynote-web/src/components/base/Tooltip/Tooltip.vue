<template>
  <Trigger
      :class="prefixCls"
      trigger="hover"
      :position="position"
      :popup-visible="computedPopupVisible"
      :disabled="disabled"
      :popup-offset="10"
      show-arrow
      :content-class="contentCls"
      :content-style="computedContentStyle"
      :arrow-class="arrowCls"
      :arrow-style="computedArrowStyle"
      :popup-container="popupContainer"
      animation-name="zoom-in-fade-out"
      auto-fit-transform-origin
      role="tooltip"
      @popup-visible-change="handlePopupVisibleChange"
  >
    <slot />
    <template #content>
      <slot name="content">{{ content }}</slot>
    </template>
  </Trigger>
</template>

<script>
import { computed, defineComponent, ref } from 'vue';
import Trigger from '../../Trigger/Trigger.vue';

export default defineComponent({
  name: 'Tooltip',
  components: {
    Trigger,
  },
  props: {
    /**
     * @zh 文字气泡是否可见
     * @en Whether the tooltip is visible
     * @vModel
     */
    popupVisible: {
      type: Boolean,
      default: undefined,
    },
    /**
     * @zh 文字气泡默认是否可见（非受控模式）
     * @en Whether the tooltip is visible by default (uncontrolled mode)
     */
    defaultPopupVisible: {
      type: Boolean,
      default: false,
    },
    /**
     * @zh 文字气泡是否禁用
     * @en Whether to disable the tooltip
     */
    disabled: {
      type: Boolean,
      default: false,
    },
    /**
     * @zh 文字气泡内容
     * @en Tooltip content
     */
    content: String,
    /**
     * @zh 弹出位置
     * @en Popup position
     * @type {import('vue').PropType.<'top' | 'tl' | 'tr' | 'bottom' | 'bl' | 'br' | 'left' | 'lt' | 'lb' | 'right' | 'rt' | 'rb'>}
     * @values 'top','tl','tr','bottom','bl','br','left','lt','lb','right','rt','rb'
     */
    position: {
      type: String,
      default: 'top',
    },
    /**
     * @zh 是否展示为迷你尺寸
     * @en Whether to display as a mini size
     */
    mini: {
      type: Boolean,
      default: false,
    },
    /**
     * @zh 弹出框的背景颜色
     * @en Background color of the popover
     */
    backgroundColor: {
      type: String,
    },
    /**
     * @zh 弹出框内容的类名
     * @en The class name of the popup content
     */
    contentClass: {
      type: [String, Array, Object],
    },
    /**
     * @zh 弹出框内容的样式
     * @en The style of the popup content
     */
    contentStyle: {
      type: Object,
    },
    /**
     * @zh 弹出框箭头的类名
     * @en The class name of the popup arrow
     */
    arrowClass: {
      type: [String, Array, Object],
    },
    /**
     * @zh 弹出框箭头的样式
     * @en The style of the popup arrow
     */
    arrowStyle: {
      type: Object,
    },
    /**
     * @zh 弹出框的挂载容器
     * @en Mount container for popup
     */
    popupContainer: {
      type: [String, Object],
    },
  },
  emits: {
    'update:popupVisible': (visible) => true,
    /**
     * @zh 文字气泡显示状态改变时触发
     * @en Emitted when the tooltip display status changes
     * @param {boolean} visible
     */
    'popupVisibleChange': (visible) => true,
  },
  setup(props, { emit }) {
    const prefixCls = 'tooltip';

    const _popupVisible = ref(props.defaultPopupVisible);
    const computedPopupVisible = computed(
        () => props.popupVisible ?? _popupVisible.value
    );

    const handlePopupVisibleChange = (visible) => {
      _popupVisible.value = visible;
      emit('update:popupVisible', visible);
      emit('popupVisibleChange', visible);
    };

    const contentCls = computed(() => [
      `${prefixCls}`,
      `${prefixCls}-content`,
      props.contentClass,
      { [`${prefixCls}-mini`]: props.mini },
    ]);

    const computedContentStyle = computed(() => {
      if (props.backgroundColor || props.contentStyle) {
        return {
          backgroundColor: props.backgroundColor,
          ...props.contentStyle,
        };
      }
      return undefined;
    });

    const arrowCls = computed(() => [
      `${prefixCls}-popup-arrow`,
      props.arrowClass,
    ]);

    const computedArrowStyle = computed(() => {
      if (props.backgroundColor || props.arrowStyle) {
        return {
          backgroundColor: props.backgroundColor,
          ...props.arrowStyle,
        };
      }
      return undefined;
    });

    return {
      prefixCls,
      computedPopupVisible,
      contentCls,
      computedContentStyle,
      arrowCls,
      computedArrowStyle,
      handlePopupVisibleChange,
    };
  },
});
</script>
<style lang="scss">
.tooltip {
  .tooltip-content {
    max-width: 350px;
    padding: 8px 12px;
    color: #fff;
    font-size: 14px;
    line-height: 1.5715;
    text-align: left;
    word-wrap: break-word;
    background-color: var(--color-tooltip-bg);
    border-radius: var(--border-radius-small);
  }

  .tooltip-mini {
    padding: 4px 12px;
    font-size: 14px;
  }

  &-popup-arrow.trigger-arrow {
    background-color: var(--color-tooltip-bg);
  }
}
</style>
