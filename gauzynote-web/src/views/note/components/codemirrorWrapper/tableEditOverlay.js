import { ViewPlugin, EditorView } from "@codemirror/view"
import { syntaxTree } from "@codemirror/language"
import { parseTable, serializeTable } from './tablePlugin.js'
import DOMPurify from "dompurify";

// ── CellOverlay：单元格编辑浮层 ──
// 点击表格单元格时，在 td/th 内部创建一个 contenteditable div，
// 隐藏原内容 div，编辑完成后将值写回文档并恢复显示。
class CellOverlay {
  /**
   * @param {EditorView} view - CodeMirror 编辑器实例
   * @param {number} tableFrom - 表格在文档中的起始位置
   * @param {number} row - 行索引（-1 为表头）
   * @param {number} col - 列索引
   * @param {object} callbacks - 回调集合 { onCommit, onCancel, onNavigate, onNewRow, onSave }
   * @param {number|undefined} cursorOffset - 光标在文本中的偏移位置
   */
  constructor(view, tableFrom, row, col, callbacks, cursorOffset) {
    this.view = view
    this.tableFrom = tableFrom
    this.row = row
    this.col = col
    this.cb = callbacks
    this.cursorOffset = cursorOffset
    this.overlay = null       // 编辑浮层 DOM 元素
    this.contentDiv = null    // 被隐藏的原内容 div
    this._committed = false   // 防止 blur 重复提交
  }

  // 创建浮层并开始编辑
  create() {
    // 定位目标单元格 DOM
    this.cellEl = this.view.dom.querySelector(
      `[data-table-from="${this.tableFrom}"] [data-row="${this.row}"][data-col="${this.col}"]`
    )
    if (!this.cellEl) return

    // 隐藏原内容 div，将 padding 让给浮层
    this.contentDiv = this.cellEl.querySelector('.cm-table-cell-content')
    if (this.contentDiv) this.contentDiv.style.display = 'none'

    // 创建可编辑浮层
    this.overlay = document.createElement('div')
    this.overlay.className = 'cm-table-cell-overlay'
    this.overlay.setAttribute('contenteditable', 'true')

    // 从文档中读取当前单元格值填充浮层
    const tableNode = this._findTableNode()
    if (tableNode) {
      const data = parseTable(tableNode, this.view.state)
      let text = ''
      if (this.row === -1) {
        text = data.headers[this.col] || ''
      } else if (this.row < data.rows.length) {
        text = data.rows[this.row][this.col] || ''
      }
      this.overlay.innerHTML = DOMPurify.sanitize(text)
      this._originalValue = text.replace(/<br>/gi, '\n')
    }

    this.overlay.addEventListener('keydown', (e) => this._handleKeydown(e))
    this.overlay.addEventListener('blur', () => this._handleBlur())

    this.cellEl.appendChild(this.overlay)
    this.overlay.focus()

    // 设置光标位置
    const sel = window.getSelection()
    if (sel) {
      const textNode = this.overlay.firstChild
      if (textNode && textNode.nodeType === Node.TEXT_NODE && this.cursorOffset !== undefined) {
        // 单行文本：定位到点击偏移处
        sel.setPosition(textNode, Math.min(this.cursorOffset, textNode.length))
        if (this.cursorOffset <= 0) {
          // 偏移为 0 表示点击在文本开头之前，选中整个文本方便替换
          let selection = window.getSelection();
          let newRange = document.createRange();
          newRange.setStart(textNode, this.cursorOffset);
          newRange.setEnd(textNode, textNode.length);
          selection.removeAllRanges();
          selection.addRange(newRange);
        }
      } else {
        // 多行或兜底：光标放到末尾
        const last = this.overlay.lastChild
        if (last) {
          const range = document.createRange()
          range.setStartAfter(last)
          range.collapse(true)
          sel.removeAllRanges()
          sel.addRange(range)
        }
      }
    }
  }

