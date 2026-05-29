<script>
import {
  cloneVNode,
  Comment,
  computed,
  defineComponent,
  h,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  Teleport,
  toRefs,
  Transition,
  watch
} from 'vue';

// --- 工具 ---
const isScrollElement = (element) => {
  return (
      element.scrollHeight > element.offsetHeight ||
      element.scrollWidth > element.offsetWidth
  );
};
const getScrollElements = (container) => {
  const scrollElements = [];
  let element = container;
  while (element && element !== document.documentElement) {
    if (isScrollElement(element)) {
      scrollElements.push(element);
    }
    element = element.parentElement ?? undefined;
  }
  return scrollElements;
};
const getTransformOrigin = (position) => {
  let originX = '0';
  if (['top', 'bottom'].includes(position)) {
    originX = '50%';
  } else if (['left', 'lt', 'lb', 'tr', 'br'].includes(position)) {
    originX = '100%';
  }
  let originY = '0';
  if (['left', 'right'].includes(position)) {
    originY = '50%';
  } else if (['top', 'tl', 'tr', 'lb', 'rb'].includes(position)) {
    originY = '100%';
  }
  return `${originX} ${originY}`;
};
// 定位计算
function calculatePosition(triggerEl, popupEl, position, offset = 0) {
  let top = 0;
  let left = 0;
  const triggerRect = triggerEl.getBoundingClientRect()
  const { width: tW, height: tH, top: tT, left: tL } = triggerRect;

  // 注意：这里我们需要基于视口(fixed)或者绝对定位(absolute + scrollY)
  // 为了兼容性，通常计算 absolute 位置时需要加上 window.scroll
  const scrollX = window.pageXOffset || document.documentElement.scrollLeft;
  const scrollY = window.pageYOffset || document.documentElement.scrollTop;
  switch (position) {
      // 上侧系列 - 弹窗整体在触发元素正上方
    case 'top':
      top = tT - popupEl.offsetHeight + scrollY - offset
      left =  tL  + tW/2 - popupEl.offsetWidth/2 + scrollX
      break;
      // 上左 - 弹窗在触发元素左上角对齐（上+左）
    case 'tl':
      top = tT - popupEl.offsetHeight + scrollY - offset
      left = tL + scrollX
      break;
      // 上右 - 弹窗在触发元素右上角对齐（上+右）
    case 'tr':
      top = tT - popupEl.offsetHeight + scrollY - offset
      left =  tL + tW - popupEl.offsetWidth + scrollX
      break;

      // 下侧系列 - 弹窗整体在触发元素正下方
    case 'bottom':
      top = tT  + tH + scrollY + offset;
      left = tL  + tW/2 - popupEl.offsetWidth/2 + scrollX
      break;
      // 下左 - 弹窗在触发元素左下角对齐（下+左）
    case 'bl':
      top = tT  + tH + scrollY + offset;
      left = tL + scrollX
      break;
      // 下右 - 弹窗在触发元素右下角对齐（下+右）
    case 'br':
      top = tT  + tH + scrollY + offset;
      left = tL + tW - popupEl.offsetWidth + scrollX
      break;

      // 左侧系列 - 弹窗整体在触发元素正左方
    case 'left':
      top = tT + (tH - popupEl.offsetHeight) / 2 + scrollY;
      left = tL - popupEl.offsetWidth + scrollX - offset;
      break;
      // 左上 - 弹窗在触发元素左上方对齐（左+上）
    case 'lt':
      top = tT + scrollY
      left = tL - popupEl.offsetWidth + scrollX - offset;
      break;
      // 左下 - 弹窗在触发元素左下方对齐（左+下）
    case 'lb':
      top = tT + scrollY - popupEl.offsetHeight + tH
      left = tL - popupEl.offsetWidth + scrollX - offset;
      break;

      // 右侧系列 - 弹窗整体在触发元素正右方
    case 'right':
      top = tT + (tH - popupEl.offsetHeight) / 2 + scrollY;
      left = tL + tW + scrollX + offset;
      break;
      // 右上 - 弹窗在触发元素右上方对齐（右+上）
    case 'rt':
      top = tT + scrollY
      left = tL + tW + scrollX + offset;
      break;
      // 右下 - 弹窗在触发元素右下方对齐（右+下）
    case 'rb':
      top = tT + scrollY - popupEl.offsetHeight + tH
      left = tL + tW + scrollX + offset;
      break;

      // 默认兜底 - 保持原有逻辑，默认展示在下方居中
    default:
      top = tT  + tH + offset + scrollY;
      left = tL  + tW/2 - popupEl.offsetWidth/2 + scrollX
  }
  // 可视区域宽高，用于边界判断
  const clientW = document.documentElement.clientWidth;
  const clientH = document.documentElement.clientHeight;

  // 边界防溢出修正：保证弹窗始终在可视区域内
  // top = Math.max(scrollY, Math.min(top, scrollY + clientH - popupEl.offsetHeight));
  left = Math.max(scrollX, Math.min(left, scrollX + clientW - popupEl.offsetWidth));

  return { top: `${top}px`, left: `${left}px` };
}
const getArrowStyle = (triggerEl, popupEl, position, {customStyle = {}}) => {
  const triggerRect = triggerEl?.getBoundingClientRect()
  if (!triggerRect) return undefined;
  const { width: tW, height: tH, top: tT, left: tL } = triggerRect;
  const scrollX = window.pageXOffset || document.documentElement.scrollLeft;
  const scrollY = window.pageYOffset || document.documentElement.scrollTop;
  if (['top', 'tl', 'tr', 'bottom', 'bl', 'br'].includes(position)) {
    let offsetLeft = Math.abs(
        tL - popupEl.offsetLeft + tW / 2
    );

    if (offsetLeft > popupEl.offsetWidth - 8) {
      if (triggerRect.width > popupEl.offsetWidth) {
        offsetLeft = popupEl.offsetWidth / 2;
      } else {
        offsetLeft = popupEl.offsetWidth - 8;
      }
    }

    if (['top', 'tl', 'tr'].includes(position)) {
      return {
        left: `${offsetLeft}px`,
        bottom: '0',
        transform: 'translate(-50%,50%) rotate(45deg)',
        ...customStyle,
      };
    }
    return {
      left: `${offsetLeft}px`,
      top: '0',
      transform: 'translate(-50%,-50%) rotate(45deg)',
      ...customStyle,
    };
  }
  let offsetTop = Math.abs(
    tT - popupEl.offsetTop + tH / 2
  );

  if (offsetTop > popupEl.offsetHeight - 8) {
    if (triggerRect.height > popupEl.offsetHeight) {
      offsetTop = popupEl.offsetHeight / 2;
    } else {
      offsetTop = popupEl.offsetHeight - 8;
    }
  }

  if (['left', 'lt', 'lb'].includes(position)) {
    return {
      top: `${offsetTop}px`,
      right: '0',
      transform: 'translate(50%,-50%) rotate(45deg)',
      ...customStyle,
    };
  }
  return {
    top: `${offsetTop}px`,
    left: '0',
    transform: 'translate(-50%,-50%) rotate(45deg)',
    ...customStyle,
  };
};

