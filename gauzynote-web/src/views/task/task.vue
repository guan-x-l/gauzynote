<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { Message } from "@/components/index.js";
import TaskKanban from "./components/TaskKanban.vue";
import TaskList from "./components/TaskList.vue";
import TaskGantt from "./components/TaskGantt.vue";
import { addAssetTask, getAssetTaskList, updateAssetTask, updateAssetTaskSort } from "@/api/assetTask.js";
import Scrollbar from "@/components/container/Scrollbar/Scrollbar.vue";

const currentView = ref('gantt');

// 视图区域动态高度计算
const taskPageRef = ref(null);
const filtersRef = ref(null);
const viewContentWrapperRef = ref(null);
const viewMaxHeight = ref(0);
let _resizeObserver = null;

function calcViewMaxHeight() {
  if (!taskPageRef.value || !filtersRef.value || !viewContentWrapperRef.value) return;
  const taskPageRect = taskPageRef.value.getBoundingClientRect();
  const viewContentWrapperRect = viewContentWrapperRef.value.getBoundingClientRect();
  const filtersRect = filtersRef.value.getBoundingClientRect();
  viewMaxHeight.value = Math.max(0, taskPageRect.height - viewContentWrapperRect.height - filtersRect.height - 64);
}

// 任务状态配置 0: 未开始, 1: 进行中, 2: 已完成, 3: 已停滞
const STATUS_MAP = {
  '0': { label: '未开始', color: 'rgb(var(--arcoblue-2))', bColor: 'rgb(var(--arcoblue-3))', bgColor: 'rgba(var(--arcoblue-1), .5)' },
  '1': { label: '进行中', color: 'rgb(var(--orange-2))', bColor: 'rgb(var(--orange-3))', bgColor: 'rgba(var(--orange-1), .5)' },
  '2': { label: '已完成', color: 'rgb(var(--green-2))', bColor: 'rgb(var(--green-3))', bgColor: 'rgba(var(--green-1), .5)' },
  '3': { label: '已停滞', color: 'rgb(var(--red-2))', bColor: 'rgb(var(--red-3))', bgColor: 'rgba(var(--red-1), .5)' },
};

// 优先级配置P:0, 1, 2
const PRIORITY_MAP = {
  '0': { label: 'P0', color: 'rgb(var(--red-1))' },
  '1': { label: 'P1', color: 'rgb(var(--red-2))' },
  '2': { label: 'P2', color: 'rgb(var(--red-3))' }
};
const KANBAN_STATUS_KEYS = ['0', '1', '2', '3'];

/** @type {import('vue').Ref<Array.<AssetTask>>} */
const tasks = ref([]);
const selectedStatus = ref([]);
const selectedPriority = ref([]);
const filterStartDate = ref(getDefaultStartDate());
const filterEndDate = ref(null);

/** 返回近两周前的日期字符串（yyyy-MM-dd） */
function getDefaultStartDate() {
  const d = new Date();
  d.setDate(d.getDate() - 14);
  return d.toISOString().slice(0, 10);
}
const showCreateModal = ref(false);
const showDetailModal = ref(false);
const createLoading = ref(false);
const detailLoading = ref(false);

function createTaskForm(task = {}) {
  return {
    taskId: task.taskId,
    taskTitle: task.taskTitle ?? '',
    taskContent: task.taskContent ?? '',
    status: task.status ?? '0',
    priority: task.priority ?? '1',
    startTime: task.startTime ?? '',
    endTime: task.endTime ?? '',
    actualCompletionTime: task.actualCompletionTime ?? '',
    sort: task.sort,
    kanbanSort: task.kanbanSort,
    ganttSort: task.ganttSort
  };
}

/** @type {import('vue').Ref<Partial<AssetTask>>} */
const createForm = ref(createTaskForm());
/** @type {import('vue').Ref<Partial<AssetTask>>} */
const detailForm = ref(createTaskForm());

/**
 * 将接口返回的任务对象补齐为页面使用的任务对象。
 * @param {AssetTask} task
 * @returns {AssetTask}
 */
function fromApiTask(task) {
  return {
    taskId: task.taskId,
    taskTitle: task.taskTitle,
    taskContent: task.taskContent,
    startTime: task.startTime,
    endTime: task.endTime,
    actualCompletionTime: task.actualCompletionTime,
    status: task.status ?? '0',
    priority: task.priority ?? '1',
    sort: task.sort ?? 0,
    kanbanSort: task.kanbanSort ?? task.sort ?? 0,
    ganttSort: task.ganttSort ?? task.sort ?? 0
  };
}

