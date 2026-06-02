<script setup>
import Tree from "@/components/container/tree/tree.vue";
import {useRoute, useRouter} from "vue-router";
import {addNodeAndNote, addResourceNode, deleteResourceNode, updateResourceNodeName, updateResourceNodeParentId} from "@/api/resourceNode.js";
import { nextTick, ref, useTemplateRef, watch} from "vue";
import TreeNodeItem from "@/components/container/tree/TreeNodeItem.vue";
import {useAppStore, useTabsStore} from "@/store/index.js";
import {createResourceNode} from "@/biz/resourceNode.js";
import {
  debounce,
  domUtils,
  getFileExtension,
  getFileNameWithoutExtension, objDeepClone,
  throttle
} from "@/utils/index.js";
import {Message, MLoading} from "@/components/index.js";
import {useWindowSize} from "@/hooks/index.js";
import {handleUploadFileList} from "@/biz/upload.js";
import {NodeType} from "@/enum/index.js";
import {getFileTab, getNoteTab} from "@/biz/tabs.js";
import {useI18n} from "vue-i18n";
import Divider from "@/components/base/Divider/Divider.vue";
import Tooltip from "@/components/base/Tooltip/Tooltip.vue";
import {uploadNetworkImage} from "@/api/upload.js";
import {$t} from "@/locales/index.js";


defineProps({
  currentMenu: String,
})
const {t} = useI18n()
const {resourceNodeList, resourceNodeListVersion, initResourceNode, updateResourceNodeNameByNodeId, getResourceNodeByNodeId, getResourceNodeByRelatedId, pushResourceNodeList, needFocusAssetNameIdList, dragNodeId} = useAppStore()
const {activeTab,activeTabId, tabsList, addViewAsTab, replaceCurrentTab, updateTabTitleByNodeId, removeTab} = useTabsStore()
const router = useRouter()
const route = useRoute()

// 选中的节点ID列表
const selectedNodeIds = ref([])
// 右键的目标节点
const contextmenuNode = ref()
// 当前展开的下拉菜单节点
const openedDropdownNode = ref(null)
// 重命名的节点id
const renameNodeId = ref(null)
const renameNodeName = ref('')

const expandCollapseStatus = ref(false)

const treeRef = useTemplateRef('treeRef')

const renameInputRef = useTemplateRef('renameInputRef')

// 拖拽的目标元素的节点id
const dragenterNodeId = ref(null)


// 监听活跃标签页变化，同步更新选中节点
watch(activeTab, updateSelectedIdsByActiveTab, {immediate: true, deep: true})

/**
 * 根据活跃标签页更新选中的节点ID
 */
function updateSelectedIdsByActiveTab() {
  if(!activeTab.value) return
  const relatedId = activeTab.value.relatedId
  if (relatedId) {
    selectedNodeIds.value = [getResourceNodeByRelatedId(relatedId, activeTab.value.nodeType)?.nodeId]
    nextTick(handlePositioningClick)
  } else {
    selectedNodeIds.value = []
  }
}

/**
 * 处理树形结构选择事件
 * @param {Object} nodeItem - 选中的节点对象
 */
function handleTreeSelect(nodeItem) {
  if (window.isFormDirty.has(activeTab.value.relatedId)) {
    if(confirm("您有未保存的更改，确定要离开吗？")){
      window.isFormDirty.delete(activeTab.value.relatedId)
    } else {
      selectedNodeIds.value = [activeTab.value.nodeId]
      return
    }
  }
  if (NodeType.isNote(nodeItem.nodeType)) {
    const tab = getNoteTab(nodeItem)
    // if (tab.componentName === activeTab.value.componentName) return
    if (tab.path === activeTab.value.path) return
    replaceCurrentTab(tab)
    router.push(tab.path)
  } else if (NodeType.isFile(nodeItem.nodeType)){
    const tab = getFileTab(nodeItem)
    replaceCurrentTab(tab)
    router.push(tab.path)
  }
}

/**
 * 更新资源节点数据
 * @param {string} [selectedId] - 选中的节点ID（可选）
 */
async function updateResourceNodeData(selectedId) {
  await initResourceNode()
  if (selectedId) {
    selectedNodeIds.value = [selectedId]
    await nextTick(handlePositioningClick)
  } else if (!selectedNodeIds.value.length && activeTab.value) {
    updateSelectedIdsByActiveTab()
  }
}


/**
 * 新增文件夹
 * @param {ResourceNode} [nodeItem] - 目标节点
 * @version 1
 */
