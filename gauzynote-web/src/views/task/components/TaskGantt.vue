<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import Split from '@/components/container/Split/Split.vue';
import Scrollbar from '@/components/container/Scrollbar/Scrollbar.vue';
import {dateFormat, isTextEllipsis} from "@/utils/index.js";
import dayjs from "@/utils/lib/dayjs.js";
import ResizeObserver from "@/components/Resize/ResizeObserver.vue";

const DAY_WIDTH = 54;
const TIMELINE_BOUNDARY_MONTHS = 6;
const GANTT_SPLIT_DEFAULT_SIZE = '200px';
const GANTT_COLUMNS = [
  { key: 'taskTitle', label: '任务', width1: 120 },
  { key: 'status', label: '状态', width1: 80 },
  { key: 'priority', label: '优先级' }
];

/**
 * 甘特图任务对象
 * @typedef {AssetTask & {
 *   startDate: Date | null,
 *   endDate: Date | null,
 *   offset: number,
 *   duration: number,
 *   hasSchedule: boolean
 * }} GanttTask
 */

/**
 * 甘特图日期列
 * @typedef {Object} GanttDateCell
 * @property {string} key - 日期唯一标识
 * @property {Date} date - 日期对象
 * @property {number} day - 日期
 * @property {boolean} isWeekend - 是否周末
 */

/**
 * 甘特图月份列
 * @typedef {Object} GanttMonthCell
 * @property {string} key - 月份唯一标识
 * @property {Date} date - 月份开始日期
 * @property {number} dayCount - 当前范围内该月份可见天数
 */

const props = defineProps({
  /** @type {import('vue').PropType<Array.<AssetTask>>} */
  tasks: Array,
  STATUS_MAP: Object,
  PRIORITY_MAP: Object,
  viewMaxHeight: Number
});
const emit = defineEmits(['openTask', 'updateTask', 'sortEnd', 'sortStar']);

const splitSize = ref(GANTT_SPLIT_DEFAULT_SIZE);
const leftTableRef = ref();
const rightMonthHeaderRef = ref();
const rightDateHeaderRef = ref();
const rightBodyRef = ref();
const hoveredTaskId = ref(null);
const hoveredBarTaskId = ref(null);
const activeTaskId = ref(null);
const isSplitMoving = ref(false);
const resizingTaskId = ref(null);
const resizePreviewLeft = ref(null);
const resizePreviewWidth = ref(null);
const resizePreviewDuration = ref(null);
const resizePreviewStartDate = ref(null);
const resizePreviewEndDate = ref(null);
let leftTableBodyElement = null;
let isSyncingFromLeft = false;
let isSyncingFromRight = false;
let hasInitializedTimelineScroll = false;
let resizeState = null;

function parseTaskDate(dateValue) {
  if (!dateValue) {
    return null;
  }
  const date = new Date(dateValue);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  date.setHours(0, 0, 0, 0);
  return date;
}

function addDays(baseDate, dayCount) {
  const date = new Date(baseDate);
  date.setDate(date.getDate() + dayCount);
  return date;
}

function addMonths(baseDate, monthCount) {
  return dayjs(baseDate).add(monthCount, 'month').startOf('day').toDate();
}

function diffDays(startDate, endDate) {
  return Math.floor((endDate - startDate) / (1000 * 60 * 60 * 24));
}

function getTodayDate() {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return today;
}

function collectTaskDates(taskList = []) {
  return taskList.flatMap(task => {
    const startDate = parseTaskDate(task.startTime);
    const endDate = parseTaskDate(task.endTime);
    return [startDate, endDate].filter(Boolean);
  });
}

function createTimelineRange(taskList = []) {
  const today = getTodayDate();
  const taskDates = collectTaskDates(taskList);

  if (!taskDates.length) {
    const startDate = addMonths(today, -TIMELINE_BOUNDARY_MONTHS);
    const endDate = addMonths(today, TIMELINE_BOUNDARY_MONTHS);
    return {
      startDate,
      totalDays: diffDays(startDate, endDate) + 1
    };
  }

  const sortedDates = [...taskDates].sort((prevDate, nextDate) => prevDate - nextDate);
  const startDate = addMonths(sortedDates[0], -TIMELINE_BOUNDARY_MONTHS);
  const latestReferenceDate = new Date(Math.max(today.getTime(), sortedDates[sortedDates.length - 1].getTime()));
  const endDate = addMonths(latestReferenceDate, TIMELINE_BOUNDARY_MONTHS);

  return {
    startDate,
    totalDays: diffDays(startDate, endDate) + 1
  };
}