/**
 * 将页面中的任务对象转换为接口提交对象。
 * @param {Partial<AssetTask>} task
 * @returns {AssetTask}
 */
function toApiTask(task) {
  return {
    taskId: task.taskId,
    taskTitle: task.taskTitle,
    taskContent: task.taskContent,
    startTime: task.startTime || null,
    endTime: task.endTime || null,
    actualCompletionTime: task.actualCompletionTime || null,
    status: task.status,
    priority: task.priority,
    sort: task.sort,
    kanbanSort: task.kanbanSort,
    ganttSort: task.ganttSort
  };
}

async function loadTaskList() {
  const params = {};
  if (filterStartDate.value) params.startDate = filterStartDate.value;
  if (filterEndDate.value) params.endDate = filterEndDate.value;
  const res = await getAssetTaskList(params);
  tasks.value = (res?.data || []).map(fromApiTask);
}

const filteredTasks = computed(() => {
  return tasks.value.filter(task => {
    const statusMatched = selectedStatus.value.length === 0 || selectedStatus.value.includes(task.status);
    const priorityMatched = selectedPriority.value.length === 0 || selectedPriority.value.includes(task.priority);
    return statusMatched && priorityMatched;
  });
});

function resetCreateForm() {
  createForm.value = createTaskForm();
}

function handleOpenCreateModal() {
  showCreateModal.value = true;
}

function handleCloseCreateModal() {
  showCreateModal.value = false;
  resetCreateForm();
}

function handleOpenTaskDetail(task) {
  detailForm.value = createTaskForm(task);
  showDetailModal.value = true;
}

function handleCloseTaskDetail() {
  showDetailModal.value = false;
  detailLoading.value = false;
}

function handleStatusFilterChange(key, checked) {
  if (checked && !selectedStatus.value.includes(key)) {
    selectedStatus.value.push(key);
    return;
  }
  selectedStatus.value = selectedStatus.value.filter(item => item !== key);
}

function handlePriorityFilterChange(key, checked) {
  if (checked && !selectedPriority.value.includes(key)) {
    selectedPriority.value.push(key);
    return;
  }
  selectedPriority.value = selectedPriority.value.filter(item => item !== key);
}

async function handleCreateTask(formData) {
  const payload = formData || createForm.value;
  const taskTitle = payload.taskTitle?.trim();
  if (!taskTitle) {
    Message.error('请输入任务标题');
    return;
  }
  createLoading.value = true;
  try {
    await addAssetTask({
      taskTitle,
      taskContent: payload.taskContent || null,
      status: payload.status,
      priority: payload.priority,
      startTime: payload.startTime || null,
      endTime: payload.endTime || null
    });
    await loadTaskList();
    handleCloseCreateModal();
    // Message.success('任务已创建');
  } catch (error) {
    Message.error('任务创建失败');
  } finally {
    createLoading.value = false;
  }
}

async function handleUpdateTask(updatedTask) {
  const previousTasks = tasks.value;
  tasks.value = tasks.value.map(task => {
    if (task.taskId !== updatedTask.taskId) {
      return task;
    }
    return {
      ...task,
      ...updatedTask
    };
  });
  try {
    await updateAssetTask(toApiTask(updatedTask));
    await loadTaskList();
    // Message.success('任务已更新');
  } catch (error) {
    tasks.value = previousTasks;
    Message.error('任务更新失败');
  }
}

function moveTaskIds(taskIds, oldIndex, newIndex) {
  if (oldIndex === newIndex) {
    return taskIds;
  }
  const nextTaskIds = [...taskIds];
  const movedTaskId = nextTaskIds.splice(oldIndex, 1)[0];
  if (movedTaskId == null) {
    return taskIds;
  }
  nextTaskIds.splice(newIndex, 0, movedTaskId);
  return nextTaskIds;
}

function sortTasksByField(taskList, sortField) {
  return [...taskList].sort((prevTask, nextTask) => {
    const prevValue = prevTask?.[sortField] ?? 0;
    const nextValue = nextTask?.[sortField] ?? 0;
    if (prevValue !== nextValue) {
      return prevValue - nextValue;
    }
    return (prevTask?.taskId ?? 0) - (nextTask?.taskId ?? 0);
  });
}

function buildSortedTasks(taskList, sortField) {
  return taskList.map((task, index) => ({
    ...task,
    [sortField]: index + 1
  }));
}

