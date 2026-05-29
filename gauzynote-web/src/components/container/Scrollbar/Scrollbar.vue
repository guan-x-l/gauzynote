<script setup>
import {computed, nextTick, onMounted, onUnmounted, ref, useAttrs} from 'vue'
import ResizeObserver from '@/components/Resize/ResizeObserver.vue'
import {getSizeConfig} from "@/utils/index.js";

defineOptions({
  name: 'Scrollbar',
  inheritAttrs: false
})

// 组件对齐 Arco 的基础 API。
const props = defineProps({
  // 滚动条展示模式：embed 为悬浮条，track 为显示轨道。
  type: {
    type: String,
    default: 'embed'
  },
  // 透传给最外层容器的 class。
  outerClass: {
    type: [String, Array, Object],
    default: undefined
  },
  // 透传给最外层容器的 style。
  outerStyle: {
    type: [String, Array, Object],
    default: undefined
  },
  thumbSize: Number
})

// 对外抛出滚动事件。
const emit = defineEmits(['scroll'])
// 接收外部透传属性，手动控制 class/style 合并。
const attrs = useAttrs()

// 真实滚动容器，所有滚动行为都作用在这里。
const scrollRef = ref()
// hover 状态决定 embed 模式下滚动条是否显隐。
const isHovering = ref(false)
// 分轴记录拖拽态，避免横纵 thumb 同时进入激活样式。
const isDraggingX = ref(false)
const isDraggingY = ref(false)
// 可视区尺寸。
const viewportWidth = ref(0)
const viewportHeight = ref(0)
// 内容总滚动尺寸。
const scrollWidth = ref(0)
const scrollHeight = ref(0)
// 当前滚动位置。
const scrollLeftValue = ref(0)
const scrollTopValue = ref(0)

// thumb 的最小长度，避免内容很长时难以拖拽。
const MIN_THUMB_SIZE = 24
// thumb 的最大长度按轨道长度比例限制，避免容器较大时滚动条过长。
const MAX_THUMB_RATIO = 0.99
// 可见 thumb 的实际厚度。
const THUMB_SIZE = props.thumbSize || 8
// 整条滚动轨道占用的厚度。
const TRACK_SIZE = THUMB_SIZE + 4
// thumb 在轨道内居中时的偏移。
const THUMB_OFFSET = (TRACK_SIZE - THUMB_SIZE) / 2
// 双轴同时存在时，为右下角预留让位，避免横纵滚动条重叠。
const CORNER_OFFSET = TRACK_SIZE - THUMB_OFFSET + 2
// 拖拽时记录起始轴、指针位置和滚动位置。
const dragState = {
  axis: '',
  startPointer: 0,
  startScroll: 0
}

const isTrack = computed(() => props.type === 'track')
// 只有内容尺寸超过可视区时才显示对应方向滚动条。
const showHorizontal = computed(() => scrollWidth.value - viewportWidth.value > 1)
const showVertical = computed(() => scrollHeight.value - viewportHeight.value > 1)
// 双轴同时出现时，轨道长度需要给角落让位。
const horizontalTrackSize = computed(() => {
  return Math.max(0, viewportWidth.value - (showVertical.value ? CORNER_OFFSET : 0))
})
const verticalTrackSize = computed(() => {
  return Math.max(0, viewportHeight.value - (showHorizontal.value ? CORNER_OFFSET : 0))
})

// 根据可视区和内容比例计算 thumb 尺寸。
function getThumbSize(isVisible, viewportSize, contentSize, trackSize) {
  if (!isVisible || !contentSize) {
    return 0
  }
  const thumbSize = (viewportSize / contentSize) * trackSize
  const maxThumbSize = trackSize * MAX_THUMB_RATIO
  return Math.min(trackSize, Math.max(MIN_THUMB_SIZE, Math.min(maxThumbSize, thumbSize)))
}

// 将真实滚动位置映射为 thumb 在轨道中的偏移。
function getThumbOffset(scrollPosition, viewportSize, contentSize, trackSize, thumbSize) {
  const maxScroll = Math.max(0, contentSize - viewportSize)
  const maxThumbOffset = Math.max(0, trackSize - thumbSize)
  if (!maxScroll || !maxThumbOffset) {
    return 0
  }
  return (scrollPosition / maxScroll) * maxThumbOffset
}

const horizontalThumbSize = computed(() => (
  getThumbSize(showHorizontal.value, viewportWidth.value, scrollWidth.value, horizontalTrackSize.value)
))

const verticalThumbSize = computed(() => (
  getThumbSize(showVertical.value, viewportHeight.value, scrollHeight.value, verticalTrackSize.value)
))

const horizontalThumbOffset = computed(() => (
  getThumbOffset(
    scrollLeftValue.value,
    viewportWidth.value,
    scrollWidth.value,
    horizontalTrackSize.value,
    horizontalThumbSize.value
  )
))

const verticalThumbOffset = computed(() => (
  getThumbOffset(
    scrollTopValue.value,
    viewportHeight.value,
    scrollHeight.value,
    verticalTrackSize.value,
    verticalThumbSize.value
  )
))