function getLeftTableBodyElement() {
  return leftTableRef.value?.$el?.querySelector?.('.table-body') || null;
}

function getRightBodyWrapElement() {
  const wrapRef = rightBodyRef.value?.wrapRef;
  return wrapRef?.value ?? wrapRef ?? null;
}

function setActiveTask(_taskId) {
  return false;
  //  行内编辑需要高亮（待定）
  // activeTaskId.value = taskId ?? null;
}

function handleOpenTask(task) {
  emit('openTask', task);
}

function handleTaskTitleClick(task) {
  setActiveTask(task?.taskId);
  handleOpenTask(task);
}

function handleRightRowEnter(taskId) {
  hoveredTaskId.value = taskId;
}

function handleRightRowLeave() {
  if (resizeState) {
    return;
  }
  hoveredTaskId.value = null;
}

function handleSortEnd(payload) {
  emit('sortEnd', payload);
}

function handleSortStar(payload) {
  emit('sortStar', payload);
}

function handleSplitMoveStart() {
  isSplitMoving.value = true;
}

function handleSplitMoveEnd() {
  isSplitMoving.value = false;
}

function syncVerticalScrollFromLeft() {
  const rightBodyWrapElement = getRightBodyWrapElement();
  if (!leftTableBodyElement || !rightBodyWrapElement || isSyncingFromRight) {
    return;
  }
  isSyncingFromLeft = true;
  rightBodyRef.value?.scrollTop(leftTableBodyElement.scrollTop);
  requestAnimationFrame(() => {
    isSyncingFromLeft = false;
  });
}

function syncScrollFromRight(event) {
  const rightBodyWrapElement = getRightBodyWrapElement() || event?.target;
  if (!rightBodyWrapElement) {
    return;
  }
  syncRightHeaderScroll(rightBodyWrapElement.scrollLeft);
  if (leftTableBodyElement && !isSyncingFromLeft) {
    isSyncingFromRight = true;
    leftTableBodyElement.scrollTop = rightBodyWrapElement.scrollTop;
    requestAnimationFrame(() => {
      isSyncingFromRight = false;
    });
  }
}

function getLeftTableRows() {
  if (!leftTableBodyElement) {
    return [];
  }
  return Array.from(leftTableBodyElement.querySelectorAll('tbody > tr.table-row'));
}

function syncLeftRowStateClasses() {
  const rowElements = getLeftTableRows();
  rowElements.forEach((rowElement, rowIndex) => {
    const task = ganttTasks.value[rowIndex];
    const isHovered = !!task && hoveredTaskId.value === task.taskId;
    const isActive = !!task && activeTaskId.value === task.taskId;
    rowElement.classList.toggle('gantt-left-row-hover', isHovered);
    rowElement.classList.toggle('gantt-left-row-active', isActive);
  });
}

function resolveTaskByLeftRowEvent(event) {
  if (!leftTableBodyElement) {
    return null;
  }
  const rowElement = event.target?.closest?.('tr.table-row');
  if (!rowElement || !leftTableBodyElement.contains(rowElement)) {
    return null;
  }
  const rowIndex = getLeftTableRows().indexOf(rowElement);
  return rowIndex >= 0 ? ganttTasks.value[rowIndex] : null;
}

function handleLeftTableMouseMove(event) {
  const task = resolveTaskByLeftRowEvent(event);
  hoveredTaskId.value = task?.taskId ?? null;
}

function handleLeftTableMouseLeave() {
  if (resizeState) {
    return;
  }
  hoveredTaskId.value = null;
}

function handleLeftTableClick(event) {
  if (resizeState) {
    return;
  }
  if (event.target?.closest?.('.table-drag-handle')) {
    return;
  }
  const task = resolveTaskByLeftRowEvent(event);
  if (task) {
    setActiveTask(task.taskId);
  }
}

function handleDocumentClick(event) {
  if (resizeState) {
    return;
  }
  const clickTarget = event.target;
  const isInsideLeftTable = !!leftTableRef.value?.$el?.contains?.(clickTarget);
  const isInsideGanttRight = !!clickTarget?.closest?.('.gantt-right');
  if (isInsideLeftTable || isInsideGanttRight) {
    return;
  }
  setActiveTask(null);
}