function addFolder(nodeItem) {
  const resourceNode = createResourceNode(nodeItem, NodeType.FOLDER.getCode(), resourceNodeList.value)
  addResourceNode(resourceNode).then(async res => {
    pushResourceNodeList(res.data)
    await updateResourceNodeData()
    if (nodeItem?.nodeId){
      treeRef.value?.expandById && treeRef.value.expandById(nodeItem.nodeId)
    }
    renameNodeId.value = res.data.nodeId
    renameNodeName.value = res.data.nodeName
    await nextTick(()=>{
      renameInputRef.value.focus()
      renameInputRef.value.select()
    })
  })
}

/**
 * 新增笔记
 * @param {ResourceNode} [nodeItem] - 目标节点
 * @version 1
 */
function addNote(nodeItem) {
  const resourceNode = createResourceNode(nodeItem, '2', resourceNodeList.value)
  addNodeAndNote(resourceNode).then(async res => {
    pushResourceNodeList(res.data)
    // 刷新节点列表并选中新创建的节点
    await updateResourceNodeData(res.data.nodeId);
    if (nodeItem?.nodeId){
      treeRef.value?.expandById && treeRef.value.expandById(nodeItem.nodeId)
    }
    needFocusAssetNameIdList.value.push(res.data.relatedId)
    // 跳转到新笔记页面
    // router.push(`/note/${res.data.relatedId}`)
    handleTreeSelect(res.data)
  })
}


// 树形结构根节点的下拉菜单项
const treeRootDropdownItems = [
  { key: 'refresh', label: ()=> t('action.refresh'), icon: 'IconRotateCw'},
  { type: 'hr'},
  { key: 'newNote', label: ()=>  t('action.newNote'), icon: 'IconFilePlus' },
  { key: 'newFolder', label: ()=>  t('action.newFolder'), icon: 'IconFolderPlus' },
];
const handleTreeDropdownSelect = (key) => {
  console.log('选中的根节点菜单项:', key);
  if (openedDropdownNode.value) {
    handleTreeNodeDropdownSelect(key, openedDropdownNode.value)
    return
  }
  if (key === 'newNote'){
    addNote()
  } else if (key === 'newFolder'){
    addFolder()
  }else if (key === 'refresh'){
    initResourceNode()
  }
}

// 树形结构节点的下拉菜单项
const treeNodeDropdownItems = [
  { key: 'newNote', label: ()=>  t('action.newNote'), icon: 'IconFilePlus' },
  { key: 'newFolder', label: ()=>  t('action.newFolder'), icon: 'IconFolderPlus' },
  { key: 'rename', label: ()=>  t('action.rename'), icon: 'IconEdit3' },
  { type: 'hr'},
  { key: 'delete', label: ()=>  t('action.delete'), style: {color: 'rgb(var(--danger-6))'}, icon: 'iconTrash' },
];
// 树形结构节点的下拉菜单项
const treeNodeDropdownItems2 = [
  { key: 'newTab', label: ()=>  t('action.newTabOpen'), icon: 'IconTab' },
  { key: 'rename', label: ()=>  t('action.rename'), icon: 'IconEdit3' },
  { type: 'hr'},
  { key: 'delete', label: ()=>  t('action.delete'), style: {color: 'rgb(var(--danger-6))'}, icon: 'IconTrash'},
];
/**
 * 处理节点下拉菜单选择事件
 * @param {Object} key - 选中的菜单项
 * @param {Object} nodeItem - 触发菜单的节点对象
 */
