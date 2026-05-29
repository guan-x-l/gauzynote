<script setup>
import {useRoute} from "vue-router";
import {updateAssetNote} from "@/api/assetNote.js";
import {
  computed,
  nextTick,
  onActivated,
  onBeforeUnmount,
  onDeactivated,
  onMounted,
  ref,
  useTemplateRef,
  watch
} from "vue";
import {Message} from "@/components/index.js";
import ToolbarNote from "@/views/note/components/ToolbarNote.vue";
import CodemirrorWrapper from "@/views/note/components/codemirrorWrapper/codemirrorWrapper.vue";
import {useAppStore, useNoteStore, useTabsStore} from "@/store/index.js";
import {updateResourceNodeNameByNote} from "@/api/resourceNode.js";
import controllerManager from "@/utils/request/RequestControllerManager.js";
import markdownIt from "@/utils/lib/markdownIt/markdownIt.js";
import {NodeType} from "@/enum/index.js";
import {pausedInterval} from "@/utils/index.js";
import {useEventListener, useKeepAliveRecoverScrollTop} from "@/hooks/index.js";
import {useI18n} from "vue-i18n";

const props = defineProps({
  componentKey: String
})

const {t} = useI18n()

// let CodemirrorWrapper = shallowRef()
const route = useRoute()
const noteId = +route.params.id;
window.__currentNoteId = noteId
const {activeTab, removeTab, updateTabTitleByRelatedId} = useTabsStore()
const {updateResourceNodeNameByRelatedId, needFocusAssetNameIdList, assetLoadingStatus, tocLineNumber,} = useAppStore()
const {getNote, noteData, noteName, noteVersion, updateNoteVersion} = useNoteStore(noteId)

assetLoadingStatus.value = false

const loading = ref(false)
const viewMode = ref('readonly')
/** @type {import('vue').Ref<string>}*/
const noteContentHtml = ref('')
/** @type {import('vue').Ref<string>}*/
const selectionText = ref('')
const noteNameRef = useTemplateRef('noteNameRef')
const markdownBody = useTemplateRef('markdownBody')
const noteWrapperRef = useTemplateRef('noteWrapperRef')
const updateStatus = ref(false)
const autoSave = pausedInterval(() => {
  submitNote(1)
}, 5000, false)
const autoSave2 = pausedInterval(() => {
  assetLoadingStatus.value = false
  nextTick(() => {
    assetLoadingStatus.value = true
  })
}, 2000, false, 1)
useKeepAliveRecoverScrollTop(noteWrapperRef)

// 按需加载
// if (!CodemirrorWrapper.value) {
//   import('@/components/codemirrorWrapper/codemirrorWrapper.vue').then(res=>{
//     CodemirrorWrapper.value = res.default
//   })
// CodemirrorWrapper = module.default
// }
watch(noteVersion, () => {
  noteName.value = noteData.value.noteName
  noteNameRef.value.value = noteName.value
  handleNoteContentHtml()
  handleNoteNameInput()
})

function init() {
  // 处理noteName的元素高度
  const observer = new ResizeObserver(entries => {
    for (let entry of entries) {
      handleNoteNameInput(entry)
    }
  });
  observer.observe(noteNameRef.value);
  getNote(loading).then(async res => {
    if (res.data) {
      updateTabTitleByRelatedId(noteId, NodeType.NOTE.getCode(), res.data.noteName)
      await nextTick(() => {
        handleNoteContentHtml()
        handleNoteNameInput(noteNameRef.value)
        if (needFocusAssetNameIdList.value.includes(noteId)) {
          noteNameRef.value.focus()
          noteNameRef.value.select()
          needFocusAssetNameIdList.value.splice(needFocusAssetNameIdList.value.indexOf(noteId), 1)
        }
        assetLoadingStatus.value = true
      })
    }
  })
}

onMounted(init)

function handleNoteContentHtml() {
  noteContentHtml.value = markdownIt.render(noteData.value.content || '');
}

/**
 *
 * @param {null|1} submitType 自动保存类型
 */
async function submitNote(submitType) {
  autoSave.pause()
  assetLoadingStatus.value = false
  const data = {
    noteId,
    content: noteData.value.content
  }
  const msg = ''
  // const msg = submitType === 1 ? '自动' : ''
  await updateAssetNote(data).then(res => {
    if (res.code === 200) {
      updateNoteVersion()
      Message.success(t('message.saveSuccess', {msg}))
      updateStatus.value = false
      window.isFormDirty.delete(noteId)
    } else {
      Message.error(t('message.saveError', {msg}))
    }
    assetLoadingStatus.value = true
  })
}

