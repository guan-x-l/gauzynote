import { ViewPlugin, Decoration, WidgetType, EditorView } from "@codemirror/view"
import { syntaxTree } from "@codemirror/language"
import { RangeSetBuilder, StateField } from "@codemirror/state"
import DOMPurify from 'dompurify';

// ════════════════════════════════════════════════════════════════
// Widget 类型定义 — 用于 Decoration.replace 替换 Markdown 语法符号
// ════════════════════════════════════════════════════════════════

// 不可见占位 widget：将 Markdown 标记语法隐藏（如 **、#、[] 等）
class InvisibleWidget extends WidgetType {
  eq() { return true }
  toDOM() { return document.createElement('span') }
  ignoreEvent() { return true }
}

// trimSpanWidget
class trimSpanWidget extends WidgetType {
  constructor(text) { super(); this.text = text }
  toDOM() {
    const span = document.createElement('span')
    span.textContent = this.text.trim()
    return span
  }
  ignoreEvent() { return true }
}

// 换行 widget：<br> 标签替换
class BrWidget extends WidgetType {
  toDOM() { return document.createElement('br') }
  ignoreEvent() { return true }
}

// 任务列表复选框 widget：[ ] / [x] 替换为可点击的 checkbox
class TaskCheckboxWidget extends WidgetType {
  constructor(checked, pos) { super(); this.checked = checked; this.pos = pos }
  toDOM() {
    const label = document.createElement('label')
    label.className = 'cm-live-preview-task-checkbox'
    const input = document.createElement('input')
    input.type = 'checkbox'
    input.checked = this.checked
    // mousedown 切换复选框状态，通过全局变量获取当前 view 实例
    input.addEventListener('mousedown', (e) => {
      e.preventDefault()
      const view = window.__cmView
      if (!view) return
      const toggled = this.checked ? '[ ]' : '[x]'
      view.dispatch({
        changes: { from: this.pos, to: this.pos + 3, insert: toggled },
        selection: { anchor: this.pos + 4 }
      })
    })
    label.appendChild(input)
    return label
  }
  ignoreEvent() { return true }
}

// HTML 标签 widget：未匹配的 HTML 标签以文本形式显示
class HtmlTagWidget extends WidgetType {
  constructor(text) { super(); this.text = text }
  toDOM() {
    const span = document.createElement('span')
    span.textContent = this.text
    return span
  }
  // 不阻止事件，允许光标进入选中复制
  ignoreEvent() { return false }
}

// 判断 HTML 内容是否应以源码形式展示（style / script 标签不能被 DOMPurify 安全处理）
function isSourceOnlyHtml(html) {
  return /^<(style|script)\b/i.test(html.trim())
}

// HTML 嵌入 widget：渲染经过 DOMPurify 消毒的 HTML 内容
class HtmlEmbedWidget extends WidgetType {
  constructor(html, isBlock) { super(); this.html = html; this.isBlock = isBlock }
  toDOM() {
    const template = document.createElement('template');
    template.innerHTML = DOMPurify.sanitize(this.html)
    return template.content.firstChild || document.createTextNode('')
  }
  ignoreEvent(event) {
    return false // 允许与嵌入 HTML 交互
  }
}

// 水平分割线 widget：替换 --- / *** / ___ 为 <hr>
class HorizontalRuleWidget extends WidgetType {
  toDOM() {
    const span = document.createElement('span')
    span.style.display = 'contents'
    const br = document.createElement('br')
    const hr = document.createElement('hr')
    hr.className = 'cm-live-preview-hr'
    span.appendChild(br)
    span.appendChild(hr)
    return span
  }
  ignoreEvent() { return true }
}

// 引用块边框 widget：替换 > 为竖线边框
class BlockquoteBorderWidget extends WidgetType {
  constructor(isActive) { super(); this.isActive = isActive;}
  toDOM() {
    const span = document.createElement('span')
    span.className = 'cm-live-preview-bq-border'
    if (!this.isActive) {
      span.style.color = "transparent"
    }
    span.innerText = '>'
    return span
  }
  ignoreEvent() { return true }
}

// 无序列表项目符号 widget：替换 -/* /+ 为 •
class BulletWidget extends WidgetType {
  toDOM() {
    const span = document.createElement('span')
    span.className = 'cm-live-preview-bullet'
    span.textContent = '• '
    return span
  }
  ignoreEvent() { return true }
}