function buildSortPayload(previousTaskList, nextTaskList, sortField) {
  const previousTaskMap = new Map(previousTaskList.map(task => [task.taskId, task]));
  return nextTaskList
    .filter(task => previousTaskMap.get(task.taskId)?.[sortField] !== task[sortField])
    .map(task => {
      const payload = {
        taskId: task.taskId
      };
      payload[sortField] = task[sortField];
      return payload;
    });
}

function persistTaskSort(sortPayload, sortField = 'sort') {
  if (!sortPayload.length) {
    return Promise.resolve();
  }
  return updateAssetTaskSort(sortPayload, sortField);
}

// 过滤条件下拖拽后，需要把隐藏任务按原相对顺序合并回完整列表。
function mergeVisibleTasksWithHidden(originalTasks, visibleBeforeIds, visibleAfterTasks) {
  const visibleBeforeSet = new Set(visibleBeforeIds);
  const hiddenDescriptors = [];
  let visibleCount = 0;

  originalTasks.forEach(task => {
    if (visibleBeforeSet.has(task.taskId)) {
      visibleCount += 1;
      return;
    }
    hiddenDescriptors.push({
      task,
      visibleCountBefore: visibleCount
    });
  });

  const mergedTasks = [...visibleAfterTasks];
  hiddenDescriptors.forEach((descriptor, index) => {
    const insertIndex = Math.min(mergedTasks.length, descriptor.visibleCountBefore + index);
    mergedTasks.splice(insertIndex, 0, descriptor.task);
  });
  return mergedTasks;
}

async function handleTaskSortEnd({ oldIndex, newIndex }) {
  if (typeof oldIndex !== 'number' || typeof newIndex !== 'number' || oldIndex === newIndex) {
    return;
  }

  await handleLinearSortEnd({ oldIndex, newIndex }, 'sort', '任务顺序保存失败');
}

async function handleLinearSortEnd({ oldIndex, newIndex }, sortField, errorMessage) {
  const previousTasks = sortTasksByField(tasks.value.map(task => ({ ...task })), sortField);
  const visibleTaskIds = sortTasksByField(filteredTasks.value, sortField).map(task => task.taskId);
  const reorderedVisibleTaskIds = moveTaskIds(visibleTaskIds, oldIndex, newIndex);
  if (reorderedVisibleTaskIds === visibleTaskIds) {
    return;
  }

  const reorderedVisibleTaskList = reorderedVisibleTaskIds
    .map(taskId => previousTasks.find(task => task.taskId === taskId))
    .filter(Boolean);
  const reorderedVisibleTaskMap = new Map(
    reorderedVisibleTaskList.map(task => [task.taskId, task])
  );

  let visibleIndex = 0;
  const mergedTasks = previousTasks.map(task => {
    if (!reorderedVisibleTaskMap.has(task.taskId)) {
      return task;
    }
    const nextTaskId = reorderedVisibleTaskIds[visibleIndex];
    visibleIndex += 1;
    return reorderedVisibleTaskMap.get(nextTaskId);
  });
  const nextTasks = buildSortedTasks(mergedTasks, sortField);
  const sortPayload = buildSortPayload(previousTasks, nextTasks, sortField);
  if (!sortPayload.length) {
    return;
  }
  tasks.value = nextTasks;

  try {
    await persistTaskSort(sortPayload, sortField);
  } catch (error) {
    tasks.value = previousTasks;
    Message.error(errorMessage);
  }
}

