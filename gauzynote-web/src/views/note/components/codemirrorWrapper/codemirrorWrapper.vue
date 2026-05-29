<script setup>
import {nextTick, onActivated, onDeactivated, onMounted, shallowRef, useTemplateRef, watch} from "vue";
import {history, defaultKeymap, historyKeymap} from "@codemirror/commands";
import {search, searchKeymap} from "@codemirror/search";
import {
  crosshairCursor,
  drawSelection, dropCursor,
  EditorView,
  highlightSpecialChars,
  keymap,
  lineNumbers, rectangularSelection
} from "@codemirror/view";
import {
  bracketMatching,
  defaultHighlightStyle, foldGutter,
  foldKeymap,
  indentOnInput, indentUnit,
  syntaxHighlighting,
  syntaxTree
} from '@codemirror/language';
import {EditorState, SelectionRange} from "@codemirror/state";
import {markdown} from "@codemirror/lang-markdown";
import {GFM} from "@lezer/markdown";
import {javascriptLanguage, typescriptLanguage} from "@codemirror/lang-javascript";
import {htmlLanguage} from "@codemirror/lang-html";
import {cssLanguage} from "@codemirror/lang-css";
import {jsonLanguage} from "@codemirror/lang-json";
import {pythonLanguage} from "@codemirror/lang-python";
import {javaLanguage} from "@codemirror/lang-java";
import {yamlLanguage} from "@codemirror/lang-yaml";
import {vueLanguage} from "@codemirror/lang-vue";
import {sql, StandardSQL} from "@codemirror/lang-sql";
import {handleUploadImageList} from "@/biz/upload.js";
import {useAppStore, useTabsStore, useThemeStore} from "@/store/index.js";
import {
  headerClassPlugin,
  headingHighlightPlugin,
  highlightedLineField, lineAttrsFieldPlugin, setVersionField,
  setHighlightedLine, versionField, syncLineHeightPlugin
} from "./cm-plugin.js";
import {codemirrorTheme} from "./cm-theme.js";
import {extractRelativeImagePath, getImageById} from "@/biz/image.js";
import {NodeType} from "@/enum/index.js";
import {Message} from "@/components/index.js";
import {customizeKeymap, handleHeadingKeyBoardEvent,} from "@/views/note/components/codemirrorWrapper/cm-key.js";
import {useI18n} from "vue-i18n";
import {oneDark} from "@codemirror/theme-one-dark";
import {uploadNetworkImage} from "@/api/upload.js";
import {livePreviewPlugin, htmlBlockField} from "./livePreviewPlugin.js";
import {tableField} from "./tablePlugin.js";
import {tableEditOverlay} from "./tableEditOverlay.js";
import {codeHighlightStyle} from "./cm-codeHighlight.js";


const props = defineProps({
  // markdown源码
  markdownSource: String,
  livePreview: {
    type: Boolean,
    default: true
  }
});
const emit = defineEmits(['save', 'update', 'selectionChange'])

const {t} = useI18n()
const {activeTab} = useTabsStore()
const {isDark} = useThemeStore()
const {tocSelectLineEnd,tocLineNumber, dragNodeId, getResourceNodeByNodeId} = useAppStore()
watch(()=>props.markdownSource, () => {
  if (view.value.state.doc.toString() !== props.markdownSource.toString()) {
    update()
  }
})
const codemirrorWrapper = useTemplateRef('codemirrorWrapper')
const view = shallowRef(null)

/**
 * 根据围栏代码块的语言标识返回对应的 CodeMirror Language 对象，
 * 实现代码块内的嵌套语言语法高亮。
 */
const sqlLanguage = sql({ dialect: StandardSQL }).language;

function codeLanguages(langName) {
  const langMap = {
    javascript: javascriptLanguage, js: javascriptLanguage,
    typescript: typescriptLanguage, ts: typescriptLanguage,
    html: htmlLanguage,
    css: cssLanguage,
    json: jsonLanguage,
    python: pythonLanguage, py: pythonLanguage,
    java: javaLanguage,
    sql: sqlLanguage,
    yaml: yamlLanguage,
    vue: vueLanguage,
  }
  return langMap[langName.toLowerCase()] || null
}