// 组件需要接管 style/class，避免外部传入 overflow 后重新出现原生滚动条。
const rootAttrs = computed(() => {
  const nextAttrs = {...attrs}
  delete nextAttrs.class
  delete nextAttrs.style
  return nextAttrs
})

const rootClass = computed(() => [
  `scrollbar-${props.type}`,
  props.outerClass,
  attrs.class,
  {
    'scrollbar-has-horizontal': showHorizontal.value,
    'scrollbar-has-vertical': showVertical.value,
    'scrollbar-is-hovering': isHovering.value,
    'scrollbar-is-dragging-x': isDraggingX.value,
    'scrollbar-is-dragging-y': isDraggingY.value
  }
])

const rootStyle = computed(() => ([
  props.outerStyle,
  {
    '--scrollbar-track-size': `${TRACK_SIZE}px`,
    '--scrollbar-thumb-size': `${THUMB_SIZE}px`,
    '--scrollbar-thumb-offset': `${THUMB_OFFSET}px`,
    '--scrollbar-corner-offset': `${CORNER_OFFSET}px`,
  },
]))
const contentStyle = computed(() => ([
  attrs.style,
  isTrack.value ? {
    height: getSizeConfig(attrs.style.height).size + TRACK_SIZE + 'px',
    paddingRight: `${TRACK_SIZE}px`,
    paddingBottom: `${TRACK_SIZE}px`,
  } : undefined
]))

function updateMetrics() {
  if (!scrollRef.value) {
    return
  }
  viewportWidth.value = scrollRef.value.clientWidth
  viewportHeight.value = scrollRef.value.clientHeight
  scrollWidth.value = scrollRef.value.scrollWidth
  scrollHeight.value = scrollRef.value.scrollHeight
  scrollLeftValue.value = scrollRef.value.scrollLeft
  scrollTopValue.value = scrollRef.value.scrollTop
}

// 真实滚动发生后，同步内部状态并继续向外派发事件。
function handleScroll(event) {
  updateMetrics()
  emit('scroll', event)
}

// 对齐浏览器 scrollTo 调用方式，兼容 scrollTo(x, y) 和对象写法。
function scrollTo(options, y) {
  if (!scrollRef.value) {
    return
  }
  if (typeof options === 'number') {
    scrollRef.value.scrollTo(options, y ?? 0)
    return
  }
  scrollRef.value.scrollTo(options)
}

// 快捷滚动到指定纵向位置。
function scrollTop(top) {
  scrollRef.value?.scrollTo({
    top
  })
}

// 快捷滚动到指定横向位置。
function scrollLeft(left) {
  scrollRef.value?.scrollTo({
    left
  })
}

// 结束拖拽时统一清理状态和全局事件。
function stopDragging() {
  dragState.axis = ''
  isDraggingX.value = false
  isDraggingY.value = false
  window.removeEventListener('mousemove', handleThumbDrag)
  window.removeEventListener('mouseup', stopDragging)
}

// 统一读取不同方向下的指针、滚动和尺寸字段，减少横纵分支重复。
function getAxisMetrics(axis) {
  const isHorizontal = axis === 'x'
  return {
    pointerKey: isHorizontal ? 'clientX' : 'clientY',
    scrollKey: isHorizontal ? 'scrollLeft' : 'scrollTop',
    trackSize: isHorizontal ? horizontalTrackSize.value : verticalTrackSize.value,
    thumbSize: isHorizontal ? horizontalThumbSize.value : verticalThumbSize.value,
    viewportSize: isHorizontal ? viewportWidth.value : viewportHeight.value,
    contentSize: isHorizontal ? scrollWidth.value : scrollHeight.value
  }
}

// 根据拖拽位移换算真实滚动距离。
function handleThumbDrag(event) {
  if (!scrollRef.value || !dragState.axis) {
    return
  }

  const {pointerKey, scrollKey, trackSize, thumbSize, viewportSize, contentSize} = getAxisMetrics(dragState.axis)
  const delta = event[pointerKey] - dragState.startPointer
  const trackRange = Math.max(1, trackSize - thumbSize)
  const scrollRange = Math.max(0, contentSize - viewportSize)
  scrollRef.value[scrollKey] = dragState.startScroll + (delta / trackRange) * scrollRange
}

// 记录拖拽起点，并开始监听全局鼠标移动。
function startThumbDrag(axis, event) {
  if (!scrollRef.value) {
    return
  }
  const {pointerKey, scrollKey} = getAxisMetrics(axis)
  event.preventDefault()
  dragState.axis = axis
  dragState.startPointer = event[pointerKey]
  dragState.startScroll = scrollRef.value[scrollKey]
  isDraggingX.value = axis === 'x'
  isDraggingY.value = axis === 'y'
  window.addEventListener('mousemove', handleThumbDrag)
  window.addEventListener('mouseup', stopDragging)
}

