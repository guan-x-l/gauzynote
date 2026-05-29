<script setup>
import LoadingWrapper from "@/components/feedback/LoadingWrapper/LoadingWrapper.vue";
import {Fragment, computed, defineComponent, onMounted, onUnmounted, ref, useSlots, nextTick} from "vue";
import {isFunction, isTextChildren, normalizeSize} from "@/utils/index.js";
import Scrollbar from "@/components/container/Scrollbar/Scrollbar.vue";

defineOptions({name: 'mTable'})
const props = defineProps({
  columns: Array,
  data: Array,
  rowKey: {
    type: [String, Function],
    default: 'key'
  },
  maxHeight: {
    type: [String, Number],
    default: undefined
  },
  loading: Boolean,
  alignCenter: Boolean,
  expand: Boolean,
  striped: Boolean,
  hoverAble: Boolean,
  bordered: {
    type: [Boolean, Object],
    default: false
  },
  draggable: {
    type: [Boolean, Object],
    default: false
  },
  resizable: {
    type: [Boolean, Object],
    default: false
  }
})
const emit = defineEmits(['SortStar', 'SortEnd', 'ColumnResizeStart', 'ColumnResize', 'ColumnResizeEnd'])
const slots = useSlots()

const expandOpenMap = ref({})
const tableHeader = ref()
const tableBody = ref()
const tableHeaderTable = ref()
let tableBodyResizeObserver = null
const hasScrollBar = ref(false)
const tableBodyHeight = ref(null)
const dragRowIndex = ref(-1)
const dragIndicatorRowIndex = ref(-1)
const dragIndicatorPlacement = ref('after')
const pressedRowIndex = ref(-1)
const isDragging = ref(false)
const dragLockedColumnWidths = ref([])
const columnWidthMap = ref({})
const resizingColumnKey = ref('')
const isColumnResizing = ref(false)
const resizeIndicatorLeft = ref(0)
const resizeStartX = ref(0)
const resizeStartWidth = ref(0)
const resizeMinWidth = ref(80)
const resizeMaxWidth = ref(Infinity)
const RenderVNodes = defineComponent({
  name: 'RenderVNodes',
  props: {
    vnodes: {
      type: [Array, Object],
      default: null,
    }
  },
  setup(renderProps) {
    return () => renderProps.vnodes
  }
})

/**
 * 递归拍平插槽节点，兼容 Fragment 场景。
 */
function flatVNodes(vNodes, result = []) {
  vNodes.filter(vNode => {
    return !isTextChildren(vNode)
  }).forEach(vNode => {
    if (!vNode) {
      return
    }
    if (Array.isArray(vNode)) {
      flatVNodes(vNode, result)
      return
    }
    if (vNode.type === Fragment && Array.isArray(vNode.children)) {
      flatVNodes(vNode.children, result)
      return
    }
    result.push(vNode)
  })
  return result
}

/**
 * 将 table-column 节点转换为 m-table 所需列配置。
 */
function resolveSlotColumns() {
  const slotNodes = slots.columns?.() || []
  const vNodeList = flatVNodes(slotNodes)
  return vNodeList
    .map((vNode, index) => {
      const columnProps = vNode.props || {}
      const dataIndex = columnProps.dataIndex || columnProps['data-index']
      const columnKey = columnProps.key || dataIndex || `__slot_column_${index}`
      const columnSlots = vNode.children || {}
      return {
        ...columnProps,
        width: columnProps.width,
        minWidth: columnProps.minWidth ?? columnProps['min-width'],
        align: columnProps.align,
        dataIndex,
        key: columnKey,
        label: columnProps.title ?? columnProps.label ?? columnKey,
        renderCell: columnSlots.cell,
      }
    })
    .filter(Boolean)
}

/**
 * 根据列配置读取默认单元格文本。
 */
function getCellValue(row, col) {
  const dataKey = col.dataIndex || col.key
  return dataKey ? row?.[dataKey] : undefined
}