function bindScrollSync() {
  const nextLeftTableBody = getLeftTableBodyElement();
  if (leftTableBodyElement === nextLeftTableBody) {
    syncLeftRowStateClasses();
    return;
  }
  if (leftTableBodyElement) {
    leftTableBodyElement.removeEventListener('scroll', syncVerticalScrollFromLeft);
    leftTableBodyElement.removeEventListener('mousemove', handleLeftTableMouseMove);
    leftTableBodyElement.removeEventListener('mouseleave', handleLeftTableMouseLeave);
    leftTableBodyElement.removeEventListener('click', handleLeftTableClick);
  }
  leftTableBodyElement = nextLeftTableBody;
  if (leftTableBodyElement) {
    leftTableBodyElement.addEventListener('scroll', syncVerticalScrollFromLeft, { passive: true });
    leftTableBodyElement.addEventListener('mousemove', handleLeftTableMouseMove, { passive: true });
    leftTableBodyElement.addEventListener('mouseleave', handleLeftTableMouseLeave, { passive: true });
    leftTableBodyElement.addEventListener('click', handleLeftTableClick);
  }
  syncLeftRowStateClasses();
}

function unbindScrollSync() {
  if (leftTableBodyElement) {
    leftTableBodyElement.removeEventListener('scroll', syncVerticalScrollFromLeft);
    leftTableBodyElement.removeEventListener('mousemove', handleLeftTableMouseMove);
    leftTableBodyElement.removeEventListener('mouseleave', handleLeftTableMouseLeave);
    leftTableBodyElement.removeEventListener('click', handleLeftTableClick);
    leftTableBodyElement = null;
  }
}

const sortedTasks = computed(() => {
  return [...(props.tasks || [])].sort((prevTask, nextTask) => {
    if ((prevTask.ganttSort ?? 0) !== (nextTask.ganttSort ?? 0)) {
      return (prevTask.ganttSort ?? 0) - (nextTask.ganttSort ?? 0);
    }
    return (prevTask.taskId ?? 0) - (nextTask.taskId ?? 0);
  });
});

const timelineBounds = computed(() => createTimelineRange(sortedTasks.value));
const timelineStartDate = computed(() => timelineBounds.value.startDate);
const timelineTotalDays = computed(() => timelineBounds.value.totalDays);

function getTimelineEndDate() {
  return addDays(timelineStartDate.value, timelineTotalDays.value - 1);
}

function syncRightHeaderScroll(scrollLeft) {
  if (rightMonthHeaderRef.value) {
    rightMonthHeaderRef.value.scrollLeft = scrollLeft + 66;
  }
  if (rightDateHeaderRef.value) {
    rightDateHeaderRef.value.scrollLeft = scrollLeft;
  }
}

function clampDateToTimeline(targetDate) {
  const normalizedDate = parseTaskDate(targetDate);
  if (!normalizedDate) {
    return null;
  }
  if (normalizedDate < timelineStartDate.value) {
    return timelineStartDate.value;
  }
  const endDate = getTimelineEndDate();
  if (normalizedDate > endDate) {
    return endDate;
  }
  return normalizedDate;
}

function getViewportAnchorDate(baseStartDate = timelineStartDate.value, totalDays = timelineTotalDays.value) {
  const rightBodyWrapElement = getRightBodyWrapElement();
  if (!rightBodyWrapElement) {
    return getTodayDate();
  }
  const centerOffset = Math.round(
    (rightBodyWrapElement.scrollLeft + rightBodyWrapElement.clientWidth / 2 - DAY_WIDTH / 2) / DAY_WIDTH
  );
  const clampedOffset = Math.max(0, Math.min(totalDays - 1, centerOffset));
  return addDays(baseStartDate, clampedOffset);
}

function scrollTimelineToDate(targetDate) {
  const normalizedDate = parseTaskDate(targetDate);
  const rightBodyWrapElement = getRightBodyWrapElement();
  if (!normalizedDate || !rightBodyWrapElement) {
    return;
  }

  const targetDateInRange = clampDateToTimeline(normalizedDate);
  const dateOffset = diffDays(timelineStartDate.value, targetDateInRange);
  const targetScrollLeft = Math.max(0, dateOffset * DAY_WIDTH - (rightBodyWrapElement.clientWidth / 2) + (DAY_WIDTH / 2));
  rightBodyRef.value?.scrollLeft(targetScrollLeft);
  syncRightHeaderScroll(targetScrollLeft);
}

function ensureInitialTimelineScroll() {
  if (hasInitializedTimelineScroll || !getRightBodyWrapElement()) {
    return;
  }
  scrollTimelineToDate(getTodayDate());
  hasInitializedTimelineScroll = true;
}

function handleScrollToToday() {
  scrollTimelineToDate(getTodayDate());
}