// 看板跨列拖拽后，需要同时持久化状态变更和列内排序值。
async function handleKanbanSortEnd({ columns }) {
  if (!Array.isArray(columns) || !columns.length) {
    return;
  }

  const previousTasks = tasks.value.map(task => ({ ...task }));
  const previousTaskMap = new Map(previousTasks.map(task => [task.taskId, { ...task }]));
  const visibleBeforeByStatus = new Map(
    KANBAN_STATUS_KEYS.map(statusKey => [
      statusKey,
      sortTasksByField(
        filteredTasks.value.filter(task => task.status === statusKey),
        'kanbanSort'
      ).map(task => task.taskId)
    ])
  );
  const columnsAfterMap = new Map(columns.map(column => [column.status, column.taskIds || []]));
  const nextTaskMap = new Map(previousTasks.map(task => [task.taskId, { ...task }]));

  KANBAN_STATUS_KEYS.forEach(statusKey => {
    const originalStatusTasks = sortTasksByField(
      previousTasks.filter(task => task.status === statusKey),
      'kanbanSort'
    );
    const visibleAfterTasks = (columnsAfterMap.get(statusKey) || [])
      .map(taskId => nextTaskMap.get(taskId))
      .filter(Boolean)
      .map(task => ({
        ...task,
        status: statusKey
      }));
    const mergedStatusTasks = mergeVisibleTasksWithHidden(
      originalStatusTasks,
      visibleBeforeByStatus.get(statusKey) || [],
      visibleAfterTasks
    );
    mergedStatusTasks.forEach((task, index) => {
      nextTaskMap.set(task.taskId, {
        ...nextTaskMap.get(task.taskId),
        status: statusKey,
        kanbanSort: index + 1
      });
    });
  });

  const nextTasks = previousTasks.map(task => nextTaskMap.get(task.taskId) || task);
  const sortPayload = buildSortPayload(previousTasks, nextTasks, 'kanbanSort');
  const statusChangedTasks = nextTasks.filter(task => {
    const previousTask = previousTaskMap.get(task.taskId);
    return previousTask && previousTask.status !== task.status;
  });
  if (!sortPayload.length && !statusChangedTasks.length) {
    return;
  }

  tasks.value = nextTasks;
  try {
    if (statusChangedTasks.length) {
      await Promise.all(statusChangedTasks.map(task => updateAssetTask(toApiTask(task))));
    }
    if (sortPayload.length) {
      await persistTaskSort(sortPayload, 'kanbanSort');
    }
    await loadTaskList();
    // Message.success('看板已更新');
  } catch (error) {
    tasks.value = previousTasks;
    Message.error('看板更新失败');
  }
}

function handleTaskSortStar() {}

async function handleGanttSortEnd({ oldIndex, newIndex }) {
  if (typeof oldIndex !== 'number' || typeof newIndex !== 'number' || oldIndex === newIndex) {
    return;
  }

  await handleLinearSortEnd({ oldIndex, newIndex }, 'ganttSort', '甘特图排序保存失败');
}

function handleGanttSortStar() {}

async function handleSaveTaskDetail() {
  const taskTitle = detailForm.value.taskTitle?.trim();
  if (!taskTitle) {
    Message.error('请输入任务标题');
    return;
  }
  detailLoading.value = true;
  try {
    await updateAssetTask(toApiTask({
      ...detailForm.value,
      taskTitle
    }));
    await loadTaskList();
    handleCloseTaskDetail();
    // Message.success('任务已更新');
  } catch (error) {
    Message.error('任务更新失败');
  } finally {
    detailLoading.value = false;
  }
}

onMounted(() => {
  loadTaskList();
  nextTick(() => {
    if (taskPageRef.value) {
      _resizeObserver = new ResizeObserver(calcViewMaxHeight);
      _resizeObserver.observe(taskPageRef.value);
      calcViewMaxHeight();
    }
  });
});

onBeforeUnmount(() => {
  if (_resizeObserver) {
    _resizeObserver.disconnect();
    _resizeObserver = null;
  }
});
</script>

