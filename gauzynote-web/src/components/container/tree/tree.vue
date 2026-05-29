<script setup>
import TreeNode from "@/components/container/tree/TreeNode.vue";
import {computed, provide, ref, watch} from "vue";
import {NodeType} from "@/enum/index.js";
import {isEmpty} from "@/utils/index.js";

const props = defineProps({
  data: Array,
  updateMark: [Number, String],
  // 多选
  multiple: Boolean,
  // 父节点是否可选
  parentNodeSelected: Boolean,
  defaultExpandedIds: {type: Array, default: []},
  defaultExpandAll: Boolean,
  selectedIds: {type: Array, default: []},
  expandedIds: {type: Array, default: []},
  // 行展开
  lineExpanded: Boolean,
  // 拖拽
  draggable: Boolean,
  search: String
})

// const emit = defineEmits(['select', 'update:selectedIds', 'update:expandedIds', 'onDragStart', 'onDragEnd', 'onDragOver', 'onDragLeave', 'onDrop'])
const emit = defineEmits({
  /**
   * @param {Object} node
   */
  select: (node)=> true,
  'update:selectedIds': (selectedIds) => true,
  'update:expandedIds': (expandedIds) => true,
  /**
   * @param e 元素
   * @param {Object} dragNode 拖拽节点
   */
  onDragStart: (e, dragNode) => true,
  /**
   * @param e 元素
   * @param {Object} dragNode 拖拽节点
   */
  onDrag: (e, dragNode) => true,
  /**
   * @param e 元素
   * @param {Object} dragNode 拖拽节点
   */
  onDragEnd: (e, dragNode) => true,
  /**
   * @param e 元素
   * @param {Object} dropNode 拖拽至目标的节点
   */
  onDragOver: (e, dropNode) => true,
  /**
   * @param e 元素
   * @param {Object} dropNode 拖拽至目标的节点
   */
  onDragLeave: (e, dropNode) => true,
  /**
   * @param e 元素
   * @param {Object} dragNode 拖拽节点
   * @param {Object} dropNode 拖拽至目标的节点
   */
  onDropDone: (e, dragNode, dropNode) => true,
})
const selectedIds = ref([...props.selectedIds])
const expandedIds = ref([...props.defaultExpandedIds])
const treeData = ref([])
// 拖拽节点
const draggable = ref(props.draggable)
const dragNode = ref(null)
const isDragging = ref(false)
// 拖拽至目标的节点
const dropNode = ref(null)
const isDragOver = ref(false)
updateTreeData()
// watch(()=>props.data, updateTreeData, {deep: false})
watch(()=>props.updateMark, updateTreeData)
watch(()=>props.search, updateTreeData)

watch(()=>props.selectedIds, ()=>{
  if (selectedIds.value.toString() !== props.selectedIds.toString()) {
    selectedIds.value = [...props.selectedIds]
  }
})

watch(()=>props.defaultExpandedIds, ()=>{
  if (expandedIds.value.toString() !== props.defaultExpandedIds.toString()) {
    expandedIds.value = [...props.defaultExpandedIds]
  }
})
watch(()=>props.draggable, ()=>{
  draggable.value = props.draggable
})
function resetNodeStatus() {
  selectedIds.value = []
  expandedIds.value = [...props.defaultExpandedIds]
}
/**
 * 将扁平化的列表转换为树形结构（使用Map索引）
 * @param {[]} nodeList - 扁平化的文件夹列表
 * @returns {[]} 树形结构数据
 */
function buildTreeOptimized2(nodeList) {
  const nodeMap = new Map();
  const rootNodes = [];
  
  // 第一步：建立ID到节点的映射
  nodeList.forEach(node => {
    nodeMap.set(node.nodeId, { ...node, nodeValue: node, children: [], expanded: false });
  });
  
  // 第二步：构建树结构
  nodeList.forEach(node => {
    const currentNode = nodeMap.get(node.nodeId);
    
    if (node.parentId === null) {
      rootNodes.push(currentNode);
    } else {
      const parentNode = nodeMap.get(node.parentId);
      if (parentNode) {
        parentNode.children.push(currentNode);
      }
    }
  });
  rootNodes.forEach(node=>{
    node.children.sort((a, b) => a.sort - b.sort);
  })
  
  return rootNodes.sort((a, b) => a.sort - b.sort);
}
/**
 * 将扁平化的列表转换为树形结构（使用Map索引）
 * @param {[]} nodeList - 扁平化的文件夹列表
 * @returns {[]} 树形结构数据
 */