async function handleTreeNodeDropdownSelect(key, nodeItem) {
  console.log('当前节点:', nodeItem);
  const {nodeId, nodeName, nodeType} = nodeItem
  if (key === 'newTab' && nodeId) {
    for (let i = 0; i < resourceNodeList.value.length; i++) {
      if (resourceNodeList.value[i].nodeId === nodeId) {
        let tab = getNoteTab(resourceNodeList.value[i])
        if (NodeType.isFile(resourceNodeList.value[i].nodeType)){
          tab = getFileTab(resourceNodeList.value[i])
        }
        addViewAsTab(tab)
        router.push(tab.path)
        break
      }
    }
  } else if(key === 'delete' && nodeId){
    for (let i = 0; i < resourceNodeList.value.length; i++) {
      if (resourceNodeList.value[i].nodeId === nodeId) {
        const r= confirm(t('resourceNode.confirmDel') + resourceNodeList.value[i].nodeName);
        if (r === true) {
          await deleteResourceNode(resourceNodeList.value[i].nodeId)
          // todo,新增或删除是否需要刷新列表，或者直接入数组中，节约资源
          updateResourceNodeData().then(res=>{
            tabsList.value.forEach(tab=>{
              if(tab.nodeId) {
                if (!resourceNodeList.value.find(fi=>fi.nodeId === tab.nodeId)){
                  removeTab(tab)
                }
              }
            })
            if (activeTab.value?.fullPath) {
              router.push(activeTab.value.fullPath)
            } else if (route.path !== '/newtab') {
              router.push('/newtab')
            }
          })
        }
        break
      }
    }
  } else if (key === 'newNote'){
    addNote(nodeItem)
  } else if (key === 'newFolder'){
    addFolder(nodeItem)
  } else if (key === 'rename'){
    renameNodeId.value = nodeId
    if (NodeType.isFile(nodeType)){
      renameNodeName.value = getFileNameWithoutExtension(nodeName)
    } else {
      renameNodeName.value = nodeName
    }
    nextTick(()=>{
      // renameInputRef.value.focus()
      renameInputRef.value.select()
    })
  }
}
/**
 * 处理节点下拉菜单展开/关闭事件
 * @param {boolean} [open] - 是否展开
 * @param {Object} [nodeItem] - 节点对象
 */
function handleTreeNodeDropdownOpenChange(open, nodeItem) {
  openedDropdownNode.value = open ? nodeItem : null;
  contextmenuNode.value = null
}

function handleExpandCollapseClick() {
  expandCollapseStatus.value = !expandCollapseStatus.value
  treeRef.value?.expandAll && treeRef.value.expandAll(expandCollapseStatus.value)
}
function handlePositioningClick() {
  treeRef.value?.expandSelectedIds && treeRef.value.expandSelectedIds()
}

const handleBlurRename = throttle((value) => {
  console.log(value)
  const currentNode = getResourceNodeByNodeId(renameNodeId.value)
  if (currentNode.nodeName === value || !value) {
    renameNodeId.value = null
    return
  }
  let extension = NodeType.isFile(currentNode.nodeType) ? getFileExtension(currentNode.nodeName, true, true) : ''
  const data = {
    nodeId: renameNodeId.value,
    nodeName: value + extension
  }
  // console.log(data)
  updateResourceNodeName(data).then(res=>{
    if (res.code === 200){
      // note.value.noteName = data.noteName
      updateTabTitleByNodeId(data.nodeId, data.nodeName)
      updateResourceNodeNameByNodeId(data.nodeId, data.nodeName)
    } else {
      renameNodeName.value = currentNode.nodeName
      // Message.error('保存失败')
    }
    renameNodeId.value = null
  })
}, 1000)
const {domHidden} = useWindowSize({watchSize: false})

watch(domHidden, ()=>{
  dragenterNodeId.value = null
})

function handleTreeNodeItemDragover(e,node) {
  if(dragNodeId.value){
    const dropNodeName = NodeType.isFolder(node.nodeType) ? node.nodeName : getResourceNodeByNodeId(node?.parentId)?.nodeName || t('resourceNode.root')
    domUtils('#dragImgDom').setStyle({
      left: `${e.clientX}px`,
      top: `${e.clientY}px`,
    }).query('.drop-node-name').get().innerText = `${t('resourceNode.moveTo')} “${dropNodeName}”`
    e.dataTransfer.dropEffect = 'move';
  }
  if (dragenterNodeId.value === node.nodeId) return
  let id = node?.parentId || '0'
  if(NodeType.isFolder(node.nodeType)) {
    id = node.nodeId
  }
  if (id !== dragenterNodeId.value){
    dragenterNodeId.value = id
  }
}
function clearDragenterNodeId() {
  // if (dragNode.value) return
  dragenterNodeId.value = null
}
function handleBodyDragover(ev) {
  domUtils('#dragImgDom').setStyle({
    left: `${ev.clientX}px`,
    top: `${ev.clientY}px`
  })
}

/**
 * @param e 元素
 * @param {Object} dragNode 拖拽节点
 * @param {Object} dropNode 拖拽至目标的节点
 */
