<script setup>
import {computed, inject, ref} from "vue";
import {NodeType} from "@/enum/index.js";

const props = defineProps({
  nodeItem: Object,
  focusVisible: Boolean,
  highlight: Boolean,
  draggable: Boolean,
})
const {
  selectedIds,
  expandedIds,
  handleExpanded,
  handleDragstart,
  handleDrag,
  handleDragend,
  handleDrop,
  handleDragleave,
  handleDragover,
  expandById,
  draggable
} = inject('tree')
const isDragging = ref(false)
const isDragOver = ref(false)
// const nodeTitle = computed(()=>{
  // if (NodeType.isImage(props.nodeItem.nodeType)) {
  //   return props.nodeItem.nodeName.substring(0, props.nodeItem.nodeName.lastIndexOf('.'))
  // }
  // return props.nodeItem.nodeName
// })
// const imageType = computed(()=>{
//   if (NodeType.isImage(props.nodeItem.nodeType)) {
//     return props.nodeItem.nodeName.substring(props.nodeItem.nodeName.lastIndexOf('.') + 1)
//   }
// })
const prefixCls = 'tree-node'
const treeNodeClassNames = computed(() => [
  `${prefixCls}-title`,
  {
    'tree-node-selected': selectedIds.value.indexOf(props.nodeItem.nodeId) > -1,
    'tree-node-focus-visible': props.focusVisible,
    'tree-node-highlight': props.highlight,
    // [`${prefixCls}-title-draggable`]: draggable.value,
    // [`${prefixCls}-title-gap-top`]:
    // isDragOver.value && isAllowDrop.value && dropPosition.value < 0,
    // [`${prefixCls}-title-gap-bottom`]:
    // isDragOver.value && isAllowDrop.value && dropPosition.value > 0,
    // [`${prefixCls}-title-highlight`]:
    // !isDragging.value &&
    // isDragOver.value &&
    // isAllowDrop.value &&
    // dropPosition.value === 0,
    // [`${prefixCls}-title-dragging`]: isDragging.value,
    // [`${prefixCls}-title-block`]: node.value.blockNode,
  },
]);
function onDragstart(e){
  e.stopPropagation();
  isDragging.value = true;
  handleDragstart(e,props.nodeItem)
}
function onDrag(e){
  handleDrag(e,props.nodeItem)
  e.dataTransfer.dropEffect = 'none';
}
function onDragend(e){
  e.stopPropagation();
  isDragging.value = false;
  isDragOver.value = false;
  handleDragend(e,props.nodeItem)
}
function onDragover(e){
  e.stopPropagation();
  e.preventDefault();
  isDragOver.value = true;
  handleDragover(e,props.nodeItem)
}
function onDragleave(e){
  e.stopPropagation();
  isDragOver.value = false;
  handleDragleave(e,props.nodeItem)
}
function onDrop(e){
  e.stopPropagation();
  e.preventDefault();
  isDragOver.value = false;
  handleDrop(e,props.nodeItem)
}
function onDragenter() {
  if (!expandedIds.value.includes(props.nodeItem.nodeId)) {
    expandById(props.nodeItem.nodeId)
  }
}
</script>

<template>
  <div class="tree-node-wrapper"
       @drag="onDrag"
       @dragstart="onDragstart"
       @dragend="onDragend"
       @dragover="onDragover"
       @dragleave="onDragleave"
       @drop="onDrop"
       :draggable="draggable"
  >
    <div class="tree-node"
         tabindex="0"
         :class="treeNodeClassNames"
    >
    <span class="tree-node-prefix">
      <icon-chevron-right
          @dragenter="onDragenter"
        @click.stop="handleExpanded(nodeItem)"
        v-if="NodeType.isFolder(nodeItem.nodeType)"
        :rotate="expandedIds.indexOf(nodeItem.nodeId) > -1 ? 90 : 0"
        size="16"/>
    </span>
<!--      <span class="text-ellipsis">-->
        <slot>
          <span v-if="nodeItem.nodeNameDraw" v-html="nodeItem.nodeNameDraw"></span>
          <span v-else>{{nodeItem.nodeName}}</span>
        </slot>
<!--      </span>-->
      <span class="tree-node-suffix" v-if="$slots.suffix">
      <slot name="suffix">

  <!--        <m-tag size="mini" color="var(&#45;&#45;color-fill-3)" v-if="NodeType.isImage(nodeItem.nodeType)">{{imageType}}</m-tag>-->

      </slot>
        </span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.tree-node-wrapper {
  padding: 2px;
}

.icon-chevron-right {
  transition: transform .2s ease-out;
}

.tree-node {
  display: flex;
  align-items: center;
  //height: 36px;
  padding: 8px 8px;
  user-select: none;
  //white-space: nowrap;
  line-height: 1.2974;
  border-radius: 4px;
}

.tree-node:hover {
  background-color: var(--color-fill-2);
}

.tree-node:focus-visible {
  outline: 0;
  background-color: var(--color-fill-2);
}

.tree-node.tree-node-selected {
  background-color: var(--color-fill-3);
}

.tree-node-focus-visible {
  box-shadow: 0 0 0 2px var(--color-border-3);
}

.tree-node-highlight {
  background-color: rgb(var(--primary-2));
}

.tree-node-title {
  flex: 1;
}
.tree-node-prefix {
  display: inline-flex;
  width: 16px;
  height: 16px;
  margin-right: 4px;
  flex-shrink: 0;
  align-items: center;
  user-select: none;
}
.tree-node-suffix{
  display: inline-flex;
  align-items: center;
  margin-left: auto;
}
</style>