// 图片 widget：渲染 <img> 元素
class ImageWidget extends WidgetType {
  constructor(src, alt, title) {
    super()
    this.src = src
    this.alt = alt
    this.title = title
  }
  eq(other) { return other.src === this.src && other.alt === this.alt && other.title === this.title }
  toDOM() {
    const img = document.createElement('img')
    img.src = this.src
    img.alt = this.alt
    if (this.title) img.title = this.title
    img.className = 'cm-live-preview-image'
    return img
  }
  // 允许事件，确保右键菜单、拖拽等操作正常
  ignoreEvent() { return false }
}

// ── 辅助函数：为 Link 内的 Image 节点添加 decoration ──
// 处理链接中的图片 ![alt](url)，隐藏语法标记、显示图片 widget。
// 返回 Image 节点的结束位置，供调用方推进 markStart 指针。
function addImageDecorations(cursor, builder, state, selFrom, selTo, linkIsActive) {
  const imgFrom = cursor.from
  const imgTo = cursor.to
  const imgIsActive = linkIsActive || (selFrom <= imgTo && selTo >= imgFrom && !(selFrom === selTo && selFrom === imgFrom))

  const imgChildren = []
  if (cursor.firstChild()) {
    do { imgChildren.push({ name: cursor.name, from: cursor.from, to: cursor.to }) } while (cursor.nextSibling())
    cursor.parent()
  }

  const linkMarks = imgChildren.filter(c => c.name === 'LinkMark')
  const urlNode = imgChildren.find(c => c.name === 'URL')
  // alt 文本位于两个 LinkMark 之间（第一个 [ 和第二个 [ 之间）
  let altFrom = imgFrom, altTo = imgTo
  if (linkMarks.length >= 2) {
    altFrom = linkMarks[0].to
    altTo = linkMarks[1].from
  }

  if (urlNode) {
    const src = state.doc.sliceString(urlNode.from, urlNode.to)
    const alt = altFrom < altTo ? state.doc.sliceString(altFrom, altTo) : ''
    const imgWidget = new ImageWidget(src, alt, '')

    if (!imgIsActive) {
      // 光不在 Image 范围内时，隐藏所有语法标记
      const reps = []
      for (const c of imgChildren) {
        if (c.name === 'LinkMark' || c.name === 'URL' || c.name === 'LinkTitle') {
          reps.push({ from: c.from, to: c.to })
        }
      }
      if (altFrom < altTo) reps.push({ from: altFrom, to: altTo })
      // 按位置排序确保 decoration 顺序正确
      reps.sort((a, b) => a.from - b.from)
      for (const r of reps) builder.add(r.from, r.to, Decoration.replace({ widget: new InvisibleWidget() }))
    }
    builder.add(imgTo, imgTo, Decoration.widget({ widget: imgWidget, side: -1 }))
  }

  return imgTo
}

// 判断节点是否在 nodeNames 内部
function insideNode(node, nodeNames) {
  let p = node.parent
  while (p && p.type.name !== 'Document') {
    if (nodeNames.includes(p.type.name)) return true
    p = p.parent
  }
  return false
}

// 判断节点是否在 Link 或 Autolink 内部
function insideLink(node) {
  return insideNode(node, ['Link', 'Autolink'])
}

