<script setup>
import {ref} from "vue";
import {useRouter} from "vue-router";
import {useThemeStore, useUserStore} from "@/store/index.js";
import {useI18n} from "vue-i18n";
import {UserType} from "@/enum/index.js";
import localStorageUtil from "@/utils/lib/localStorageUtil.js";
import {commonKeys} from "@/constants/cacheKeys.js";
import dayjs from "@/utils/lib/dayjs.js";

const router = useRouter()
const {userType} = useUserStore()
const {loadTheme, currentTheme} = useThemeStore()
const {locale, t} = useI18n()

const show = defineModel({type: Boolean})
const menuData = [
  {
    id: 1,
    label: 'setting.menu.accountAndSecurity',
  },
  {
    id: 2,
    label: 'setting.menu.universal',
  },
  {
    id: 4,
    label: 'setting.menu.regarding',
  },
]
const locales = [
  {
    value: 'zh-CN',
    label: '简体中文'
  }, {
    value: 'en-US',
    label: 'English'
  }
]
const themes = [
  {
    value: 'system',
    label: 'setting.themes.system'
  }, {
    value: 'light',
    label: 'setting.themes.light'
  }, {
    value: 'dark',
    label: 'setting.themes.dark'
  }
]


const currentMenu = ref(1)
const theme = ref(currentTheme.value)

/**
 * 切换左侧设置菜单高亮项。
 */
function handleSelectMenu(item) {
  currentMenu.value = item.id
}


/**
 * 切换主题模式并持久化。
 */
function handleChangeTheme() {
  loadTheme(theme.value)
}

/**
 * 切换语言并同步 dayjs 与本地缓存。
 */
function handleChangeLocales(e) {
  // 需要刷新
  locale.value = e.target.value
  dayjs.locale(locale.value === 'zh-CN' ? 'zh-cn' : 'en')
  localStorageUtil.set(commonKeys.LANGUAGE, locale.value)
}

/**
 * 跳转至独立的用户管理页面，并关闭设置弹窗。
 */
function handleGoToUserManagementPage() {
  show.value = false
  router.push("/system/user")
}
</script>

