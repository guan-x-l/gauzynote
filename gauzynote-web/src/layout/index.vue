<script setup>
import {onMounted, ref, watch} from "vue";
import {useRoute, useRouter} from "vue-router";
import Sidebar from "@/layout/components/Sidebar/index.vue";
import {useAppStore, useTabsStore, useUserStore} from "@/store/index.js";
import {MLoading} from "@/components/index.js";
import SidebarRight from "@/layout/components/SidebarRight/index.vue";
import {NodeType} from "@/enum/index.js";
import LayoutTabs from "@/layout/components/LayoutTabs.vue";
import {createUUID, domUtils} from "@/utils/index.js";
import {getImageTab, getNoteTab} from "@/biz/tabs.js";
import {useI18n} from "vue-i18n";
import Tooltip from "@/components/base/Tooltip/Tooltip.vue";
import Split from "@/components/container/Split/Split.vue";
import {commonKeys} from "@/constants/cacheKeys.js";
import LAYOUT from "@/constants/layout.js";
import {useSidebar} from "@/layout/hooks/useSidebar.js";
import LeftNav from "@/layout/components/LeftNav.vue";

const {updateUserinfo} = useUserStore()
const {dragNodeId, resourceNodeList, initAppData, initResourceNode, getResourceNodeByRelatedId} = useAppStore()
const {
  initTabsData,
  activeTab,
  findTab,
  addViewAsTab,
  replaceCurrentTab,
  tabsList,
  cachedViews,
  updateCurrentTab,
  closedAllTabs,
  addCachedView,
  activeTabId,
} = useTabsStore()

initAppData()
initTabsData()

const initStatus = ref(false)
const route = useRoute()
const router = useRouter()
const {t} = useI18n()

const currentMenu = ref('folderList')

// 存储动态加载的组件列表
let dynamicComponentsMap = {};
// 当前激活的动态组件键名，每个tab的键都是唯一，键结构为 `${route.name}_${route.params.id}_${uuid}`
const activeComponentKey = ref(null);

// 监听路由路径变化
watch(route, async () => {
  // console.log('route changed:');
  await loadAndSwitchDynamicComponent();
  await initView();
  updateDynamicComponentMap()
}, {deep: true});
// 处理相同路径不同tab切换
watch(activeTabId, async () => {
  if (route.path !== activeTab.value?.path) return
  // console.log('activeTab changed:');
  await loadAndSwitchDynamicComponent();
  await initView();
  updateDynamicComponentMap()
}, {deep: true});

function handleReplaceCurrentTab() {
  let nodeTypeEnum = NodeType.getByInfo(route.name);
  if (!nodeTypeEnum) {
    replaceCurrentTab(route)
    return
  }
  const {nodeId, nodeType, relatedId, nodeName} = getResourceNodeByRelatedId(+route.params.id, nodeTypeEnum.getCode()) || {}
  replaceCurrentTab({
    ...route,
    componentName: activeComponentKey.value,
    title: nodeName || activeComponentKey.value,
    nodeId,
    nodeType,
    relatedId,
  })
}

/**
 * 加载动态组件并切换激活状态
 * 逻辑：根据路由元信息中的dynamicComponent加载组件
 */
async function loadAndSwitchDynamicComponent() {
  activeComponentKey.value = null
  if (!route.meta.dynamicComponent) return;
  // const componentKey = `${route.name}_${route.params.id}`
  let componentKey = activeTab.value?.componentName
  if (!activeTab.value?.componentName.startsWith(`${route.name}_${route.params?.id || ''}`)) {
    componentKey = `${route.name}_${route.params?.id||''}_${createUUID()}`
    handleReplaceCurrentTab()
  }
  if (!dynamicComponentsMap[componentKey]) {
    await route.meta.dynamicComponent.then(res => {
      dynamicComponentsMap[componentKey] = {...res.default, name: componentKey}
    })
  }
  activeComponentKey.value = componentKey
}

/**
 * 根据键名移除动态组件
 */
function updateDynamicComponentMap() {
  const keys = tabsList.value.map(item => item.componentName)
  for (const key in dynamicComponentsMap) {
    if (!keys.includes(key)) {
      delete dynamicComponentsMap[key]
    }
  }
}

/**
 * 处理标签页点击事件
 * @param {Tab} tab - 标签页对象
 */
function handleTabClick(tab) {
  router.push(tab.fullPath)
  updateCurrentTab(tab)
}

/**
 * 初始化视图并添加到标签页
 */
async function initView() {
  // 若视图已存在则直接返回
  const tab = findTab(route)
  if (tab) {
    if (tab.isOnly && activeTabId.value !== tab.tabId) {
      activeTabId.value = tab.tabId
    }
    return
  }
  handleReplaceCurrentTab()
}

function addNewTab() {
  activeComponentKey.value = null
  router.push('/newtab')
  addViewAsTab({
    "path": "/newtab",
    "name": "newTab",
    "fullPath": "/newtab",
    "meta": {
      "title": t("router.title.newTab")
    }
  })
  // addViewAsTab(route)
}

