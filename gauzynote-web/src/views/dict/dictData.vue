<script setup>
import {computed, onMounted, ref, watch} from "vue";
import {useRoute} from "vue-router";
import {Message} from "@/components/index.js";
import {
  addDictData,
  deleteDictData,
  getDictDataList,
  updateDictData
} from "@/api/dictData.js";
import {getDictTypeOptions} from "@/api/dictType.js";
import {useDictStore} from "@/store/index.js";

const route = useRoute();
const statusDictType = "sys_normal_disable";
const yesNoDictType = "sys_yes_no";
const {loadDictList, getDictItems, getDictLabel} = useDictStore();

const loading = ref(false);
const submitLoading = ref(false);
const tableData = ref([]);
const dictTypeOptions = ref([]);
const showEditModal = ref(false);
const isEditMode = ref(false);

const queryForm = ref({
  dictType: route.query?.dictType || '',
  dictLabel: "",
  status: ""
});

const editForm = ref(createEmptyEditForm());

const modalTitle = computed(() => (isEditMode.value ? "编辑字典数据" : "新增字典数据"));
const statusOptions = computed(() => getDictItems(statusDictType));
const yesNoOptions = computed(() => getDictItems(yesNoDictType));

/**
 * 创建空的编辑表单，避免弹窗状态相互污染。
 */
function createEmptyEditForm() {
  return {
    dictCode: undefined,
    dictSort: 0,
    dictLabel: "",
    dictValue: "",
    dictType: "",
    status: "0",
    isDefault: "N",
    remark: ""
  };
}

/**
 * 加载可选字典类型，用于筛选与编辑表单。
 */
async function loadDictTypeOptions() {
  const res = await getDictTypeOptions();
  dictTypeOptions.value = Array.isArray(res?.data) ? res.data : [];
}

/**
 * 加载字典数据列表，并按后端 canOperate 字段控制按钮状态。
 */
async function loadDictDataList() {
  loading.value = true;
  try {
    const res = await getDictDataList({...queryForm.value});
    tableData.value = Array.isArray(res?.data) ? res.data : [];
  } finally {
    loading.value = false;
  }
}

/**
 * 提交筛选条件并刷新列表。
 */
function handleSearch() {
  loadDictDataList();
}

/**
 * 重置筛选条件并刷新列表。
 */
function handleResetQuery() {
  queryForm.value = {
    dictType: "",
    dictLabel: "",
    status: ""
  };
  loadDictDataList();
}

/**
 * 打开新增弹窗并初始化表单。
 */
function handleOpenAddModal() {
  isEditMode.value = false;
  editForm.value = createEmptyEditForm();
  if (dictTypeOptions.value.length > 0) {
    editForm.value.dictType = dictTypeOptions.value[0].dictType;
  }
  showEditModal.value = true;
}

/**
 * 打开编辑弹窗并回填当前行数据。
 */
