<script setup>
import {computed, onMounted, ref} from "vue";
import {useI18n} from "vue-i18n";
import {Message} from "@/components/index.js";
import {useUserStore} from "@/store/index.js";
import {addUser, deleteUser, getUserList, updateUserOfAdmin, updateUserStatus} from "@/api/user.js";
import {UserType} from "@/enum/index.js";
import UserStatus from "@/enum/UserStatus.js";

const {t} = useI18n();
const {userinfo, userType} = useUserStore();

const loading = ref(false);
const submitLoading = ref(false);
const showAddUserModal = ref(false);
const showAddUserResultModal = ref(false);
const userList = ref([]);
const newUsername = ref("");
const createdUserInfo = ref({
  username: "",
  password: ""
});

const isAdmin = computed(() => UserType.isAdmin(userType.value));

/**
 * 加载用户列表，仅管理员可访问该能力。
 */
async function loadUserList() {
  if (!isAdmin.value) {
    userList.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getUserList();
    userList.value = Array.isArray(res?.data) ? res.data : [];
  } finally {
    loading.value = false;
  }
}

/**
 * 切换用户状态，并作为 switch 的切换前拦截函数返回结果。
 */
async function handleStatusBeforeChange(user, newValue) {
  const targetStatus = newValue
    ? UserStatus.NORMAL.getCode()
    : UserStatus.DISABLED.getCode();
  await updateUserStatus({userId: user.userId, status: targetStatus});
  return true;
}

/**
 * 删除指定用户帐号。
 */
async function handleRemoveUser(user) {
  if (!window.confirm(`确定删除 ${user.username} 账户吗？`)) {
    return;
  }
  await deleteUser(user.userId);
  Message.success("删除成功");
  await loadUserList();
}

/**
 * 更新指定用户角色，仅管理员可操作且不能修改自己。
 */
async function handleUpdateUserType(event, user) {
  if (userinfo.value.username === user.username) {
    return;
  }
  await updateUserOfAdmin({userId: user.userId, userType: event.target.value});
  await loadUserList();
}

/**
 * 新增用户帐号，并弹出后端返回的初始化密码。
 */
async function handleAddUser() {
  const username = newUsername.value.trim();
  if (!username) {
    return;
  }
  submitLoading.value = true;
  try {
    const res = await addUser({username});
    await loadUserList();
    showAddUserModal.value = false;
    createdUserInfo.value = {
      username: res?.data?.username || "",
      password: res?.data?.password || ""
    };
    showAddUserResultModal.value = true;
    newUsername.value = "";
  } finally {
    submitLoading.value = false;
  }
}

/**
 * 打开新增帐号弹框并重置表单值。
 */
function handleOpenAddUserModal() {
  newUsername.value = "";
  showAddUserModal.value = true;
}

/**
 * 关闭新增帐号弹框并重置提交状态。
 */
function handleCloseAddUserModal() {
  showAddUserModal.value = false;
  submitLoading.value = false;
}

/**
 * 关闭新增帐号成功结果弹框。
 */
function handleCloseAddUserResultModal() {
  showAddUserResultModal.value = false;
}

onMounted(() => {
  loadUserList();
});
</script>

<template>
  <section class="user-page">
    <div v-if="!isAdmin" class="no-permission-card">
      当前账号无权限访问用户管理页面。
    </div>

    <template v-else>
      <div class="page-actions">
        <m-button type="outline" @click="handleOpenAddUserModal">
          {{ t('setting.addAccount') }}
        </m-button>
      </div>

      <div class="table-card">
        <m-table :data="userList" :loading="loading" striped align-center>
          <template #columns>
            <table-column :title="() => t('setting.account')" data-index="username"></table-column>
            <table-column :title="() => t('setting.nickname')" data-index="nickname"></table-column>
            <table-column :title="() => t('user.userType.title')" data-index="userType">
              <template #cell="{record}">
                <select
                  v-if="userinfo.username !== record.username && !UserStatus.isDeleted(record.delFlag)"
                  class="native-select"
                  @change="handleUpdateUserType($event, record)"
                >
                  <option
                    v-for="item in UserType.values()"
                    :key="item.getCode()"
                    :value="item.getCode()"
                    :selected="item.getCode() === record.userType"
                  >
                    {{ t(`user.userType.${item.getCode()}`) }}
                  </option>
                </select>
                <span v-else>{{ t(`user.userType.${record.userType}`) }}</span>
              </template>
            </table-column>
            <table-column :title="() => t('user.status.title')" data-index="status">
              <template #cell="{record}">
                <span v-if="userinfo.username === record.username">{{ t(`user.status.${record.delFlag}`) }}</span>
                <m-switch
                  v-else-if="!UserStatus.isDeleted(record.delFlag)"
                  :model-value="UserStatus.isNormal(record.status)"
                  :before-change="newValue => handleStatusBeforeChange(record, newValue)"
                >
                  <template #checked>{{ t('user.status.0') }}</template>
                  <template #unchecked>{{ t('user.status.1') }}</template>
                </m-switch>
                <span v-else>{{ t('user.status.2') }}</span>
              </template>
            </table-column>
            <table-column :title="() => t('operation')" :width="80">
              <template #cell="{record}">
                <m-button
                  v-if="!UserStatus.isDeleted(record.delFlag) && userinfo.username !== record.username"
                  size="mini"
                  status="danger"
                  type="text"
                  @click="handleRemoveUser(record)"
                >
                  {{ t('user.status.remove') }}
                </m-button>
              </template>
            </table-column>
          </template>
        </m-table>
      </div>

      <modal
        :show="showAddUserModal"
        :title="t('setting.addAccount')"
        width="520px"
        :loading="submitLoading"
        :mask-closable="false"
        @ok="handleAddUser"
        @cancel="handleCloseAddUserModal"
      >
        <m-form layout="vertical" label-align="left">
          <m-form-item prop="username" :label="t('setting.account')">
            <m-input
              v-model.trim="newUsername"
              :placeholder="t('setting.addAccountPlaceholder')"
              :max-length="30"
              show-word-limit
            />
          </m-form-item>
        </m-form>
      </modal>

      <modal
        :show="showAddUserResultModal"
        title="新增帐号成功"
        width="520px"
        :hide-cancel="true"
        :mask-closable="false"
        @ok="handleCloseAddUserResultModal"
        @cancel="handleCloseAddUserResultModal"
      >
        <div class="result-content">
          <p>帐号：{{ createdUserInfo.username || '-' }}</p>
          <p>初始密码：{{ createdUserInfo.password || '-' }}</p>
        </div>
      </modal>
    </template>
  </section>
</template>

<style scoped lang="scss">
.user-page {
  flex: 1;
  overflow: hidden;
  overflow-y: auto;
  padding: 24px 28px;
}

.table-card {
  margin-top: 12px;
}

.page-actions {
  display: flex;
  justify-content: flex-end;
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

.no-permission-card {
  margin-top: 20px;
  padding: 20px;
  border: 1px solid var(--color-border-2);
  border-radius: var(--border-radius-medium);
  color: var(--color-text-3);
  background-color: var(--color-fill-1);
}

.result-content {
  line-height: 1.8;
}
</style>
