<script setup>
import {computed, onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import {Message} from "@/components/index.js";
import {
  addDictType,
  deleteDictType,
  getDictTypeList,
  updateDictType
} from "@/api/dictType.js";
import {useDictStore} from "@/store/index.js";

const router = useRouter();
const statusDictType = "sys_normal_disable";
const {loadDictByType, getDictItems, getDictLabel} = useDictStore();

const loading = ref(false);
const submitLoading = ref(false);
const tableData = ref([]);
const showEditModal = ref(false);
const isEditMode = ref(false);

const queryForm = ref({
  dictName: "",
  dictType: "",
  status: ""
});

const editForm = ref(createEmptyEditForm());

const modalTitle = computed(() => (isEditMode.value ? "编辑字典类型" : "新增字典类型"));
const statusOptions = computed(() => {
  const dictItems = getDictItems(statusDictType);
  if (dictItems.length) {
    return dictItems.map(item => ({
      label: item.dictLabel,
      value: item.dictValue
    }));
  }
  return [];
});

/**
 * 创建空的编辑表单，避免弹窗状态相互污染。
 */
function createEmptyEditForm() {
  return {
    dictId: undefined,
    dictName: "",
    dictType: "",
    status: "0",
    remark: ""
  };
}

/**
 * 加载字典类型列表，并按后端 canOperate 字段做按钮控制。
 */
async function loadDictTypeList() {
  loading.value = true;
  try {
    const res = await getDictTypeList({...queryForm.value});
    tableData.value = Array.isArray(res?.data) ? res.data : [];
  } finally {
    loading.value = false;
  }
}

/**
 * 提交筛选条件并刷新列表。
 */
function handleSearch() {
  loadDictTypeList();
}

/**
 * 重置筛选条件并刷新列表。
 */
function handleResetQuery() {
  queryForm.value = {
    dictName: "",
    dictType: "",
    status: ""
  };
  loadDictTypeList();
}

/**
 * 打开新增弹窗并初始化表单。
 */
function handleOpenAddModal() {
  isEditMode.value = false;
  editForm.value = createEmptyEditForm();
  showEditModal.value = true;
}

/**
 * 打开编辑弹窗并回填当前行数据。
 */
function handleOpenEditModal(row) {
  if (!row?.canOperate) {
    Message.warning("当前字典类型无操作权限");
    return;
  }
  isEditMode.value = true;
  editForm.value = {
    dictId: row.dictId,
    dictName: row.dictName || "",
    dictType: row.dictType || "",
    status: row.status || "0",
    remark: row.remark || ""
  };
  showEditModal.value = true;
}

/**
 * 关闭编辑弹窗并清空提交状态。
 */
function handleCloseEditModal() {
  showEditModal.value = false;
  submitLoading.value = false;
}

/**
 * 校验并提交字典类型新增/编辑。
 */
async function handleSubmitEdit() {
  if (!editForm.value.dictName.trim()) {
    Message.warning("请输入字典名称");
    return;
  }
  if (!editForm.value.dictType.trim()) {
    Message.warning("请输入字典类型");
    return;
  }

  submitLoading.value = true;
  try {
    const payload = {
      ...editForm.value,
      dictName: editForm.value.dictName.trim(),
      dictType: editForm.value.dictType.trim(),
      remark: editForm.value.remark?.trim() || ""
    };
    if (isEditMode.value) {
      await updateDictType(payload);
      Message.success("字典类型更新成功");
    } else {
      await addDictType(payload);
      Message.success("字典类型新增成功");
    }
    showEditModal.value = false;
    await loadDictTypeList();
  } finally {
    submitLoading.value = false;
  }
}

/**
 * 删除可操作的字典类型数据。
 */
async function handleDelete(row) {
  if (!row?.canOperate) {
    Message.warning("当前字典类型无操作权限");
    return;
  }
  if (!window.confirm(`确定删除字典类型「${row.dictName || row.dictType}」吗？`)) {
    return;
  }
  await deleteDictType(row.dictId);
  Message.success("删除成功");
  await loadDictTypeList();
}

/**
 * 切换到字典数据管理页面。
 */
function goToDictDataPage(row) {
  if (row?.dictType) {
    router.push(`/dict/data?dictType=${row.dictType}`);
  } else {
    router.push("/dict/data");
  }
}

/**
 * 格式化状态展示文本。
 */
function getStatusText(row) {
  return getDictLabel(statusDictType, row.status);
}

onMounted(async () => {
  await loadDictByType(statusDictType);
  await loadDictTypeList();
});
</script>

<template>
  <section class="dict-page">
    <div class="filter-card">
      <div class="filter-grid">
        <m-input v-model="queryForm.dictName" placeholder="按字典名称筛选"/>
        <m-input v-model="queryForm.dictType" placeholder="按字典类型筛选"/>
        <select v-model="queryForm.status" class="native-select">
          <option value="">全部状态</option>
          <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </div>
      <div class="filter-actions flex">
        <m-button type="primary" @click="handleSearch">查询</m-button>
        <m-button type="base" @click="handleResetQuery">重置</m-button>
        <m-button type="outline" @click="handleOpenAddModal">新增字典类型</m-button>
      </div>
    </div>

    <div class="table-card">
      <m-table :data="tableData" :loading="loading" striped align-center>
        <template #columns>
          <table-column title="字典名称" data-index="dictName"></table-column>
          <table-column title="字典类型" data-index="dictType"></table-column>
          <table-column title="状态" data-index="status" :width="100">
            <template #cell="{record}">
              <span :class="record.status === '0' ? 'status-enabled' : 'status-disabled'">{{ getStatusText(record) }}</span>
            </template>
          </table-column>
          <table-column title="归属">
            <template #cell="{record}">
              <span v-if="record.userId">我</span>
              <span v-else class="remark-text">系统公共</span>
            </template>
          </table-column>
          <table-column title="备注">
            <template #cell="{record}">
              <span class="remark-text">{{ record.remark || "-" }}</span>
            </template>
          </table-column>
          <table-column title="操作" :width="190">
            <template #cell="{record}">
              <div class="row-actions flex justify-center">
                <m-button
                  size="mini"
                  type="text"
                  v-if="record?.canOperate"
                  @click="handleOpenEditModal(record)"
                >
                  编辑
                </m-button>
                <m-button
                  size="mini"
                  type="text"
                  @click="goToDictDataPage(record)"
                >
                  列表
                </m-button>
                <m-button
                  size="mini"
                  status="danger"
                  type="text"
                  v-if="record?.canOperate"
                  @click="handleDelete(record)"
                >
                  删除
                </m-button>
              </div>
            </template>
          </table-column>
        </template>
      </m-table>
    </div>

    <modal
      :show="showEditModal"
      :title="modalTitle"
      width="620px"
      :loading="submitLoading"
      @ok="handleSubmitEdit"
      @cancel="handleCloseEditModal"
    >
      <m-form layout="vertical" label-align="left" auto-label-width>
        <m-form-item label="字典名称" required>
          <m-input v-model="editForm.dictName" :max-length="100" placeholder="请输入字典名称"/>
        </m-form-item>
        <m-form-item label="字典类型" required>
          <m-input v-model="editForm.dictType" :max-length="100" placeholder="请输入字典类型，如 user_status"/>
        </m-form-item>
        <m-form-item label="状态">
          <select v-model="editForm.status" class="native-select">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </m-form-item>
        <m-form-item label="备注">
          <textarea
            v-model="editForm.remark"
            class="remark-input"
            maxlength="500"
            placeholder="可选，最多 500 字"
          ></textarea>
        </m-form-item>
      </m-form>
    </modal>
  </section>
</template>

<style scoped lang="scss">
.dict-page {
  flex: 1;
  overflow: hidden;
  overflow-y: auto;
  padding: 24px 28px;
}

.filter-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.filter-actions {
  gap: 8px;
  margin-top: 12px;
}

.table-card {
  margin-top: 24px;
}

.native-select {
  width: 100%;
  height: 32px;
  border: 1px solid var(--color-border-2);
  border-radius: var(--border-radius-small);
  background-color: var(--color-bg-1);
  color: var(--color-text-1);
  padding: 0 8px;
}

.row-actions {
  gap: 4px;
}

.status-enabled {
  color: rgb(var(--success-6));
}

.status-disabled {
  color: rgb(var(--danger-6));
}

.remark-text {
  color: var(--color-text-3);
}

.remark-input {
  width: 100%;
  min-height: 90px;
  resize: vertical;
  border: 1px solid var(--color-border-2);
  border-radius: var(--border-radius-small);
  background-color: var(--color-bg-1);
  color: var(--color-text-1);
  padding: 8px;
  line-height: 1.6;
}
</style>