// 点击轨道时，让 thumb 中心尽量对齐点击位置。
function scrollByTrack(axis, event) {
  if (!scrollRef.value) {
    return
  }

  const currentTarget = event.currentTarget
  const rect = currentTarget.getBoundingClientRect()
  const {pointerKey, scrollKey, trackSize, thumbSize, viewportSize, contentSize} = getAxisMetrics(axis)
  const positionKey = axis === 'x' ? 'left' : 'top'
  const trackRange = Math.max(1, trackSize - thumbSize)
  const clickOffset = event[pointerKey] - rect[positionKey] - thumbSize / 2
  const nextOffset = Math.min(Math.max(0, clickOffset), trackRange)
  const scrollRange = Math.max(0, contentSize - viewportSize)
  scrollRef.value[scrollKey] = (nextOffset / trackRange) * scrollRange
}

// 初次挂载后同步一次尺寸，后续由 ResizeObserver 组件负责监听变化。
onMounted(async () => {
  await nextTick()
  updateMetrics()
})

// 组件销毁时释放全局拖拽事件。
onUnmounted(() => {
  stopDragging()
})

// 暴露原生滚动控制能力，方便外部通过 ref 操作。
defineExpose({
  scrollTo,
  scrollTop,
  scrollLeft,
  update: updateMetrics,
  wrapRef: scrollRef
})
</script>

<template>
  <ResizeObserver @resize="updateMetrics">
    <div
      v-bind="rootAttrs"
      class="scrollbar"
      :class="rootClass"
      :style="rootStyle"
      @mouseenter="isHovering = true"
      @mouseleave="isHovering = false"
    >
      <div
        ref="scrollRef"
        class="scrollbar-content"
        :style="contentStyle"
        @scroll="handleScroll"
      >
        <ResizeObserver @resize="updateMetrics">
            <slot />
        </ResizeObserver>
      </div>

      <div
        v-if="showVertical"
        class="scrollbar-bar scrollbar-bar-y"
        @mousedown.self="scrollByTrack('y', $event)"
      >
        <div
          class="scrollbar-thumb scrollbar-thumb-y"
          :style="{
            height: `${verticalThumbSize}px`,
            transform: `translateY(${verticalThumbOffset}px)`
          }"
          @mousedown.stop="startThumbDrag('y', $event)"
        ></div>
      </div>

      <div
        v-if="showHorizontal"
        class="scrollbar-bar scrollbar-bar-x"
        @mousedown.self="scrollByTrack('x', $event)"
      >
        <div
          class="scrollbar-thumb scrollbar-thumb-x"
          :style="{
            width: `${horizontalThumbSize}px`,
            transform: `translateX(${horizontalThumbOffset}px)`
          }"
          @mousedown.stop="startThumbDrag('x', $event)"
        ></div>
      </div>
    </div>
  </ResizeObserver>
</template>

<style scoped lang="scss">
.scrollbar {
  position: relative;
  //width: 100%;
  //overflow: hidden;
}

.scrollbar-content {
  //width: 100%;
  //height: 100%;
  overflow: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.scrollbar-content::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

.scrollbar-content::-webkit-scrollbar-corner {
  display: none;
  background: transparent;
}

.scrollbar-bar {
  position: absolute;
  user-select: none;
  transition: opacity .1s ease;
  z-index: 100;
}

.scrollbar-bar-y {
  top: 0;
  right: 0;
  box-sizing: border-box;
  width: var(--scrollbar-track-size);
  height: 100%;
}

.scrollbar.scrollbar-has-horizontal .scrollbar-bar-y {
  bottom: var(--scrollbar-corner-offset);
}

.scrollbar-bar-x {
  bottom: 0;
  left: 0;
  box-sizing: border-box;
  width: 100%;
  height: var(--scrollbar-track-size);
}

/*.scrollbar.scrollbar-has-vertical .scrollbar-bar-x {
  right: var(--scrollbar-corner-offset);
}*/

.scrollbar-thumb {
  position: absolute;
  height: var(--scrollbar-thumb-size);
  border-radius: 6px;
  background-color: var(--color-neutral-4);
  transition: background-color .2s ease, opacity .2s ease;
}

.scrollbar-thumb:hover {
  background-color: var(--color-neutral-6);
}

.scrollbar-thumb-y {
  width: var(--scrollbar-thumb-size);
  top: 0;
  margin: 0 var(--scrollbar-thumb-offset);
}

.scrollbar-thumb-x {
  margin: var(--scrollbar-thumb-offset) 0;
}

.scrollbar-embed .scrollbar-bar {
  background-color: transparent;
  opacity: 0;
}

.scrollbar-embed .scrollbar-thumb {
  opacity: .75;
}

.scrollbar-embed.scrollbar-is-hovering .scrollbar-bar
, .scrollbar-embed.scrollbar-is-dragging-x .scrollbar-bar-x,
.scrollbar-embed.scrollbar-is-dragging-y .scrollbar-bar-y {
  opacity: 1;
}

.scrollbar-track .scrollbar-bar {
  background-color: var(--color-neutral-1);
  opacity: 1;
}


.scrollbar-track .scrollbar-thumb {
  opacity: 1;
}

/*.scrollbar-is-dragging-x .scrollbar-thumb-x,
.scrollbar-is-dragging-y .scrollbar-thumb-y {
  background-color: var(--color-neutral-6);
}*/
</style>
