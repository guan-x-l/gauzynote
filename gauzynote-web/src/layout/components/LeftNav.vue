<script setup >
import {onMounted, ref, useTemplateRef} from "vue";
import {useTabsStore, useUserStore} from "@/store/index.js";
import {initializePassword, updateUserPwd} from "@/api/user.js";
import {Message} from "@/components/index.js";
import {useI18n} from "vue-i18n";
import SettingModal from "@/layout/components/SettingModal.vue";
import Tooltip from "@/components/base/Tooltip/Tooltip.vue";
import {useRouter} from "vue-router";

const emit = defineEmits(['handleSidebarClick'])
const props = defineProps({
  sidebarVisible: Boolean
})
const router = useRouter()
const {userinfo, signOut, isDefaultModifyPwd} = useUserStore()
const {addViewAsTab} = useTabsStore()
const {t} = useI18n()

const navs = [
  {
    key: 'folderList',
    title: 'layout.folderList',
    iconComponent: 'IconFolder'
  },
/*   {
     key: 'fileAll',
     title: '资源管理',
     iconComponent: 'IconHardDrive'
   },*/

  /* {
     key: 'markList',
     title: '书签',
     iconComponent: 'IconBookmark'
   },
   {
     key: 'searchList',
     title: '搜索',
     iconComponent: 'IconSearch'
   },*/
]
const navs2 = [
  {
    key: 'task',
    title: 'layout.task',
    iconComponent: 'IconCheckCircle',
    path: '/task',
    noSelect: true
  },
  /*{
    key: 'dict',
    title: 'layout.dict',
    iconComponent: 'IconBook',
    path: '/dict/type',
    noSelect: true
  },*/
]

const currentNav = defineModel()

function handleNavClick(nav) {
  const {key, path, noSelect} = nav
  if (!key || key === currentNav.value) return;
  if (!noSelect) currentNav.value = key
  if (path) {
    router.push(path)
  }
}


const modalShowOfUpdatePwd = ref(false)
const modalShowOfSetting = ref(false)
const submitStatus = ref(false)
const formRef = useTemplateRef('formRef')
const form = ref({
  currentPassword: '',
  newPassword:'',
  confirmPassword: ''
})
function handleUpdatePwdCancel() {
  modalShowOfUpdatePwd.value = false
  formRef.value.reset()
}
function handleGoToAccountPage() {
  router.push("/system/account")
}
function handleSignOut() {
  if (confirm(t('message.confirmLogout'))) {
    signOut()
  }
}
function handleSubmitUpdatePwd() {
  if (submitStatus.value) return
  submitStatus.value = true
  let api = updateUserPwd
  if (isDefaultModifyPwd.value) {
    api = initializePassword
  }
  api({...form.value}).then(res=>{
    console.log(res)
    if (res.code === 200){
      isDefaultModifyPwd.value = false
      Message.success(t('message.updateSuccessPwd'))
      handleUpdatePwdCancel()
    }
  }).finally(()=>{
    submitStatus.value = false
  })
}

onMounted(() => {
  if (isDefaultModifyPwd.value){
    Message.warning(t('message.modifyPwd'))
    modalShowOfUpdatePwd.value = true
  }
})
</script>