function formatTaskDate(date) {
  return date ? dayjs(date).format('YYYY-MM-DD') : null;
}

function getTaskDisplayedDuration(task) {
  if (resizingTaskId.value === task.taskId && resizePreviewDuration.value != null) {
    return resizePreviewDuration.value;
  }
  return task.duration;
}

function getTaskBarStyle(task) {
  const left = resizingTaskId.value === task.taskId && resizePreviewLeft.value != null
    ? `${resizePreviewLeft.value}px`
    : `calc(var(--gantt-day-width) * ${task.offset})`;
  const width = resizingTaskId.value === task.taskId && resizePreviewWidth.value != null
    ? `${resizePreviewWidth.value}px`
    : `calc(var(--gantt-day-width) * ${getTaskDisplayedDuration(task)})`;
  return {
    left,
    width,
    background: props.STATUS_MAP?.[task.status]?.color,
    '--bar-border-color': props.STATUS_MAP?.[task.status]?.bColor
  };
}

function clearResizeState() {
  resizeState = null;
  resizingTaskId.value = null;
  resizePreviewLeft.value = null;
  resizePreviewWidth.value = null;
  resizePreviewDuration.value = null;
  resizePreviewStartDate.value = null;
  resizePreviewEndDate.value = null;
  document.removeEventListener('mousemove', handleTaskResizeMove);
  document.removeEventListener('mouseup', handleTaskResizeEnd);
  document.body.style.removeProperty('user-select');
  document.body.style.removeProperty('cursor');
}

function handleTaskBarEnter(taskId) {
  hoveredBarTaskId.value = taskId;
}

function handleTaskBarLeave(taskId) {
  if (resizingTaskId.value === taskId) {
    return;
  }
  if (hoveredBarTaskId.value === taskId) {
    hoveredBarTaskId.value = null;
  }
}

function handleTaskResizeStart(task, edge, event) {
  if (!task?.hasSchedule || task.duration <= 0 || !task.startDate || !task.endDate) {
    return;
  }
  const initialLeft = task.offset * DAY_WIDTH;
  const initialWidth = task.duration * DAY_WIDTH;
  const maxDuration = Math.max(1, timelineTotalDays.value - task.offset);
  resizeState = {
    taskId: task.taskId,
    task,
    edge,
    startX: event.clientX,
    initialLeft,
    initialWidth,
    initialOffset: task.offset,
    initialDuration: task.duration,
    initialStartDate: task.startDate,
    initialEndDate: task.endDate,
    maxDuration,
    maxWidth: maxDuration * DAY_WIDTH,
    maxLeft: initialLeft + initialWidth - DAY_WIDTH
  };
  resizingTaskId.value = task.taskId;
  resizePreviewLeft.value = initialLeft;
  resizePreviewWidth.value = initialWidth;
  resizePreviewDuration.value = task.duration;
  resizePreviewStartDate.value = task.startDate;
  resizePreviewEndDate.value = task.endDate;
  hoveredBarTaskId.value = task.taskId;
  hoveredTaskId.value = task.taskId;
  document.addEventListener('mousemove', handleTaskResizeMove);
  document.addEventListener('mouseup', handleTaskResizeEnd);
  document.body.style.userSelect = 'none';
  document.body.style.cursor = 'ew-resize';
}

function handleTaskResizeMove(event) {
  if (!resizeState) {
    return;
  }
  if (resizeState.edge === 'left') {
    const nextLeft = Math.max(
      0,
      Math.min(resizeState.maxLeft, resizeState.initialLeft + event.clientX - resizeState.startX)
    );
    const nextWidth = resizeState.initialWidth - (nextLeft - resizeState.initialLeft);
    const nextOffset = Math.round(nextLeft / DAY_WIDTH);
    const nextDuration = Math.max(
      1,
      resizeState.initialDuration - (nextOffset - resizeState.initialOffset)
    );
    resizePreviewLeft.value = nextLeft;
    resizePreviewWidth.value = nextWidth;
    resizePreviewDuration.value = nextDuration;
    resizePreviewStartDate.value = addDays(
      resizeState.initialStartDate,
      nextOffset - resizeState.initialOffset
    );
    resizePreviewEndDate.value = resizeState.initialEndDate;
    return;
  }

  const nextWidth = Math.max(
    DAY_WIDTH,
    Math.min(resizeState.maxWidth, resizeState.initialWidth + event.clientX - resizeState.startX)
  );
  const nextDuration = Math.max(
    1,
    Math.min(resizeState.maxDuration, Math.round(nextWidth / DAY_WIDTH))
  );
  resizePreviewLeft.value = resizeState.initialLeft;
  resizePreviewWidth.value = nextWidth;
  resizePreviewDuration.value = nextDuration;
  resizePreviewStartDate.value = resizeState.initialStartDate;
  resizePreviewEndDate.value = addDays(
    resizeState.initialEndDate,
    nextDuration - resizeState.initialDuration
  );
}