<template>
<!--<scrollbar>-->
  <div ref="taskPageRef" class="task-page flex flex-col flex-1">
    <div ref="viewContentWrapperRef" class="view-content-wrapper flex-shrink-0">
      <div class="view-nav flex items-center justify-center">
        <div class="day-radio-group flex">
          <m-radio-group class="flex flex-1" v-model="currentView" type="button">
            <m-radio class="flex-1" :value="'kanban'">
              <span class="day-radio-name">任务状态看板</span>
            </m-radio>
            <m-radio class="flex-1" :value="'list'">
              <span class="day-radio-name">所有任务</span>
            </m-radio>
            <m-radio class="flex-1" :value="'gantt'">
              <span class="day-radio-name">进展甘特图</span>
            </m-radio>
          </m-radio-group>
        </div>
      </div>
    </div>

    <div ref="filtersRef" class="filters-wrapper flex items-center justify-between flex-wrap">
      <div class="filters flex items-center flex-wrap" style="gap: 16px;">
        <div class="filter-group flex items-center">
          <div class="filter-label" style="margin-right: 8px;">状态</div>
          <div class="filter-tags flex flex-wrap" style="gap: 8px;">
            <m-tag
              v-for="(val, key) in STATUS_MAP"
              :key="key"
              checkable
              :bordered="!selectedStatus.includes(key)"
              :checked="selectedStatus.includes(key)"
              color="arcoblue"
              @check="(checked) => handleStatusFilterChange(key, checked)"
            >
              {{ val.label }}
            </m-tag>
          </div>
        </div>
        <div class="filter-group flex items-center">
          <div class="filter-label" style="margin-right: 8px;">优先级</div>
          <div class="filter-tags flex flex-wrap" style="gap: 8px;">
            <m-tag
              v-for="(val, key) in PRIORITY_MAP"
              :key="key"
              checkable
              :bordered="!selectedPriority.includes(key)"
              :checked="selectedPriority.includes(key)"
              color="arcoblue"
              @check="(checked) => handlePriorityFilterChange(key, checked)"
            >
              {{ val.label }}
            </m-tag>
          </div>
        </div>
        <div class="filter-group flex items-center">
          <div class="filter-label" style="margin-right: 8px;">截止日期</div>
          <div class="flex items-center" style="gap: 8px;">
            <input v-model="filterStartDate" type="date" class="native-input" style="width: 140px;" @change="loadTaskList"/>
            <span>—</span>
            <input v-model="filterEndDate" type="date" class="native-input" style="width: 140px;" @change="loadTaskList"/>
          </div>
        </div>
      </div>
      <m-button type="primary" size="mini" shape="rectangle" @click="handleOpenCreateModal"> <template #icon><icon-plus/></template> 新建任务</m-button>
    </div>

    <modal
      :show="showCreateModal"
      title="新建任务"
      width="620px"
      :loading="createLoading"
      :mask-closable="false"
      :footer="false"
      @cancel="handleCloseCreateModal"
    >
      <m-form v-model="createForm" @submit="handleCreateTask" label-align="left" auto-label-width>
        <m-form-item label="任务标题" prop="taskTitle" required>
          <m-input v-model="createForm.taskTitle" :max-length="100" placeholder="请输入任务标题"/>
        </m-form-item>
        <m-form-item label="任务内容">
          <textarea
            v-model="createForm.taskContent"
            class="remark-input"
            maxlength="300"
            placeholder="可选，最多 300 字"
          ></textarea>
        </m-form-item>
        <m-form-item label="状态">
          <select v-model="createForm.status" class="native-select">
            <option v-for="(val, key) in STATUS_MAP" :key="key" :value="key">{{ val.label }}</option>
          </select>
        </m-form-item>
        <m-form-item label="优先级">
          <select v-model="createForm.priority" class="native-select">
            <option v-for="(val, key) in PRIORITY_MAP" :key="key" :value="key">{{ val.label }}</option>
          </select>
        </m-form-item>
        <m-form-item label="开始时间">
          <input v-model="createForm.startTime" type="date" class="native-input"/>
        </m-form-item>
        <m-form-item label="截止时间">
          <input v-model="createForm.endTime" type="date" class="native-input"/>
        </m-form-item>
        <m-form-item label="实际完成时间">
          <input v-model="createForm.actualCompletionTime" type="date" class="native-input"/>
        </m-form-item>
        <m-form-item>
          <m-button type="primary" size="large" style="margin-right: 16px" html-type="submit" :loading="createLoading">确定</m-button>
          <m-button type="secondary" size="large" @click="handleCloseCreateModal">取消</m-button>
        </m-form-item>
      </m-form>
    </modal>

    <modal
      :show="showDetailModal"
      title="任务详情"
      width="620px"
      :loading="detailLoading"
      :mask-closable="false"
      :footer="false"
      @cancel="handleCloseTaskDetail"
    >
      <m-form label-align="left" auto-label-width>
        <m-form-item label="任务标题" required>
          <m-input v-model="detailForm.taskTitle" :max-length="100" placeholder="请输入任务标题"/>
        </m-form-item>
        <m-form-item label="任务内容">
          <textarea
            v-model="detailForm.taskContent"
            class="remark-input"
            maxlength="300"
            placeholder="可选，最多 300 字"
          ></textarea>
        </m-form-item>
        <m-form-item label="状态">
          <select v-model="detailForm.status" class="native-select">
            <option v-for="(val, key) in STATUS_MAP" :key="key" :value="key">{{ val.label }}</option>
          </select>
        </m-form-item>
        <m-form-item label="优先级">
          <select v-model="detailForm.priority" class="native-select">
            <option v-for="(val, key) in PRIORITY_MAP" :key="key" :value="key">{{ val.label }}</option>
          </select>
        </m-form-item>
        <m-form-item label="开始时间">
          <input v-model="detailForm.startTime" type="date" class="native-input"/>
        </m-form-item>
        <m-form-item label="截止时间">
          <input v-model="detailForm.endTime" type="date" class="native-input"/>
        </m-form-item>
        <m-form-item label="实际完成时间">
          <input v-model="detailForm.actualCompletionTime" type="date" class="native-input"/>
        </m-form-item>
        <m-form-item>
          <m-button type="primary" size="large" style="margin-right: 16px" :loading="detailLoading" @click="handleSaveTaskDetail">保存</m-button>
          <m-button type="secondary" size="large" @click="handleCloseTaskDetail">取消</m-button>
        </m-form-item>
      </m-form>
    </modal>

    <!-- 视图组件 -->