function getExtensions() {
  const exts = [
    /*lineNumbers({domEventHandlers: (e)=>{
        console.log(e)
      }}),*/
    lineNumbers(),
    indentUnit.of("    "),
    // highlightActiveLineGutter(), // 激活行高亮;给选中行的行号加高亮类
    highlightSpecialChars(),
    history(),
    // foldGutter(),
    drawSelection(),
    dropCursor(),
    EditorState.allowMultipleSelections.of(true),
    indentOnInput(),
    syntaxHighlighting(codeHighlightStyle, { fallback: defaultHighlightStyle }),
    bracketMatching(),
    EditorView.lineWrapping,
    // closeBrackets(),
    // autocompletion(),
    rectangularSelection(),
    crosshairCursor(),
    search({top: true}),
    // highlightActiveLine(),
    // highlightSelectionMatches(),
    keymap.of([
      ...defaultKeymap.filter(fi=>fi.key !== 'Mod-i'),
      // ...closeBracketsKeymap,
      ...historyKeymap,
      ...searchKeymap,
      ...foldKeymap,
      // indentWithTab, 事件已被 smartIndent 重写
      // ...completionKeymap,
      // ...lintKeymap,
      {
        key: 'Mod-s',
        run(_view) {
          emit('save', view.value.state.doc.toString())
          return true
        }
      },
      ...customizeKeymap
    ]),
    // 编辑器的语言支持（GFM 扩展：删除线、任务列表、表格）
    // codeLanguages 启用围栏代码块嵌套语言解析，实现语法高亮
    markdown({ extensions: GFM, codeLanguages }),
    headerClassPlugin,
    highlightedLineField,
    versionField,
    headingHighlightPlugin,
    lineAttrsFieldPlugin,
    syncLineHeightPlugin,
    codemirrorTheme,
    // cursorTooltip(),
    EditorView.updateListener.of((viewUpd) => {
      if (viewUpd.docChanged) {
        updateCallback()
      }
      if (viewUpd.selectionSet) {
        const { from, to } = viewUpd.state.selection.main
        emit('selectionChange', from !== to ? viewUpd.state.sliceDoc(from, to) : '')
      }
    }),
    /*   EditorView.dragMovesSelection.of((e)=>{
   // e.stopPropagation()
   console.log(e)
   e.preventDefault()

 }),*/
    // 添加监听
    EditorView.domEventHandlers({
      keydown: (event, view) => {
        // 判断是否为 Meta+Alt+数字 组合。解决macbook电脑按下按键无反应的问题
        if (
            event.metaKey && // Meta 键（⌘）按下
            event.altKey &&  // Alt 键（⌥）按下
            /^Digit[1-6]$/.test(event.code) // 按下 1-6 数字键
        ) {
          handleHeadingKeyBoardEvent(view, event.keyCode - 48); // 执行标题设置逻辑
          event.preventDefault(); // 阻止特殊字符生成
          event.stopPropagation(); // 阻止其他事件处理
          return true; // 标记事件已处理
        }
      },
      click(e,v) {
        // console.log(e)
        // console.log(v)
        // const posAtDOM =  v.posAtDOM(e.target)
        // console.log(posAtDOM)
        // console.log(v.domAtPos(posAtDOM))
        // console.log(e)
        // console.log(view.value)
        // console.log(view.value.hasFocus)
        //   console.log(view.value.state.selection)
        //   console.log(view.value.state.selection.main.from)
        //   console.log( view.value.state.doc.lineAt(view.value.state.selection.main.from))
        // console.log(view.value.state.selection.main)
        // console.log(view.value.state.doc.toString())
        // Ctrl/Cmd+click on link text → open URL
        if ((e.metaKey || e.ctrlKey) && e.target) {
          // todo 需要优化，覆盖面不全
          const pos = v.posAtCoords({ x: e.clientX, y: e.clientY })
          if (pos == null) return
          const linkEl = e.target.closest('.cm-live-preview-link-text')
          const imgEl = e.target.closest('.cm-live-preview-image')
          if (linkEl || imgEl) {
            const tree = syntaxTree(v.state)
            const node = tree.resolveInner(pos, 1)
            let linkNode = null
            for (let n = node; n && n.name !== 'Document'; n = n.parent) {
              if (n.name === 'Link' || n.name === 'Autolink') { linkNode = n; break }
            }
            if (linkNode) {
              const cursor = linkNode.cursor()
              if (cursor.firstChild()) {
                do {
                  if (cursor.name === 'URL') {
                    const href = v.state.doc.sliceString(cursor.from, cursor.to)
                    if (href) {
                      e.preventDefault()
                      window.open(href, '_blank')
                      return true
                    }
                  }
                } while (cursor.nextSibling())
              }
            }
          }
        }
      },
      // contextmenu(e){
      // console.log(e)
      // },
      // dragover(e, v){
      //   e.preventDefault()
      // },
      // drop: async (event, v) => {
      //
      // },
    })
  ]
  if (props.livePreview) {
    exts.push(livePreviewPlugin)
    exts.push(htmlBlockField)
    exts.push(tableField)
    exts.push(...tableEditOverlay)
  }
  return exts
}
// 创建 EditorView
function createView() {
  const viewState = EditorState.create({
    doc: props.markdownSource,
    extensions: isDark.value ? [...getExtensions(), oneDark] : getExtensions()
  })
  view.value = new EditorView({
    state: viewState,
    parent: codemirrorWrapper.value,
    lineWrapping: true,
  })
  nextTick(()=>{
    view.value.requestMeasure();
  })
}

function reconfigureState() {
  if (!view.value) return
  const oldState = view.value.state
  const newState = EditorState.create({
    doc: oldState.doc,
    extensions: isDark.value ? [...getExtensions(), oneDark] : getExtensions()
  })
  view.value.setState(newState)
}