function updateNoteName() {
  if (noteData.value.noteName === noteName.value) return
  const data = {
    noteId,
    noteName: noteName.value
  }
  updateResourceNodeNameByNote(data).then(res => {
    if (res.code === 200) {
      noteData.value.noteName = data.noteName
      updateNoteVersion()
      updateTabTitleByRelatedId(noteId, NodeType.NOTE.getCode(), data.noteName)
      updateResourceNodeNameByRelatedId(noteId, NodeType.NOTE.getCode(), data.noteName)
    } else {
      noteName.value = noteData.value.noteName
    }
  })
}

async function handleViewModeClick(v) {
  const lineNumber = getScrollLineNumber()
  viewMode.value = v
  if (updateStatus.value) {
    await submitNote(1)
  }
  assetLoadingStatus.value = false
  nextTick(() => {
    assetLoadingStatus.value = true
    if (lineNumber >= 0 && v === 'readonly' && noteWrapperRef.value.scrollTop > 0) {
      document.querySelector(`.markdown-body *[data-source-line="${lineNumber}"]`)?.scrollIntoView({block: 'start'});
    } else if (lineNumber >= 0 && noteWrapperRef.value.scrollTop > 0) {
      tocLineNumber.value = lineNumber
    }
  })

}

function handleNoteNameInput(e) {
  if (!noteNameRef.value) return
  noteName.value = noteNameRef.value.value.replace(/\n/g, '')
  noteNameRef.value.value = noteName.value
  noteNameRef.value.style.height = 'auto';
  noteNameRef.value.style.height = `${noteNameRef.value.scrollHeight}px`;
}

function handleNoteNameBlur(e) {
  noteName.value = noteName.value.trim() ? noteName.value.trim() : t('empty.untitled')
  updateNoteName()
}

function handleCodemirrorSave(value) {
  submitNote()
}

function handleCodemirrorUpdate(value) {
  noteData.value.content = value
  autoSave.restart()
  autoSave2.restart()
  updateStatus.value = true
  window.isFormDirty.add(noteId)
}

function updateNoteNameOfActiveTab() {
  if (noteData.value && activeTab.value && activeTab.value?.title !== noteData.value.noteName) {
    noteData.value.noteName = activeTab.value.title
    noteName.value = activeTab.value.title
  }
}

useEventListener(document, 'selectionchange', () => {
  setTimeout(() => {
    selectionText.value = window?.getSelection?.()?.toString?.() || ''
  }, 50)
})

/**
 * @author www.EtestE.com
 */
function countChineseCharacters(value) {
  // 获取输入文本（修正选择器写法）
  const words = value;
  const charMap = {}; // 用于记录字符是否已统计过（去重）
  let iTotal = 0; // 汉字总数
  let iNumwords = 0; // 不重复汉字数
  let sTotal = 0; // 非单字节字符总数（全角字符）
  let sNumwords = 0; // 不重复非单字节字符数
  let eTotal = 0; // 单字节字符总数（半角字符）
  let inum = 0; // 数字总数

  // 合并为一次遍历
  for (let i = 0; i < words.length; i++) {
    const c = words.charAt(i);
    // 判断是否为汉字
    if (/[\u4e00-\u9fa5]/.test(c)) {
      iTotal++;
      if (!charMap[c]) {
        iNumwords++;
        charMap[c] = true;
      }
    }
    // 判断是否为非单字节字符（全角）
    if (/[^\x00-\xff]/.test(c)) {
      sTotal++;
      if (!charMap[c]) {
        sNumwords++;
        charMap[c] = true;
      }
    } else {
      // 单字节字符（半角）
      eTotal++;
    }
    // 判断是否为数字
    if (/[0-9]/.test(c)) {
      inum++;
    }
  }

  return {
    // 汉字
    // hanzi: iTotal,
    // 汉字+数字
    zishu: iTotal + inum,
    // // 全角中非汉字的部分（标点等）
    // biaodian: sTotal - iTotal,
    // // 半角中非数字的部分（字母、符号等）
    // zimu: eTotal - inum,
    // // 数字
    // shuzi: inum,
    // ci: (words ? words?.match(/\S+/g) || [] : []).length,
    // // 总字符数：汉字*2 + 其他全角*2 + 半角*1
    zifu: iTotal * 2 + (sTotal - iTotal) * 2 + eTotal,
  }
}

const codemirrorRef = useTemplateRef('codemirrorRef')
const cmSelectionText = ref('')

