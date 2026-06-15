import {computed, ref} from "vue";
import localStorageUtil from "@/utils/lib/localStorageUtil.js";
import {commonKeys} from "@/constants/cacheKeys.js";
import {$t} from "@/locales";

/**
 * 标签页列表
 * @type {import('vue').Ref<Tab[]>}
 */
const tabsList = ref([
   /* {
        tabId: 1,
        name: 'newTab',
        path: '/newtab',
        fullPath: '/newtab',
        title: '新标签页',
        nodeId: null,
        tabIndex: 1,
        isPin: false,
        componentName: 'newTab' // 组件名称，用于缓存
    }*/
]);
/**
 * 缓存的组件名称列表
 * @type {import('vue').Ref<string[]>}
 */
const cachedViews = ref([]);
/**
 * 当前激活的标签页Id
 * @type {import('vue').Ref<Number | null>}
 */
const activeTabId = ref()
/** 标签页ID计数器 */
const tabId = ref(1)
/** 标签页Index计数器 */
const tabIndex = ref(1)

/**
 * 当前激活的标签页
 * @type {import('vue').ComputedRef<Tab> | null}
 */
const activeTab = computed(()=>tabsList.value.find(item=>item.tabId === activeTabId.value))

/**
 * 当前激活的标签页的资源id
 * @type {import('vue').ComputedRef<Number> | null}
 */
const activeTabRelatedId = computed(()=>activeTab.value?.relatedId || null)


function initTabsData(route) {
    let id =  localStorageUtil.get(commonKeys.ACTIVE_TAB_ID, null)
    const arr = localStorageUtil.get(commonKeys.TAB_LIST, [])
    arr.forEach((item, i) => {
        if (item.tabId === id) {
            // cachedViews.value = [item.componentName]
            id = i + 1
        }
        item.tabId = i + 1
        item.tabIndex = i + 1
    })
    tabId.value = Math.max(...arr.map(item=>item.tabId), 1)
    tabIndex.value = Math.max(...arr.map(item=>item.tabIndex), 1)
    if (arr.filter(item=>item.path === route.path).length <= 0) {
        const tab = _getTabByView(route)
        arr.push(tab)
        tabId.value = tab.tabId
        id = tab.tabId
    }
    // cachedViews.value = arr.map(item=>item.componentName)
    tabsList.value = arr
    activeTabId.value = id || tabId.value
}


/**
 * 根据视图对象获取标签页对象
 * @param {Object} view - 视图对象
 * @param {number} [reuseId] - 复用的tabId
 * @returns {Tab} 标签页对象
 */
function _getTabByView(view,reuseId) {
    if (view.meta?.title) {
        view.title = $t(`router.title.${view.name}`)
    }
    return {
        tabId: reuseId ?? ++tabId.value,
        name: view.name,
        path: view.path,
        fullPath: view.fullPath,
        title: view.title || view.meta?.title || $t('empty.untitled'),
        nodeId: view.nodeId || null,
        nodeType: view.nodeType || null,
        relatedId: view.relatedId || null,
        tabIndex: ++tabIndex.value,
        isPin: false,
        isOnly: view.isOnly || view.meta?.isOnly || false,
        componentName: view.componentName || view.name // 组件名称，用于缓存
    }
}
/**
 * 添加到缓存列表（去重）
 * @param {Tab} tab
 */
function addCachedView(tab) {
    const componentKey = tab.componentName || tab.name;
    if (!cachedViews.value.includes(componentKey)) {
        cachedViews.value.push(componentKey);
    }
}
/**
 * 从缓存列表中移除
 * @param {Tab} tab
 */
function removeCachedView(tab) {
    if (!tab?.componentName) return

    const cacheIndex = cachedViews.value.indexOf(tab.componentName)
    if (cacheIndex > -1 && countComponentNamesInTabsList(tab.componentName) === 1) {
        cachedViews.value.splice(cacheIndex, 1)
    }
}
/**
 * 添加标签页
 * @param {Tab} tab
 */
function addTab(tab) {
    tabsList.value.push(tab)
    handleTabsUpdate()
}

/**
 * 添加视图为标签页
 * @param {Object} view - 视图对象
 */
function addViewAsTab(view) {
    // console.log(view)
    const tab = _getTabByView(view)
    activeTabId.value = tab.tabId
    addCachedView(tab)
    addTab(tab)
    // console.log(activeTab.value)
}
/**
 * 替换当前标签页
 * @param {Object} view - 视图对象
 */