// ════════════════════════════════════════════════════════════════
// 核心函数：遍历可见区域的语法树，构建所有 live preview decorations
// ════════════════════════════════════════════════════════════════
function buildDecorations(view) {
  const builder = new RangeSetBuilder()
  const { state } = view
  const tree = syntaxTree(state)
  const selFrom = state.selection.main.from
  const selTo = state.selection.main.to

  /**
   * 判断选区是否与 [from, to] 范围重叠；光标恰在 from 且无选区时返回 false，
   * 避免切换模式后默认 (0,0) 选区误触发 isActive
   * @param {number} from
   * @param {number} to
   * @returns {boolean}
   */
  function inRange(from, to) {
    return selFrom <= to && selTo >= from - 1 && !(selFrom === selTo && selFrom === from - 1)
  }

  // ── 预扫描：匹配 HTML 标签对 ──
  // 在可见区域内找出所有配对的 HTML 开始/结束标签，
  // 配对的标签将被渲染为真实 HTML，未配对则以文本形式显示。
  // htmlPairs: openingTag.from → closingTag.to（配对标记）
  //            closingTag.from → true（标记为已处理）
  const htmlPairs = new Map()
  // 自闭合/空元素，不需要匹配结束标签
  const voidElements = new Set(['br', 'hr', 'img', 'input', 'meta', 'link', 'area', 'base', 'col', 'embed', 'source', 'track', 'wbr'])
  const htmlTags = []
  for (const { from, to } of view.visibleRanges) {
    tree.iterate({
      from, to,
      enter(ref) {
        if (ref.name === 'HTMLTag') {
          htmlTags.push({ from: ref.from, to: ref.to, text: state.doc.sliceString(ref.from, ref.to) })
        }
      }
    })
  }
  // 用栈式深度匹配找出配对的开始/结束标签
  for (let i = 0; i < htmlTags.length; i++) {
    const tag = htmlTags[i]
    if (htmlPairs.has(tag.from)) continue
    const isSelfClosing = /\/\s*>$/.test(tag.text)
    const openMatch = tag.text.match(/^<\/?([a-zA-Z][\w-]*)/)
    if (!openMatch) continue
    const tagName = openMatch[1].toLowerCase()
    if (isSelfClosing || voidElements.has(tagName)) continue
    // 跳过结束标签（它们会在匹配到开始标签时被处理）
    if (tag.text.startsWith('</')) continue
    // 从当前位置向后查找匹配的结束标签
    let depth = 1
    for (let j = i + 1; j < htmlTags.length; j++) {
      if (htmlPairs.has(htmlTags[j].from)) continue
      const closeMatch = htmlTags[j].text.match(/^<\/([a-zA-Z][\w-]*)/)
      if (closeMatch && closeMatch[1].toLowerCase() === tagName) {
        depth--
        if (depth === 0) {
          htmlPairs.set(tag.from, htmlTags[j].to)
          htmlPairs.set(htmlTags[j].from, true) // 标记结束标签已处理
          break
        }
      } else {
        // 嵌套的同名开始标签 → 深度 +1
        const nestedOpen = htmlTags[j].text.match(/^<([a-zA-Z][\w-]*)/)
        if (nestedOpen && nestedOpen[1].toLowerCase() === tagName && !/\/\s*>$/.test(htmlTags[j].text)) {
          depth++
        }
      }
    }
  }

  // 遍历可见范围，为每种语法节点添加 decoration
  for (const { from, to } of view.visibleRanges) {
    tree.iterate({
      from, to,
      enter(nodeRef) {
        const name = nodeRef.name
        // console.log(name)
        // console.log(state.doc.sliceString(nodeRef.from, nodeRef.to))

        // ── ATX 标题（ATXHeading1~6） ──
        // 添加行 decoration 改变字号，不隐藏 # 标记（由 HeaderMark 处理）
        if (name.startsWith('ATXHeading')) {
          const match = name.match(/^ATXHeading(\d)$/)
          if (match) {
            builder.add(
              nodeRef.from, nodeRef.from,
              Decoration.line({ class: `cm-live-preview-heading-${match[1]}` })
            )
          }
          return
        }

        // ── 标题标记 # ──
        // 光不在标题行内时隐藏 # 及紧随其后的一个空格，保证标题文本左对齐
        if (name === 'HeaderMark') {
          const parent = nodeRef.node.parent
          if (parent && /^ATXHeading\d$/.test(parent.type.name)) {
            const isActive = inRange(parent.from, parent.to)
            if (!isActive) {
              let endPos = nodeRef.to
              // 将 # 后的空格也纳入隐藏范围，保证标题文本左对齐
              if (endPos < state.doc.length && state.doc.sliceString(endPos, endPos + 1) === ' ') {
                endPos = endPos + 1
              }
              builder.add(
                nodeRef.from, endPos,
                Decoration.replace({ widget: new InvisibleWidget() })
              )
            }
          }
        }

        // ── 强调标记 ** 和 * ──
        // 用外层 StrongEmphasis/Emphasis 的选中状态判断所有嵌套标记，
        // 光标在 *** 边缘时也能激活所有层级的标记显示
        if (name === 'EmphasisMark') {
          const parent = nodeRef.node.parent
          if (parent && (parent.type.name === 'StrongEmphasis' || parent.type.name === 'Emphasis')) {
            const isLink = insideLink(nodeRef.node)
            let parentFrom = parent.from
            let parentTo = parent.to
            if (parent.parent && (parent.parent.type.name === 'StrongEmphasis' || parent.parent.type.name === 'Emphasis')){
              parentFrom = parent.parent.from
              parentTo = parent.parent.to
            }
            const isActive = inRange(parentFrom, parentTo)
            const isStrong = parent.type.name === 'StrongEmphasis'
            const units = isStrong ? 2 : 1
            if (!isActive) {
              builder.add(
                nodeRef.from, nodeRef.to,
                Decoration.replace({ widget: new InvisibleWidget() })
              )
              if (isLink && nodeRef.to <= parent.to - units){
                builder.add(parent.from + units, parent.to - units, Decoration.mark({ class: 'cm-live-preview-link-text' }))
              }
            } else {
              if (isLink){
                builder.add(parent.from, parent.to, Decoration.mark({ class: 'cm-live-preview-link-text' }))
              }
            }
            // 仅在首个 EmphasisMark（开标记）上添加粗体/斜体样式 decoration
            if (nodeRef.from === parent.from) {
              const styleClass = isStrong ? 'cm-live-preview-strong' : 'cm-live-preview-emphasis'
              builder.add(parent.from, parent.to, Decoration.mark({ class: styleClass }))
            }
          }
        }

        // ── 行内代码标记 ` ──
        // 隐藏反引号并添加代码背景色样式
        if (name === 'CodeMark') {
          const parent = nodeRef.node.parent
          const linkClass = insideLink(nodeRef.node) ? "cm-live-preview-link-text" : ""
          if (parent && parent.type.name === 'InlineCode') {
            const isActive = inRange(parent.from, parent.to)
            if (!isActive) {
              builder.add(
                nodeRef.from, nodeRef.to,
                Decoration.replace({ widget: new InvisibleWidget() })
              )
            }
            // 只在开头的 CodeMark 上添加 mark decoration（避免重复）
            if (nodeRef.from === parent.from) {
              if (isActive) {
                // 光在行内代码内时，整个范围加样式
                builder.add(
                  parent.from, parent.to,
                  Decoration.mark({ class: 'cm-live-preview-inline-code ' + linkClass })
                )
              } else {
                // 光不在行内代码内时，仅在内容部分加样式（排除前后的 `）
                let closeMarkFrom = parent.to
                const cursor = parent.cursor()
                if (cursor.firstChild()) {
                  do {
                    if (cursor.name === 'CodeMark' && cursor.from !== nodeRef.from) {
                      closeMarkFrom = cursor.from
                    }
                  } while (cursor.nextSibling())
                }
                const contentStart = nodeRef.to
                if (contentStart < closeMarkFrom) {
                  builder.add(
                    contentStart, closeMarkFrom,
                    Decoration.mark({ class: 'cm-live-preview-inline-code ' + linkClass })
                  )
                }
              }
            }
          }
        }

        // ── 围栏代码块 ──
        // 为所有行添加背景色，首行/末行分别添加圆角样式
        if (name === 'FencedCode' || name === 'CodeBlock') {
          const firstLine = state.doc.lineAt(nodeRef.from)
          const lastLine = state.doc.lineAt(nodeRef.to)
          const lineCount = lastLine.number - firstLine.number + 1
          for (let i = firstLine.number; i <= lastLine.number; i++) {
            const line = state.doc.line(i)
            let cls = 'cm-live-preview-code-block'
            if (i === firstLine.number) cls += ' cm-live-preview-code-block-first'
            if (i === lastLine.number) cls += ' cm-live-preview-code-block-last'
            builder.add(line.from, line.from, Decoration.line({ class: cls }))
          }
        }

        // ── HTML 注释 ──
        // 默认隐藏，光选中时显示原文
        if (name === 'CommentBlock') {
          if (!(inRange(nodeRef.from, nodeRef.to))) {
            builder.add(nodeRef.from, nodeRef.to, Decoration.replace({ widget: new InvisibleWidget() }))
          }
          return false
        }

        // ── 转义符 \ ──
        // 隐藏反斜杠，保留被转义字符显示（如 \* 显示为 *）
        if (name === 'Escape' && !insideLink(nodeRef.node)) {
          const line = state.doc.lineAt(nodeRef.from)
          const isActive = inRange(line.from, line.to)
          if (!isActive) {
            builder.add(nodeRef.from, nodeRef.from + 1, Decoration.replace({ widget: new InvisibleWidget() }))
          }
        }

        // ── 任务列表标记 [ ] / [x] ──
        // 替换 - [ ] 及之前的 ListMark 为复选框 widget
        if (name === 'TaskMarker') {
          const parent = nodeRef.node.parent
          // 向前查找 ListMark（- 或 *）的起始位置
          let listMarkFrom = nodeRef.from - 2
          if (parent) {
            const cursor = parent.cursor()
            if (cursor.firstChild()) {
              do {
                if (cursor.name === 'ListMark') { listMarkFrom = cursor.from; break }
              } while (cursor.nextSibling())
            }
          }
          const isActive = inRange(listMarkFrom, nodeRef.to)
          if (!isActive) {
            const text = state.doc.sliceString(nodeRef.from, nodeRef.from + 3)
            builder.add(listMarkFrom, nodeRef.to, Decoration.replace({
              widget: new TaskCheckboxWidget(text === '[x]', nodeRef.from)
            }))
          }
        }

        // ── 删除线标记 ~~ ──
        if (name === 'StrikethroughMark') {
          const parent = nodeRef.node.parent
          if (parent && parent.type.name === 'Strikethrough') {
            const isActive = inRange(parent.from, parent.to)
            const isLink = insideLink(nodeRef.node)
            if (!isActive) {
              builder.add(
                nodeRef.from, nodeRef.to,
                Decoration.replace({ widget: new InvisibleWidget() })
              )
              if (isLink && nodeRef.to <= parent.to - 2){
                builder.add(parent.from + 2, parent.to - 2, Decoration.mark({ class: 'cm-live-preview-link-text' }))
              }
            } else {
              if (isLink){
                builder.add(parent.from, parent.to, Decoration.mark({ class: 'cm-live-preview-link-text' }))
              }
            }
            // 仅在首个 StrikethroughMark（开标记）上添加删除线样式 decoration
            if (nodeRef.from === parent.from) {
              builder.add(parent.from, parent.to, Decoration.mark({ class: 'cm-live-preview-strike' }))
            }
          }
        }

        // ── HTML 标签 ──
        // 配对的开始/结束标签：将整个 HTML 片段替换为真实渲染
        // 未配对的标签：以文本形式显示，<br> 特殊处理为 BrWidget
        if (name === 'HTMLTag') {
          const pairInfo = htmlPairs.get(nodeRef.from)
          // 配对的结束标签：已由开始标签处理，跳过
          if (pairInfo === true) { return false }
          // 配对的开始标签：替换整个 HTML 元素
          if (pairInfo != null) {
            const isActive = inRange(nodeRef.from, pairInfo)
            if (!isActive) {
              const html = state.doc.sliceString(nodeRef.from, pairInfo)
              // style / script 跳过 widget 渲染，保留源码展示
              if (!isSourceOnlyHtml(html)) {
                builder.add(nodeRef.from, pairInfo, Decoration.replace({ widget: new HtmlEmbedWidget(html) }))
              }
            }
            return false
          }
          // 未配对标签：以文本形式显示或处理 <br>
          const isActive = inRange(nodeRef.from, nodeRef.to)
          if (!isActive) {
            const tagText = state.doc.sliceString(nodeRef.from, nodeRef.to)
            if (/^<br\s*\/?\s*>$/i.test(tagText)) {
              builder.add(nodeRef.from, nodeRef.to, Decoration.replace({ widget: new BrWidget() }))
            } else {
              builder.add(nodeRef.from, nodeRef.to, Decoration.replace({ widget: new HtmlTagWidget(tagText) }))
            }
          }
          return false
        }

        // ── 图片 ![alt](url) ──
        // 非活跃时隐藏语法标记显示图片；活跃时显示原文。
        // 跳过 Link 内部的 Image（由 LinkMark 处理器统一处理，避免 decoration 顺序冲突）
        if (name === 'Image' && nodeRef.node.parent?.type.name !== 'Link') {
          const isActive = inRange(nodeRef.from, nodeRef.to)
          const children = []
          let altFrom = nodeRef.from, altTo = nodeRef.to
          let urlNode = null
          const cursor = nodeRef.node.cursor()
          if (cursor.firstChild()) {
            do {
              children.push({ name: cursor.name, from: cursor.from, to: cursor.to })
              if (cursor.name === 'URL') urlNode = { from: cursor.from, to: cursor.to }
            } while (cursor.nextSibling())
          }
          // alt 文本位于第 1 个和第 2 个 LinkMark 之间
          const linkMarks = children.filter(c => c.name === 'LinkMark')
          if (linkMarks.length >= 2) {
            altFrom = linkMarks[0].to
            altTo = linkMarks[1].from
          }
          if (urlNode) {
            const src = state.doc.sliceString(urlNode.from, urlNode.to)
            const alt = altFrom < altTo ? state.doc.sliceString(altFrom, altTo) : ''
            const imgWidget = new ImageWidget(src, alt, '')
            if (!isActive) {
              // 收集所有需要隐藏的语法标记，按位置排序后逐个添加 decoration
              const replacements = []
              for (const c of children) {
                if (c.name === 'LinkMark' || c.name === 'URL' || c.name === 'LinkTitle') {
                  replacements.push({ from: c.from, to: c.to })
                }
              }
              if (altFrom < altTo) {
                replacements.push({ from: altFrom, to: altTo })
              }
              replacements.sort((a, b) => a.from - b.from)
              for (const r of replacements) {
                builder.add(r.from, r.to, Decoration.replace({ widget: new InvisibleWidget() }))
              }
            }
            // 图片 widget 放在末尾，side:-1 与 LinkMark replace 的 startSide:-1 对应
            builder.add(nodeRef.to, nodeRef.to, Decoration.widget({ widget: imgWidget, side: -1 }))
          }
          return false
        }

        // ── 自动链接（裸 URL 无尖括号） ──
        // 无子节点的 Autolink 即为裸 URL，显示为链接样式
        if (name === 'Autolink') {
          const cursor = nodeRef.node.cursor()
          // 仅处理裸 URL（无子节点）；尖括号自动链接有 LinkMark 子节点
          if (!cursor.firstChild()) {
            const isActive = inRange(nodeRef.from, nodeRef.to)
            if (!isActive) {
              builder.add(nodeRef.from, nodeRef.to, Decoration.mark({ class: 'cm-live-preview-link-text' }))
            }
          }
        }

        // ── 链接标记 [ ] ( ) 和 < > ──
        // 处理 Markdown 链接语法：隐藏括号，给链接文本加样式。
        // 链接内可包含图片（如 [text ![img](src)](url)），需分段处理。
        if (name === 'LinkMark') {
          const parent = nodeRef.node.parent
          if (parent && (parent.type.name === 'Link' || parent.type.name === 'Autolink')) {
            const ch = state.doc.sliceString(nodeRef.from, Math.min(nodeRef.from + 1, state.doc.length))
            const isLink = parent.type.name === 'Link'
            const isActive = inRange(parent.from, parent.to)

            // 开始括号 [ 或 < ：需要给链接文本加 mark decoration
            if (ch === '[' || ch === '<') {
              if (isLink) {
                // 遍历 Link 子节点，分段添加链接文本样式，并在遇到 Image 节点时
                // 调用 addImageDecorations 插入图片 widget
                let closeBracketPos = parent.to
                const cursor = parent.cursor()
                if (cursor.firstChild()) {
                  let bracketCount = 0
                  let markStart = nodeRef.to
                  do {
                    const cName = cursor.name
                    // console.log(cName)
                    // console.log(state.doc.sliceString(cursor.from, cursor.to))
                    if (cName === 'LinkMark') {
                      bracketCount++
                      if (bracketCount === 1) {
                        // 隐藏第一个 [（在此处处理而非循环前，确保 builder 顺序正确）
                        if (!isActive) {
                          builder.add(nodeRef.from, nodeRef.to, Decoration.replace({ widget: new InvisibleWidget() }))
                        }
                        continue
                      }
                      if (bracketCount === 2) closeBracketPos = cursor.from // 记录 ] 位置
                      // 给 markStart 到当前括号之间的文本添加链接样式
                      if (markStart < cursor.from) {
                        builder.add(markStart, cursor.from, Decoration.mark({ class: 'cm-live-preview-link-text' }))
                      }
                      markStart = cursor.to
                    } else if (cName === 'Image') {
                      // 链接内嵌图片：添加链接文本样式后插入图片 widget
                      if (markStart < cursor.from) {
                        builder.add(markStart, cursor.from, Decoration.mark({ class: 'cm-live-preview-link-text' }))
                      }
                      markStart = addImageDecorations(cursor, builder, state, selFrom, selTo, isActive)
                    } else if (cName === 'Escape') {
                      // 隐藏转义反斜杠
                      if (!isActive) {
                        builder.add(cursor.from, cursor.from + 1, Decoration.replace({ widget: new InvisibleWidget() }))
                      }
                      markStart = Math.max(markStart, cursor.to)
                    }
                    // else if (cName === 'HTMLTag') {
                      // builder.add(cursor.from, cursor.to, Decoration.mark({ class: 'cm-live-preview-link-text' }))
                      // markStart = Math.max(markStart, cursor.to)
                    // }
                  else {
                      markStart = Math.max(markStart, cursor.to)
                    }
                  } while (cursor.nextSibling())
                }
              } else {
                // Autolink：隐藏 < > 并将 URL 显示为链接样式
                if (!isActive) {
                  builder.add(nodeRef.from, nodeRef.to, Decoration.replace({ widget: new InvisibleWidget() }))
                }
                const cursor = parent.cursor()
                if (cursor.firstChild()) {
                  do {
                    if (cursor.name === 'URL') {
                      builder.add(cursor.from, cursor.to, Decoration.mark({ class: 'cm-live-preview-link-text' }))
                      break
                    }
                  } while (cursor.nextSibling())
                }
              }
            } else {
              // 非开始的 LinkMark（] 、 ) 、 > 等）：直接隐藏
              if (!isActive) {
                builder.add(nodeRef.from, nodeRef.to, Decoration.replace({ widget: new InvisibleWidget() }))
              }
            }
            return
          }
        }

        // ── Link 内的 URL ──
        // 非活跃时隐藏，避免显示冗余的 URL 文本
        if (name === 'URL') {
          const parent = nodeRef.node.parent

          if (parent && parent.type.name === 'Link') {
            const isActive = inRange(parent.from, parent.to)
            if (!isActive) {
              // 仅隐藏链接目标 URL（出现在 ] 之后），
              // 不隐藏链接文本中的 URL（如 [https://example.com](https://example.com) 的文本部分）
              let closingBracketBefore = false
              const cursor2 = parent.cursor()
              let lc = 0
              if (cursor2.firstChild()) {
                do {
                  if (cursor2.name === 'LinkMark') lc++
                  if (lc >= 2 && cursor2.to <= nodeRef.from) { closingBracketBefore = true; break }
                } while (cursor2.nextSibling() && cursor2.from < nodeRef.from)
              }
              if (closingBracketBefore) {
                builder.add(nodeRef.from, nodeRef.to, Decoration.replace({ widget: new InvisibleWidget() }))
              }
            }
          }
          if (['Link', 'LinkReference', 'Paragraph'].includes(parent.type.name)){
            builder.add(nodeRef.from, nodeRef.to, Decoration.mark({class: 'cm-live-preview-link-text' }))
          }
        }

        // ── Link 内的 Title ──
        // 非活跃时隐藏（如 "title" in [text](url "title")）
        if (name === 'LinkTitle') {
          const parent = nodeRef.node.parent
          if (parent && parent.type.name === 'Link') {
            const isActive = inRange(parent.from, parent.to)
            if (!isActive) {
              builder.add(nodeRef.from, nodeRef.to, Decoration.replace({ widget: new InvisibleWidget() }))
            }
          }
        }

        // ── 水平分割线 --- / *** / ___ ──
        // 整行替换为 <hr> widget
        if (name === 'HorizontalRule') {
          const line = state.doc.lineAt(nodeRef.from)
          const isActive = inRange(line.from, line.to)
          builder.add(
              nodeRef.from, nodeRef.from,
              Decoration.line({ class: `cm-line-hr` })
          )
          if (!isActive) {
            builder.add(
                line.from, line.to,
              Decoration.replace({ widget: new HorizontalRuleWidget() })
            )
          }
        }

        // ── 无序列表标记 - / * / + ──
        // 隐藏标记和紧跟的空格，替换为 • 符号；跳过任务列表项（由 TaskMarker 处理）
        if (name === 'ListMark') {
          const parent = nodeRef.node.parent
          // 向上查找 ListItem 祖先（ListMark 可能在 Paragraph 内）
          let li = parent
          while (li && li.type.name !== 'ListItem' && li.type.name !== 'Document') li = li.parent
          if (li && li.type.name === 'ListItem') {
            const grandparent = li.parent
            if (grandparent && grandparent.type.name === 'BulletList') {
              // 检查是否为任务列表项（含 [ ] 或 [x]），任务列表不处理
              const liText = state.doc.sliceString(li.from, Math.min(li.to, li.from + 20))
              const isTask = /-\s+\[[ x]\]/.test(liText)
              if (!isTask) {
                const markLine = state.doc.lineAt(nodeRef.from)
                const isActive = inRange(markLine.from, markLine.to)
                if (!isActive) {
                  builder.add(
                    nodeRef.from, nodeRef.to + 1,
                    Decoration.replace({ widget: new BulletWidget() })
                  )
                }
              }
            }
          }
        }

        // ── 引用标记 > ──
        // > 替换为竖线边框 widget；文本内容用 Decoration.mark 包裹为 inline-block，
        if (name === 'QuoteMark') {
          builder.add(
              nodeRef.from, nodeRef.from,
              Decoration.line({
                attributes: { style: `padding-inline-start:16px;text-indent: -11px;` }
              })
          )
          const line = state.doc.lineAt(nodeRef.from)
          const isActive = inRange(line.from, line.to)
          builder.add(
              nodeRef.from, nodeRef.to,
              Decoration.replace({ widget: new BlockquoteBorderWidget(isActive) })
          )
        }
      }
    })
  }

  return builder.finish()
}