  // 从文档语法树查找 Table 节点，用于读取/写入单元格数据
  _findTableNode() {
    const tree = syntaxTree(this.view.state)
    const node = tree.resolve(this.tableFrom, 1)
    let tableNode = node
    while (tableNode && tableNode.name !== 'Table' && tableNode.name !== 'Document') {
      tableNode = tableNode.parent
    }
    return tableNode && tableNode.name === 'Table' ? tableNode : null
  }

  // 处理浮层内的键盘事件
  _handleKeydown(e) {
    if (e.isComposing) return // 跳过输入法组合态
    if ((e.metaKey || e.ctrlKey) && !e.shiftKey && e.key === 'z') {
      // Ctrl+Z：阻止冒泡防止 CM 误触发；contenteditable 原生撤回由浏览器处理。
      // 当所有本地编辑已撤回（值等于初始值）时，阻止默认行为并转发给 CM 执行文档级撤回
      e.stopPropagation()
      if (this.getValue() === this._originalValue) {
        e.preventDefault()
        this._committed = true
        this.cb.onUndo(this, e.metaKey, e.ctrlKey)
      }
    } else if ((e.metaKey || e.ctrlKey) && e.shiftKey && e.key === 'z') {
      // Ctrl+Shift+Z：浏览器原生重做优先；rAF 异步检查值是否变化，
      // 无变化说明重做栈已空，销毁浮层转发给 CM 执行文档级重做
      e.stopPropagation()
      const valBefore = this.getValue()
      requestAnimationFrame(() => {
        if (this._committed || !this.overlay) return
        if (this.getValue() === valBefore) {
          this._committed = true
          this.cb.onRedo(this, e.metaKey, e.ctrlKey)
        }
      })
    } else if ((e.metaKey || e.ctrlKey) && e.key === 'y') {
      // Ctrl+Y（Windows 重做）：同上逻辑
      e.stopPropagation()
      const valBefore = this.getValue()
      requestAnimationFrame(() => {
        if (this._committed || !this.overlay) return
        if (this.getValue() === valBefore) {
          this._committed = true
          this.cb.onRedo(this, e.metaKey, e.ctrlKey)
        }
      })
    } else if ((e.metaKey || e.ctrlKey) && e.key === 's') {
      // Cmd/Ctrl+S：提交并触发全局保存
      e.preventDefault()
      this._committed = true
      this.cb.onSave(this, e.metaKey, e.ctrlKey)
    } else if (e.key === 'Enter' && e.shiftKey) {
      // Shift+Enter：在当前行下方插入新行，光标定位到同列
      e.preventDefault()
      this._committed = true
      this.cb.onNewRow(this)
    } else if (e.key === 'Enter') {
      // Enter（无修饰键）：让浏览器自然插入换行（产生 <div>）
    } else if (e.key === 'Tab') {
      // Tab/Shift+Tab：提交当前单元格并导航到下一个/上一个
      e.preventDefault()
      this._committed = true
      this.cb.onNavigate(this, e.shiftKey ? 'prev' : 'next')
    }
  }

  // 浮层失去焦点时自动提交（延时 100ms 防止与按键回调竞态）
  _handleBlur() {
    setTimeout(() => {
      if (this._committed) return
      if (this.overlay && document.activeElement !== this.overlay) {
        this._committed = true
        this.cb.onCommit(this)
      }
    }, 0)
  }

  // 获取浮层当前文本内容
  // 使用 innerText 而非 textContent，因为 Chrome 下 contenteditable 回车产生 <div>，
  // textContent 不会在 <div> 之间插入换行符，而 innerText 会返回包含换行的文本
  getValue() {
    return this.overlay ? this.overlay.innerText : ''
  }

  // 销毁浮层，恢复原内容 div 显示
  destroy() {
    if (this.overlay) {
      this.overlay.remove()
      this.overlay = null
    }
    if (this.contentDiv) {
      this.contentDiv.style.display = ''
      this.contentDiv = null
    }
  }
}