/**
 * 归一化列配置，兼容 min-width 写法。
 */
function normalizeColumnConfig(columnConfig = {}) {
  return {
    ...columnConfig,
    minWidth: columnConfig.minWidth ?? columnConfig['min-width'],
  }
}

function parseWidthToNumber(widthValue) {
  if (typeof widthValue === 'number' && Number.isFinite(widthValue)) {
    return widthValue
  }
  if (typeof widthValue === 'string') {
    const parsedValue = parseFloat(widthValue)
    return Number.isFinite(parsedValue) ? parsedValue : undefined
  }
  return undefined
}

function clampColumnWidth(width, minWidth, maxWidth) {
  return Math.min(maxWidth, Math.max(minWidth, width))
}

function getColumnResizeKey(col, columnIndex) {
  return String(col?.key ?? col?.dataIndex ?? columnIndex)
}

function getStoredColumnWidth(col, columnIndex) {
  return columnWidthMap.value[getColumnResizeKey(col, columnIndex)]
}

function getColumnWidthValue(col, columnIndex) {
  const runtimeWidth = getStoredColumnWidth(col, columnIndex)
  return runtimeWidth ? `${runtimeWidth}px` : normalizeSize(col?.width)
}

function isResizingColumnCell(col, columnIndex) {
  return isColumnResizing.value && resizingColumnKey.value === getColumnResizeKey(col, columnIndex)
}

function getColumnCellClass(col, columnIndex) {
  return {
    'table-column-resizable': resizableConfig.value.enabled,
    'table-column-resize-active': isResizingColumnCell(col, columnIndex),
  }
}

/**
 * 计算列的宽度样式。
 */
function getColumnStyle(col, columnIndex) {
  const width = getColumnWidthValue(col, columnIndex)
  const minWidth = normalizeSize(col?.minWidth)
  if (isDragging.value && dragLockedColumnWidths.value.length) {
    const lockOffset = showDragHandleColumn.value ? 1 : 0
    const lockedWidth = dragLockedColumnWidths.value[columnIndex + lockOffset]
    if (lockedWidth) {
      return {
        width: lockedWidth,
        minWidth: lockedWidth,
        maxWidth: lockedWidth,
      }
    }
  }
  return {
    width,
    minWidth,
  }
}

/**
 * 计算列的文本对齐样式。
 */
function getColumnAlignStyle(col) {
  return {
    textAlign: col?.align || undefined
  }
}

/**
 * 统一列配置来源：优先使用 columns 插槽，其次回退到 props.columns。
 */
const renderColumns = computed(() => {
  const slotColumns = resolveSlotColumns()
  if (slotColumns.length) {
    return slotColumns.map(item => normalizeColumnConfig(item))
  }
  const propColumns = Array.isArray(props.columns) ? props.columns : []
  return propColumns.map(item => normalizeColumnConfig(item))
})

/**
 * 归一化拖拽配置，支持 `true` 与对象配置。
 */
const draggableConfig = computed(() => {
  const enabled = !!props.draggable
  if (!enabled) return { enabled: false, type: 'row', width: '40px' }

  const opts = props.draggable === true ? {} : props.draggable
  return {
    enabled: true,
    type: opts.type === 'handle' ? 'handle' : 'row',
    width: normalizeSize(opts.width) || '40px'
  }
})

