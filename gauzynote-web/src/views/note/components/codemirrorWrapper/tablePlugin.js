import { Decoration, WidgetType, EditorView } from "@codemirror/view"
import { syntaxTree } from "@codemirror/language"
import { RangeSetBuilder, StateField } from "@codemirror/state"
import { ensureTableEditCommitted } from './tableEditOverlay.js'
import DOMPurify from "dompurify";

// ── 将 Lezer Table 节点解析为结构化数据 ──
// 返回 { headers: string[], align: ('left'|'center'|'right')[], rows: string[][] }
function parseTable(node, state) {
  const headers = []
  const rows = []
  const align = []
  const cursor = node.cursor()
  if (cursor.firstChild()) {
    do {
      const cName = cursor.name
      if (cName === 'TableHeader') {
        // 解析表头行
        collectCells(cursor, state, headers)
      } else if (cName === 'TableRow') {
        // 解析数据行
        const r = []
        collectCells(cursor, state, r)
        if (r.length) rows.push(r)
      } else if (cName === 'TableDelimiter') {
        // 从分隔符行解析对齐方式（如 :---, :---:, ---:）
        const delimCursor = cursor.node.cursor()
        if (delimCursor.firstChild()) {
          do {
            if (delimCursor.name === 'TableCell') {
              const text = state.doc.sliceString(delimCursor.from, delimCursor.to).trim()
              if (text.startsWith(':') && text.endsWith(':')) align.push('center')
              else if (text.endsWith(':')) align.push('right')
              else align.push('left')
            } else {
              // 分隔符节点直接包含 --- 文本（无子 TableCell）
              const text = state.doc.sliceString(delimCursor.from, delimCursor.to)
              if (text.includes(':')) {
                if (text.startsWith(':') && text.endsWith(':')) align.push('center')
                else if (text.endsWith(':')) align.push('right')
                else align.push('left')
              }
            }
          } while (delimCursor.nextSibling())
        }
      }
    } while (cursor.nextSibling())
  }
  // 确保对齐数组长度与表头列数一致，不足的默认左对齐
  while (align.length < headers.length) align.push('left')
  return { headers, align, rows }
}

// 收集行内的单元格文本到 target 数组
// Lezer 行内子节点为 TableDelimiter(|) 和 TableCell 交替出现。
// 纯空格单元格不产生 TableCell，导致两个连续 TableDelimiter，
// 通过检测连续 TD 来补全空单元格。
function collectCells(rowCursor, state, target) {
  const cellCursor = rowCursor.node.cursor()
  const nodes = []
  if (cellCursor.firstChild()) {
    do {
      nodes.push({ name: cellCursor.name, from: cellCursor.from, to: cellCursor.to })
    } while (cellCursor.nextSibling())
  }

  // 遍历 TD/TC 序列，检测连续 TableDelimiter 补空单元格
  let prevWasTD = false
  for (const n of nodes) {
    if (n.name === 'TableCell') {
      target.push(unescapeCell(state.doc.sliceString(n.from, n.to).trim()))
      prevWasTD = false
    } else if (n.name === 'TableDelimiter') {
      if (prevWasTD) target.push('') // 连续两个 TD = 空单元格
      prevWasTD = true
    }
  }
}

// ── 管道符转义/去转义 ──
// GFM 表格中 \| 表示字面量 |，\\ 表示字面量 \

// 序列化时转义：先 \ → \\，再 | → \|
function escapeCell(text) {
  return text.replace(/\\/g, '\\\\').replace(/\|/g, '\\|')
}

// 解析时去转义：先 \| → |，再 \\ → \（顺序重要，不能颠倒）
function unescapeCell(text) {
  return text.replace(/\\\|/g, '|').replace(/\\\\/g, '\\')
}

// ── 将结构化数据序列化为 Markdown 表格 ──
function serializeTable(headers, align, rows) {
  const lines = []
  // 表头行
  lines.push('| ' + headers.map((h, i) => padCell(h, align[i])).join(' | ') + ' |')
  // 分隔符行（携带对齐信息）
  lines.push('| ' + align.map(a => {
    if (a === 'center') return ':---:'
    if (a === 'right') return '---:'
    return '---'
  }).join(' | ') + ' |')
  // 数据行
  for (const row of rows) {
    lines.push('| ' + row.map((c, i) => padCell(c, align[i])).join(' | ') + ' |')
  }
  return lines.join('\n')
}