function handleTaskResizeEnd() {
  if (!resizeState) {
    return;
  }
  const { task, edge, initialDuration, initialStartDate, initialEndDate } = resizeState;
  const previewDuration = resizePreviewDuration.value ?? initialDuration;
  const previewStartDate = resizePreviewStartDate.value ?? initialStartDate;
  const previewEndDate = resizePreviewEndDate.value ?? initialEndDate;
  if (previewDuration === initialDuration) {
    clearResizeState();
    return;
  }
  emit('updateTask', {
    ...task,
    startTime: formatTaskDate(edge === 'left' ? previewStartDate : initialStartDate),
    endTime: formatTaskDate(edge === 'right' ? previewEndDate : initialEndDate)
  });
  clearResizeState();
}

function handleGanttBarContentResize(e){
  const ellipsis = isTextEllipsis(e.target.querySelector('.gantt-bar-title'));
  if (ellipsis){
    let clientRect = e.target.getBoundingClientRect()
      e.target.querySelector('.gantt-bar-label-outer').style.left = clientRect.width + 20 + 'px'
      e.target.querySelector('.gantt-bar-label-outer').style.display = 'block'
  } else {
    e.target.querySelector('.gantt-bar-label-outer').style.display = 'none'
  }
}

const timelineRange = computed(() => ({
  startDate: timelineStartDate.value,
  totalDays: timelineTotalDays.value
}));

/** @type {import('vue').ComputedRef<Array.<GanttDateCell>>} */
const ganttDates = computed(() => {
  const { startDate, totalDays } = timelineRange.value;
  return Array.from({ length: totalDays }, (_, dayIndex) => {
    const date = addDays(startDate, dayIndex);
    return {
      key: date.toISOString(),
      date,
      day: date.getDate(),
      isWeekend: date.getDay() === 0 || date.getDay() === 6
    };
  });
});

/** @type {import('vue').ComputedRef<Array.<GanttMonthCell>>} */
const ganttMonths = computed(() => {
  /** @type {Array.<GanttMonthCell>} */
  const months = [];
  ganttDates.value.forEach((dateCell) => {
    const monthKey = dateFormat(dateCell.date, 'YYYY-MM');
    const lastMonth = months[months.length - 1];
    if (lastMonth?.key === monthKey) {
      lastMonth.dayCount += 1;
      return;
    }
    months.push({
      key: monthKey,
      date: dateCell.date,
      dayCount: 1
    });
  });
  return months;
});

/** @type {import('vue').ComputedRef<Array.<GanttTask>>} */
const ganttTasks = computed(() => {
  const { startDate, totalDays } = timelineRange.value;
  return sortedTasks.value.map(task => {
    const startDateValue = parseTaskDate(task.startTime);
    const endDateValue = parseTaskDate(task.endTime);
    const hasSchedule = !!(startDateValue && endDateValue);

    if (!hasSchedule) {
      return {
        ...task,
        startDate: startDateValue,
        endDate: endDateValue,
        offset: 0,
        duration: 0,
        hasSchedule: false
      };
    }

    const normalizedStart = startDateValue <= endDateValue ? startDateValue : endDateValue;
    const normalizedEnd = startDateValue <= endDateValue ? endDateValue : startDateValue;
    const rawOffset = diffDays(startDate, normalizedStart);
    const rawEndOffset = diffDays(startDate, normalizedEnd);
    const visibleStart = Math.max(0, rawOffset);
    const visibleEnd = Math.min(totalDays - 1, rawEndOffset);

    return {
      ...task,
      startDate: normalizedStart,
      endDate: normalizedEnd,
      offset: visibleStart,
      duration: Math.max(0, visibleEnd - visibleStart + 1),
      hasSchedule: visibleEnd >= 0 && visibleStart < totalDays
    };
  });
});

const timelineStyle = computed(() => {
  const startDow = timelineRange.value.startDate.getDay();
  return {
    width: `${ganttDates.value.length * DAY_WIDTH}px`,
    minWidth: `${ganttDates.value.length * DAY_WIDTH}px`,
    '--gantt-day-width': `${DAY_WIDTH}px`,
    '--gantt-weekend-offset-x': `${-startDow * DAY_WIDTH}px`
  };
});