// ── ViewPlugin：在每次文档/视口/选区变化时重建 live preview decorations ──
export const livePreviewPlugin = ViewPlugin.fromClass(class {
  constructor(view) {
    this.decorations = buildDecorations(view)
    window.__cmView = view // 全局引用供 TaskCheckboxWidget 使用
  }
  update(update) {
    if (update.docChanged || update.viewportChanged || update.selectionSet) {
      this.decorations = buildDecorations(update.view)
    }
    window.__cmView = update.view
  }
}, { decorations: v => v.decorations })

// ── StateField：HTMLBlock 跨行替换 ──
// HTMLBlock 可能跨多行，ViewPlugin 的 decoration 不能跨 \n，因此用 StateField 处理
function buildHtmlBlockDecorations(state) {
  const builder = new RangeSetBuilder()
  const tree = syntaxTree(state)
  const selFrom = state.selection.main.from
  const selTo = state.selection.main.to
  tree.iterate({
    from: 0, to: state.doc.length,
    enter(ref) {
      if (ref.name === 'HTMLBlock') {
        if (!(selFrom <= ref.to && selTo >= ref.from && !(selFrom === selTo && selFrom === ref.from))) {
          const html = state.doc.sliceString(ref.from, ref.to)
          // line decoration 让容器变为 block 布局
          builder.add(ref.from, ref.from, Decoration.line({ class: 'cm-html-embed cm-embed-block' }))
          // style / script 跳过 widget 渲染，保留源码展示
          if (!isSourceOnlyHtml(html)) {
            builder.add(ref.from, ref.to, Decoration.replace({ widget: new HtmlEmbedWidget(html, true) }))
          }
        }
        return false
      }
    }
  })
  return builder.finish()
}

export const htmlBlockField = StateField.define({
  create(state) {
    return buildHtmlBlockDecorations(state)
  },
  update(decorations, tr) {
    return buildHtmlBlockDecorations(tr.state)
  },
  provide: field => EditorView.decorations.from(field)
})
