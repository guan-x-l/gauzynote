<script setup>

import {useAppStore, useTabsStore} from "@/store/index.js";
import {onMounted} from "vue";
import {addNodeAndNote} from "@/api/resourceNode.js";
import {createResourceNode} from "@/biz/resourceNode.js";
import {useRouter} from "vue-router";
import {getNoteTab} from "@/biz/tabs.js";

const {assetLoadingStatus, resourceNodeList, pushResourceNodeList, initResourceNode, needFocusAssetNameIdList} = useAppStore()
const {replaceCurrentTab} = useTabsStore()
const router = useRouter()
assetLoadingStatus.value = false
onMounted(()=>{
  assetLoadingStatus.value = true
})

function createNewNote() {
  const resourceNode = createResourceNode(undefined, '2', resourceNodeList.value)
  addNodeAndNote(resourceNode).then(async res => {
    pushResourceNodeList(res.data)
    await initResourceNode()
    needFocusAssetNameIdList.value.push(res.data.relatedId)
    const tab = getNoteTab(res.data)
    replaceCurrentTab(tab)
    await router.push(tab.path)
  })
}

function askNotificationPermission() {
  // 检查浏览器是否支持通知
  if (!("Notification" in window)) {
    console.log("此浏览器不支持通知。");
    return;
  }
  Notification.requestPermission().then((permission) => {
    // 根据用户的回答显示或隐藏按钮
    // notificationBtn.style.display = permission === "granted" ? "none" : "block";
  });
}
// askNotificationPermission()
function a() {
  // 检查浏览器是否支持 Notification API
  if (!("Notification" in window)) {
    alert("当前浏览器不支持桌面通知");
    return;
  }

// 检查权限状态
  if (Notification.permission === "granted") {
    var n = new Notification("通知标题", {
      body: "这是通知的正文内容",
      icon: "https://example.com/icon.png"
    })

    n.onclick = () => {
      console.log('点击了通知');
    };

    n.onclose = () => {
      console.log('关闭了通知');
    };

    n.onerror = () => {
      console.log('通知出错');
    };

    n.onshow = () => {
      console.log('显示了通知');
    };
  } else if (Notification.permission !== "denied") {
// 请求权限
    Notification.requestPermission().then(permission => {
      if (permission === "granted") {
        new Notification("通知标题", {
          body: "这是通知的正文内容",
          icon: "https://example.com/icon.png"
        });
      }
    });
  }
}

</script>

<template>
<div class="flex justify-center flex-col items-center flex-1">
<!--  <router-link to="/index">index</router-link>-->
<!--  <router-link to="/iconpages">iconpages</router-link>-->
<!--  <m-button id="enable" @click="a" size="large">测试通知</m-button>-->
  <m-button size="large" @click="createNewNote">创建新文件</m-button>
</div>
</template>

<style scoped>

</style>