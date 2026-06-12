<script setup>
import { onMounted, ref, watch } from "vue";
import { Message } from "@/components/index.js";
import { useI18n } from "vue-i18n";
import { useAppStore } from "@/store/index.js";
import RecycleBinList from "./components/RecycleBinList.vue";
import RecycleBinGrid from "./components/RecycleBinGrid.vue";
import { getRecycleBinList, restoreRecycleBin, permanentDeleteRecycleBin } from "@/api/recycleBin.js";

const { t } = useI18n();
const { initResourceNode, recycleBinChangeVersion } = useAppStore();

const currentView = ref('grid');
const items = ref([]);
const loading = ref(false);

const RESOURCE_TYPE_MAP = {
  '1': { icon: 'IconFolder'},
  '2': { icon: 'IconFileText'},
  '3': { icon: 'IconFile'},
};

async function loadList() {
  loading.value = true;
  try {
    const res = await getRecycleBinList();
    items.value = (res?.data || []).map(item => ({
      ...item,
      icon: RESOURCE_TYPE_MAP[item.resourceType].icon
    }));
  } catch (e) {
    Message.error(t('message.loadingError'));
  } finally {
    loading.value = false;
  }
}

async function handleRestore(item) {
  if (!confirm(t('recycleBin.confirmRestore'))) return;
  try {
    await restoreRecycleBin(item.recycleId);
    Message.success(t('message.success'));
    await initResourceNode();
    await loadList();
  } catch (e) {
    Message.error(t('message.error'));
  }
}

async function handlePermanentDelete(item) {
  if (!confirm(t('recycleBin.confirmPermanentDelete'))) return;
  try {
    await permanentDeleteRecycleBin(item.recycleId);
    Message.success(t('message.success'));
    await initResourceNode();
    await loadList();
  } catch (e) {
    Message.error(t('message.error'));
  }
}

onMounted(() => {
  loadList();
});

// 监听回收站变更信号（来自 FileList 删除等操作），自动刷新列表
let versionOnMount = recycleBinChangeVersion.value;
watch(recycleBinChangeVersion, (newVal) => {
  if (newVal !== versionOnMount) {
    loadList();
  }
});
</script>

<template>
  <div class="recycle-bin-page flex flex-col flex-1">
    <div class="view-header flex-shrink-0">
      <div class="view-nav flex items-center justify-center">
        <div class="radio-group-wrapper flex">
          <m-radio-group class="flex flex-1" v-model="currentView" type="button">
            <m-radio class="flex-1" value="grid">
              <span class="radio-name">{{ t('recycleBin.gridView') }}</span>
            </m-radio>
            <m-radio class="flex-1" value="list">
              <span class="radio-name">{{ t('recycleBin.listView') }}</span>
            </m-radio>
          </m-radio-group>
        </div>
      </div>
    </div>

    <div class="view-content flex-1">
      <RecycleBinList
        v-if="currentView === 'list'"
        :items="items"
        :loading="loading"
        @restore="handleRestore"
        @permanent-delete="handlePermanentDelete"
      />
      <RecycleBinGrid
        v-if="currentView === 'grid'"
        :items="items"
        :loading="loading"
        @restore="handleRestore"
        @permanent-delete="handlePermanentDelete"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.recycle-bin-page {
  flex: 1;
  overflow: hidden;
  padding: 32px;
}

.view-header {
  border: 1px solid var(--color-border-1);
  border-radius: var(--border-radius-large);
  overflow: hidden;

  .view-nav {
    // padding handled by radio group
  }

  .radio-group-wrapper {
    text-align: center;
    width: 50%;
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

    .radio-name {
      display: block;
      font-size: 14px;
      padding: 2px 0;
      font-weight: 500;
    }
  }
}

.view-content {
  margin-top: 20px;
  overflow: hidden;
  overflow-y: auto;
}
</style>