const dragHandleWidth = computed(() => draggableConfig.value.width)
const isRowDragType = computed(() => draggableConfig.value.enabled && draggableConfig.value.type === 'row')
const showDragHandleColumn = computed(() => draggableConfig.value.enabled && draggableConfig.value.type === 'handle')
const resizableConfig = computed(() => {
  const enabled = !!props.resizable
  if (!enabled) {
    return {
      enabled: false,
      minWidth: 80,
      maxWidth: Infinity,
      handleWidth: '10px',
    }
  }
  const opts = props.resizable === true ? {} : props.resizable
  const minWidth = parseWidthToNumber(opts.minWidth)
  const maxWidth = parseWidthToNumber(opts.maxWidth)
  return {
    enabled: true,
    minWidth: minWidth ?? 80,
    maxWidth: maxWidth ?? Infinity,
    handleWidth: normalizeSize(opts.handleWidth) || '10px',
  }
})
const borderedConfig = computed(() => {
  const enabled = !!props.bordered
  if (!enabled) {
    return {
      wrapper: false,
      col: false,
      row: false,
    }
  }
  if (props.bordered === true) {
    return {
      wrapper: true,
      col: true,
      row: true,
    }
  }
  const opts = props.bordered || {}
  return {
    wrapper: !!opts.wrapper,
    col: !!opts.col,
    row: !!opts.row,
  }
})
const tableHeaderClass = computed(() => ({
  'table-bordered-wrapper': borderedConfig.value.wrapper,
  'table-bordered-col': borderedConfig.value.col,
  'table-bordered-row': borderedConfig.value.row,
}))
const tableBodyClass = computed(() => ({
  'table-bordered-wrapper': borderedConfig.value.wrapper,
  'table-bordered-col': borderedConfig.value.col,
  'table-bordered-row': borderedConfig.value.row,
}))

function getRowRenderKey(row, rowIndex, suffix = 'base') {
  let rowIdentity
  if (typeof props.rowKey === 'function') {
    rowIdentity = props.rowKey(row)
  } else {
    rowIdentity = row?.[props.rowKey]
  }
  return `${rowIdentity ?? rowIndex}-${suffix}`
}

function handleDragMouseDown(rowIndex) {
  if (!draggableConfig.value.enabled || draggableConfig.value.type !== 'row') {
    return
  }
  pressedRowIndex.value = rowIndex
}

function lockColumnWidths() {
  if (!tableHeaderTable.value) {
    return
  }
  const tableHeaders = tableHeaderTable.value.querySelectorAll('thead th')
  dragLockedColumnWidths.value = Array.from(tableHeaders).map(tableHeader => `${tableHeader.getBoundingClientRect().width}px`)
}

function handleDragStart(event, rowIndex) {
  if (!draggableConfig.value.enabled) {
    return
  }
  lockColumnWidths()
  dragRowIndex.value = rowIndex
  dragIndicatorRowIndex.value = rowIndex
  dragIndicatorPlacement.value = 'after'
  pressedRowIndex.value = rowIndex
  isDragging.value = true
  emit('SortStar', {oldIndex: rowIndex})
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(rowIndex))
  }
}

function createRowDragGhost(rowElement) {
  const rowRect = rowElement.getBoundingClientRect()
  const dragGhost = document.createElement('div')
  dragGhost.style.width = `${Math.round(rowRect.width)}px`
  dragGhost.style.height = `${Math.max(1, Math.round(rowRect.height))}px`
  dragGhost.style.background = 'rgba(0, 0, 0, 0.06)'
  dragGhost.style.boxSizing = 'border-box'
  dragGhost.style.position = 'fixed'
  dragGhost.style.pointerEvents = 'none'
  dragGhost.style.cursor = 'grabbing'
  document.body.appendChild(dragGhost)
  return dragGhost
}

function handleHandleDragStart(event, rowIndex) {
  handleDragStart(event, rowIndex)
  if (!event.dataTransfer) {
    return
  }
  const targetElement = event.currentTarget
  const rowElement = targetElement?.closest?.('tr')
  if (!rowElement) {
    return
  }
  const {left, top} = rowElement.getBoundingClientRect()
  const offsetX = Math.max(0, Math.round(event.clientX - left))
  const offsetY = Math.max(0, Math.round(event.clientY - top))
  const dragGhost = createRowDragGhost(rowElement)
  event.dataTransfer.setDragImage(dragGhost, offsetX, offsetY)
  setTimeout(() => {
    dragGhost.remove()
  }, 0)
}