const todayOffset = computed(() => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const offset = diffDays(timelineRange.value.startDate, today);
  if (offset < 0 || offset >= timelineRange.value.totalDays) {
    return -1;
  }
  return offset;
});

onMounted(() => {
  nextTick(bindScrollSync);
  document.addEventListener('click', handleDocumentClick);
});

watch(sortedTasks, () => {
  nextTick(() => {
    bindScrollSync();
    ensureInitialTimelineScroll();
    syncScrollFromRight();
  });
}, { deep: true, immediate: true });

watch(
  () => [timelineStartDate.value.getTime(), timelineTotalDays.value],
  async (_, oldValue) => {
    if (!getRightBodyWrapElement() || !oldValue) {
      return;
    }
    const previousAnchorDate = hasInitializedTimelineScroll
      ? getViewportAnchorDate(new Date(oldValue[0]), oldValue[1])
      : getTodayDate();
    await nextTick();
    if (!getRightBodyWrapElement()) {
      return;
    }
    scrollTimelineToDate(previousAnchorDate);
  }
);

watch([hoveredTaskId, activeTaskId], () => {
  nextTick(syncLeftRowStateClasses);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick);
  unbindScrollSync();
  clearResizeState();
});
</script>

<template>
  <div class="gantt-view">
<!--    <Scrollbar class="flex" :outer-style="`height:${viewMaxHeight+'px'};overflow: hidden;`">-->
<!--    <div v-if="ganttTasks.length" class="gantt-main" :style="`height:${viewMaxHeight+'px'};height: 100%`">-->
    <div v-if="ganttTasks.length" class="gantt-main" style="height: 100%">
      <Split
        class="gantt-split"
        direction="horizontal"
        v-model:size="splitSize"
        min1="320px"
        max1="720px"
        @move-start="handleSplitMoveStart"
        @move-end="handleSplitMoveEnd"
      >
        <template #first>
          <div class="gantt-left">
            <m-table
              ref="leftTableRef"
              :data="ganttTasks"
              row-key="taskId"
              :draggable="{ type: 'handle', width: 40 }"
              resizable
              :bordered="{col: true}"
              @sort-end="handleSortEnd"
              @sort-star="handleSortStar"
            >
              <template #columns>
                <table-column title="任务" data-index="taskTitle" :width="GANTT_COLUMNS[0].width">
                  <template #cell="{ record }">
                    <div class="gantt-table-title" @click.stop="handleTaskTitleClick(record)">
                      <div class="gantt-table-title__text text-ellipsis">{{ record.taskTitle }}</div>
                    </div>
                  </template>
                </table-column>
<!--                <table-column title="状态" data-index="status" :width="GANTT_COLUMNS[1].width">
                  <template #cell="{ record }">
                    <m-tag size="small" :color="STATUS_MAP[record.status]?.color">{{ STATUS_MAP[record.status]?.label || '-' }}</m-tag>
                  </template>
                </table-column>-->
<!--                <table-column title="优先级" data-index="priority" :width="GANTT_COLUMNS[2].width">
                  <template #cell="{ record }">
                    <m-tag size="small" :color="PRIORITY_MAP[record.priority]?.color" class="priority-tag">
                      {{ PRIORITY_MAP[record.priority]?.label || '-' }}
                    </m-tag>
                  </template>
                </table-column>-->
              </template>
            </m-table>
          </div>
        </template>

        <template #second>
          <div class="gantt-right" :class="{ 'is-split-moving': isSplitMoving }">
            <div class="gantt-right-header">
              <div class="gantt-right-header-top flex items-center">
                <div ref="rightMonthHeaderRef" class="gantt-right-header-scroll">
                  <div class="gantt-timeline-month-header flex" :style="timelineStyle">
                    <div
                      v-for="monthCell in ganttMonths"
                      :key="monthCell.key"
                      class="gantt-month-cell"
                      :style="{ width: monthCell.dayCount * DAY_WIDTH + 'px' }"
                    >
                      <span class="month-text">{{ `${dateFormat(monthCell.date, "YYYY年M月")}` }}</span>
                    </div>
                  </div>
                </div>
