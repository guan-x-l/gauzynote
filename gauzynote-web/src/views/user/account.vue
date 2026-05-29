<script setup>
import {computed, onMounted, ref} from "vue";
import {useI18n} from "vue-i18n";
import {Message} from "@/components/index.js";
import {useUserStore} from "@/store/index.js";
import {updateUser} from "@/api/user.js";
import {
  getWebAuthnRegisterOptions,
  listWebAuthnCredentials,
  renameWebAuthnCredential,
  revokeWebAuthnCredential,
  verifyWebAuthnRegister
} from "@/api/webauthn.js";
import {credentialToJSON, isWebAuthnSupported, normalizeWebAuthnError, toRegistrationOptions} from "@/utils/webauthn.js";

const {t} = useI18n();
const {userinfo} = useUserStore();

const loading = ref(false);
const bindLoading = ref(false);
const renameLoading = ref(false);
const accountSaving = ref(false);
const passkeyList = ref([]);

const nickname = ref("");
const editingNickname = ref(false);

const renameModalVisible = ref(false);
const renameCredentialId = ref("");
const renameCredentialName = ref("");

const passkeySupported = computed(() => isWebAuthnSupported());

/**
 * 生成浏览器展示名称，作为新建通行密钥时的默认名称。
 */
function detectBrowserDisplayName() {
  const ua = navigator.userAgent || "";
  const normalized = ua.toLowerCase();
  let os = "Unknown OS";
  if (normalized.includes("windows")) {
    os = "Windows";
  } else if (normalized.includes("mac os x") || normalized.includes("macintosh")) {
    os = "macOS";
  } else if (normalized.includes("android")) {
    os = "Android";
  } else if (normalized.includes("iphone") || normalized.includes("ipad") || normalized.includes("ios")) {
    os = "iOS";
  } else if (normalized.includes("linux")) {
    os = "Linux";
  }
  let browser = "Unknown";
  if (normalized.includes("edg/")) {
    browser = "Edge";
  } else if (normalized.includes("chrome/") && !normalized.includes("edg/")) {
    browser = "Chrome";
  } else if (normalized.includes("firefox/")) {
    browser = "Firefox";
  } else if (normalized.includes("safari/") && !normalized.includes("chrome/")) {
    browser = "Safari";
  }
  return `${browser} on ${os}`;
}

/**
 * 加载当前用户已绑定通行密钥列表。
 */
async function loadPasskeyList() {
  loading.value = true;
  try {
    const res = await listWebAuthnCredentials();
    passkeyList.value = Array.isArray(res?.data) ? res.data : [];
  } finally {
    loading.value = false;
  }
}

/**
 * 保存昵称修改。
 */
async function handleSaveNickname() {
  const value = nickname.value.trim();
  if (!value) {
    Message.warning(t("account.nicknameRequired"));
    return;
  }
  accountSaving.value = true;
  try {
    await updateUser({nickname: value});
    userinfo.value.nickname = value;
    editingNickname.value = false;
    Message.success(t("account.nicknameSaved"));
  } finally {
    accountSaving.value = false;
  }
}

/**
 * 创建并绑定一个新的通行密钥。
 */
async function handleCreatePasskey() {
  if (bindLoading.value) {
    return;
  }
  if (!passkeySupported.value) {
    Message.warning(t("setting.passkeyNotSupported"));
    return;
  }
  bindLoading.value = true;
  try {
    const optionsRes = await getWebAuthnRegisterOptions();
    const challengeId = optionsRes?.data?.challengeId;
    const publicKeyOptions = optionsRes?.data?.publicKey;
    if (!challengeId || !publicKeyOptions) {
      throw new Error(t("setting.passkeyBindOptionMissing"));
    }
    const credential = await navigator.credentials.create({
      publicKey: toRegistrationOptions(publicKeyOptions)
    });
    if (!credential) {
      throw new Error(t("setting.passkeyBindNoCredential"));
    }
    const payload = credentialToJSON(credential);
    payload.credentialName = detectBrowserDisplayName();
    payload.userAgent = navigator.userAgent || "";
    const verifyRes = await verifyWebAuthnRegister(challengeId, payload);
    if (verifyRes?.code !== 200) {
      throw new Error(verifyRes?.msg || t("setting.passkeyBindFailed"));
    }
    Message.success(t("setting.passkeyBindSuccess"));
    await loadPasskeyList();
  } catch (error) {
    const normalized = normalizeWebAuthnError(error);
    Message.error(normalized.message || t("setting.passkeyBindFailed"));
  } finally {
    bindLoading.value = false;
  }
}

/**
 * 打开重命名弹框并回填当前名称。
 */
function handleOpenRenameModal(record) {
  renameCredentialId.value = record.credentialId;
  renameCredentialName.value = record.credentialName || "";
  renameModalVisible.value = true;
}