// ── tableEditManager：表格编辑管理器（ViewPlugin） ──
// 监听 view.dom 上的 mousedown 事件，通过事件委托判断点击的单元格，
// 创建 CellOverlay 开始编辑。管理 overlay 生命周期（提交、取消、导航）。
const tableEditManager = ViewPlugin.fromClass(class {
  constructor(view) {
    this.view = view
    this.activeOverlay = null  // 当前活跃的编辑浮层
    this.pendingCell = null    // 待处理的单元格点击信息

    // 在 view.dom 上监听 mousedown，通过事件委托处理表格单元格点击
    this._mousedownHandler = (e) => this._handleMouseDown(e)
    this.view.dom.addEventListener('mousedown', this._mousedownHandler)
  }

  // CodeMirror 视图更新时同步 view 引用
  update(update) {
    this.view = update.view
    if (this.activeOverlay) {
      this.activeOverlay.view = update.view
    }
  }

  // 处理 mousedown 事件：判断点击目标是否为表格单元格
  _handleMouseDown(e) {
    const cell = e.target.closest('[data-table-cell]')
    if (!cell) {
      // 点击非单元格区域 → 提交当前编辑
      if (this.activeOverlay) this._commitCurrent()
      return
    }

    const tableEl = cell.closest('[data-table-from]')
    if (!tableEl) return

    const tableFrom = parseInt(tableEl.dataset.tableFrom)
    const row = parseInt(cell.dataset.row)
    const col = parseInt(cell.dataset.col)

    // 如果点击的是正在编辑的单元格，不做任何操作
    if (this.activeOverlay) {
      if (this.activeOverlay.tableFrom === tableFrom &&
          this.activeOverlay.row === row &&
          this.activeOverlay.col === col) {
        return
      }
      // 点击其他单元格 → 先提交当前编辑
      this._commitCurrent()
    }

    // 通过浏览器 API 获取点击位置对应的文本偏移，用于精确定位光标
    let cursorOffset = undefined
    if (e.clientX !== undefined) {
      const range = document.caretRangeFromPoint(e.clientX, e.clientY)
      if (range && range.startContainer) {
        cursorOffset = range.startOffset
      }
    }

    // 延迟到 requestMeasure 中启动编辑，确保 DOM 已稳定
    this.pendingCell = { tableFrom, row, col, cursorOffset }
    this.view.requestMeasure({
      read: () => {
        if (!this.pendingCell) return
        const { tableFrom, row, col, cursorOffset } = this.pendingCell
        this.pendingCell = null
        this._startEdit(tableFrom, row, col, cursorOffset)
      }
    })
  }

  // 启动单元格编辑
  _startEdit(tableFrom, row, col, cursorOffset) {
    if (this.activeOverlay) return
    // 标记表单为脏，确保页面/tab 关闭时触发未保存提示
    if (window.__currentNoteId) window.isFormDirty.add(window.__currentNoteId)
    this.activeOverlay = new CellOverlay(this.view, tableFrom, row, col, {
      onCommit: (overlay) => this._commitCurrent(),
      onCancel: (overlay) => this._cancelCurrent(),
      onNavigate: (overlay, dir) => this._navigate(dir),
      onNewRow: (overlay) => this._addRowAndEdit(overlay),
      onSave: (overlay, metaKey, ctrlKey) => this._saveAndCommit(overlay, metaKey, ctrlKey),
      onUndo: (overlay, metaKey, ctrlKey) => this._undoAndCommit(overlay, metaKey, ctrlKey),
      onRedo: (overlay, metaKey, ctrlKey) => this._redoAndCommit(overlay, metaKey, ctrlKey)
    }, cursorOffset)
    this.activeOverlay.create()
  }
  // 将浮层中的值写回文档中对应的单元格位置
  // 值中的 \n 转为 <br> 存储（GFM 表格不支持原生换行）
  _writeCellValue(tableFrom, row, col, value) {
    value = value.replace(/\n/g, '<br>')
    const tree = syntaxTree(this.view.state)
    const node = tree.resolve(tableFrom, 1)
    let tableNode = node
    while (tableNode && tableNode.name !== 'Table' && tableNode.name !== 'Document') {
      tableNode = tableNode.parent
    }
    if (!tableNode || tableNode.name !== 'Table') return false

    const data = parseTable(tableNode, this.view.state)
    if (row === -1) {
      if (col >= 0 && col < data.headers.length) data.headers[col] = value
    } else {
      if (row >= 0 && row < data.rows.length && col >= 0 && col < data.rows[row].length) {
        data.rows[row][col] = value
      }
    }
    const md = serializeTable(data.headers, data.align, data.rows)
    const existing = this.view.state.doc.sliceString(tableNode.from, tableNode.to)
    // 仅内容有变化时才派发变更，避免不必要的重渲染
    if (existing !== md) {
      this.view.dispatch({
        changes: { from: tableNode.from, to: tableNode.to, insert: md }
      })
    }
    return true
  }

  // 提交当前编辑：读取浮层值 → 写回文档 → 销毁浮层
  _commitCurrent() {
    if (!this.activeOverlay) return
    const overlay = this.activeOverlay
    const value = overlay.getValue()
    const { tableFrom, row, col } = overlay
    overlay.destroy()
    this.activeOverlay = null
    this._writeCellValue(tableFrom, row, col, value)
  }

  // Cmd/Ctrl+S：提交当前编辑后，聚焦 CM 并派发键盘事件触发 Mod-s 保存逻辑
  _saveAndCommit(overlay, metaKey, ctrlKey) {
    const value = overlay.getValue()
    const { tableFrom, row, col } = overlay
    overlay.destroy()
    this.activeOverlay = null
    this._writeCellValue(tableFrom, row, col, value)
    this.view.focus()
    this.view.contentDOM.dispatchEvent(new KeyboardEvent('keydown', {
      key: 's',
      code: 'KeyS',
      metaKey,
      ctrlKey,
      bubbles: true,
      cancelable: true
    }))
  }

  // 取消当前编辑：销毁浮层但不写回
  _cancelCurrent() {
    if (!this.activeOverlay) return
    this.activeOverlay.destroy()
    this.activeOverlay = null
  }

  // Ctrl+Z 已撤回全部本地编辑后：销毁浮层不写回，聚焦 CM 后转发 Ctrl+Z 事件执行文档级撤回
  _undoAndCommit(overlay, metaKey, ctrlKey) {
    overlay.destroy()
    this.activeOverlay = null
    this.view.focus()
    this.view.contentDOM.dispatchEvent(new KeyboardEvent('keydown', {
      key: 'z',
      code: 'KeyZ',
      metaKey,
      ctrlKey,
      bubbles: true,
      cancelable: true
    }))
  }

  // Ctrl+Shift+Z / Ctrl+Y 重做栈已空：销毁浮层不写回，聚焦 CM 后转发重做事件执行文档级重做
  _redoAndCommit(overlay, metaKey, ctrlKey) {
    overlay.destroy()
    this.activeOverlay = null
    this.view.focus()
    this.view.contentDOM.dispatchEvent(new KeyboardEvent('keydown', {
      key: 'z',
      code: 'KeyZ',
      shiftKey: true,
      metaKey,
      ctrlKey,
      bubbles: true,
      cancelable: true
    }))
  }

  // Shift+Enter：提交当前单元格 → 在下方插入新行 → 开始编辑新行同列单元格
  // 将「写回当前值」和「插入新行」合并为一次 dispatch，避免嵌套 requestMeasure 问题
  _addRowAndEdit(overlay) {
    const value = overlay.getValue().replace(/\n/g, '<br>')
    const { tableFrom, row, col } = overlay
    overlay.destroy()
    this.activeOverlay = null

    const tree = syntaxTree(this.view.state)
    const node = tree.resolve(tableFrom, 1)
    let tableNode = node
    while (tableNode && tableNode.name !== 'Table' && tableNode.name !== 'Document') {
      tableNode = tableNode.parent
    }
    if (!tableNode || tableNode.name !== 'Table') return

    const data = parseTable(tableNode, this.view.state)
    // 先更新当前单元格的值
    if (row === -1) {
      if (col >= 0 && col < data.headers.length) data.headers[col] = value
    } else if (row >= 0 && row < data.rows.length && col >= 0 && col < data.rows[row].length) {
      data.rows[row][col] = value
    }
    // 在当前行下方插入新行
    const cols = data.rows.length ? data.rows[0].length : data.headers.length
    const insertPos = Math.max(0, row + 1)
    data.rows.splice(insertPos, 0, Array(cols).fill(""))

    const md = serializeTable(data.headers, data.align, data.rows)
    const existing = this.view.state.doc.sliceString(tableNode.from, tableNode.to)
    if (existing !== md) {
      this.view.dispatch({
        changes: { from: tableNode.from, to: tableNode.to, insert: md }
      })
    }

    // dispatch 完成后，在 requestMeasure 中启动新单元格编辑
    this.view.requestMeasure({
      read: () => {
        this._startEdit(tableFrom, insertPos, col, 0)
      }
    })
  }

  // Tab/Shift+Tab：提交当前单元格 → 计算下一个单元格位置 → 开始编辑
  // 超出表格范围时聚焦回 CodeMirror
  _navigate(dir) {
    if (!this.activeOverlay) return
    const overlay = this.activeOverlay
    const value = overlay.getValue()
    const { tableFrom, row, col } = overlay

    overlay.destroy()
    this.activeOverlay = null
    this._writeCellValue(tableFrom, row, col, value)

    this.view.requestMeasure({
      read: () => {
        const tree = syntaxTree(this.view.state)
        const node = tree.resolve(tableFrom, 1)
        let tableNode = node
        while (tableNode && tableNode.name !== 'Table' && tableNode.name !== 'Document') {
          tableNode = tableNode.parent
        }
        if (!tableNode || tableNode.name !== 'Table') return

        const data = parseTable(tableNode, this.view.state)
        const totalRows = data.rows.length
        const totalCols = data.headers.length

        let nextRow = row
        let nextCol = col

        if (dir === 'next') {
          nextCol++
          if (nextCol >= totalCols) {
            nextCol = 0
            nextRow++
            // 超出最后一行 → 聚焦回 CodeMirror，不再进入下一个 overlay
            if (nextRow >= totalRows) { this.view.focus(); return }
          }
        } else {
          nextCol--
          if (nextCol < 0) {
            nextCol = totalCols - 1
            nextRow--
            // 超出表头 → 聚焦回 CodeMirror
            if (nextRow < -1) { this.view.focus(tableNode.from - 1); return }
          }
        }

        if (this.activeOverlay) return
        this.activeOverlay = new CellOverlay(this.view, tableFrom, nextRow, nextCol, {
          onCommit: (o) => this._commitCurrent(),
          onCancel: (o) => this._cancelCurrent(),
          onNavigate: (o, d) => this._navigate(d),
          onNewRow: (o) => this._addRowAndEdit(o),
          onSave: (o, metaKey, ctrlKey) => this._saveAndCommit(o, metaKey, ctrlKey),
          onUndo: (o, metaKey, ctrlKey) => this._undoAndCommit(o, metaKey, ctrlKey),
          onRedo: (o, metaKey, ctrlKey) => this._redoAndCommit(o, metaKey, ctrlKey)
        }, 0)
        this.activeOverlay.create()
      }
    })
  }

  // 插件销毁时提交活跃浮层并清理事件监听（tab 关闭场景）
  destroy() {
    if (this.activeOverlay) {
      this._commitCurrent()
    }
    if (this._mousedownHandler) {
      this.view.dom.removeEventListener('mousedown', this._mousedownHandler)
      this._mousedownHandler = null
    }
  }
}, {
  eventHandlers: {}
})

// 确保表格编辑浮层已提交（供外部 API 调用前使用）
export function ensureTableEditCommitted(view) {
  const plugin = view.plugin(tableEditManager)
  if (plugin && plugin.activeOverlay) {
    plugin._commitCurrent()
  }
}

export const tableEditOverlay = [
  tableEditManager,
  EditorView.theme({
    ".cm-table-cell-overlay": {
      padding: "6px 12px",
      outline: "none",
    },
  })
]
