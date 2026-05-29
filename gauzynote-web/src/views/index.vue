<script setup>
//test
import {onMounted, reactive, ref, useTemplateRef} from "vue";
import {useRouter} from "vue-router";
import {Message} from "@/components/index.js";
import {useUserStore} from "@/store/index.js";
import {useEventListener, useMergeState} from "@/hooks/index.js";
import {formatBytes, processFileList} from "@/utils/index.js";
import {getStorageSpace} from "@/api/upload.js";
import Tooltip from "@/components/base/Tooltip/Tooltip.vue";
import Split from "@/components/container/Split/Split.vue";
import Checkbox from "@/components/base/Checkbox/checkbox.vue";

defineOptions({
  name: 'index'
})
const {userinfo} = useUserStore()
const router = useRouter()


let a = 1

function aa() {
  a++
  Message.info('导入方式调用成功！' + a)
  Message.warning('导入方式调用成功！' + a)
  Message.success('导入方式调用成功！' + a)
  Message.error('导入方式调用成功！' + a)
}
function bb() {
  a++
  Message.warning(`导入方式调用成功`+ a, {duration: 0})
}
function cc() {
  a++
  Message.warning(`导入方式调用成功`+ a, {duration: 0,position: 'bottom-center'})
}
const menuItems = [
  { key: 'copy', label: '复制' },
  { key: 'paste', label: '粘贴' },
  { key: 'delete', label: '删除' },
]
const handleSelect = (item) => {
  console.log('选中项:', item)
  console.log(item)
}

const modalShow = ref(false)
onMounted(()=>{
})
const isOpen = ref(false);

const handleChange = (visible) => {
  console.log('弹出框状态改变:', visible);
};

const storageSpace = ref({})

getStorageSpace().then(res => {
  storageSpace.value = res.data
})
const b = ref('yes')
const value1 = ref('1');
const size = ref(300);
const size2 = ref(300);
const value2 = ref(['1']);
const value3 = ref(['1']);

const columns = [
  {
    label: 'key',
    dataIndex: 'key',
    width: 200
  },
  {
    label: 'Name',
    dataIndex: 'name',
    width: 200
  },
  {
    label: 'Salary',
    dataIndex: 'salary',
  },
];
const data = ref([
    {
  key: '1',
  name: 'Jane Doe',
  salary: 23000,
}, {
  key: '2',
  name: 'Alisa Ross',
  salary: 25000,
}, {
  key: '3',
  name: 'Kevin Sandra',
  salary: 22000,
}, {
  key: '4',
  name: 'Ed Hellen',
  salary: 17000,
}
]);
const sortState = ref({
  oldIndex: -1,
  newIndex: -1
})

function moveArrayItem(list, oldIndex, newIndex) {
  if (!Array.isArray(list) || oldIndex === newIndex) {
    return list
  }
  const nextList = [...list]
  const movedItem = nextList.splice(oldIndex, 1)[0]
  if (movedItem === undefined) {
    return list
  }
  nextList.splice(newIndex, 0, movedItem)
  return nextList
}

function handleSortEnd({oldIndex, newIndex}) {
  console.log(oldIndex, newIndex)
  if (typeof oldIndex !== 'number' || typeof newIndex !== 'number') {
    return
  }
  sortState.value = {oldIndex, newIndex}
  data.value = moveArrayItem(data.value, oldIndex, newIndex)
  Message.success(`排序完成: ${oldIndex} -> ${newIndex}`)
}

function handleSortStar({oldIndex}) {
  console.log(oldIndex)
  sortState.value = {oldIndex, newIndex: -1}
  Message.info(`开始拖拽: ${oldIndex}`)
}
</script>