function resolveDropPlacement(rowRect, hoverOffsetY) {
  // 为每行顶部插入增加触发缓冲，避免必须贴着边框才能触发。
  const topInsertBuffer = Math.max(8, Math.min(16, rowRect.height * 0.35))
  return hoverOffsetY <= topInsertBuffer ? 'before' : 'after'
}

function handleDragOver(event, rowIndex) {
  if (!isDragging.value) {
    return
  }
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  const currentRow = event.currentTarget
  const rowRect = currentRow.getBoundingClientRect()
  const hoverOffsetY = event.clientY - rowRect.top
  dragIndicatorRowIndex.value = rowIndex
  dragIndicatorPlacement.value = resolveDropPlacement(rowRect, hoverOffsetY)
}

function handleDrop(event, rowIndex) {
  if (!isDragging.value || dragRowIndex.value < 0 || !Array.isArray(props.data)) {
    return
  }
  event.preventDefault()

  const sourceIndex = dragRowIndex.value
  const indicatorIndex = rowIndex >= 0 ? rowIndex : dragIndicatorRowIndex.value
  if (indicatorIndex < 0) {
    handleDragEnd()
    return
  }

  const insertPosition = dragIndicatorPlacement.value === 'before' ? indicatorIndex : indicatorIndex + 1
  let targetIndex = Math.min(props.data.length, Math.max(0, insertPosition))
  if (sourceIndex < targetIndex) {
    targetIndex -= 1
  }
  emit('SortEnd', {
    oldIndex: sourceIndex,
    newIndex: targetIndex
  })
  handleDragEnd()
}

function handleDragEnd() {
  dragRowIndex.value = -1
  dragIndicatorRowIndex.value = -1
  dragIndicatorPlacement.value = 'after'
  pressedRowIndex.value = -1
  isDragging.value = false
  dragLockedColumnWidths.value = []
}

function handleWindowMouseUp() {
  if (!isDragging.value) {
    pressedRowIndex.value = -1
  }
}

function getHeaderCellResizeIndicatorLeft(headerCell) {
  if (!tableHeaderTable.value || !headerCell) {
    return 0
  }
  const tableRect = tableHeaderTable.value.getBoundingClientRect()
  const headerRect = headerCell.getBoundingClientRect()
  return headerRect.right - tableRect.left
}

function handleColumnResizeStart(event, col, columnIndex) {
  if (!resizableConfig.value.enabled) {
    return
  }
  event.preventDefault()
  event.stopPropagation()
  const headerCell = event.currentTarget?.closest?.('th')
  if (!headerCell) {
    return
  }
  const currentWidth = Math.round(headerCell.getBoundingClientRect().width)
  const columnMinWidth = parseWidthToNumber(col?.minWidth)
  const columnMaxWidth = parseWidthToNumber(col?.maxWidth)
  resizeStartX.value = event.clientX
  resizeStartWidth.value = currentWidth
  resizeMinWidth.value = columnMinWidth ?? resizableConfig.value.minWidth
  resizeMaxWidth.value = columnMaxWidth ?? resizableConfig.value.maxWidth
  resizingColumnKey.value = getColumnResizeKey(col, columnIndex)
  resizeIndicatorLeft.value = getHeaderCellResizeIndicatorLeft(headerCell)
  isColumnResizing.value = true
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', handleColumnResizeMove)
  window.addEventListener('mouseup', handleColumnResizeEnd)
  emit('ColumnResizeStart', {
    column: col,
    columnKey: resizingColumnKey.value,
    colIndex: columnIndex,
    width: currentWidth,
  })
}

function handleColumnResizeMove(event) {
  if (!isColumnResizing.value || !resizingColumnKey.value) {
    return
  }
  const offsetX = event.clientX - resizeStartX.value
  const nextWidth = clampColumnWidth(
    Math.round(resizeStartWidth.value + offsetX),
    resizeMinWidth.value,
    resizeMaxWidth.value
  )
  columnWidthMap.value = {
    ...columnWidthMap.value,
    [resizingColumnKey.value]: nextWidth
  }
  resizeIndicatorLeft.value += event.clientX - resizeStartX.value
  resizeStartX.value = event.clientX
  resizeStartWidth.value = nextWidth
  const columnIndex = renderColumns.value.findIndex((col, index) => getColumnResizeKey(col, index) === resizingColumnKey.value)
  const column = columnIndex >= 0 ? renderColumns.value[columnIndex] : undefined
  emit('ColumnResize', {
    column,
    columnKey: resizingColumnKey.value,
    colIndex: columnIndex,
    width: nextWidth,
  })
}