<!--                <divider direction="vertical" />-->
                <div class="gantt-right-header-actions flex">
                  <m-button type="text" size="mini" @click="handleScrollToToday">今天</m-button>
                </div>
              </div>

              <div class="gantt-right-header-bottom">
                <div ref="rightDateHeaderRef" class="gantt-right-header-scroll">
                  <div class="gantt-timeline-date-header" :style="timelineStyle">
                    <div
                      v-for="dateCell in ganttDates"
                      :key="dateCell.key"
                      class="gantt-cell-header flex items-center justify-center"
                      :class="{ 'is-weekend': dateCell.isWeekend}"
                    >
                      <span class="date-text">{{ dateCell.day }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <Scrollbar ref="rightBodyRef" class="gantt-right-body" @scroll="syncScrollFromRight">
              <div class="gantt-right-body-content">
                <div
                  v-for="task in ganttTasks"
                  :key="task.taskId"
                  class="gantt-timeline-row"
                  :class="{
                    'is-hovered': hoveredTaskId === task.taskId,
                    'is-active': activeTaskId === task.taskId,
                  }"
                  :style="timelineStyle"
                  @click="setActiveTask(task.taskId)"
                  @mouseenter="handleRightRowEnter(task.taskId)"
                  @mouseleave="handleRightRowLeave"
                >
                  <resize-observer @resize="handleGanttBarContentResize">
                  <div
                    v-if="task.hasSchedule && task.duration > 0"
                    class="gantt-bar"
                    @dblclick.stop="handleTaskTitleClick(task)"
                    :class="{
                      'is-handle-visible': hoveredBarTaskId === task.taskId || resizingTaskId === task.taskId,
                      'is-resizing': resizingTaskId === task.taskId
                    }"
                    :style="getTaskBarStyle(task)"
                    @mouseenter="handleTaskBarEnter(task.taskId)"
                    @mouseleave="handleTaskBarLeave(task.taskId)"
                  >
                    <button type="button" class="gantt-bar-resize-handle is-left"
                      @mousedown.stop.prevent="handleTaskResizeStart(task, 'left', $event)"
                    >||</button>

                      <div class="gantt-bar-content">
                        <span class="gantt-bar-label gantt-bar-title text-ellipsis">{{ task.taskTitle }}</span>
                        <span class="gantt-bar-label gantt-bar-label-outer">{{ task.taskTitle }}</span>
                        <span class="gantt-bar-label flex-shrink-0">{{ getTaskDisplayedDuration(task) }}天</span>
                      </div>


                    <button type="button" class="gantt-bar-resize-handle is-right"
                      @mousedown.stop.prevent="handleTaskResizeStart(task, 'right', $event)"
                    >||</button>
                  </div>
                  </resize-observer>
                </div>
                <div
                    v-if="todayOffset >= 0"
                    class="gantt-today-line"
                    :style="{ left: `calc(${DAY_WIDTH}px * ${todayOffset} + (${DAY_WIDTH}px / 2))` }"
                ></div>
              </div>
            </Scrollbar>
          </div>
        </template>
      </Split>
    </div>
    <div v-else class="gantt-empty">
      <div class="gantt-empty-title">暂无任务</div>
    </div>
<!--    </Scrollbar>-->
  </div>
</template>

<style scoped lang="scss">
.gantt-view {
}

.gantt-main {
  min-width: 100%;
  border: 1px solid var(--color-border-1);
  border-radius: var(--border-radius-large);
}

.gantt-split {
  height: 100%;
}

.gantt-right {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: rgba(255, 255, 255, 0.9);
}

.gantt-right.is-split-moving {
  pointer-events: none;
}

.gantt-right-header {
  padding-top: 4px;
  box-shadow: inset 0 -1px 0 var(--color-border-2);
  background: #fff;
}

.gantt-right-header-scroll {
  overflow: hidden;
}

.gantt-right-body {
  flex: 1;
  min-height: 0;
}

.gantt-right-body-content {
  position: relative;
  min-height: 100%;
}

.gantt-timeline-date-header,
.gantt-timeline-row {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(var(--gantt-day-width), var(--gantt-day-width)));
}

.gantt-timeline-month-header {
  height: 26px;
  position: relative;
}
.gantt-right-header-actions{
  padding: 0 var(--spacing-4);
}


.gantt-timeline-date-header {
  min-height: 38px;
}

/*
.gantt-date-cell {
  width: var(--gantt-day-width);
  min-width: var(--gantt-day-width);
  box-sizing: border-box;
  border-right: 1px solid var(--color-border-2);
}
*/

.gantt-month-cell {
  padding: 0 8px;
}

.gantt-cell-header {
  padding: 8px 0;
}

.month-text {
  font-size: 12px;
  line-height: 1;
  color: var(--color-text-3);
  position: sticky;
  left: 8px;
}

