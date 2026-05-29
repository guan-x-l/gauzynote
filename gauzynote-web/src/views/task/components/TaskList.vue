<script setup>
import { computed } from 'vue';
const props = defineProps({
  /** @type {import('vue').PropType<Array.<AssetTask>>} */
  tasks: Array,
  STATUS_MAP: Object,
  PRIORITY_MAP: Object,
  viewMaxHeight: Number
});

const emit = defineEmits(['openTask', 'sortEnd', 'sortStar']);

/** @type {import('vue').ComputedRef<Array.<Task>>} */
const sortedTasks = computed(() => {
  return [...(props.tasks || [])].sort((prevTask, nextTask) => {
    if ((prevTask.sort ?? 0) !== (nextTask.sort ?? 0)) {
      return (prevTask.sort ?? 0) - (nextTask.sort ?? 0);
    }
    return (prevTask.taskId ?? 0) - (nextTask.taskId ?? 0);
  });
});


function handleOpenTask(task) {
  emit('openTask', task);
}

function handleEdit(task) {
  handleOpenTask(task)
}

function handleSortEnd(payload) {
  emit('sortEnd', payload);
}

function handleSortStar(payload) {
  emit('sortStar', payload);
}

</script>

<template>
  <div class="list-view">
    <m-table
      :data="sortedTasks"
      row-key="taskId"
      hoverAble
      resizable
      :max-height="viewMaxHeight"
      striped
      border-bottom
      :draggable="{ type: 'handle', width: 40 }"
      @sort-end="handleSortEnd"
      @sort-star="handleSortStar"
    >
      <template #columns>
        <table-column title="ID" data-index="taskId" width="60">
          <template #cell="{ record }">
            #{{ record.taskId }}
          </template>
        </table-column>

        <table-column title="标题" data-index="taskTitle">
          <template #cell="{ record }">
            <span class="font-bold text-ellipsis-3-row">{{ record.taskTitle }}</span>
          </template>
        </table-column>

        <table-column title="状态" data-index="status" width="120">
          <template #cell="{ record }">
            <m-tag size="small" :color="STATUS_MAP[record.status]?.color">{{ STATUS_MAP[record.status]?.label }}</m-tag>
          </template>
        </table-column>

        <table-column title="优先级" data-index="priority" width="100">
          <template #cell="{ record }">
            <m-tag size="small" :color="PRIORITY_MAP[record.priority]?.color" style="border-radius: 99px">{{ PRIORITY_MAP[record.priority]?.label }}</m-tag>
          </template>
        </table-column>

        <table-column title="开始时间" data-index="startTime" width="160">
          <template #cell="{ record }">
            <span>{{ record.startTime || '-' }}</span>
          </template>
        </table-column>

        <table-column title="截止时间" data-index="endTime" width="160">
          <template #cell="{ record }">
            <span>{{ record.endTime || '-' }}</span>
          </template>
        </table-column>

        <table-column title="实际完成时间" data-index="actualCompletionTime" width="160">
          <template #cell="{ record }">
            <span>{{ record.actualCompletionTime || '-' }}</span>
          </template>
        </table-column>

        <table-column title="操作" data-index="actions" width="120" align="center">
          <template #cell="{ record }">
              <m-button type="outline" size="mini" @click="handleEdit(record)">编辑</m-button>
          </template>
        </table-column>
      </template>
    </m-table>
  </div>
</template>

<style scoped>
.font-bold {
  font-weight: 500;
}
</style>
