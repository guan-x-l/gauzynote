<script setup>
import ExpandTransition from "@/components/container/tree/ExpandTransition.vue";
import {inject} from "vue";
import TreeNodeItem from "@/components/container/tree/TreeNodeItem.vue";
import {NodeType} from "@/enum/index.js";

const props = defineProps({
  treeData: {
    type: Array,
    required: true
  }
})
const emit = defineEmits(['select'])
const {expandedIds} = inject('tree')
const toggle = (item) => {
  emit('select', item)
}
</script>

<template>
  <ul>
    <li v-for="item in treeData" :key="item.nodeId" >
      <div @click="toggle(item)" @keyup.space.stop="toggle(item)">
        <slot :node="item">
            <tree-node-item :node-item="item"></tree-node-item>
        </slot>
      </div>
      <ExpandTransition
        v-if="NodeType.isFolder(item.nodeType)"
        :key="item.nodeId + 'ExpandTransition'"
        :expanded="expandedIds.indexOf(item.nodeId) > -1"
        >
          <TreeNode @select="toggle" v-if="expandedIds.indexOf(item.nodeId) > -1" :treeData="item.children">
            <template #default="slotProps">
              <!-- 传递子组件的插槽数据给上层 -->
              <slot v-bind="slotProps" />
            </template>
          </TreeNode>
      </ExpandTransition>
    </li>
  </ul>
</template>

<style lang="scss" scoped>
ul {
  margin-block: 0;
  padding-inline-start: 0;
  overflow: hidden;
  transition: height .2s ease-in-out;
}

ul ul{
  padding-inline-start: 16px;
}

li {
  list-style-type: none;
  //padding: 2px;
}
//.tree-node-wrapper {
  //list-style-type: none;
  //padding: 2px;
//}
ul ul li{
  border-left: 1px solid #999;
  padding-left: 8px;
}
</style>
