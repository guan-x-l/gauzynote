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
    <loading-wrapper :loading="loading">
      <div v-if="items.length === 0" class="empty-state">
        {{ t('empty.description') }}
      </div>

      <div v-else class="card-grid">
        <div
          v-for="item in items"
          :key="item.recycleId"
          class="card-item flex flex-col"
        >
          <div class="card-icon flex items-center justify-center">
            <component :is="item.icon" size="40" />
          </div>
          <div class="card-name text-break text-ellipsis-2-row" :title="item.resourceName">
            {{ item.resourceName }}
          </div>
          <div class="card-actions flex items-center justify-center">
            <m-button type="outline" size="mini" status="success" @click="handleRestore(item)">
              {{ t('recycleBin.restore') }}
            </m-button>
            <m-button type="outline" size="mini" status="danger" @click="handlePermanentDelete(item)">
              {{ t('recycleBin.permanentDelete') }}
            </m-button>
          </div>
        </div>
      </div>
    </loading-wrapper>
  </div>
</template>

<style scoped>
.empty-state{
  text-align: center;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
}

.card-item {
  padding: 20px 16px 16px;
  border: 1px solid var(--color-border-2);
  border-radius: var(--border-radius-medium);
  background-color: var(--color-bg-1);
  transition: box-shadow 0.2s, border-color 0.2s;
  gap: 10px;
}

.card-item:hover {
  border-color: var(--color-primary-light-3);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.card-icon {
  height: 56px;
  color: var(--color-text-3);
}

.card-name {
  text-align: center;
  font-size: 14px;
  font-weight: 500;
}

.card-actions {
  gap: 8px;
}
</style>