function  replaceCurrentTab(view) {
    if (!activeTabId.value) {
        addViewAsTab(view);
        return;
    }
    const index = tabsList.value.findIndex(item => item.tabId === activeTabId.value)
    if (index === -1) {
        addViewAsTab(view);
        return;
    }

    // 先移除旧标签页的缓存
    const oldTab = tabsList.value[index]
    if (oldTab.isPin || oldTab.isOnly) {
        addViewAsTab(view);
        return;
    }

    // 创建新标签页（复用旧tabId）
    const newTab = _getTabByView(view, oldTab.tabId)
    if (newTab.isOnly && oldTab.name !== 'newTab') {
        addViewAsTab(view);
        return;
    }
    Object.assign(oldTab, {
        ...newTab,
        // tabIndex: oldTab.tabIndex + 1
    })

    removeCachedView(oldTab)

    handleTabsUpdate()

    // 添加新缓存
    addCachedView(newTab)
    // activeTabId.value = newTab
}
/**
 * 更新当前激活的标签页
 * @param {Tab} tab - 标签页对象
 */
function updateCurrentTab(tab) {
    if (activeTabId.value !== tab.tabId) {
        // addCachedView(activeTab.value)
        activeTabId.value = tab.tabId
        addCachedView(tab)
        handleTabsUpdate()
    }
}

/**
 * 根据relatedId更新标签页
 * @param relatedId
 * @param nodeType
 * @param {string} title
 */
function updateTabTitleByRelatedId(relatedId,nodeType, title) {
    let update = false
    tabsList.value.forEach(tab => {
        if (tab.relatedId === relatedId  && tab.nodeType === nodeType) {
            tab.title = title;
            update = true
        }
    })
    if (update) {
        handleTabsUpdate()
    }
}
/**
 * 根据nodeId更新标签页
 * @param nodeId
 * @param {string} title
 */
function updateTabTitleByNodeId(nodeId,title) {
    let update = false
    tabsList.value.forEach(tab => {
        if (tab.nodeId === nodeId) {
            tab.title = title;
            update = true
        }
    })
    if (update) {
        handleTabsUpdate()
    }
}
function changeTabFixed(tab) {
    Object.assign(tab, { isPin: !tab.isPin })
    handleTabsUpdate()
}

/**
 * 删除指定标签页
 * @param {Tab} tab - 要删除的标签页
 * @returns {Promise<{tabsList: Tab[], currentPath: Tab | null}>}
 */
function removeTab(tab) {
    return new Promise(resolve => {
        const index = tabsList.value.findIndex(item => item.tabId === tab.tabId)
        if (index === -1) {
            resolve({ tabsList: tabsList.value })
            return
        }

        // 移除缓存
        removeCachedView(tabsList.value[index])

        // 处理激活标签页切换
        if (tab.tabId === activeTabId.value) {
            // 如果删除的是最后一个标签页，则激活前一个；否则激活后一个
            if (tabsList.value.length > 1) {
                activeTabId.value = index === tabsList.value.length - 1
                    ? tabsList.value[index - 1].tabId
                    : tabsList.value[index + 1].tabId;
            } else {
                activeTabId.value = null;
            }
            /*activeTab.value = tabsList.value.length > 1
                ? (index === tabsList.value.length - 1
                    ? tabsList.value[index - 1]
                    : tabsList.value[index + 1])
                : null;*/
        }


        // 从列表中移除标签页
        tabsList.value.splice(index, 1)
        handleTabsUpdate()
        resolve({ tabsList: tabsList.value })
    })
}

/**
 * 关闭全部标签
 */
function closedAllTabs() {
    tabsList.value = [];
    cachedViews.value = [];
    activeTabId.value = null;
}


/**
 * 检查视图是否已存在
 * @param {Object} view - 视图对象
 * @returns {Tab}
 */
function findTab(view) {
    return tabsList.value.find(item=>item.path === view.path)
}
/**
 * 统计标签页列表中组件名称出现的次数
 * @param {string} componentName - 组件名称
 * @returns {number} 出现次数
 */
function countComponentNamesInTabsList(componentName) {
    return tabsList.value.filter(item=>item.componentName === componentName).length
}

function handleTabsUpdate() {
    localStorageUtil.set(commonKeys.TAB_LIST, tabsList.value)
    localStorageUtil.set(commonKeys.ACTIVE_TAB_ID, activeTabId.value)
}

/**
 * tabsStore
 * @description 标签页状态管理模块 提供标签页的添加、删除、切换、缓存等核心功能
 */
export const useTabsStore = () => {

    return {
        initTabsData,
        addViewAsTab,
        tabsList,
        cachedViews,
        addCachedView,
        removeTab,
        closedAllTabs,
        replaceCurrentTab,
        updateCurrentTab,
        updateTabTitleByRelatedId,
        updateTabTitleByNodeId,
        activeTab,
        tabIndex,
        activeTabId,
        activeTabRelatedId,
        findTab,
        countComponentNamesInTabsList,
        changeTabFixed
    }
}