<!--    <div class="view-container">-->
      <TaskKanban
        v-if="currentView === 'kanban'"
        :tasks="filteredTasks"
        :STATUS_MAP="STATUS_MAP"
        :PRIORITY_MAP="PRIORITY_MAP"
        @open-task="handleOpenTaskDetail"
        @sort-end="handleKanbanSortEnd"
      />

      <TaskList
        v-if="currentView === 'list'"
        :tasks="filteredTasks"
        :STATUS_MAP="STATUS_MAP"
        :PRIORITY_MAP="PRIORITY_MAP"
        :viewMaxHeight="viewMaxHeight"
        @open-task="handleOpenTaskDetail"
        @updateTask="handleUpdateTask"
        @sort-end="handleTaskSortEnd"
        @sort-star="handleTaskSortStar"
      />

      <TaskGantt
        v-if="currentView === 'gantt'"
        :tasks="filteredTasks"
        :STATUS_MAP="STATUS_MAP"
        :PRIORITY_MAP="PRIORITY_MAP"
        :viewMaxHeight="viewMaxHeight"
        @open-task="handleOpenTaskDetail"
        @update-task="handleUpdateTask"
        @sort-end="handleGanttSortEnd"
        @sort-star="handleGanttSortStar"
      />
    </div>
<!--  </div>-->
<!--</scrollbar>-->
</template>

<style scoped lang="scss">
.task-page {
  --shadow: 0 1px 3px rgba(42, 37, 32, 0.04), 0 4px 12px rgba(42, 37, 32, 0.03);
  --shadow-lg: 0 2px 8px rgba(42, 37, 32, 0.06), 0 12px 32px rgba(42, 37, 32, 0.06);
}
//:deep(.scrollbar-content){
//  display: flex;
//  flex-direction: column;
//}
.task-page {
  flex: 1;
  overflow: hidden;
  overflow-y: auto;
  //overflow: hidden;
  padding: 32px;
}
.filters-wrapper{
  padding: 8px 0 18px 0;
}
.filters {
  gap: 16px;
}
.filter-group {
  input {
    height: 1.5em;
  }
}

.filter-label {
  margin-right: 8px;
  font-weight: bold;
}

.filter-tags {
  gap: 8px;
}

.view-content-wrapper {
  border: 1px solid var(--color-border-1);
  border-radius: var(--border-radius-large);
  //box-shadow: var(--shadow);
  overflow: hidden;

  .view-nav {
    //padding: 8px 20px; /* Reduced padding from 20px to shrink height */
  }

  .day-radio-group {
    text-align: center;
    width: 60%;
    margin: 0 auto;

    :deep(.m-radio-group-button) {
      border-top-left-radius: 0;
      border-top-right-radius: 0;
      background-color: transparent;
    }

    :deep(.m-radio-button) {
      border-radius: 6px;
      border: 1px solid transparent;

      &:hover {
        background-color: var(--color-primary-light-1);
      }

      &.m-radio--checked {
        color: #fff;
        background-color: rgb(var(--primary-5));
      }
    }

    .day-radio-name {
      display: block;
      font-size: 14px;
      padding: 2px 0; /* Reduced padding from 4px to shrink height */
      font-weight: 500;
    }
  }
}

.view-label :deep(.divider__line.is-left){
  flex: 0;
}

.native-select, .native-input, .remark-input {
  width: 100%;
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
  background-color: var(--color-bg-1);
  color: var(--color-text-1);
}

.native-select, .native-input {
  height: 32px;
  padding: 0 8px;
}

.remark-input {
  min-height: 96px;
  padding: 8px;
  resize: vertical;
}
</style>