function handleCmSelectionChange(text) {
  cmSelectionText.value = text
}

const countCharsAndWords = computed(() => {
  // CodeMirror 虚拟视口渲染下，window.getSelection() 只捕获可见 DOM 节点，
  // 全选或跨视口选择时结果不完整，需通过 CodeMirror state API 获取准确选中文本
  let str
  if (cmSelectionText.value) {
    str = cmSelectionText.value
  } else {
    str = selectionText.value || markdownBody.value?.innerText || noteData.value?.content || ''
  }
  return countChineseCharacters(str)
})

watch(activeTab, updateNoteNameOfActiveTab, {deep: true})

onActivated((e) => {
  if (noteData.value) {
    window.__currentNoteId = noteId
    assetLoadingStatus.value = false
    nextTick(() => {
      assetLoadingStatus.value = true
    })
  }
})

onDeactivated(() => {
  assetLoadingStatus.value = false
})
onBeforeUnmount(() => {
  autoSave.pause()
  autoSave2.pause()
  controllerManager.cancel(`/asset/note/${noteId}`)
})

/**
 * 获取滚动容器中所有在可视区域内的子元素
 * @param {HTMLElement} scrollContainer 可滚动的父元素
 * @param {HTMLElement} container position 容器
 * @param {HTMLElement[]} children 子元素
 * @param {boolean} [strict=false] 是否严格匹配（仅完全可见的元素）
 * @returns {HTMLElement} 可视区域内的子元素列表
 */
function getVisibleChildren(scrollContainer, container, children, strict = false) {
  // 滚动容器的可视区域边界
  const {scrollTop, clientHeight} = scrollContainer;
  const viewportTop = scrollTop; // 可视区域顶部（相对于容器）
  const viewportBottom = scrollTop + clientHeight; // 可视区域底部（相对于容器）

  // 筛选可见元素
  return children.find(child => {
    // 跳过隐藏元素（display: none 或 visibility: hidden）
    if (child.style.visibility === 'hidden' ||
        child.offsetParent === null ||
        child.innerText === '\n' ||
        ['BR', 'UL', 'OL'].includes(child.nodeName)) return false;

    // 子元素的边界（相对于滚动容器）
    const childTop = child.offsetTop + container.offsetTop; // 元素顶部距离容器顶部的距离
    const offsetHeight = child.offsetHeight; // 元素自身高度
    const childBottom = childTop + offsetHeight; // 元素底部距离容器顶部的距离
    // 如果元素高度大于容器高度，同时位于底部返回true
    if (offsetHeight >= clientHeight && viewportBottom <= childBottom) return true
    if (strict) {
      // 严格模式：元素完全在可视区域内
      return childTop >= viewportTop && childBottom <= viewportBottom;
    } else {
      // 非严格模式：元素至少有一部分在可视区域内
      return childTop >= viewportTop
    }
  });
}

function getScrollLineNumber(e) {
  // 根据视图模式确定选择器和相关配置
  const {containerSelector, targetSelector, strict} = {
    readonly: {
      containerSelector: '.markdown-body',
      targetSelector: '.markdown-body *[data-source-line]',
      strict: false
    },
    sourcecode: {
      containerSelector: '.cm-editor',
      targetSelector: '.cm-lineWrapping .cm-line',
      strict: false
    },
    livePreview: {
      containerSelector: '.cm-editor',
      targetSelector: '.cm-lineWrapping .cm-line',
      strict: false
    }
  }[viewMode.value]

  try {
    // 获取容器元素和目标元素列表
    const container = document.querySelector(containerSelector);
    const targetElements = Array.from(document.querySelectorAll(targetSelector));

    // 获取可见子元素
    const visibleDom = getVisibleChildren(noteWrapperRef.value, container, targetElements, strict);

    if (!visibleDom) return -1;

    // 获取行号（优先自身，必要时检查父元素）
    let line = visibleDom.getAttribute('data-source-line');
    if (!line && viewMode.value === 'readonly') {
      line = visibleDom.parentElement?.getAttribute('data-source-line');
    }
    // console.log(line)
    return +line;
  } catch (e) {
    return -1
  }
}

// TODO 拦截浏览器搜索，并手动实现（Ctrl+F）搜索功能
</script>