function buildTreeOptimized(nodeList, keyword, searchFields = ['nodeName']) {
  const nodeMap = new Map();
  const rootNodes = [];

  // 第一步：建立ID到节点的映射
  nodeList.forEach(node => {
    nodeMap.set(node.nodeId, { ...node, nodeValue: node, children: [], expanded: false });
  });

  // 第二步：构建树结构
  nodeList.forEach(node => {
    const currentNode = nodeMap.get(node.nodeId);

    if (isEmpty(node.parentId)) {
      rootNodes.push(currentNode);
    } else {
      const parentNode = nodeMap.get(node.parentId);
      if (parentNode) {
        parentNode.children.push(currentNode);
      }
    }
  });
  // 对各级子节点进行排序
  const sortTree = (nodes) => {
    nodes.forEach(node => {
      node.children.sort((a, b) => a.sort - b.sort);
      sortTree(node.children);
    });
  };
  sortTree(rootNodes);

  // 如果没有关键词，直接返回完整树
  if (!keyword || !keyword.trim()) {
    return rootNodes.sort((a, b) => a.sort - b.sort);
  }

  // 高亮处理函数：将文本中匹配的关键词用高亮标签包裹
  const highlightText = (text, keyword) => {
    if (!text || typeof text !== 'string') return text;

    // 转义正则特殊字符，避免正则注入
    const escapedKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    // 忽略大小写，全局匹配
    const regex = new RegExp(`(${escapedKeyword})`, 'gi');
    // 使用高亮标签替换匹配的关键词
    return text.replace(regex, `<strong class="highlight">$1</strong>`);
  };

  // 模糊筛选（自身或子集有匹配则保留）
  const filterTree = (nodes) => {
    return nodes.reduce((filtered, node) => {
      // 递归筛选子节点
      const filteredChildren = filterTree(node.children);

      // 检查节点自身是否匹配
      const isNodeMatch = searchFields.some(field => {
        const value = node[field] || '';
        return value.toString().toLowerCase().includes(keyword.toLowerCase());
      });

      // 如果节点自身匹配，或者子节点有匹配，就保留该节点
      if (isNodeMatch || filteredChildren.length > 0) {
        // 创建节点副本，避免修改原数据
        const newNode = { ...node, children: filteredChildren };

        // 如果节点自身匹配，对指定字段进行高亮处理
        if (isNodeMatch) {
          searchFields.forEach(field => {
            if (newNode[field] !== undefined) {
              newNode['nodeNameDraw'] = highlightText(newNode[field], keyword);
            }
          });
          // 匹配时自动展开节点
          newNode.expanded = true;
        }
        filtered.push(newNode);
      }

      return filtered;
    }, []);
  };

  // 筛选并返回结果
  return filterTree(rootNodes);
}
function updateExpandedIds(node) {
  if (expandedIds.value.indexOf(node.nodeId) > -1) {
    expandedIds.value.splice(expandedIds.value.indexOf(node.nodeId), 1)
  } else if (NodeType.isFolder(node.nodeType)){
    expandedIds.value.push(node.nodeId)
  }
}
function handleSelect(node) {
  if(props.lineExpanded) {
    updateExpandedIds(node)
  }
  
  if (props.parentNodeSelected || !NodeType.isFolder(node.nodeType)) {
    if (props.multiple) {
      if (selectedIds.value.indexOf(node.nodeId) > -1) {
        selectedIds.value.splice(selectedIds.value.indexOf(node.nodeId), 1)
      } else {
        selectedIds.value.push(node.nodeId)
      }
    } else if (selectedIds.value.indexOf(node.nodeId) <= -1){
      selectedIds.value = [node.nodeId]
    }
  }
  
  emit('update:selectedIds', selectedIds.value)
  if(props.lineExpanded){
    emit('update:expandedIds', expandedIds.value)
  }
  emit('select', node)
}

function handleExpanded(node) {
  updateExpandedIds(node)
  emit('update:expandedIds', expandedIds.value)
}

function handleDragstart(e,node) {
  dragNode.value = node
  emit('onDragStart', e, dragNode.value)
}

function handleDrag(e,node) {
  emit('onDrag', e, dragNode.value)
}

function handleDragend(e) {

  emit('onDragEnd',e, dragNode.value)
}
function handleDragover(e,node) {

  dropNode.value = node
  emit('onDragOver',e, dropNode.value)
}
function handleDragleave(e) {

  emit('onDragLeave',e, dropNode.value)
}
function handleDrop(e) {
  emit('onDropDone', e, dragNode.value,dropNode.value)
}
function handleTreeDrop(e) {
  emit('onDropDone', e, dragNode.value)
}

function updateTreeData() {
  if (props.defaultExpandAll){
    expandAll()
  }
  treeData.value = buildTreeOptimized(props.data, props.search)
}

/**
 * 设置全部节点的展开状态
 * @param {boolean} expanded 展开状态
 */
function expandAll(expanded = true) {
  expandedIds.value = expanded ? props.data.filter((node) => NodeType.isFolder(node.nodeType)).map(item => item.nodeId) : []
}

/**
 * 设置所有选中节点为展开
 */
function expandSelectedIds() {
  let ids = new Set([...selectedIds.value, ...expandedIds.value])
  selectedIds.value.forEach(id=>{
    ids.add(id)
    let targetNode = props.data.find(item => item.nodeId === id)
    while (true) {
      const parentNode = props.data.find(item => item.nodeId === targetNode.parentId)
      if (parentNode) {
        ids.add(parentNode.nodeId)
        targetNode = parentNode
      } else{
        break
      }
    }
  })
  expandedIds.value = Array.from(ids)
}
/**
 * 设置节点为展开
 */
function expandById(nodeId) {
  let ids = new Set([...expandedIds.value])
  ids.add(nodeId)
  expandedIds.value = Array.from(ids)
}


emit('update:expandedIds', expandedIds.value)
// 提供上下文
provide('tree', {
  selectedIds,
  expandedIds,
  isDragging,
  isDragOver,
  handleExpanded,
  handleDragstart,
  handleDrag,
  handleDragend,
  handleDragover,
  handleDragleave,
  handleDrop,
  expandById,
  draggable,
})
defineExpose({
  expandAll,
  expandSelectedIds,
  expandById,
  updateTreeData,
  resetNodeStatus
})
</script>

<template>
  <TreeNode
    :treeData="treeData"
    @select="handleSelect">
    <template #default="slotProps">
      <!-- 传递子组件的插槽数据给上层 -->
      <slot v-bind="slotProps" />
    </template>
  </TreeNode>
</template>

<style scoped>

</style>