function handleColumnResizeEnd() {
  if (!isColumnResizing.value || !resizingColumnKey.value) {
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    window.removeEventListener('mousemove', handleColumnResizeMove)
    window.removeEventListener('mouseup', handleColumnResizeEnd)
    return
  }
  const columnKey = resizingColumnKey.value
  const width = columnWidthMap.value[columnKey] ?? resizeStartWidth.value
  const columnIndex = renderColumns.value.findIndex((col, index) => getColumnResizeKey(col, index) === columnKey)
  const column = columnIndex >= 0 ? renderColumns.value[columnIndex] : undefined
  window.removeEventListener('mousemove', handleColumnResizeMove)
  window.removeEventListener('mouseup', handleColumnResizeEnd)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  isColumnResizing.value = false
  resizingColumnKey.value = ''
  resizeIndicatorLeft.value = 0
  resizeStartWidth.value = 0
  emit('ColumnResizeEnd', {
    column,
    columnKey,
    colIndex: columnIndex,
    width,
  })
}

function watchTableBodyWidth() {
  tableBodyResizeObserver = new ResizeObserver((entries) => {
    hasScrollBar.value = entries[0].target.offsetWidth > entries[0].target.clientWidth
    if (tableHeader.value){
      tableBodyHeight.value = props.maxHeight - tableHeader.value.getBoundingClientRect().height
    }
  });
  tableBodyResizeObserver.observe(tableBody.value);
}

onMounted(() => {
  if (props.maxHeight && 'ResizeObserver' in window) {
    watchTableBodyWidth()
  }
  window.addEventListener('mouseup', handleWindowMouseUp)
})
onUnmounted(() => {
  window.removeEventListener('mousemove', handleColumnResizeMove)
  window.removeEventListener('mouseup', handleColumnResizeEnd)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  tableBodyResizeObserver?.unobserve && tableBody.value && tableBodyResizeObserver.unobserve(tableBody.value)
  window.removeEventListener('mouseup', handleWindowMouseUp)
})
</script>

<template>
  <div class="table-container">
    <loading-wrapper :loading="loading">
      <div class="table-header" ref="tableHeader"
           :class="[
             {'table-scroll-header': hasScrollBar && maxHeight},
             tableHeaderClass
           ]">
        <table class="table" :class="{'table-center': alignCenter}" ref="tableHeaderTable">
          <colgroup>
            <col v-if="showDragHandleColumn" :style="{width: dragHandleWidth, minWidth: dragHandleWidth}">
            <col v-for="(col, colIndex) in renderColumns" :key="col.key || col.dataIndex" :style="getColumnStyle(col, colIndex)">
          </colgroup>
          <thead>
          <tr>
            <th v-if="showDragHandleColumn"
                class="table-drag-th"
                :style="{width: dragHandleWidth, minWidth: dragHandleWidth}">
            </th>
            <th
              v-for="(col, colIndex) in renderColumns"
              :key="col.key || col.dataIndex"
              :class="getColumnCellClass(col, colIndex)"
              :style="getColumnStyle(col, colIndex)"
            >