async function handleDrop(e, dragNode, dropNode) {
  const dt = e.dataTransfer
  /*  e.dataTransfer.items[0].getAsString((item)=>{
      if (!isValidUrl(item)){
        console.log('非url')
      } else {
        console.log(urlToFile(item))
      }
      console.log(item)
    })*/
  // 场景1：拖拽的是文件（如直接从文件夹/其他页面拖拽的图片文件）
  if (dt.files.length > 0) {
    const load = MLoading.show()
    const id = dragenterNodeId.value === '0' ? null : dragenterNodeId.value
    await handleUploadFileList(dt.files, id)
    clearDragenterNodeId()
    await updateResourceNodeData();
    load.close()
    return
  } else if (dt.items.length > 0) {// 场景2：拖拽的是图片元素（从其他网页拖拽的img标签）
    const load = MLoading.show()
    const parentNodeId = dragenterNodeId.value === '0' ? null : dragenterNodeId.value
    for (let i = 0; i < dt.items.length; i++) {
      const item = dt.items[i];
      // 检查是否是图片类型的数据
      if (item.kind === 'string' && item.type.match(/^text\/html/)) {
        // 获取HTML内容（包含img标签）
        const html = await new Promise((resolve) => {
          item.getAsString(resolve);
        });
        // 解析HTML，提取图片URL
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = html;
        const img = tempDiv.querySelector('img');
        if (img && img.src) {
          const formData = new FormData();
          formData.append('fileUrl', img.src);
          if (parentNodeId) {
            formData.append('parentNodeId', parentNodeId);
          }
          const result = await uploadNetworkImage(formData);
          if (result.code === 200) {
            // 上传成功反馈
            Message.success($t('upload.success', {fileName: result.data.fileName}));
          }
        }
        tempDiv.remove()
      }
    }
    clearDragenterNodeId()
    await updateResourceNodeData();
    load.close()
    return
  }
  if (!dragNode.nodeId) return
  // 目录节点
  let parentId = dropNode?.parentId || null
  if(NodeType.isFolder(dropNode?.nodeType)) {
    parentId = dropNode.nodeId
  }
  if (dragNode.nodeId === dropNode?.nodeId || dragNode?.parentId === parentId) {
    clearDragenterNodeId()
    return
  }
  await updateResourceNodeParentId({
    nodeId: dragNode.nodeId,
    parentId,
  }).then(res => {
    Object.assign(resourceNodeList.value.find(item=>item.nodeId === dragNode.nodeId), {parentId})
    resourceNodeListVersion.value++
  })
  // updateResourceNodeData()
  clearDragenterNodeId()
  dragNodeId.value = null
}
function handleDragStart(e,node) {
  domUtils('#dragImgDom')
      .setStyle({
        display: 'block',
        left: `${e.clientX}px`,
        top: `${e.clientY}px`,
      })
      .query('.drag-node-name')
      .get().innerText = node.nodeName
  e.dataTransfer.setDragImage(document.querySelector('#dragImgEmpty'), 0, 0);

  dragNodeId.value = node.nodeId
  document.addEventListener('dragover', handleBodyDragover);
}
function handleFileListDragleave(e) {
  document.querySelector('#dragImgDom .drop-node-name').innerText = ``
  clearDragenterNodeId()
}
function handleDragEnd() {
  document.getElementById('dragImgDom').style.display = 'none'
  document.removeEventListener('dragover', handleBodyDragover);
  dragNodeId.value = null
}
</script>