<template>
  <div class="nav-container flex">
    <nav class="nav flex flex-col flex-shrink-0">
      <ul class="nav__menu flex flex-col items-center">
        <li class="nav__menu-item" v-for="item in navs" :key="item.key">
          <Tooltip :content="t(item.title)" position="right" :mouseEnterDelay="300" mini>
            <m-button size="small" type="base"
                      class="nav__menu-btn"
                      :aria-label="t(item.title)"
                      :class="{'nav__menu-btn--active': item.key === currentNav}"
                      @click="handleNavClick(item)">
              <template #icon>
                <component :is="item.iconComponent" size="20"></component>
              </template>
            </m-button>
          </Tooltip>
        </li>
        <li class="nav__menu-item">
          <divider thickness="2px"/>
        </li>
        <li class="nav__menu-item" v-for="item in navs2" :key="item.key">
          <Tooltip :content="t(item.title)" position="right" :mouseEnterDelay="300" mini>
            <m-button size="small" type="base" class="nav__menu-btn" :aria-label="t(item.title)"
                      @click="handleNavClick(item)">
              <template #icon>
                <component :is="item.iconComponent" size="20"></component>
              </template>
            </m-button>
          </Tooltip>
        </li>
      </ul>
      <div class="nav__footer flex flex-col items-center">
        <dropdown>
          <div>
            <m-button size="small" type="base" aria-label="user">
              <template #icon>
                <icon-user size="22"/>
              </template>
            </m-button>
          </div>
          <template #content>
            <dropdown-option align="center" @click="handleGoToAccountPage">{{userinfo.nickname}}</dropdown-option>
            <dropdown-option @click="modalShowOfUpdatePwd = true">{{t('layout.nav.updatePwd')}}</dropdown-option>
            <divider thickness="2px" direction="horizontal"/>
            <dropdown-option @click="handleSignOut" style="color: rgb(var(--danger-6))">
              <template #icon>
                <icon-log-out/>
              </template>
              {{t('layout.nav.SignOut')}}
            </dropdown-option>
          </template>
        </dropdown>
        <m-button size="small" type="base" @click="modalShowOfSetting=true" aria-label="settings">
          <template #icon>
            <icon-settings size="22"/>
          </template>
        </m-button>
      </div>
    </nav>
    <modal
        :show="modalShowOfUpdatePwd"
        width="600px"
        :title="isDefaultModifyPwd ? t('layout.nav.modifyPwd') : t('layout.nav.updatePwd')"
        @cancel="handleUpdatePwdCancel"
        :footer="false">
      <m-form ref="formRef" v-model="form" @submit="handleSubmitUpdatePwd" auto-label-width>
        <m-form-item v-if="!isDefaultModifyPwd" prop="currentPassword" :label="t('updatePwd.currentPassword')" required >
          <m-input v-model="form.currentPassword" autofocus :placeholder="t('updatePwd.currentPasswordPlaceholder')" type="password" autocomplete="off"></m-input>
        </m-form-item>
        <m-form-item prop="newPassword" :label="t('updatePwd.newPassword')" required>
          <m-input v-model="form.newPassword" :placeholder="t('updatePwd.newPasswordPlaceholder')" type="password" autocomplete="off"></m-input>
        </m-form-item>
        <m-form-item prop="confirmPassword" :label="t('updatePwd.confirmPassword')" required>
          <m-input v-model="form.confirmPassword" :placeholder="t('updatePwd.confirmPasswordPlaceholder')" type="password" autocomplete="off"></m-input>
        </m-form-item>
        <m-form-item>
          <m-button type="primary" size="large" style="margin-right: 16px" html-type="submit" :loading="submitStatus">{{ t('okText') }}</m-button>
          <m-button type="secondary" size="large" @click="handleUpdatePwdCancel">{{ t('cancelText') }}</m-button>
        </m-form-item>
      </m-form>
    </modal>
    <setting-modal v-model="modalShowOfSetting"></setting-modal>
  </div>
</template>

<style scoped>
.nav {
  position: relative;
  width: 100%;
  background-color: var(--color-bg-3);
  border-right: 1px solid var(--color-fill-4);
}

.nav .nav__menu {
  margin: 0;
  padding: 0 7px 0 8px;
  user-select: none;

  .nav__menu-item {
    display: flex;
    justify-content: center;
    width: 100%;
    margin-top: 16px;
    list-style-type: none;
  }

  .nav__menu-item .nav__menu-btn.nav__menu-btn--active {
    background-color: var(--color-fill-3);
  }
}

.nav .nav__footer {
  margin-top: auto;
  row-gap: 16px;
  padding: 24px 0;
}

</style>