<template>
  <loading-wrapper class="note-wrapper note-loading flex flex-1" :loading="loading">
    <div class="note">
      <toolbar-note :note="noteData" v-model:view-mode="viewMode" @viewModeClick="handleViewModeClick"></toolbar-note>
      <div ref="noteWrapperRef" @scrolla="getScrollLineNumber" class="note-wrapper dress-up-scrollbar asset-wrapper">
        <form>
          <label for="note-textarea-name" style="opacity: 0;width: 0;height: 0;position: absolute;">笔记名称</label>
          <textarea
              id="note-textarea-name"
              ref="noteNameRef"
              v-bind:value="noteName"
              class="note-name text-break"
              rows="1"
              @input="handleNoteNameInput"
              @keyup.enter="updateNoteName"
              @blur="handleNoteNameBlur"></textarea>
        </form>
        <keep-alive>
          <div
              v-if="viewMode==='readonly' && noteContentHtml"
              ref="markdownBody"
              class="markdown-body"
              v-html="noteContentHtml"></div>
          <codemirror-wrapper
              ref="codemirrorRef"
              v-else-if="(viewMode==='sourcecode' || viewMode==='livePreview') && !!noteData"
              v-bind:markdown-source="noteData.content"
              :noteVersion="noteVersion"
              :live-preview="viewMode === 'livePreview'"
              @save="handleCodemirrorSave"
              @update="handleCodemirrorUpdate"
              @selection-change="handleCmSelectionChange"
          ></codemirror-wrapper>
        </keep-alive>
      </div>
      <div class="note-footer flex items-center">
        <!--        <m-button type="base" size="mini" shape="square">
                  <template #icon>
                    <icon-book-open size="18" v-if="viewMode==='readonly'"/>
                    <icon-code size="18" v-if="viewMode==='sourcecode'"/>
                    <icon-edit size="18" v-if="viewMode==='realTimeViewing'"/>
                  </template>
                </m-button>-->
        <div>{{ countCharsAndWords.zishu }}个字</div>
        <div>{{ countCharsAndWords.zifu }}个字符</div>
        <!--        <div>{{countCharsAndWords.hanzi}}个汉字</div>-->
        <!--        <div>{{countCharsAndWords.zimu}}个字母</div>-->
        <!--        <div>{{countCharsAndWords.ci}}个词</div>-->
      </div>
    </div>
  </loading-wrapper>
</template>
<style lang="scss">
@use "sass:meta";

.theme-light {
  @include meta.load-css("highlight.js/styles/github.css");
  @include meta.load-css("github-markdown-css/github-markdown-light.css");
}

.theme-dark {
  @include meta.load-css("highlight.js/styles/github-dark.css");
  @include meta.load-css("github-markdown-css/github-markdown-dark.css");
}
.theme-dark .markdown-body{
  background-color: var(--color-bg-1);
}
pre code.hljs.text-break {
  word-wrap: break-word;
  word-break: break-all;
  white-space: pre-wrap;
}

.theme-light .markdown-body h1,
.theme-light .markdown-body h2,
.theme-light .markdown-body h3,
.theme-light .markdown-body h4,
.theme-light .markdown-body h5,
.theme-light .markdown-body h6 {
  padding: 0;
  border: 0;
  border-radius: var(--border-radius-medium);
}

.theme-light .markdown-body a {
  word-wrap: break-word;
  word-break: break-all;
  white-space: pre-line;
}

.markdown-body table {
  display: table !important;
  width: 100% !important;
  word-wrap: break-word;
  word-break: break-all;
  white-space: pre-line;
}

.markdown-body img {
  //width: 61%;
}
</style>
<style lang="scss" scoped>
.note-wrapper {
  position: relative;
}

.markdown-body {
  max-width: 764px;
  margin: 0 auto;
  padding-bottom: 200px;
  line-height: inherit;
  user-select: text;
  position: relative;
}

.note {
  flex: 1;
  width: 100%;
  display: flex;
  flex-direction: column;
  min-height: 100%;
  //margin-right: 2px;

  textarea {
    width: 100%;
    height: auto;
    overflow: hidden;
    resize: none;
  }

}

.note-name {
  display: block;
  max-width: 764px;
  margin: 0.67em auto;
  padding: 8px 0;
  font-size: 2em;
  font-weight: bold;
  color: inherit;
  background-color: transparent;
  border: 0;
  outline: 0;
}

:deep(.toolbar-container) {
  padding-left: 32px;
  padding-right: 32px;
}

.note-footer {
  position: fixed;
  right: 0;
  bottom: 0;
  column-gap: 12px;
  padding: 2px 8px;
  font-size: 14px;
  border-top-left-radius: 8px;
  background-color: var(--color-bg-1);
  border-top: 1px solid var(--color-border-2);
  border-left: 1px solid var(--color-border-2);
  z-index: 100;
  user-select: none;
}
</style>