// --- 3. 组件定义 ---
export default defineComponent({
  name: 'Trigger',
  props: {
    popupVisible: {
      type: Boolean,
      default: undefined
    },
    defaultPopupVisible: {
      type: Boolean,
      default: false
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
     * @type {import('vue').PropType.<'hover' | 'click' | 'focus'>}
     */
    trigger: {
      type: [String, Array],
      default: 'hover'
    },
    /**
     * @type {import('vue').PropType.<'top' | 'tl' | 'tr' | 'bottom' | 'bl' | 'br' | 'left' | 'lt' | 'lb' | 'right' | 'rt' | 'rb'>}
     */
    position: {
      type: String,
      default: 'bottom'
    },
    /**
     * 弹出框的偏移量（弹出框距离触发器的偏移距离）
     */
    popupOffset: {
      type: Number,
      default: 10
    },
    /**
     * mouseenter事件延时触发的时间（毫秒）
     */
    mouseEnterDelay: {
      type: Number,
      default: 100
    },
    /**
     * mouseleave事件延时触发的时间（毫秒）
     */
    mouseLeaveDelay: {
      type: Number,
      default: 100
    },
    /**
     * 是否在点击外部区域时关闭弹出框
     */
    clickOutsideToClose: {
      type: Boolean,
      default: true
    },
    /**
     * 弹出动画的name
     */
    animationName: {
      type: String,
      default: 'fade-in-out'
    },
    /**
     * 是否挂载在 body 元素下
     */
    renderToBody: {
      type: Boolean,
      default: true
    },
    /**
     * 弹出框是否显示箭头
     */
    showArrow: {
      type: Boolean,
      default: false
    },
    /**
     * 弹出框内容的类名
     */
    contentClass: [String, Array, Object],
    /**
     * 弹出框内容的样式
     */
    contentStyle: Object,
    /**
     * 弹出框箭头的类名
     */
    arrowClass: [String, Array, Object],
    /**
     * 弹出框箭头的样式
     */
    arrowStyle: Object,
    /**
     * @zh 弹出框的挂载容器
     * @en Mount container for popup
     */
    popupContainer: {
      type: [String, Object],
    },
  },
  emits: ['update:popupVisible', 'popupVisibleChange'],
  setup(props, { slots, emit, attrs }) {
    const { popupContainer } = toRefs(props);
    const visible = ref(props.defaultPopupVisible);
    const popupRef = ref(null);
    const arrowRef = ref(null);
    const triggerNodeRef = ref(null); // 触发元素DOM
    const popupStyle = ref({});
    const popupWrapperStyle = ref({});
    const arrowStyle = ref({});

    // 状态管理
    let delayTimer = null;
    let outsideListenerAdded = false;
    // 存储所有已绑定的滚动父级元素，以便 cleanup
    let scrollableParents = [];

    const computedVisible = computed(() => {
      return props.popupVisible !== undefined ? props.popupVisible : visible.value;
    });

    const triggerMethods = computed(() => [].concat(props.trigger));

    const updatePopupStyle = () => {
      updatePosition()
      const triggerEl = triggerNodeRef.value;
      const popupEl = popupRef.value;
      // const triggerRect = triggerEl.getBoundingClientRect();
      // const popupRect = popupEl.getBoundingClientRect();
      if (props.showArrow) {
        nextTick(() => {
          arrowStyle.value = getArrowStyle(triggerEl, popupEl, props.position,{customStyle: props.arrowStyle});
        });
      }
    }

    // --- 位置更新核心逻辑 ---
    const updatePosition = () => {
      if (!computedVisible.value || !popupRef.value || !triggerNodeRef.value) return;

      const triggerEl = triggerNodeRef.value;
      const popupEl = popupRef.value;
      // const triggerRect = triggerEl.getBoundingClientRect();
      // const popupRect = popupEl.getBoundingClientRect();

      const style = calculatePosition(triggerEl, popupEl, props.position, props.popupOffset);
      const transformOrigin =  getTransformOrigin(props.position)
      popupStyle.value = {
        position: 'absolute',
        zIndex: 2000,
        ...style,
      };
      popupWrapperStyle.value = {
        transformOrigin,
      };
    };


    // --- 事件监听管理 (滚动 & Resize) ---
    // 绑定监听器
    const bindScrollEvents = () => {
      if (!triggerNodeRef.value) return;

      // 1. 获取所有滚动的父级元素
      const scrollElements = getScrollElements(triggerNodeRef.value);

      // 2. 遍历绑定
      scrollElements.forEach((el) => {
        // 使用 passive: true 优化滚动性能
        el.addEventListener('scroll', updatePopupStyle, { passive: true });
        scrollableParents.push(el);
      });

      // 3. Window 的滚动和缩放也必须监听
      window.addEventListener('scroll', updatePopupStyle, { passive: true });
      window.addEventListener('resize', updatePopupStyle, { passive: true });
    };

    // 解绑监听器
    const unbindScrollEvents = () => {
      scrollableParents.forEach((el) => {
        el.removeEventListener('scroll', updatePopupStyle);
      });
      scrollableParents = []; // 清空引用

      window.removeEventListener('scroll', updatePopupStyle);
      window.removeEventListener('resize', updatePopupStyle);
    };

    // --- 切换可见性逻辑 ---
    const changeVisible = (newVisible, delay = 0) => {
      if (newVisible === computedVisible.value && !delayTimer) return;

      const update = () => {
        visible.value = newVisible;
        emit('update:popupVisible', newVisible);
        emit('popupVisibleChange', newVisible);

        if (newVisible) {
          // DOM 更新后计算位置并绑定事件
          nextTick(() => {
            updatePopupStyle();
            // 避免重复绑定
            unbindScrollEvents();
            bindScrollEvents();
          });
        } else {
          // 隐藏时解绑事件，节省性能
          unbindScrollEvents();
        }
      };

      if (delayTimer) clearTimeout(delayTimer);
      if (delay > 0) {
        delayTimer = setTimeout(update, delay);
      } else {
        update();
      }
    };

    // --- 交互事件处理 ---
    const handleMouseEnter = (e) => {
      if (props.disabled) return
      if (triggerMethods.value.includes('hover')) {
        changeVisible(true, props.mouseEnterDelay);
      }
      attrs.onMouseenter?.(e);
    };

    const handleMouseLeave = (e) => {
      if (props.disabled) return
      if (triggerMethods.value.includes('hover')) {
        changeVisible(false, props.mouseLeaveDelay);
      }
      attrs.onMouseleave?.(e);
    };

    const handleClick = (e) => {
      if (props.disabled) return
      if (triggerMethods.value.includes('click')) {
        changeVisible(!computedVisible.value);
      }
      attrs.onClick?.(e);
    };

    const handleFocus = (e) => {
      if (props.disabled) return
      if (triggerMethods.value.includes('focus')) changeVisible(true);
      attrs.onFocus?.(e);
    };

    const handleBlur = (e) => {
      if (props.disabled) return
      if (triggerMethods.value.includes('focus')) changeVisible(false);
      attrs.onBlur?.(e);
    };

    // --- Click Outside ---
    const handleOutsideClick = (e) => {
      const target = e.target;
      if (
          triggerNodeRef.value?.contains(target) ||
          popupRef.value?.contains(target)
      ) {
        return;
      }
      changeVisible(false);
    };

    // --- Watchers & Lifecycle ---
    watch(computedVisible, (val) => {
      if (props.clickOutsideToClose) {
        if (val) {
          if (!outsideListenerAdded) {
            // 延迟添加，防止触发按钮的点击事件冒泡直接触发 outside click
            setTimeout(() => {
              document.addEventListener('click', handleOutsideClick);
              outsideListenerAdded = true;
            }, 0);
          }
        } else {
          if (outsideListenerAdded) {
            document.removeEventListener('click', handleOutsideClick);
            outsideListenerAdded = false;
          }
          // 确保隐藏时解绑滚动事件
          unbindScrollEvents();
        }
      }
    });

    onMounted(() => {
      if (computedVisible.value) {
        nextTick(() => {
          updatePopupStyle();
          bindScrollEvents();
        });
      }
    });

    onBeforeUnmount(() => {
      if (outsideListenerAdded) {
        document.removeEventListener('click', handleOutsideClick);
      }
      unbindScrollEvents();
    });

    return () => {
      const children = slots.default?.() || [];
      // 1. 劫持 Trigger 元素
      let triggerVNode = null;
      if (children.length > 0) {
        // 过滤注释节点，拿到第一个有效子节点
        let firstChild = children.find(c => c.type !== Comment) || children[0];
        // 兼容 teleport(v-fgt) 和 Fragment(v-frag) 碎片节点（保留，兜底）
        firstChild = firstChild.type === Symbol.for('v-fgt') ? firstChild.children[0] : firstChild;
        firstChild = firstChild.type === Symbol.for('v-frag') ? firstChild.children[0] : firstChild;
        const originalRef = firstChild.ref;
        const mergedRef = (target) => {
          triggerNodeRef.value = target?.$el ? target.$el : target || null
          // 兼容原有ref逻辑
          if (typeof originalRef === 'function') originalRef(target);
          else if (originalRef && typeof originalRef === 'object') originalRef.value = target;
        };
        triggerVNode = cloneVNode(firstChild, {
          // ...attrs,
          ref: mergedRef,
          onClick: handleClick,
          onMouseenter: handleMouseEnter,
          onMouseleave: handleMouseLeave,
          onFocus: handleFocus,
          onBlur: handleBlur
        });
      }

      // 2. Popup 内容
      let popupContent = null;
      if (computedVisible.value || slots.content) {
        popupContent = h(
            'div',
            {
              ref: popupRef,
              class: ['trigger-popup', `trigger-popup-position-${props.position}`, attrs.class],
              style: [popupStyle.value],
              onMouseenter: handleMouseEnter, // Hover 保持显示
              onMouseleave: handleMouseLeave
            },
            h(
                'div',
                {
                  class: ['trigger-popup-wrapper'],
                  style: [popupWrapperStyle.value],
                },
                h(
                    'div',
                    {
                      class: ['trigger-content', props.contentClass],
                      style: [props.contentStyle]
                    },
                    slots.content?.()
                ),
                props.showArrow ? h(
                    'div',
                    {
                      ref: arrowRef,
                      class: ['trigger-arrow', props.arrowClass],
                      style: [arrowStyle.value]
                    }
                ): null
                // {props.showArrow && (
                //       <div
                //           ref={arrowRef}
                //           class={[`${prefixCls}-arrow`, props.arrowClass]}
                //           style={arrowStyle.value}
                //       />
                //   )}
            ),
        );
      }

      return [
        triggerVNode,
        h(
            Teleport,
            { to: popupContainer.value || 'body', disabled: !props.renderToBody },
            h(
                Transition,
                { name: props.animationName, appear: true }, () => (computedVisible.value ? popupContent : null),
            )
        )
      ];
    };
  }
});
</script>

<style scoped lang="scss">
@import '@/assets/style/animation.css';
/* 弹出层基础样式 */
.trigger-popup {
  position: absolute;
}
.trigger-popup-wrapper{
}
.trigger-arrow{
  position: absolute;
  z-index: -1;
  display: block;
  box-sizing: border-box;
  width: 8px;
  height: 8px;
  background-color: var(--color-bg-5);
  content: "";
}
.trigger-popup.trigger-popup-position-top .trigger-arrow,
.trigger-popup.trigger-popup-position-tl .trigger-arrow,
.trigger-popup.trigger-popup-position-tr .trigger-arrow {
  border-top: none;
  border-left: none;
  border-bottom-right-radius: var(--border-radius-small);
}

.trigger-popup.trigger-popup-position-bottom .trigger-arrow,
.trigger-popup.trigger-popup-position-bl .trigger-arrow,
.trigger-popup.trigger-popup-position-br .trigger-arrow {
  border-right: none;
  border-bottom: none;
  border-top-left-radius: var(--border-radius-small);
}

.trigger-popup.trigger-popup-position-left .trigger-arrow,
.trigger-popup.trigger-popup-position-lt .trigger-arrow,
.trigger-popup.trigger-popup-position-lb .trigger-arrow {
  border-bottom: none;
  border-left: none;
  border-top-right-radius: var(--border-radius-small);
}

.trigger-popup.trigger-popup-position-right .trigger-arrow,
.trigger-popup.trigger-popup-position-rt .trigger-arrow,
.trigger-popup.trigger-popup-position-rb .trigger-arrow {
  border-top: none;
  border-right: none;
  border-bottom-left-radius: var(--border-radius-small);
}
</style>