function handleCloseAllTabs() {
  closedAllTabs()
  addNewTab()
  dynamicComponentsMap = {}
}


function handleDragover(e) {
  domUtils('#dragImgDom').setStyle({
    left: `${e.clientX}px`,
    top: `${e.clientY}px`,
  }).query('.drop-node-name').get().innerText = t('resourceNode.newTab')
  e.dataTransfer.dropEffect = 'copy';
}

function handleDragleave(e) {
  document.querySelector('#dragImgDom .drop-node-name').innerText = ``
}

function handleDrop(e) {
  for (let i = 0; i < resourceNodeList.value.length; i++) {
    if (resourceNodeList.value[i].nodeId === dragNodeId.value) {
      let tab = getNoteTab(resourceNodeList.value[i])
      if (NodeType.isImage(resourceNodeList.value[i].nodeType)) {
        tab = getImageTab(resourceNodeList.value[i])
      }
      addViewAsTab(tab)
      router.push(tab.path)
      break
    }
  }
}


const {minWidth, maxWidth, defaultSize, sidebarVisible, size, splitRef, onMoving, onMoveStart, onMoveEnd, onSidebarVisibleChange} = useSidebar({
  minWidth: LAYOUT.LEFT_SIDEBAR_NAV_WIDTH,
  maxWidth: 0.8,
  sizeKey: commonKeys.BOUNDARY_LEFT_WIDTH,
  visibleKey: commonKeys.BOUNDARY_LEFT_VISIBLE,
  defaultSize: LAYOUT.MIN_LEFT_SIDEBAR_LIST_WIDTH + LAYOUT.LEFT_SIDEBAR_NAV_WIDTH,
  defaultVisible: true,
  splitRefKey: 'splitRef',
  handleMoving: (e)=>{
    const left = splitRef.value.$el.getBoundingClientRect().left
    if (sidebarVisible.value && e.x < left + minWidth) {
      sidebarVisible.value = false
    } else if (e.x - left > minWidth) {
      sidebarVisible.value = true
    }
  },
  handleMoveStart: () => domUtils(splitRef.value.firstPaneRef).addClass('no-transition'),
  handleMoveEnd: () => domUtils(splitRef.value.firstPaneRef).removeClass('no-transition')
})

const {minWidth:minWidthRight, defaultSize:defaultSizeRight, sidebarVisible: sidebarVisibleRight, size: sizeRight, splitRef: splitRightRef, onMoving: onRightMoving, onMoveStart: onRightMoveStart, onMoveEnd: onRightMoveEnd, onSidebarVisibleChange: onSidebarVisibleRightChange} = useSidebar({
  minWidth: 0,
  maxWidth: 0.8,
  sizeKey: commonKeys.BOUNDARY_RIGHT_WIDTH,
  visibleKey: commonKeys.BOUNDARY_RIGHT_VISIBLE,
  defaultSize: LAYOUT.MIN_RIGHT_SIDEBAR_LIST_WIDTH + 1,
  defaultVisible: true,
  splitRefKey: 'splitRightRef',
  handleMoving: (e)=>{
    const right = splitRightRef.value.$el.getBoundingClientRect().right
    if (sidebarVisibleRight.value && e.x + 40 > right) {
      sidebarVisibleRight.value = false
    } else if (e.x + defaultSizeRight < right) {
      sidebarVisibleRight.value = true
    }
  },
  handleMoveStart: () => domUtils(splitRightRef.value.secondPaneRef).addClass('no-transition'),
  handleMoveEnd: () => domUtils(splitRightRef.value.secondPaneRef).removeClass('no-transition')
})

onMounted(() => {
  const load = MLoading.show()
  updateUserinfo();
  initResourceNode().then(res => {
    initStatus.value = true
    if (activeTab.value) {
      if (route.meta.dynamicComponent) {
        loadAndSwitchDynamicComponent()
      }
      addCachedView(activeTab.value)
      router.replace(activeTab.value.fullPath)
    } else {
      initView()
    }
    load.close()
  })
})
</script>

