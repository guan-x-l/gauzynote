<script setup>
import {computed, nextTick, onMounted, ref, useTemplateRef, watch,} from "vue";
import {ControllablePoller, debounce} from "@/utils/index.js";
import Tree from "@/components/container/tree/tree.vue";
import {useAppStore, useNoteStore, useTabsStore} from "@/store/index.js";
import {NodeType} from "@/enum/index.js";
import {gsap} from "gsap";
import {useI18n} from "vue-i18n";
import Tooltip from "@/components/base/Tooltip/Tooltip.vue";

defineOptions({name: 'Outline'})


const {t} = useI18n()
const {assetLoadingStatus, tocSelectLineEnd} = useAppStore()
const {activeTab, activeTabRelatedId} = useTabsStore()
const {noteData} = useNoteStore(activeTabRelatedId)

const isNote = computed(() => activeTab.value && NodeType.isNote(activeTab.value.nodeType))

const expandCollapseStatus = ref(true)
const showSearch = ref(false)
const search = ref('')
const treeRef = useTemplateRef('treeRef')

function handleHeadings(headings) {
  const toc = [];
  if (!headings || !headings.length) return toc;

  const parentList = new Set()
  headings.forEach((heading, index) => {
    // 获取标题级别
    const level = heading.level
    let parentId = null
    // 确定父级：找到比当前级别低的最近级别项
    if (level > 1 && toc.length !== 0) {
      for (let i = toc.length - 1; i >= 0; i--) {
        if (toc[i].level < level) {
          parentId = toc[i].id;
          break;
        }
      }
    }
    toc.push({
      ...heading,
      id: index + 1,
      level,
      parentId  // 父级ID，用于标记父子关系
    });
    if (parentId) {
      parentList.add(parentId)
    }
  });
  toc.forEach(item => {
    if (parentList.has(item.id)) {
      item.nodeType = '1'
    }
  })
  return toc;
}

function handleExpandCollapseClick() {
  expandCollapseStatus.value = !expandCollapseStatus.value
  treeRef.value?.expandAll && treeRef.value.expandAll(expandCollapseStatus.value)
}

function handlePositioningClick() {
  treeRef.value?.expandSelectedIds && treeRef.value.expandSelectedIds()
}