watch([isDark, () => props.livePreview], () => {
  reconfigureState()
})
function update() {
  view.value.dispatch({
    changes: {
      from: 0,
      to: view.value.state.doc.length, // 结束的位置
      insert: props.markdownSource
    }
  })
}

function updateCallback() {
  if (view.value.state.doc.toString() !== props.markdownSource.toString()){
    setHighlightedLine(view.value, null)
    emit('update', view.value.state.doc.toString())
  }
}

async function handleDrop(event) {
  if (dragNodeId.value) {
    console.log(dragNodeId.value)
    console.log(getResourceNodeByNodeId(dragNodeId.value))
    const node = getResourceNodeByNodeId(dragNodeId.value)

    if (!NodeType.isImage(node.nodeType)){
      Message.warning(t('codemirror.dropNoImage'))
      return
    }

    const res = await getImageById(node.relatedId)
    console.log(res)
    if (res){
      const relativePath = extractRelativeImagePath(res.imagePath, import.meta.env.VITE_IMAGE_BASE_DIR);
      const imageSrc = import.meta.env.VITE_IMAGE_BASE_URL + '/image/' + relativePath
      view.value.dispatch({
        changes: {
          from: view.value.posAtCoords({ x: event.clientX, y: event.clientY }),
          insert: `![${res.imageName}](${imageSrc})`
        }
      })
    }
    return
  }
  const files = event.dataTransfer.files;
  if (files.length){
    const uploadResults = await handleUploadImageList(files, activeTab.value.nodeId)
    console.log(uploadResults)
    uploadResults.forEach(item=>{
      if (item.success){
        view.value.dispatch({
          changes: {
            from: view.value.posAtCoords({ x: event.clientX, y: event.clientY }),
            insert: `![${item.name}](${item.result.data.filePath})`
          }
        })
      }
    })
    // clearDragenterNodeId()
    // await updateResourceNodeData();
    // load.close()
  } else if (event.dataTransfer.items) {// 场景2：拖拽的是图片元素（从其他网页拖拽的img标签）
    for (let i = 0; i < event.dataTransfer.items.length; i++) {
      const item = event.dataTransfer.items[i];
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
          formData.append('parentNodeId', activeTab.value.nodeId);
          const result = await uploadNetworkImage(formData);
          if (result.code === 200) {
            view.value.dispatch({
              changes: {
                from: view.value.posAtCoords({ x: event.clientX, y: event.clientY }),
                insert: `![${result.data.fileName}](${result.data.filePath})`
              }
            })
          }
        }
      }
    }
  }
}

 function handleTocLineNumber(v) {
  if (tocLineNumber.value < 0) return
  const line = view.value.state.doc.line(tocLineNumber.value)
  let a = view.value.domAtPos(line.from)
   console.log(a)
  view.value.dispatch({
    // yMargin需要通过行高计算一致
    // 模式切换时定位到同一行，粗略的方式是匹配两边行数，切换后定位。有时间可以调研是否有更好的方式
    effects: EditorView.scrollIntoView(line.from, { y: "start", yMargin: 10})
  })
  tocLineNumber.value = -1
  // if (v) {
    setTimeout(()=>{
      nextTick(()=>{
        setVersionField(view.value)
      })
    },100)
  // }
}


watch(()=>tocSelectLineEnd.value, ()=>{
  if (tocSelectLineEnd.value < 0) return
  // 滚动到该行
  // view.value.scrollIntoView()
  view.value.dispatch({
    selection: new SelectionRange(tocSelectLineEnd.value, tocSelectLineEnd.value),
    // 可选：将光标位置滚动到视图中
    // scrollIntoView: true
    effects: EditorView.scrollIntoView(tocSelectLineEnd.value, { y: "center"})
  })
  view.value.focus()
  const line = view.value.state.doc.lineAt(tocSelectLineEnd.value)
  setHighlightedLine(view.value, line.number)
  tocSelectLineEnd.value = -1
})
watch(tocLineNumber, ()=>handleTocLineNumber())

onActivated(() => {
  // 调用时机为首次挂载
  // 以及每次从缓存中被重新插入时
  // keydownEv.restart()
})

onDeactivated(() => {
  // 在从 DOM 上移除、进入缓存
  // 以及组件卸载时调用
  // keydownEv.stop()
})
onMounted(() => {
  createView()
  handleTocLineNumber(true)
  // document.addEventListener('keydown', (e) => {
  //   console.log(e)
  // })
})
</script>

<template>
  <div
      ref="codemirrorWrapper"
      class="codemirror-wrapper"
      @dragover.prevent
      @drop.prevent="handleDrop">
  
  </div>
</template>
<style>
.ͼp{
  background-color: transparent;
}
.cm-live-preview-bq-border:before{
  content: "";
}
.cm-live-preview-code-block:before{
  content: "";
}
.cm-live-preview-html-block:before{
  content: "";
}
</style>
<style lang="scss" scoped>
.codemirror-wrapper {
  max-width: 764px;
  line-height: 1.6251;
  margin: 0 auto;
}
</style>
