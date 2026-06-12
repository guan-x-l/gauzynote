<script setup>
import { useI18n } from "vue-i18n";

const { t } = useI18n();

const props = defineProps({
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
});

const emit = defineEmits(['restore', 'permanentDelete']);

function handleRestore(item) {
  emit('restore', item);
}

function handlePermanentDelete(item) {
  emit('permanentDelete', item);
}
</script>

<template>
  <div class="flex-1">
    <m-table
      :data="items"
      row-key="recycleId"
      hoverAble
      striped
      border-bottom
      :loading="loading"
    >
      <template #columns>

        <table-column :title="t('recycleBin.resourceName')" data-index="resourceName" :min-width="200">
          <template #cell="{ record }">
            <div class="flex items-center" style="gap: 8px;">
              <component :is="record.icon" size="18" class="flex-shrink-0" />
              <span class="font-bold text-ellipsis">{{ record.resourceName }}</span>
            </div>
          </template>
        </table-column>

        <table-column :title="t('recycleBin.deleteTime')" data-index="deleteTime" :width="200">
          <template #cell="{ record }">
            <span>{{ record.deleteTime }}</span>
          </template>
        </table-column>

        <table-column :title="t('operation')" data-index="actions" :width="200" align="center">
          <template #cell="{ record }">
            <div class="flex items-center justify-center" style="gap: 8px;">
              <m-button type="outline" size="mini" status="success" @click="handleRestore(record)">
                {{ t('recycleBin.restore') }}
              </m-button>
              <m-button type="outline" size="mini" status="danger" @click="handlePermanentDelete(record)">
                {{ t('recycleBin.permanentDelete') }}
              </m-button>
            </div>
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