// 为单元格内容补齐空格 padding 并转义管道符
function padCell(text, align) {
  const trimmed = escapeCell(text.trim())
  if (align === 'right') return ' ' + trimmed
  if (align === 'center') return ' ' + trimmed + ' '
  return ' ' + trimmed + ' '
}

// ── TableWidget：渲染表格的 DOM Widget ──
// CodeMirror Decoration.replace 使用此 widget 替换原始 Markdown 表格文本
class TableWidget extends WidgetType {
  constructor(tableData, tableFrom) {
    super()
    this.headers = tableData.headers
    this.align = tableData.align
    this.rows = tableData.rows
    this.tableFrom = tableFrom // 表格在文档中的起始位置，用于编辑时定位
  }

  // 构建表格 DOM 结构，包裹在 contenteditable="false" 中防止浏览器全选时
  // 选中 widget 内渲染文本（尤其是表格在文档第一行时）。不影响 overlay 编辑。
  toDOM() {
    const wrapper = document.createElement('div')
    wrapper.setAttribute('contenteditable', 'false')
    wrapper.className = "cm-live-preview-table-wrapper";

    const table = document.createElement('table')
    table.className = 'cm-live-preview-table'
    table.dataset.tableFrom = String(this.tableFrom)

    // 表头
    const thead = document.createElement('thead')
    const headerRow = document.createElement('tr')
    for (let i = 0; i < this.headers.length; i++) {
      const th = document.createElement('th')
      th.dataset.col = i
      th.dataset.row = -1 // -1 表示表头行
      th.dataset.tableCell = 'true'

      const content = document.createElement('div')
      content.className = 'cm-table-cell-content'
      content.innerHTML = DOMPurify.sanitize(this.headers[i])
      if (this.align[i]) content.style.textAlign = this.align[i]
      th.appendChild(content)
      headerRow.appendChild(th)
    }
    thead.appendChild(headerRow)
    table.appendChild(thead)

    // 表体
    const tbody = document.createElement('tbody')
    for (let r = 0; r < this.rows.length; r++) {
      const tr = document.createElement('tr')
      for (let c = 0; c < this.rows[r].length; c++) {
        const td = document.createElement('td')
        td.dataset.col = c
        td.dataset.row = r
        td.dataset.tableCell = 'true'

        const content = document.createElement('div')
        content.className = 'cm-table-cell-content'
        content.innerHTML = DOMPurify.sanitize(this.rows[r][c])
        if (this.align[c]) content.style.textAlign = this.align[c]
        td.appendChild(content)
        tr.appendChild(td)
      }
      tbody.appendChild(tr)
    }
    table.appendChild(tbody)
    wrapper.appendChild(table)
    return wrapper
  }

  // CodeMirror 用 eq 判断 widget 是否需要重建；
  // 必须比较 headers 和 rows 内容，否则数据变更后 widget DOM 不会更新
  eq(other) {
    if (!(other instanceof TableWidget)) return false
    if (this.tableFrom !== other.tableFrom) return false
    if (this.headers.length !== other.headers.length) return false
    if (this.rows.length !== other.rows.length) return false
    for (let i = 0; i < this.headers.length; i++) {
      if (this.headers[i] !== other.headers[i]) return false
    }
    for (let r = 0; r < this.rows.length; r++) {
      if (this.rows[r].length !== other.rows[r].length) return false
      for (let c = 0; c < this.rows[r].length; c++) {
        if (this.rows[r][c] !== other.rows[r][c]) return false
      }
    }
    return true
  }

  // 阻止 CodeMirror 处理表格内的所有事件，交由 overlay 管理
  ignoreEvent() { return true }
}

// ── StateField：为所有 Table 节点生成 Decoration ──
// 用 Decoration.replace 将原始 Markdown 替换为 TableWidget
function buildTableDecorations(state) {
  const builder = new RangeSetBuilder()
  const tree = syntaxTree(state)
  tree.iterate({
    from: 0, to: state.doc.length,
    enter(ref) {
      if (ref.name === 'Table') {
        const tableData = parseTable(ref.node, state)
        const widget = new TableWidget(tableData, ref.from)
        // line decoration 用于给表格行添加容器样式
        builder.add(ref.from, ref.from, Decoration.line({ class: 'cm-live-preview-table-line' }))
        // replace decoration 将整个 Table 节点替换为 widget
        builder.add(ref.from, ref.to, Decoration.replace({ widget }))
        return false // 跳过 Table 内部子节点遍历
      }
    }
  })
  return builder.finish()
}