<template>
  <div class="index-page" style="position: relative;">
    index
    <loading-wrapper  text="加载中">
    <p style="margin: 0 auto;width: 50%;">
      {{userinfo}}
    </p>
    </loading-wrapper>
    <br>
    <p>总存储空间{{formatBytes(storageSpace.storageSpace)}}；已使用存储空间{{formatBytes(storageSpace.usedStorageSpace)}}</p>
    <m-table
        style="width: 80%;margin: auto"
        :columns="columns"
        :data="data"
        row-key="key"
        resizable
        striped
        bordered
        @sort-end="handleSortEnd"
        @sort-star="handleSortStar"
        :draggable="{ type: 'handle', width: 40 }" />
    <div style="width: 80%;margin: 20px auto">
    <scrollbar type="embed" style="height:200px;overflow: auto;">
      <div style="height: 400px;width: 2000px; background-color: var(--color-primary-light-4);border: 1px solid red">Content</div>
    </scrollbar>
      <br>
    <scrollbar type="track" style="height:200px;">
      <div style="height: 400px;width: 2000px; background-color: var(--color-primary-light-4);border: 1px solid red">Content</div>
    </scrollbar>
    </div>

    <p>排序状态：{{ sortState }}</p>
    {{value2}}
    <m-checkbox value="1" v-model="value2">Option 1</m-checkbox>
    <br>
    {{value3}}
    <br>
    <m-checkbox-group v-model="value3">
      <m-checkbox value="1" disabled>Option 1</m-checkbox>
      <m-checkbox :value="2" disabled>Option 2</m-checkbox>
      <m-checkbox :value="3">Option 3</m-checkbox>
    </m-checkbox-group>
    <br>
    <m-checkbox-group direction="vertical" v-model="value3">
      <m-checkbox value="1" disabled>Option 1</m-checkbox>
      <m-checkbox :value="2" disabled>Option 2</m-checkbox>
      <m-checkbox :value="3">Option 3</m-checkbox>
    </m-checkbox-group>
    <split :style="{
      height: '200px',
      width: '100%',
      minWidth: '500px',
      border: '1px solid var(--color-border)'
    }"
           v-model:size="size"
    >
      <template #first>
        <div>Left</div>
      </template>
      <template #second>
        <div>
          <split :style="{height: '200px'}"   resizeSecond   v-model:size="size2">
            <template #first><div>Top</div></template>
            <template #second><div>Bottom</div></template>
          </split>
        </div>
      </template>
    </split>
    <m-button>
      <template #icon><icon-home/></template>
      button
    </m-button>
    <m-button size="mini">
      <template #icon><icon-home/></template>
      button
    </m-button>
    <m-button loading>
      button
    </m-button>
    <m-button disabled size="large">
      button
    </m-button>
    <br>
    <m-button type="base">
      <template #icon><icon-home/></template>
      button
    </m-button>
    <m-button  type="base" size="mini">
      <template #icon><icon-home/></template>
      button
    </m-button>
    <m-button type="base" loading>
      button
    </m-button>
    <m-button type="base" disabled>
      button
    </m-button>
    <br>
    <m-button type="primary">
      <template #icon><icon-home/></template>
      button
    </m-button>
    <m-button  type="primary" size="mini">
      <template #icon><icon-home/></template>
      button
    </m-button>
    <m-button type="primary" loading>
      button
    </m-button>
    <m-button type="primary" disabled>
      button
    </m-button>
    <br>
    <m-button type="outline">
      <template #icon><icon-home/></template>
      button
    </m-button>
    <m-button  type="outline" size="mini">
      <template #icon><icon-home/></template>
      button
    </m-button>
    <m-button type="outline" loading>
      button
    </m-button>
    <m-button type="outline" disabled>
      button
    </m-button>
    <br>
    <m-button type="secondary" loading status="danger" size="mini" >button</m-button>
    <m-button type="primary" status="danger" size="mini" >button</m-button>
    <m-button type="primary" status="danger" size="mini" loading>button</m-button>
    <m-button type="primary" status="danger" size="mini" disabled>button</m-button>
    <br>
    <m-button type="text">button</m-button>
    <m-button type="text" loading>button</m-button>
    <m-button type="text" disabled>button</m-button>
    <br>
    <m-button type="text" status="danger">button</m-button>
    <m-button type="text" status="danger" loading>button</m-button>
    <m-button type="text" status="danger" disabled>button</m-button>
    <m-button type="primary" long>button</m-button>
    <m-button type="primary" size="large" shape="circle">
      <template #icon><icon-home/></template>
    </m-button>
    <m-button type="primary" size="large" shape="square">
      <template #icon><icon-home/></template>
    </m-button>

    <div style="width: 50%;margin: 0 auto">
      <m-input placeholder="Please enter something" size="small"/>
      <m-input placeholder="Please enter something" error/>
      <m-input placeholder="Please enter something" default-value="22" disabled/>
    </div>
    <div class="flex items-center justify-center" style="gap: 8px">
      <m-switch v-model="b" checked-value="yes" unchecked-value="no">
        <template #checked>
          ON
        </template>
        <template #unchecked>
          OFF
        </template>
      </m-switch>
      <m-switch disabled></m-switch>
      <m-switch loading></m-switch>
      <m-switch default-checked loading></m-switch>
    </div>
    <br>
    <m-radio value="1" v-model="value1">MRadio</m-radio>
    <div style="background-color: var(--color-fill-2);display: inline-block">
      <m-radio value="2" v-model="value1" type="button">MRadio</m-radio>
      <m-radio value="3" v-model="value1" type="button">MRadio</m-radio>
    </div>
    <br>
    <m-radio-group v-model="value1" type="button">
      <m-radio value="4" disabled>MRadio</m-radio>
      <m-radio value="5">MRadio</m-radio>
    </m-radio-group>
    <br>
    <m-radio-group v-model="value1"
                   :options="[
        {
          value: '4',
          label: 'MRadio',
        },
        {
          value: '5',
          label: 'MRadio',
          disabled: true,
        }]"
    >
    </m-radio-group>
    <br>
    <icon-home/>
    <router-link to="404">404</router-link>
    <br>

    <button @click="modalShow=true">modalShow</button>
    <button @click="aa">msg</button>
    <button @click="bb">msg2</button>
    <button @click="cc">msg3</button>
    <br>
    <Divider/>
    带文本的分割线
    <Divider >
      分割内容
    </Divider>
    
     左侧文本分割线
    <Divider  orientation="left">
      左侧文本
    </Divider>
    
     右侧文本分割线
    <Divider  orientation="right">
      右侧文本
    </Divider>
    
     虚线分割线
    <Divider dashed />
    
     自定义颜色
    <Divider color="#1890ff" />
    垂直分割线
    <div>
      <span>Rain</span>
      <Divider direction="vertical" />
      <span>Home</span>
      <Divider direction="vertical" dashed/>
      <span>Grass</span>
    </div>
    <br>
    <div>
      <m-tag bordered>tag</m-tag>
      <m-tag checkable>tag</m-tag>
      <m-tag checkable bordered>tag</m-tag>
      <m-tag checkable color="arcoblue">tag</m-tag>
      <m-tag bordered checkable color="arcoblue">tag</m-tag>
      <m-tag checkable color="red">tag</m-tag>
      <m-tag color="red">tag</m-tag>
    </div>

    <dropdown :key="11" @select="handleSelect">
      <div class="target-area">
        左键点击这个区域会显示菜单
      </div>
      <template #content>
        <dropdown-option v-for="item in menuItems" :key="item.key" :value="item" :label="item.label">
        </dropdown-option>
      </template>
    </dropdown>
    <dropdown :key="22" trigger="contextmenu" @select="handleSelect">
      <div class="target-area">
        右键点击这个区域会显示菜单
      </div>
      <template #content>
        <dropdown-option v-for="item in menuItems" :key="item.key" :value="item" :label="item.label">
        </dropdown-option>
      </template>
    </dropdown>
    <div class="container">
      <h2>trigger</h2>

      <Trigger trigger="click" position="rb" :popup-offset="0" showArrow>
        <button class="btn">点.击</button>

        <template #content>
          <div class="demo-arrow flex items-center justify-center">
            <icon-image />
          </div>
        </template>
      </Trigger>
      <br>

      <Trigger trigger="hover" position="right" :popup-offset="15" showArrow>
        <button class="btn primary">悬停我 (Hover)</button>

        <template #content>
          <div class="demo-arrow">
            <icon-image />
          </div>
        </template>
      </Trigger>

      <div style="margin-top: 20px;">
        <p>当前状态: {{ isOpen }}</p>
        <Trigger v-model:popupVisible="isOpen" trigger="click" position="bottom" showArrow>
          <div class="box">受控 DIV 元素</div>
          <template #content>
            <div class="demo-arrow">
              <icon-image />
            </div>
          </template>
        </Trigger>
      </div>
      <div>
        <Tooltip content="aaaaaaaaa">
          <button>aaa</button>
        </Tooltip>
      </div>
    </div>

    <div style="height: 500px;"></div>

    <modal :show="modalShow" width="500px" content="modal" @cancel="modalShow = false">
    </modal>

  </div>

</template>


<style lang="scss" scoped>
.trigger-demo-translate{
  box-shadow: 0 2px 8px #00000026;
  padding: 10px;
  width: 200px;
  background-color: #fff;
  border-radius: 4px;
}
.index-page{
  text-align: center;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  overflow-y: auto;
}
.target-area{
  width: 300px;
  height: 100px;
  background-color: #d9d9d9;
}
.svg-box{
  width: 300px;
  height: 40px;
  margin: 50px auto;
  position: relative;
  border: 1px solid #999;
  .svg-box-mask{
    position: absolute;
    top: 0;
    left: -16px;
    right: -16px;
    bottom: 0;
    display: block;
    z-index: 1;
  }
  svg{
    display: block;
    width: 100%;
    height: 100%;
  }
}
.demo-arrow {
  box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.15);
  padding: 10px;
  width: 200px;
  height: 120px;
  background-color: var(--color-bg-popup);
  border-radius: 4px;
}
#editorViewTest{
  text-align: left;
}
</style>