<template>
  <modal :show="show" width="900px" :header="false" @cancel="show=false" :footer="false" :body-style="{padding: '0'}">
    <div class="setting-modal-body flex">
      <div class="setting-sidebar">
        <h3>{{ t('setting.title') }}</h3>
        <template v-for="item in menuData" :key="item.id">
          <div class="setting-sidebar-item"
               @click="handleSelectMenu(item)"
               v-if="!item.useType || item.useType === userType"
               :class="{'setting-sidebar-item-selected': item.id === currentMenu}"
               >{{ t(item.label) }}
          </div>
        </template>
      </div>
      <divider direction="vertical" style="height: auto;padding: 0"></divider>
      <div class="setting-content dress-up-scrollbar">
        <h4>{{t('setting.menu.universal')}}</h4>
        <m-form auto-label-width label-align="left" style="width: 400px" layout="vertical">
          <m-form-item :label="t('setting.themeMode')">
            <div class="flex flex-wrap" style="gap: 8px">
              <m-radio v-for="item in [themes[0]]" :value="item.value"  v-model="theme" @change="handleChangeTheme">
                {{ t(item.label) }}
              </m-radio>
              <m-radio-group @change="handleChangeTheme" v-model="theme">
                <m-radio v-for="item in [themes[1],themes[2]]" :key="item.value"
                         :value="item.value"
                         class="margin-0 padding-0">
                  <template #radio>
                    <div style="width: 192px;border: 1px solid var(--color-neutral-3);border-radius: var(--border-radius-medium);">
                      <div v-if="item.value==='dark'" style="padding: var(--spacing-4);background-color: var(--color-menu-dark-bg);border-top-left-radius: var(--border-radius-medium);border-top-right-radius: var(--border-radius-medium);">
                        <div class="w-fill flex" style="gap: 8px">
                          <div style="background-color: rgba(255, 255, 255, 0.16);border-radius: 50%;width: 20px;height: 20px"/>
                          <div style="width: 130px;background-color: rgba(255, 255, 255, 0.08);height: 20px;"></div>
                        </div>
                        <br>
                        <div class="w-fill flex" style="gap: 8px">
                          <div style="background-color: rgba(255, 255, 255, 0.16);border-radius: 50%;width: 20px;height: 20px"/>
                          <div style="width: 100px;background-color: rgb(14,50,166);height: 20px;"></div>
                        </div>
                      </div>
                      <div v-else style="padding: var(--spacing-4);background-color: #fff;border-top-left-radius: var(--border-radius-medium);border-top-right-radius: var(--border-radius-medium);">
                        <div class="w-fill flex" style="gap: 8px">
                          <div style="background-color: rgb(201,205,212);border-radius: 50%;width: 20px;height: 20px"/>
                          <div style="width: 130px;background-color: rgb(242,243,245);height: 20px;"></div>
                        </div>
                        <br>
                        <div class="w-fill flex" style="gap: 8px">
                          <div style="background-color: rgb(201,205,212);border-radius: 50%;width: 20px;height: 20px"/>
                          <div style="width: 100px;background-color: rgb(148,191,255);height: 20px;"></div>
                        </div>
                      </div>
                      <divider margin="0"/>
                      <div style="padding: var(--spacing-4) var(--spacing-2) var(--spacing-2)">
                        <m-radio :value="item.value">{{ t(item.label) }}</m-radio>
                      </div>
                    </div>
                  </template>
                </m-radio>
              </m-radio-group>
            </div>
          </m-form-item>
          <m-form-item :label="t('setting.displayLanguage')">
            <select @change="handleChangeLocales">
              <option v-for="item in locales" :value="item.value" :selected="locale === item.value">{{ item.label }}
              </option>
            </select>
          </m-form-item>
          <m-form-item v-if="UserType.isAdmin(userType)" :label="t('setting.menu.userManagement')">
            <m-button type="outline" @click="handleGoToUserManagementPage">
              {{ t('setting.goToUserManagement') }}
            </m-button>
          </m-form-item>
        </m-form>
        <divider/>
        <h4>{{t('setting.menu.regarding')}}</h4>
        <h4>快捷键</h4>
        <table style="letter-spacing: 1px;border-collapse: collapse;">
          <thead>
          <tr>
            <th>操作</th>
            <th>win</th>
            <th>mac</th>
          </tr>
          </thead>
          <tbody>
          <tr>
            <td>1~6级标题</td>
            <td>Ctrl + Alt + 1~6</td>
            <td>⌘ + ⌥ + 1~6</td>
          </tr>
          <tr>
            <td>加粗</td>
            <td>Ctrl + B</td>
            <td>⌘ + B</td>
          </tr>
          <tr>
            <td>斜体</td>
            <td>Ctrl + I</td>
            <td>⌘ + I</td>
          </tr>
          <tr>
            <td>引用</td>
            <td>Ctrl + Shift + ></td>
            <td>⌘ + Shift + ></td>
          </tr>
          <tr>
            <td>删除线</td>
            <td>Ctrl + Shift + X</td>
            <td>⌘ + Shift + X</td>
          </tr>
          <tr>
            <td>代码块</td>
            <td>Ctrl + Shift + C</td>
            <td>⌘ + Shift + C</td>
          </tr>
          </tbody>
        </table>
        <!--          <div style="height: 1000px;"></div>-->
      </div>
    </div>
  </modal>
</template>

<style scoped lang="scss">
.setting-sidebar {
  flex: 1;
  padding: 0 16px;
  background-color: var(--color-fill-1);
}

.setting-sidebar-item {
  margin: 2px;
  padding: 8px 16px;
  line-height: 1.2974;
  border-radius: 4px;
  user-select: none;
}

.setting-sidebar-item:hover {
  color: rgb(var(--primary-5));
}

.setting-sidebar-item.setting-sidebar-item-selected {
  background-color: var(--color-bg-5);
  color: rgb(var(--primary-6));
}

.setting-content {
  flex: 3;
  max-height: 60vh;
  padding: 0 16px 2.5em;
  margin-top: 2.5em;
  overflow-y: scroll;
  //background-color: var(--color-fill-1);
}

.setting-content h4 {
  margin-block-start: 1em;
  margin-block-end: 1em;
}

.gray-text {
  color: var(--color-text-3);
  font-size: 0.875em;
}

.passkey-operation-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

th, td {
  border: 1px solid rgb(160 160 160);
  padding: 8px 10px;
}
thead,
tfoot {
  background-color: rgb(228 240 245);
}
</style>