function handleOpenEditModal(row) {
  if (!row?.canOperate) {
    Message.warning("当前字典数据无操作权限");
    return;
  }
  isEditMode.value = true;
  editForm.value = {
    dictCode: row.dictCode,
    dictSort: Number(row.dictSort ?? 0),
    dictLabel: row.dictLabel || "",
    dictValue: row.dictValue || "",
    dictType: row.dictType || "",
    status: row.status || "0",
    isDefault: row.isDefault || "N",
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
 * 校验并提交字典数据新增/编辑。
 */
async function handleSubmitEdit() {
  if (!editForm.value.dictType) {
    Message.warning("请选择字典类型");
    return;
  }
  if (!editForm.value.dictLabel.trim()) {
    Message.warning("请输入字典标签");
    return;
  }
  if (!editForm.value.dictValue.trim()) {
    Message.warning("请输入字典键值");
    return;
  }

  submitLoading.value = true;
  try {
    const payload = {
      ...editForm.value,
      dictSort: Number(editForm.value.dictSort || 0),
      dictLabel: editForm.value.dictLabel.trim(),
      dictValue: editForm.value.dictValue.trim(),
      remark: editForm.value.remark?.trim() || ""
    };
    if (isEditMode.value) {
      await updateDictData(payload);
      Message.success("字典数据更新成功");
    } else {
      await addDictData(payload);
      Message.success("字典数据新增成功");
    }
    showEditModal.value = false;
    await loadDictDataList();
  } finally {
    submitLoading.value = false;
  }
}

/**
 * 删除可操作的字典数据。
 */
async function handleDelete(row) {
  if (!row?.canOperate) {
    Message.warning("当前字典数据无操作权限");
    return;
  }
  if (!window.confirm(`确定删除字典数据「${row.dictLabel}」吗？`)) {
    return;
  }
  await deleteDictData(row.dictCode);
  Message.success("删除成功");
  await loadDictDataList();
}

/**
 * 格式化默认值展示文本。
 */
function getDefaultText(row) {
  return getDictLabel(yesNoDictType, row.isDefault);
}

/**
 * 格式化状态展示文本。
 */
function getStatusText(row) {
  return getDictLabel(statusDictType, row.status);
}

watch(()=>route.query?.dictType, ()=>{
  queryForm.value.dictType = route.query?.dictType || ''
})

onMounted(async () => {
  await loadDictList([statusDictType, yesNoDictType]);
  await loadDictTypeOptions();
  await loadDictDataList();
});
</script>

<template>
  <section class="dict-page">
    <div class="filter-card">
      <div class="filter-grid">
        <select v-model="queryForm.dictType" class="native-select">
          <option value="">全部字典类型</option>
          <option v-for="item in dictTypeOptions" :key="item.dictId" :value="item.dictType">
            {{ item.dictName }} ({{ item.dictType }})
          </option>
        </select>
        <m-input v-model="queryForm.dictLabel" placeholder="按字典标签筛选"/>
        <select v-model="queryForm.status" class="native-select">
          <option value="">全部状态</option>
          <option v-for="item in statusOptions" :key="item.dictValue" :value="item.dictValue">{{ item.dictLabel }}</option>
        </select>
      </div>
      <div class="filter-actions flex">
        <m-button type="primary" @click="handleSearch">查询</m-button>
        <m-button type="base" @click="handleResetQuery">重置</m-button>
        <m-button type="outline" @click="handleOpenAddModal">新增字典数据</m-button>
      </div>
    </div>

    <div class="table-card">
      <m-table :data="tableData" :loading="loading" striped align-center>
        <template #columns>
          <table-column title="字典类型" data-index="dictType"></table-column>
          <table-column title="标签" data-index="dictLabel"></table-column>
          <table-column title="键值" data-index="dictValue"></table-column>
          <table-column title="排序" data-index="dictSort" :width="60"></table-column>
          <table-column title="默认" data-index="isDefault">
            <template #cell="{record}">
              <span>{{ getDefaultText(record) }}</span>
            </template>
          </table-column>
          <table-column title="状态" data-index="status" :width="60">
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
          <table-column title="操作" :width="140">
            <template #cell="{record}">
              <div class="row-actions flex">
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
      width="680px"
      :loading="submitLoading"
      @ok="handleSubmitEdit"
      @cancel="handleCloseEditModal"
    >
      <m-form layout="vertical" label-align="left" auto-label-width>
        <div class="form-grid">
          <m-form-item label="字典类型" required>
            <select v-model="editForm.dictType" class="native-select">
              <option value="" disabled>请选择字典类型</option>
              <option v-for="item in dictTypeOptions" :key="item.dictId" :value="item.dictType">
                {{ item.dictName }} ({{ item.dictType }})
              </option>
            </select>
          </m-form-item>
          <m-form-item label="排序">
            <m-input v-model="editForm.dictSort" type="number" placeholder="请输入排序，默认 0"/>
          </m-form-item>
          <m-form-item label="字典标签" required>
            <m-input v-model="editForm.dictLabel" :max-length="100" placeholder="请输入字典标签"/>
          </m-form-item>
          <m-form-item label="字典键值" required>
            <m-input v-model="editForm.dictValue" :max-length="100" placeholder="请输入字典键值"/>
          </m-form-item>
          <m-form-item label="默认值">
            <select v-model="editForm.isDefault" class="native-select">
              <option v-for="item in yesNoOptions" :key="item.dictValue" :value="item.dictValue">{{ item.dictLabel }}</option>
            </select>
          </m-form-item>
          <m-form-item label="状态">
            <select v-model="editForm.status" class="native-select">
              <option v-for="item in statusOptions" :key="item.dictValue" :value="item.dictValue">{{ item.dictLabel }}</option>
            </select>
          </m-form-item>
        </div>
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

.form-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
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