.date-text {
  font-size: 12px;
  color: var(--color-text-1);
}

.gantt-timeline-row {
  min-height: 40px;
  align-items: stretch;
  background-color: rgba(255, 255, 255, 0.78);
  background-image:
    repeating-linear-gradient(
      to right,
      transparent 0,
      transparent calc(var(--gantt-day-width) - 1px),
      var(--color-border-2) calc(var(--gantt-day-width) - 1px),
      var(--color-border-2) var(--gantt-day-width)
    ),
    repeating-linear-gradient(
      to right,
      rgba(var(--gray-2), 0.5) 0,
      rgba(var(--gray-2), 0.5) var(--gantt-day-width),
      transparent var(--gantt-day-width),
      transparent calc(var(--gantt-day-width) * 6),
      rgba(var(--gray-2), 0.5) calc(var(--gantt-day-width) * 6),
      rgba(var(--gray-2), 0.5) calc(var(--gantt-day-width) * 7)
    );
  background-position: 0 0, var(--gantt-weekend-offset-x) 0;
  background-repeat: repeat;
  cursor: pointer;
  transition: color 350ms ease, background-color 350ms ease;
}

.gantt-timeline-row:hover {
  background-color: rgba(248, 250, 252, 0.92);
}

.gantt-timeline-row.is-hovered {
  background-color: var(--color-fill-2);
}

.gantt-timeline-row.is-active {
  background-color: rgba(219, 234, 254, 0.96);
}

.gantt-bar {
  position: absolute;
  top: 50%;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  transform: translateY(-50%);
  border-top: 2px solid transparent;
  border-bottom: 2px solid transparent;
  border-radius: var(--border-radius-large);
  z-index: 1;
  //overflow: hidden;
}

.gantt-bar-content {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  line-height: 1;
}

.gantt-bar-label {
  font-size: 12px;
}
.gantt-bar-label-outer{
  position: absolute;
  display: none;
  right: -100%;
  white-space: nowrap;
}

.gantt-bar-resize-handle {
  width: 12px;
  height: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  background: var(--bar-border-color);
  font-size: 12px;
  line-height: 1;
  user-select: none;
  opacity: 0;
  //transition: opacity 120ms ease;
}

.gantt-bar-resize-handle.is-left{
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
}
.gantt-bar-resize-handle.is-right{
  border-top-right-radius: 6px;
  border-bottom-right-radius: 6px;
}
.gantt-bar-resize-handle.is-left,
.gantt-bar-resize-handle.is-right{
  cursor: ew-resize;
}

.gantt-bar.is-handle-visible{
  border-color: var(--bar-border-color);
}
.gantt-bar.is-handle-visible .gantt-bar-resize-handle,
.gantt-bar.is-resizing .gantt-bar-resize-handle {
  opacity: 1;
}

.gantt-today-line {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background-color: rgb(var(--primary-5));
  transform: translateX(-0.5px);
  z-index: 2;
  pointer-events: none;
  &::after{
    content: "";
    position: absolute;
    left: 50%;
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background-color: rgb(var(--primary-5));
    transform: translateX(-50%);
  }
}

.priority-tag {
  border-radius: 999px;
}

.gantt-table-title {
  cursor: pointer;
}

.gantt-table-title__text {
  font-size: 14px;
  color: var(--color-text-1);
}

.gantt-empty {
  padding: 56px 24px;
  text-align: center;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.6) 0%, rgba(255, 255, 255, 0.92) 100%);
}

.gantt-empty-title {
  font-size: 14px;
  color: var(--color-text-1);
}

.gantt-left :deep(.table-header) {
  background-color: #fff;
  border-radius: 0;

  & th {
  height: 68px;
  padding-top: 0;
  padding-bottom: 0;
  //border-right: 1px solid var(--color-border-2);
  }
  //& th > div{
  //  color: var(--color-text-1);
  //}
  & th.table-drag-th{
    border: 0;
  }
}

.gantt-left :deep(.table-body) {
  overflow-x: hidden;
  .table-row td{
    height: 40px;
    padding-top: 0;
    padding-bottom: 0;
    //vertical-align: middle;
    //border-right: 1px solid var(--color-border-2);
  }

  .table-row td.table-drag-cell{
    border: 0;
  }
}

.gantt-left :deep(.table-body .table-row.gantt-left-row-hover td) {
  background-color: var(--color-fill-2);
}

.gantt-left :deep(.table-body .table-row.gantt-left-row-active td) {
  background-color: rgba(219, 234, 254, 0.96);
}
</style>