<template>
  <div
    class="file-list flex flex-col"
    @dragleave="handleFileListDragleave"
  >
    <div class="file-list__header flex items-center justify-center">
      <Tooltip :content="t('action.newNote')" position="bottom" :mouseEnterDelay="300" mini>
        <m-button type="base" size="small" @click="addNote()" :aria-label="t('action.newNote')">
          <template #icon><icon-file-plus size="18"/></template>
        </m-button>
      </Tooltip>
      <Tooltip :content="t('action.newFolder')" position="bottom" :mouseEnterDelay="300" mini>
        <m-button type="base" size="small" @click="addFolder()" :aria-label="t('action.newFolder')">
          <template #icon><icon-folder-plus size="18"/></template>
        </m-button>
      </Tooltip>
      <Tooltip :content="t('action.selectTheFileToOpen')" position="bottom" :mouseEnterDelay="300" mini>
        <m-button type="base" size="small" @click="handlePositioningClick" :aria-label="t('action.selectTheFileToOpen')">
          <template #icon><icon-crosshair size="18"/></template>
        </m-button>
      </Tooltip>
      <Tooltip :content="expandCollapseStatus ? t('action.collapseAll') : t('action.expandAll')" position="bottom" :mouseEnterDelay="300" mini>
        <m-button type="base" size="small" @click="handleExpandCollapseClick" :aria-label="expandCollapseStatus ? t('action.collapseAll') : t('action.expandAll')">
          <template #icon>
            <icon-chevron-down-up v-if="expandCollapseStatus" size="18"/>
            <icon-chevron-up-down v-else size="18"/>
          </template>
        </m-button>
      </Tooltip>
    </div>
    <dropdown
        trigger="contextmenu"
        position="br"
        @select="handleTreeDropdownSelect"
        @show="handleTreeNodeDropdownOpenChange(true, contextmenuNode)"
        @hide="handleTreeNodeDropdownOpenChange()">
      <div
        class="file-list__content flex-1 flex flex-col min-h-0" >
        <tree class="file-list__content-tree flex-1 dress-up-scrollbar"
              :aria-label="t('layout.folderList')"
              :draggable="!renameNodeId"
              ref="treeRef"
              :data="resourceNodeList"
              line-expanded
              :update-mark="resourceNodeListVersion"
              @dragleave="debounce(clearDragenterNodeId, 200)"
              @dragover.prevent="handleTreeNodeItemDragover($event, {nodeId: '0'})"
              @drop.prevent="handleDrop($event, {nodeId: dragNodeId}, {})"
              @select="handleTreeSelect"
              
              @on-drag-start="handleDragStart"
              @on-drag-leave="debounce(clearDragenterNodeId, 200)"
              @on-drop-done="handleDrop"
              @on-drag-over="handleTreeNodeItemDragover"
              @on-drag-end="handleDragEnd"
              v-model:selected-ids="selectedNodeIds">
          <template #default="{ node }">
              <tree-node-item
                @contextmenu="contextmenuNode =node.nodeValue"
                :key="node.nodeId"
                :node-item="node"
                :focus-visible="openedDropdownNode?.nodeId === node.nodeId || renameNodeId === node.nodeId"
                :highlight="dragenterNodeId === node.nodeId"
              >
                <m-input draggable="false" ref="renameInputRef" class="rename" size="fit" @blur="handleBlurRename" @pressEnter="handleBlurRename" v-if="renameNodeId === node.nodeId" v-model="renameNodeName"  autocomplete="off" spellcheck="false" @click.stop @keyup.space.stop/>
                <span class="text-ellipsis" v-else-if="NodeType.isFile(node.nodeType)">{{getFileNameWithoutExtension(node.nodeName)}}</span>
                <span class="text-ellipsis" v-else>{{node.nodeName}}</span>
                <template #suffix v-if="NodeType.isFile(node.nodeType)">
                  <m-tag size="mini">{{ getFileExtension(node.nodeName) }}</m-tag>
                </template>
              </tree-node-item>
          </template>
        </tree>
      </div>
      <template #content v-if="openedDropdownNode">
        <template v-for="item in (NodeType.isFolder(openedDropdownNode.nodeType) ? treeNodeDropdownItems : treeNodeDropdownItems2)">
          <divider v-if="item.type === 'hr'" />
          <dropdown-option v-else :value="item.key" :style="item.style">
            <template #icon v-if="item.icon">
              <component :is="item.icon"></component>
            </template>
            {{item.label()}}
          </dropdown-option>
        </template>
      </template>
      <template #content v-else>
        <template v-for="item in treeRootDropdownItems">
          <divider v-if="item.type === 'hr'" />
          <dropdown-option v-else :value="item.key" :style="item.style">
            <template #icon v-if="item.icon">
              <component :is="item.icon"></component>
            </template>
            {{item.label()}}
          </dropdown-option>
        </template>
      </template>
    </dropdown>
    <Teleport to="body">
      <div id="dragImgDom">
        <div class="flex items-center"><icon-file style="margin-right: var(--spacing-2)"/><div class="drag-node-name"></div></div>
        <div class="drop-node-name"></div>
      </div>
      <div id="dragImgEmpty"></div>
    </Teleport>
  </div>
</template>
<style>
#dragImgEmpty{
  position: fixed;
  width: 1px;
  height: 1px;
  opacity: 0;
  left: -999px;
  bottom: 999px;
}
#dragImgDom {
  display: none;
  position: absolute;
  width: max-content;
  padding: 4px 8px;
  background-color: rgb(var(--gray-10));
  color: rgb(var(--gray-1));
  font-size: 14px;
  border-radius: 4px;
  pointer-events: none;
  z-index: 999;
  opacity: .9;
  transform: translate(12px, 12px);
}
#dragImgDom .drop-node-name{
  color: rgb(var(--gray-4));
}
</style>
<style lang="scss" scoped>
.file-list {
  height: 100%;
  padding-right: 3px;
}

.file-list__header {
  padding: 16px;
  font-size: 18px;
  gap: 4px;
}

.file-list__content-tree {
  position: relative;
  padding: 0 16px 32px;
  overflow: hidden;
  overflow-y: auto;
}
</style>