// 从Markdown生成目录
const generateTocFromMarkdown = (mdContent) => {
  const toc = [];
// 匹配可能带有列表前缀的Markdown标题
// 支持: # 标题, - ## 二级标题, * ### 三级标题, 1. #### 四级标题等格式
//   const headingRegex = /^(?:[-*+]|\d+\.)?\s*(#{1,6})\s+(.*?)(?:\s+#*)?$/gm;
  // const headingRegex = /^(?:\s*(?:[-*+]|\d+\.)\s+)?(#{1,6})\s+(.*?)(?:\s+#*)?$/gm
  const headingRegex = /^(?:\s*(?:[-*+]|\d+\.)\s+)?(#{1,6})\s(.*?)$/gm

  let match;
  while ((match = headingRegex.exec(mdContent)) !== null) {
    // 确保匹配到了标题标记
    if (match[1]) {
      // console.log(match)
      const level = match[1].length; // #的数量即标题级别
      const title = match[2].trim();  // 标题文本
      const mdText = match[0].replace(/\n+$/, '')
      // console.log(match.index + mdText.length)
      toc.push({level, title, lineEnd: match.index + mdText.length});
      // toc.push({ level, title, mdText, lineEnd: match.index + trimNewlines(match[0]).length });
    }
  }
  return handleHeadings(toc)
};
// 从DOM生成目录
const generateTocFromDom = (dom) => {
  const toc = [];
  if (!dom) return toc;
  // 获取所有标题元素
  const headings = dom.querySelectorAll('h1, h2, h3, h4, h5, h6');
  return handleHeadings(Array.from(headings).map(heading => {
    return {
      level: parseInt(heading.tagName.charAt(1)),
      title: heading.innerText,
      domId: heading.id || null
    }
  }))
};
let preToc = null
let tocTimeline = null; // 存储上一次的动画时间线，用于销毁
async function handleTreeSelect(nodeItem) {
  // 1. 第一步：彻底清除上一次的动画和样式残留
  if (tocTimeline) {
    tocTimeline.kill(); // 终止上一次未完成的所有动画（包括延迟动画）
    tocTimeline = null; // 清空时间线引用
  }
  if (preToc) {
    gsap.set(preToc, {backgroundColor: ''})
    preToc = null
  }
  preToc = document.getElementById(nodeItem.domId)
  if (!nodeItem.domId) {
    tocSelectLineEnd.value = nodeItem.lineEnd
  }
  if (preToc) {
    preToc.scrollIntoView({block: "center"});
    tocTimeline = gsap.timeline({
      onComplete: () => {
        // 动画全部完成后，重置状态（避免内存泄漏）
        preToc = null;
        tocTimeline = null;
      }
    });
    tocTimeline
        .to(preToc, {
          backgroundColor: "var(--color-primary-3)",
          duration: 0.3,
          ease: "power1.inOut" // 平滑缓动，减少闪烁感
        })
        .to(preToc, {
          backgroundColor: "transparent",
          duration: 0.3, // 增加清除动画的时长，避免瞬间闪烁
          delay: 1,
          ease: "power1.inOut"
        });
    /* gsap.to(preToc, {backgroundColor: 'var(--primary-3)', duration: .3})
     gsap.to(preToc, {backgroundColor: '', delay: 1}).then(res=>{
       preToc = null
     })*/
  }
}

const outlineList = ref([])
const outlineListLoad = ref(true)
const outlineListVersion = ref(1)
const selectedNodeIds = ref([])
const outlineDomPoller = new ControllablePoller(
    () => document.querySelector(`.note-wrapper[data-key="${activeTab.value.componentName}"] .markdown-body`) || document.querySelector(`.note-wrapper[data-key="${activeTab.value.componentName}"] .cm-content`),
    {
      interval: 100,
      timeout: 10000
    }
)
let outlineTimeoutId = 0

/**
 * 处理大纲列表逻辑，根据当前激活标签和加载状态，更新大纲列表数据
 */
function handleOutlineList() {
  clearTimeout(outlineTimeoutId)
  const activeNode = activeTab?.value;
  const isAssetLoading = assetLoadingStatus.value;
  // 非有效节点：直接清空大纲
  if ((!activeNode?.nodeType || isNote.value && isAssetLoading && !noteData.value?.content) || !isNote.value) {
    resetOutlineState([], false);
    return;
  }
  // 是笔记且资产加载完成：设置加载状态
  if (isNote.value && !isAssetLoading) {
    outlineTimeoutId = setTimeout(() => {
      outlineListLoad.value = true;
    }, 50);
  } else if (isNote.value && isAssetLoading) {
    outlineDomPoller.restart().then(res => {
      const rawOutline = res.className === 'markdown-body'
          ? generateTocFromDom(res)
          : generateTocFromMarkdown(noteData.value.content);
      // 转换为统一格式
      const formattedOutline = rawOutline.map(item => ({
        ...item,
        nodeId: item.id,
        nodeName: item.title,
        sort: item.id
      }));

      resetOutlineState(formattedOutline, false);
      clearTimeout(outlineTimeoutId)
    }).catch(err => {
      console.error('大纲轮询失败:', err);
      resetOutlineState([], false);
    })
  }
}

/**
 * 重置大纲状态
 */
function resetOutlineState(list, isLoading) {
  outlineList.value = list;
  outlineListVersion.value++;
  outlineListLoad.value = isLoading;
}

function changeSearch(v) {
  search.value = v;
}

const inputSearch = debounce(changeSearch, 300)

watch([assetLoadingStatus, isNote], handleOutlineList)
onMounted(() => {
  nextTick(handleOutlineList)
})
</script>

<template>
  <div class="outline-list flex-1 flex flex-col min-h-0">
    <div class="outline-list__header flex items-center justify-center">
      <Tooltip :content="t('action.searchFilter')" position="bottom" :mouseEnterDelay="300" mini>
        <m-button type="base" style="margin-right: 4px;" size="small" @click="showSearch=!showSearch" :aria-label="t('action.searchFilter')">
          <template #icon>
            <icon-search size="18"/>
          </template>
        </m-button>
      </Tooltip>
      <m-button type="base" style="margin-right: 4px;" size="small" @click="handlePositioningClick" aria-label="select current title">
        <template #icon>
          <icon-crosshair size="18"/>
        </template>
      </m-button>
      <Tooltip :content="expandCollapseStatus ? t('action.collapseAll') : t('action.expandAll')" position="bottom" :mouseEnterDelay="300" mini>
        <m-button type="base" size="small" @click="handleExpandCollapseClick" :aria-label="expandCollapseStatus ? t('action.collapseAll') : t('action.expandAll')">
          <template #icon>
            <icon-chevron-down-up v-if="expandCollapseStatus" size="18"/>
            <icon-chevron-up-down v-else size="18"/>
          </template>
        </m-button>
      </Tooltip>
    </div>
    <div style="padding: 8px 16px;" v-if="showSearch">
      <m-input :placeholder="t('sidebar.searchTitle')" @press-enter="changeSearch" autofocus @input="inputSearch">
        <template #prefix>
          <IconSearch></IconSearch>
        </template>
      </m-input>
    </div>
    <div class="outline-list__content flex-1 dress-up-scrollbar">
      <loading-wrapper :loading="outlineListLoad">
        <tree style="padding: 0 16px"
              ref="treeRef"
              :data="outlineList"
              parent-node-selected
              :update-mark="outlineListVersion"
              :search="search"
              @select="handleTreeSelect"
              :default-expand-all="outlineList.length < 30"
              v-model:selected-ids="selectedNodeIds">
        </tree>
        <div v-if="!outlineList.length && !outlineListLoad" style="text-align: center;color: var(--color-text-3)">{{t('empty.noOutline')}}</div>
      </loading-wrapper>
    </div>
  </div>
</template>

<style scoped>
.outline-list__header {
  padding: 16px;
  font-size: 18px;
}

.outline-list__content {
  overflow: hidden;
  overflow-y: auto;
  padding-bottom: 32px;
}

:deep(.tree-node-title) {
  word-wrap: break-word;
  word-break: break-all;
  white-space: pre-line;
}
</style>
