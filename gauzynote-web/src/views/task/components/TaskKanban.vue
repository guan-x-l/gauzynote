<script setup>
import { ref, watch } from 'vue';
import draggable from 'vuedraggable';

/**
 * 看板列对象
 * @typedef {Object} KanbanColumn
 * @property {string} key - 状态值
 * @property {string} title - 列标题
 * @property {Array.<AssetTask>} items - 列内任务列表
 */

const props = defineProps({
  /** @type {import('vue').PropType<Array.<AssetTask>>} */
  tasks: Array,
  STATUS_MAP: Object,
  PRIORITY_MAP: Object
});

const emit = defineEmits(['openTask', 'sortEnd']);
const KANBAN_STATUS_KEYS = ['0', '1', '2', '3'];

/** @type {import('vue').Ref<Array.<KanbanColumn>>} */
// vuedraggable 会直接改动渲染数组，因此看板内部维护一份本地列数据并在 props 变化时同步。
const kanbanColumns = ref([]);

/**
 * 按看板排序值返回新数组，避免直接改动原任务列表。
 * @param {Array.<AssetTask>} [taskList=[]]
 * @returns {Array.<AssetTask>}
 */
function sortByKanban(taskList = []) {
  return [...taskList].sort((prevTask, nextTask) => {
    return (prevTask.kanbanSort ?? 0) - (nextTask.kanbanSort ?? 0);
  });
}
/**
 * 将任务列表按状态拆分为看板列数据。
 * @param {Array.<AssetTask>} [taskList=[]]
 * @returns {Array.<KanbanColumn>}
 */
function buildColumns(taskList = []) {
  return KANBAN_STATUS_KEYS.map(statusKey => ({
    key: statusKey,
    title: props.STATUS_MAP?.[statusKey]?.label || statusKey,
    items: sortByKanban(taskList.filter(task => task.status === statusKey)).map(task => ({ ...task }))
  }));
}

function syncColumns() {
  kanbanColumns.value = buildColumns(props.tasks || []);
}

function handleColumnChange(statusKey, event) {
  if (!event?.added?.element) {
    return;
  }
  event.added.element.status = statusKey;
}

function handleDragEnd(event) {
  if (!event || (event.oldIndex === event.newIndex && event.from === event.to)) {
    return;
  }
  emit('sortEnd', {
    columns: kanbanColumns.value.map(column => ({
      status: column.key,
      taskIds: column.items.map(task => task.taskId)
    }))
  });
}

function handleOpenTask(task) {
  emit('openTask', task);
}

watch(() => props.tasks, syncColumns, { immediate: true, deep: true });
</script>

<template>
  <div class="kanban-board">
    <div v-for="col in kanbanColumns" :key="col.key"
         :style="{'--background-color': STATUS_MAP[col.key].bgColor}"
         class="kanban-column flex flex-col">
      <div class="kanban-header flex items-center">
        <m-tag class="kanban-title" :color="STATUS_MAP[col.key].color">{{ col.title }}</m-tag>
        <span class="kanban-count">{{ col.items.length }}</span>
      </div>
      <Scrollbar class="flex flex-1"
                 :thumb-size="4"
                 style="width: 100%;display: flex;"
                 outer-style="overflow: hidden;">
        <draggable
          v-model="col.items"
          class="kanban-body flex-1"
          item-key="taskId"
          group="task-kanban"
          ghost-class="kanban-card-ghost"
          drag-class="kanban-card-drag"
          chosen-class="kanban-card-chosen"
          :animation="180"
          @change="handleColumnChange(col.key, $event)"
          @end="handleDragEnd"
        >
          <template #item="{ element: task }">
            <div class="kanban-card text-break" @click="handleOpenTask(task)">
              <div class="card-header card-title text-ellipsis-3-row">{{ task.taskTitle }}</div>
              <div class="card-content text-ellipsis-3-row">{{ task.taskContent || '暂无任务描述' }}</div>
              <div class="card-date">{{ task.startTime || '-' }} ~ {{ task.endTime || '-' }}</div>
              <m-tag size="small" :color="PRIORITY_MAP[task.priority]?.color" class="priority-tag">{{ PRIORITY_MAP[task.priority]?.label }}</m-tag>
              <div class="card-date">{{ task.actualCompletionTime || '-' }}</div>
            </div>
          </template>
        </draggable>
      </Scrollbar>
    </div>
  </div>
</template>

<style scoped>
.kanban-board {
  gap: 12px;
  overflow: hidden;
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
}

.kanban-column {
  --background-color: "";
  background: linear-gradient(var(--background-color) 0%, var(--color-fill-1) 100%);
  border-radius: var(--border-radius-large);
  min-height: 150px;
  max-height: 100%;
}

.kanban-header {
  padding: var(--spacing-6) var(--spacing-6) var(--spacing-3);
}

.kanban-title {
  border-radius: 99px;
}

.kanban-count {
  margin-left: var(--spacing-4);
  font-size: 12px;
}

.kanban-body{
  height: max-content;
  min-height: 100%;
  padding: var(--spacing-3) var(--spacing-6) var(--spacing-6);
}

.kanban-card {
  background-color: var(--color-bg-1);
  border: 1px solid var(--color-border-1);
  border-radius: var(--border-radius-large);
  padding: 12px;
  margin-bottom: 12px;
  font-size: 12px;
  color: var(--color-text-3);
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.kanban-card-chosen,
.kanban-card-drag {
  cursor: grabbing;
}

.kanban-card-ghost {
  opacity: 0.45;
  border-style: dashed;
}

.kanban-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}

.card-header {
  margin-bottom: 12px;
}
.card-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
}
.priority-tag{
  margin: var(--spacing-4) 0;
  border-radius: 99px;
}

.card-content {
  margin-bottom: 12px;
  color: var(--color-text-2);
}

.card-date {
  color: var(--color-text-3);
}
</style>