/**
 * 提交通行密钥重命名。
 */
async function handleConfirmRename() {
  if (!renameCredentialId.value) {
    return;
  }
  const value = renameCredentialName.value.trim();
  if (!value) {
    Message.warning(t("account.passkeyNameRequired"));
    return;
  }
  renameLoading.value = true;
  try {
    const res = await renameWebAuthnCredential(renameCredentialId.value, value);
    if (res?.code !== 200) {
      throw new Error(res?.msg || t("account.passkeyRenameFailed"));
    }
    Message.success(t("account.passkeyRenameSuccess"));
    renameModalVisible.value = false;
    await loadPasskeyList();
  } catch (error) {
    Message.error(error?.message || t("account.passkeyRenameFailed"));
  } finally {
    renameLoading.value = false;
  }
}

/**
 * 删除指定通行密钥。
 */
async function handleRemovePasskey(record) {
  if (!window.confirm(t("account.passkeyDeleteConfirm"))) {
    return;
  }
  await revokeWebAuthnCredential(record.credentialId);
  Message.success(t("account.passkeyDeleteSuccess"));
  await loadPasskeyList();
}

onMounted(() => {
  nickname.value = userinfo.value.nickname || "";
  loadPasskeyList();
});
</script>

<template>
  <section class="passkey-page">
    <m-form layout="vertical" label-align="left" style="max-width: 460px">
        <m-form-item :label="t('setting.account')">
          <span>{{ userinfo.username }}</span>
        </m-form-item>
        <m-form-item :label="t('setting.nickname')">
          <div class="account-row flex items-center">
            <m-input
              v-model.trim="nickname"
              :disabled="!editingNickname"
              :max-length="30"
              show-word-limit
            />
            <m-button v-if="!editingNickname" type="outline" @click="editingNickname = true">
              {{ t("action.rename") }}
            </m-button>
            <m-button v-else type="primary" :loading="accountSaving" @click="handleSaveNickname">
              {{ t("okText") }}
            </m-button>
          </div>
        </m-form-item>
      </m-form>
    <divider/>
    <div class="table-card">
      <div class="page-actions flex justify-between">
        <h3 class="margin-0">{{ t("setting.passkey") }}</h3>
        <m-button type="outline" size="small" :loading="bindLoading" @click="handleCreatePasskey">
          {{ t("account.createPasskey") }}
        </m-button>
      </div>
      <m-table :data="passkeyList" :loading="loading" striped align-center>
        <template #columns>
          <table-column :title="() => t('account.passkeyName')" data-index="credentialName">
            <template #cell="{record}">
              <span>{{ record.credentialName || '-' }}</span>
            </template>
          </table-column>
          <table-column :title="() => t('account.passkeyBrowser')" data-index="browserName">
            <template #cell="{record}">
              <span>{{ record.browserName || '-' }}</span>
            </template>
          </table-column>
          <table-column :title="() => t('account.passkeyLastUsedTime')" data-index="lastUsedTime">
            <template #cell="{record}">
              <span>{{ record.lastUsedTime || '-' }}</span>
            </template>
          </table-column>
          <table-column :title="() => t('account.passkeyCreateTime')" data-index="createTime">
            <template #cell="{record}">
              <span>{{ record.createTime || '-' }}</span>
            </template>
          </table-column>
          <table-column :title="() => t('operation')" :width="160">
            <template #cell="{record}">
              <m-button size="mini" type="text" @click="handleOpenRenameModal(record)">
                {{ t("action.rename") }}
              </m-button>
              <m-button size="mini" status="danger" type="text" @click="handleRemovePasskey(record)">
                {{ t("action.delete") }}
              </m-button>
            </template>
          </table-column>
        </template>
      </m-table>
    </div>
    <modal
      :show="renameModalVisible"
      :title="t('account.passkeyRenameTitle')"
      width="520px"
      :loading="renameLoading"
      :mask-closable="false"
      @ok="handleConfirmRename"
      @cancel="renameModalVisible = false"
    >
      <m-form layout="vertical" label-align="left">
        <m-form-item :label="t('account.passkeyName')">
          <m-input
            v-model.trim="renameCredentialName"
            :max-length="64"
            :placeholder="t('account.passkeyRenamePlaceholder')"
            show-word-limit
          />
        </m-form-item>
      </m-form>
    </modal>
  </section>
</template>

<style scoped lang="scss">
.passkey-page {
  flex: 1;
  overflow: hidden;
  overflow-y: auto;
  padding: 24px 28px;
}

.account-row {
  gap: 8px;
}

.table-card {
  margin-top: 12px;
}

.page-actions {
  margin-bottom: 12px;
}
</style>