<template>
  <section class="layout flex" v-if="initStatus">
    <split v-model:size="size" class="layout__left-split flex-1" ref="splitRef"
           :min="sidebarVisible ? defaultSize : minWidth"
           :max="sidebarVisible ? maxWidth : minWidth"
           :disabled="!sidebarVisible"
           @move-start="onMoveStart"
           @move-end="onMoveEnd"
           @moving="onMoving">
      <template #first>
        <div class="layout__left-sidebar flex flex-col">
          <div class="layout__left-sidebar-header flex items-center">
            <Tooltip :content="sidebarVisible ? t('action.collapse') : t('action.expand')" position="right" :mouseEnterDelay="300" mini>
              <m-button size="small" type="base" @click="onSidebarVisibleChange" :aria-label="sidebarVisible ? t('action.collapse') : t('action.expand')">
                <template #icon>
                  <icon-sidebar size="24"/>
                </template>
              </m-button>
            </Tooltip>
          </div>
          <div class="layout__left-sidebar-container__wrapper flex flex-1">
            <left-nav v-model="currentMenu"></left-nav>
            <Sidebar :current-menu="currentMenu"></Sidebar>
          </div>
        </div>
      </template>
      <template #second>
        <split
            ref="splitRightRef"
            resizeSecond
            style="height: 100%"
            :min="sidebarVisibleRight ? defaultSizeRight : minWidthRight"
            :max="sidebarVisibleRight ? maxWidth : minWidthRight"
            :disabled="!sidebarVisibleRight"
            @move-start="onRightMoveStart"
            @move-end="onRightMoveEnd"
            @moving="onRightMoving"
            class="layout__right-split flex-1"
            v-model:size="sizeRight">
          <template #first>
            <main class="layout__container flex-1 flex flex-col">
              <div class="layout__header flex items-center" @dragover.prevent="handleDragover" @drop.prevent="handleDrop"
                   @dragleave="handleDragleave">
                <layout-tabs @initView="initView" @handleTabClick="handleTabClick"></layout-tabs>
                <Tooltip :content="t('router.title.newTab')" position="bottom" :mouseEnterDelay="300" mini>
                  <m-button class="layout__tabs-push-btn" type="base" size="small" shape="circle" @click="addNewTab" :aria-label="t('router.title.newTab')">
                    <template #icon>
                      <icon-plus size="20"/>
                    </template>
                  </m-button>
                </Tooltip>
                <div class="layout__header-suffix flex items-center">
                  <dropdown position="bl">
                    <m-button size="small" type="base" :style="{'margin-right': sidebarVisibleRight ? '0' : '42px'}" aria-label="tabs down">
                      <template #icon>
                        <icon-chevron-down size="24"/>
                      </template>
                    </m-button>
                    <template #content>
                      <dropdown-option v-for="item in tabsList" @click="handleTabClick(item)">
                        <template #icon>
                          <icon-file-text v-if="item.nodeId"/>
                          <icon-file v-else/>
                        </template>
                        <div style="max-width: 200px" class="text-ellipsis">{{ item.title }}</div>
                      </dropdown-option>
                      <divider/>
                      <dropdown-option @click="handleCloseAllTabs">
                        <template #icon>
                          <icon-close/>
                        </template>
                        {{ t('action.closeAll') }}
                      </dropdown-option>
                    </template>
                  </dropdown>
                  <m-button size="small" type="base" @click="onSidebarVisibleRightChange" class="layout__right-sidebar-btn" :aria-label="sidebarVisible ? t('action.collapse') : t('action.expand')">
                    <template #icon>
                      <icon-sidebar size="24" rotate="180"/>
                    </template>
                  </m-button>
                </div>
              </div>
              <section class="flex-1 flex min-h-0" style="padding-right: 3px">
                <router-view v-slot="{Component, route}">
                  <KeepAlive :include="cachedViews">
                    <component v-if="activeComponentKey" :is="dynamicComponentsMap[activeComponentKey]"
                               :key="activeComponentKey" :componentKey="activeComponentKey"
                               :data-key="activeComponentKey"/>
                    <component v-else :is="Component" :key="route.fullPath"/>
                  </KeepAlive>
                </router-view>
              </section>
            </main>
          </template>
          <template #second>
            <sidebar-right :currentMenu="currentMenu"></sidebar-right>
          </template>
        </split>
      </template>
    </split>
  </section>
</template>

<style lang="scss" scoped>
.layout {
  width: 100%;
  height: 100%;
  background-color: var(--color-bg-1);
}

.layout__left-split{
  width: 100%;
}

.layout__left-sidebar{
  height: 100vh;
  overflow: auto;
}
.layout__left-sidebar .layout__left-sidebar-header {
  height: 40px;
  padding: 0 8px;
  border-bottom: 1px solid var(--color-fill-4);
  background-color: var(--color-bg-3);
}
.layout__left-sidebar-container__wrapper{
  overflow: hidden;
}

.layout__container {
  position: relative;
  min-width: 0;
  height: 100vh;
  overflow: hidden;
}

.layout__header {
  position: relative;
  height: 40px;
  padding: 0 16px;
  background-color: var(--color-bg-3);
  border-bottom: 1px solid var(--color-fill-4);
}

.layout__header-suffix {
  margin-left: auto;
}

.layout__tabs-push-btn {
  border-radius: 50%;
  padding: 4px;
  margin-left: 4px;
  margin-right: 18px;
}

.layout__right-sidebar-btn {
  position: fixed;
  right: 8px;
  z-index: 2;
}

:deep(.layout__left-split > .split-pane.split-pane-first) {
  transition: flex 0.2s ease-out;
}

:deep(.layout__right-split > .split-pane.split-pane-second) {
  transition: flex 0.2s ease-out;
  overflow: hidden;
}
</style>