<!--              <div>{{ col.label }}</div>-->
              <slot :name="`column-${col.key}`" :col="col">
                <div :style="getColumnAlignStyle(col)">{{ isFunction(col.label) ? col.label() : col.label }}</div>
              </slot>
              <span
                v-if="resizableConfig.enabled && colIndex < renderColumns.length - 1"
                class="table-column-resize-handle"
                :style="{width: resizableConfig.handleWidth}"
                @mousedown="handleColumnResizeStart($event, col, colIndex)"
              ></span>
            </th>
          </tr>
          </thead>
        </table>
      </div>
      <Scrollbar class="flex" :outer-style="`height:${tableBodyHeight+'px'};overflow: hidden;`">
        <div class="table-body" ref="tableBody"
             :class="tableBodyClass">
          <table class="table"
                 :class="{'table-loading':loading && !data.length, 'table-center': alignCenter, 'table-striped': striped, 'table-hoverAble': hoverAble}">
            <colgroup>
              <col v-if="showDragHandleColumn" :style="{width: dragHandleWidth, minWidth: dragHandleWidth}">
              <col v-for="(col, colIndex) in renderColumns" :key="col.key || col.dataIndex" :style="getColumnStyle(col, colIndex)">
            </colgroup>
            <tbody v-if="data.length">
            <template v-for="(row, rowIndex) in data" :key="getRowRenderKey(row, rowIndex, 'group')">
              <tr
                class="table-row"
                :class="{
                  'table-row-drag-active': dragRowIndex === rowIndex || pressedRowIndex === rowIndex,
                  'table-row-drop-indicator-bottom': isDragging && (
                    (dragIndicatorPlacement === 'after' && dragIndicatorRowIndex === rowIndex) ||
                    (dragIndicatorPlacement === 'before' && dragIndicatorRowIndex - 1 === rowIndex)
                  ),
                  'table-row-drop-indicator-top': isDragging && dragIndicatorPlacement === 'before' && dragIndicatorRowIndex === 0 && rowIndex === 0
                }"
                :draggable="isRowDragType"
                @mousedown="isRowDragType ? handleDragMouseDown(rowIndex) : undefined"
                @dragstart="isRowDragType ? handleHandleDragStart($event, rowIndex) : undefined"
                @dragover="handleDragOver($event, rowIndex)"
                @drop="handleDrop($event, rowIndex)"
                @dragend="handleDragEnd"
                @dblclick="expandOpenMap[rowIndex] = !expandOpenMap[rowIndex]"
              >
                <td v-if="showDragHandleColumn" class="table-drag-cell">
                  <span
                    class="table-drag-handle"
                    draggable="true"
                    @mousedown.stop="handleDragMouseDown(rowIndex)"
                    @dragstart="handleHandleDragStart($event, rowIndex)"
                    @dragend="handleDragEnd"
                  ><icon-drag-bot-vertical></icon-drag-bot-vertical></span>
                </td>
                <td
                  v-for="(col, colIndex) in renderColumns"
                  :key="col.key || col.dataIndex"
                  :class="getColumnCellClass(col, colIndex)"
                  :style="getColumnAlignStyle(col)"
                >
                  <slot :name="col.key" :row="row" :index="rowIndex" :record="row" :column="col" :rowIndex="rowIndex">
                    <render-v-nodes
                      v-if="col.renderCell"
                      :vnodes="col.renderCell({record: row, column: col, rowIndex})"
                    />
                    <template v-else>
                      {{ col.format ? col.format(row) : getCellValue(row, col) }}
                    </template>
                  </slot>
                </td>
              </tr>
              <tr class="table-row table-row-expand" v-if="expand && expandOpenMap[rowIndex]">
                <td :colspan="renderColumns.length + (showDragHandleColumn ? 1 : 0)">
                  <slot name="expand" :row="row" :index="rowIndex"></slot>
                </td>
              </tr>
            </template>
            </tbody>
            <tbody v-else-if="!loading">
            <tr class="table-row" style="text-align: center">
              <td :colspan="renderColumns.length + (showDragHandleColumn ? 1 : 0)">无数据</td>
            </tr>
            </tbody>
          </table>
        </div>
      </Scrollbar>
    </loading-wrapper>
  </div>
</template>

<style scoped lang="scss">
.table-container{
  overflow: hidden;
}

