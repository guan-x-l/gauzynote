// 判断是否为有序列表行
import {INDENT_SIZE} from "@/views/note/components/codemirrorWrapper/cm-constants.js";
import {indentLess, indentMore} from "@codemirror/commands";
// 判断是否为有序列表行
function isOrderedListLine(line) {
    return /^\s*\d+\.\s/.test(line);
}

// 解析有序列表行
function parseOrderedListLine(line){
    const match = line.match(/^(\s*)(\d+)\.\s(.*)$/);
    if (!match) return null;
    return { indent: match[1], number: match[2], content: match[3] };
}

// 获取连续的有序列表块（前后无非列表行或空行）
function getOrderedListBlock(state, lineNum) {
    const doc = state.doc;
    let start = lineNum;
    let end = lineNum;
    while (start > 1 && isOrderedListLine(doc.line(start - 1).text)) start--;
    while (end < doc.lines && isOrderedListLine(doc.line(end + 1).text)) end++;
    return { start, end };
}

// 重新编号整个块，返回每一行的最终文本（包括缩进变化和序号重排）
function renumberBlock(
    state,
    startLine,
    endLine,
    tempLines // 已被缩进修改的行
) {
    const result = new Map();
    const lines= [];

    // 收集块内所有行，优先使用 tempLines 中的修改
    for (let i = startLine; i <= endLine; i++) {
        const original = state.doc.line(i).text;
        const text = tempLines.get(i) ?? original;
        lines.push({ num: i, text });
    }

    // 维护每个缩进级别的计数器（缩进级别 = 前导空格数）
    const counters = new Map();
    let lastIndent = -1;

    for (const { num, text } of lines) {
        const parsed = parseOrderedListLine(text);
        if (!parsed) {
            // 非有序列表行（理论上不会出现），原样保留
            result.set(num, text);
            lastIndent = -1;
            counters.clear();
            continue;
        }

        const indentLen = parsed.indent.length;

        // 根据缩进变化更新计数器
        if (indentLen > lastIndent) {
            counters.set(indentLen, 1);               // 进入子列表，从1开始
        } else if (indentLen < lastIndent) {
            // 返回上层，清理更深层的计数器
            for (const k of counters.keys()) {
                if (k > indentLen) counters.delete(k);
            }
            const cur = counters.get(indentLen) ?? 0;
            counters.set(indentLen, cur + 1);         // 同层递增
        } else {
            const cur = counters.get(indentLen) ?? 0;
            counters.set(indentLen, cur + 1);         // 同层递增
        }
        lastIndent = indentLen;

        const newNumber = counters.get(indentLen);
        const newLine = `${parsed.indent}${newNumber}. ${parsed.content}`;
        result.set(num, newLine);
    }

    return result;
}

// 智能缩进命令（Tab / Shift-Tab）
export default function smartIndent(view, direction) {
    const { state, dispatch } = view;
    const tempLines = new Map(); // 存放缩进修改后的临时行

    // 1. 为每个选区所在行计算新的缩进
    for (const range of state.selection.ranges) {
        const line = state.doc.lineAt(range.from);
        const parsed = parseOrderedListLine(line.text);
        if (!parsed) {
            // 非有序列表行，使用默认缩进命令
            return direction === 'increase' ? indentMore(view) : indentLess(view);
        }

        let newIndent;
        if (direction === 'increase') {
            newIndent = parsed.indent + ' '.repeat(INDENT_SIZE);
        } else {
            if (parsed.indent.length >= INDENT_SIZE) {
                newIndent = parsed.indent.slice(0, -INDENT_SIZE);
            } else {
                continue; // 无法继续反缩进
            }
        }
        const tempLine = `${newIndent}${parsed.number}. ${parsed.content}`;
        tempLines.set(line.number, tempLine);
    }

    if (tempLines.size === 0) return false;

    // 2. 对每个受影响的列表块进行整体重编号
    const processedBlocks = new Set();
    const finalLines = new Map(); // 所有需要修改的行最终文本

    for (const lineNum of tempLines.keys()) {
        const { start, end } = getOrderedListBlock(state, lineNum);
        const blockKey = `${start}:${end}`;
        if (processedBlocks.has(blockKey)) continue;
        processedBlocks.add(blockKey);

        // 收集当前块内所有被缩进修改的行
        const blockTemp = new Map();
        for (const [ln, text] of tempLines) {
            if (ln >= start && ln <= end) blockTemp.set(ln, text);
        }
        const renumbered = renumberBlock(state, start, end, blockTemp);
        for (const [ln, newText] of renumbered) {
            finalLines.set(ln, newText);
        }
    }

    // 3. 生成变更（与原始文档对比）
    const changes = [];
    for (const [ln, newText] of finalLines) {
        const original = state.doc.line(ln).text;
        if (newText !== original) {
            changes.push({
                from: state.doc.line(ln).from,
                to: state.doc.line(ln).to,
                insert: newText,
            });
        }
    }

    if (changes.length === 0) return false;
    dispatch(state.update({ changes }));
    return true;
}