export const tableField = StateField.define({
  create(state) { return buildTableDecorations(state) },
  update(deco, tr) { return buildTableDecorations(tr.state) },
  provide: f => EditorView.decorations.from(f)
})

// ── 表格操作 API ──
// 提供增删行列、设置对齐等操作，所有方法在操作前先提交当前编辑中的 overlay
export function getTableAPI(view) {
  // 向上查找 Table 节点的辅助函数
  const findTable = (pos) => {
    const tree = syntaxTree(view.state)
    const node = tree.resolve(pos, 1)
    let tableNode = node
    while (tableNode && tableNode.name !== 'Table' && tableNode.name !== 'Document') tableNode = tableNode.parent
    return tableNode && tableNode.name === 'Table' ? tableNode : null
  }

  // 重新序列化并替换整个表格
  const replaceTable = (tableNode, data) => {
    const md = serializeTable(data.headers, data.align, data.rows)
    view.dispatch({ changes: { from: tableNode.from, to: tableNode.to, insert: md } })
  }

  return {
    // 在末尾追加一行
    addRow(pos) {
      ensureTableEditCommitted(view)
      const tableNode = findTable(pos)
      if (!tableNode) return
      const data = parseTable(tableNode, view.state)
      if (data.rows.length) {
        const cols = data.rows[0].length
        data.rows.push(Array(cols).fill(''))
      }
      replaceTable(tableNode, data)
    },
    // 删除指定行
    removeRow(pos, rowIndex) {
      ensureTableEditCommitted(view)
      const tableNode = findTable(pos)
      if (!tableNode) return
      const data = parseTable(tableNode, view.state)
      if (rowIndex >= 0 && rowIndex < data.rows.length) data.rows.splice(rowIndex, 1)
      replaceTable(tableNode, data)
    },
    // 在指定位置插入一行
    insertRow(pos, rowIndex) {
      ensureTableEditCommitted(view)
      const tableNode = findTable(pos)
      if (!tableNode) return
      const data = parseTable(tableNode, view.state)
      const cols = data.rows.length ? data.rows[0].length : data.headers.length
      data.rows.splice(rowIndex, 0, Array(cols).fill(''))
      replaceTable(tableNode, data)
    },
    // 在末尾追加一列
    addCol(pos) {
      ensureTableEditCommitted(view)
      const tableNode = findTable(pos)
      if (!tableNode) return
      const data = parseTable(tableNode, view.state)
      data.headers.push('')
      data.align.push('left')
      for (const row of data.rows) row.push('')
      replaceTable(tableNode, data)
    },
    // 删除指定列（至少保留一列）
    removeCol(pos, colIndex) {
      ensureTableEditCommitted(view)
      const tableNode = findTable(pos)
      if (!tableNode) return
      const data = parseTable(tableNode, view.state)
      if (data.headers.length <= 1) return
      data.headers.splice(colIndex, 1)
      data.align.splice(colIndex, 1)
      for (const row of data.rows) row.splice(colIndex, 1)
      replaceTable(tableNode, data)
    },
    // 在指定位置插入一列
    insertCol(pos, colIndex) {
      ensureTableEditCommitted(view)
      const tableNode = findTable(pos)
      if (!tableNode) return
      const data = parseTable(tableNode, view.state)
      data.headers.splice(colIndex, 0, '')
      data.align.splice(colIndex, 0, 'left')
      for (const row of data.rows) row.splice(colIndex, 0, '')
      replaceTable(tableNode, data)
    },
    // 设置列对齐方式
    setAlign(pos, alignments) {
      ensureTableEditCommitted(view)
      const tableNode = findTable(pos)
      if (!tableNode) return
      const data = parseTable(tableNode, view.state)
      for (let i = 0; i < alignments.length && i < data.align.length; i++) {
        data.align[i] = alignments[i]
      }
      replaceTable(tableNode, data)
    }
  }
}

export { parseTable, serializeTable }