.table-header {
  background-color: var(--color-neutral-2);
  transition: color 350ms ease, background-color 350ms ease;
  //border-radius: 4px 4px 0 0;
  overflow: hidden;
  position: relative;
}

.table-scroll-header {
  overflow-y: scroll;
  &::-webkit-scrollbar {
    background-color: transparent;
  }
}

.table-body {
  //--table-body-max-height: auto;
  //max-height: var(--table-body-max-height);
  //overflow: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
  border: 0;
  table-layout: fixed;
  text-align: left;

  thead {
    tr {
      th {
        padding: var(--spacing-4) var(--spacing-8);
        position: relative;

        div {
          color: var(--color-text-2);
          font-size: .8em;
          text-align: left;
          transition: color 350ms ease, background-color 350ms ease;
        }
      }
    }
  }

  tbody {

    .table-row {
      td {
        padding: var(--spacing-4) var(--spacing-8);
        color: var(--color-text-1);
        font-size: .8em;
        transition: color 350ms ease, background-color 350ms ease;
        word-wrap: break-word;
        word-break: break-all;
      }

     /* &:last-of-type {
        td:first-child {
          border-radius: 0 0 0 4px;
        }

        td:last-child {
          border-radius: 0 0 4px 0;
        }
      }*/
    }

    .table-row-expand td {
      padding: var(--spacing-8) var(--spacing-8);
      border-bottom: 3px solid var(--color-border-2);
    }
  }
}

.table-column-resize-active {
  user-select: none;
}

.table-column-resize-handle {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 2;
  height: 100%;
  cursor: col-resize;
  transform: translateX(50%);
}

.table-header.table-bordered-col th.table-column-resize-active,
.table-body.table-bordered-col td.table-column-resize-active{
  border-right-color: rgb(var(--primary-6));
}

.table-striped .table-row:nth-child(2n) {
  background-color: var(--color-fill-1);
}

.table-hoverAble .table-row:hover {
  td {
    transition-duration: 150ms;
    background-color: var(--color-fill-1);
  }
}

.table-row-drag-active td {
  background-color: var(--color-primary-light-1);
}

.table-row-drop-indicator-bottom > td {
  box-shadow: inset 0 -2px 0 rgb(var(--primary-5));
}

.table-row-drop-indicator-top > td {
  box-shadow: inset 0 2px 0 rgb(var(--primary-5));
}

.table-drag-th,
.table-drag-cell {
  width: 40px;
  min-width: 40px;
  text-align: center;
}

.table-drag-handle {
  display: inline-block;
  cursor: grab;
  color: var(--color-text-3);
  user-select: none;
  font-weight: 600;
  letter-spacing: 1px;
}

.table-drag-handle:active {
  cursor: grabbing;
}

.table-header.table-bordered-wrapper {
  border-top: 1px solid var(--color-neutral-3);
  border-left: 1px solid var(--color-neutral-3);
  border-right: 1px solid var(--color-neutral-3);
}

.table-body.table-bordered-wrapper {
  border-left: 1px solid var(--color-neutral-3);
  border-right: 1px solid var(--color-neutral-3);
  border-bottom: 1px solid var(--color-neutral-3);
}

.table-header.table-bordered-col th,
.table-body.table-bordered-col td {
  border-right: 1px solid var(--color-neutral-3);
}

.table-header.table-bordered-col.table-bordered-wrapper th:last-child,
.table-body.table-bordered-col.table-bordered-wrapper td:last-child {
  border-right: 0;
}

.table-header.table-bordered-row th,
.table-body.table-bordered-row .table-row:not(.table-row-expand) td{
  border-bottom: 1px solid var(--color-neutral-3);
}

/*.table-header th:last-child,
.table-body td:last-child {
  border-right: 0;
}*/

/*.table-body .table-row:not(.table-row-expand):last-child td {
  border-bottom: 0;
}*/


.table-loading {
  min-height: 200px;
}

.table-center {
  thead > tr > th > div, tbody > tr > td {
    text-align: center;
  }
}

</style>
