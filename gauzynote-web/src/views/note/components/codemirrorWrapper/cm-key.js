import {indentLess, indentMore} from "@codemirror/commands";
import smartIndent from "@/views/note/components/codemirrorWrapper/smartIndent.js";

export function handleHeadingKeyBoardEvent(view, level) {
    const { state } = view;
    const { from: selFrom, to: selTo } = state.selection.main;
    const line = state.doc.lineAt(selFrom);
    const lineStart = line.from;
    const lineEnd = line.to;
    const lineText = state.sliceDoc(lineStart, lineEnd);

    // --------------------------
    // 步骤1：识别多级列表前缀（含缩进）
    // --------------------------
    // 匹配多级列表前缀：
    // - 有序列表：缩进空格 + 数字 + ". "（如 "  3. "）
    // - 无序列表：缩进空格 + "- " 或 "* "（如 "    - "）
    const listPrefixMatch = lineText.match(/^\s*?(?:\d+\. |[-*+] )/);
    const listPrefix = listPrefixMatch ? listPrefixMatch[0] : "";
    const listPrefixLength = listPrefix.length;


    // --------------------------
    // 步骤2：在列表前缀后判断标题标记
    // --------------------------
    // 内容起始位置 = 行首 + 列表前缀长度
    const contentStart = lineStart + listPrefixLength;
    // 提取列表前缀后的内容（可能包含标题标记）
    const contentAfterList = lineText.slice(listPrefixLength);

    // 匹配标题标记（# 后面必须跟空格，且不在列表前缀内）
    const headingMatch = contentAfterList.match(/^#{1,6} /);
    const currentLevel = headingMatch ? headingMatch[0].trim().length : 0;
    const headingMarkerLength = headingMatch ? headingMatch[0].length : 0;

    // --------------------------
    // 步骤3：计算修改范围和选区偏移
    // --------------------------
    let changes, selection;
    const headingMarkerEnd = contentStart + headingMarkerLength; // 标题标记结束位置

    if (currentLevel === level) {
        // 情况1：当前已是目标级别 → 移除标题标记
        changes = {
            from: contentStart,
            to: headingMarkerEnd,
            insert: ""
        };
        const offsetDiff = -headingMarkerLength; // 选区向左偏移（减去标题标记长度）
        selection = {
            anchor: Math.max(lineStart, selFrom + offsetDiff), // 防止选区超出行首
            head: Math.max(lineStart, selTo + offsetDiff)
        };
    } else {
        // 情况2：设置为目标级别标题
        const newHeadingMarker = "#".repeat(level) + " ";
        if (currentLevel === 0) {
            // 子情况2.1：无标题 → 在列表前缀后插入标题标记
            changes = {
                from: contentStart,
                to: contentStart,
                insert: newHeadingMarker
            };
            // 选区向右偏移（增加新标题标记长度）
            selection = {
                anchor: selFrom + newHeadingMarker.length,
                head: selTo + newHeadingMarker.length
            };
        } else {
            // 子情况2.2：替换现有标题标记
            changes = {
                from: contentStart,
                to: headingMarkerEnd,
                insert: newHeadingMarker
            };
            // 选区偏移 = 新标记长度 - 旧标记长度
            const offsetDiff = newHeadingMarker.length - headingMarkerLength;
            selection = {
                anchor: selFrom + offsetDiff,
                head: selTo + offsetDiff
            };
        }
    }

    view.dispatch({ changes, selection });
    return true;
}
export const Mod_s = {
    key: 'Mod-s',
    // run(_view) {
    //     console.log('ctrl+s')
    //     emit('save', view.value.state.doc.toString())
    //     return true
    // },
}
export const Mod_b = {
    key: 'Mod-b',
    run: (view) => {
        const { state } = view;
        const { from: selFrom, to: selTo } = state.selection.main;
        const selectedText = state.sliceDoc(selFrom, selTo);
        const docLength = state.doc.length;

        // 计算包裹状态（合并重复的sliceDoc调用）
        const isSelfWrapped = selectedText.startsWith('**') && selectedText.endsWith('**')
        const hasValidPrefix = selFrom >= 2;
        const hasValidSuffix = selTo + 2 <= docLength;
        const isSurroundWrapped = hasValidPrefix && hasValidSuffix
            && state.sliceDoc(selFrom - 2, selFrom) === '**'
            && state.sliceDoc(selTo, selTo + 2) === '**'
            && selectedText.trim() !== '';

        let changes, selection;

        if (isSelfWrapped) {
            // 移除内部标记
            changes = {
                from: selFrom,
                to: selTo,
                insert: selectedText.slice(2, -2)
            };
            selection = {
                anchor: selFrom,
                head: selTo - 4
            };
        } else if (isSurroundWrapped) {
            // 移除外部标记
            changes = {
                from: selFrom - 2,
                to: selTo + 2,
                insert: selectedText
            };
            selection = {
                anchor: selFrom - 2,
                head: selTo - 2
            };
        } else {
            // 添加外部标记
            changes = {
                from: selFrom,
                to: selTo,
                insert: `**${selectedText}**`
            };
            selection = {
                anchor: selFrom + 2,
                head: selTo + 2
            };
        }

        view.dispatch({ changes, selection });
        return true;
    }
}
export const Mod_Alt_level = Array.from({ length: 6 }, (_, i) => {
    const level = i + 1; // 1-6级标题
    return {
        key: `Mod-Alt-${level}`, // 快捷键：meta+alt+1 到 meta+alt+6
        preventDefault: true,
        priority: 100,
        run: (view) => {
            return handleHeadingKeyBoardEvent(view, level);
        }
    };
})
export const Mod_i = {
    key: "Mod-i",
    run: (view) => {
        const { state } = view;
        const { from: selFrom, to: selTo } = state.selection.main;
        const selectedText = state.sliceDoc(selFrom, selTo);
        const docLength = state.doc.length;

        // 计算包裹状态（合并重复的sliceDoc调用）
        const isSelfWrapped = selectedText.startsWith('*') && selectedText.endsWith('*') && selectedText.length > 2;
        const hasValidPrefix = selFrom >= 1;
        const hasValidSuffix = selTo + 1 <= docLength;
        const isSurroundWrapped = hasValidPrefix && hasValidSuffix
            && state.sliceDoc(selFrom - 1, selFrom) === '*'
            && state.sliceDoc(selTo, selTo + 1) === '*'
            && (state.sliceDoc(selFrom - 2, selFrom) !== '**'
                && state.sliceDoc(selTo, selTo + 2) !== '**')
            || (state.sliceDoc(selFrom - 3, selFrom) === '***'
                && state.sliceDoc(selTo, selTo + 3) === '***')
            && selectedText.trim() !== '';

        let changes, selection;
        console.log(isSelfWrapped, isSurroundWrapped)
        if (isSelfWrapped) {
            // 移除内部标记
            changes = {
                from: selFrom,
                to: selTo,
                insert: selectedText.slice(1, -1)
            };
            selection = {
                anchor: selFrom,
                head: selTo - 2
            };
        } else if (isSurroundWrapped) {
            // 移除外部标记
            changes = {
                from: selFrom - 1,
                to: selTo + 1,
                insert: selectedText
            };
            selection = {
                anchor: selFrom - 1,
                head: selTo - 1
            };
        } else {
            // 添加外部标记
            changes = {
                from: selFrom,
                to: selTo,
                insert: `*${selectedText}*`
            };
            selection = {
                anchor: selFrom + 1,
                head: selTo + 1
            };
        }

        view.dispatch({ changes, selection });
        return true;
    }
}
export const Mod_Shift_x = {
    key: "Mod-Shift-x",
    run: (view) => {
        const { state } = view;
        const { from: selFrom, to: selTo } = state.selection.main;
        const selectedText = state.sliceDoc(selFrom, selTo);
        const docLength = state.doc.length;

        // 计算包裹状态（合并重复的sliceDoc调用）
        const isSelfWrapped = selectedText.startsWith('~~') && selectedText.endsWith('~~') && selectedText.length > 4;
        const hasValidPrefix = selFrom >= 2;
        const hasValidSuffix = selTo + 2 <= docLength;
        const isSurroundWrapped = hasValidPrefix && hasValidSuffix
            && state.sliceDoc(selFrom - 2, selFrom) === '~~'
            && state.sliceDoc(selTo, selTo + 2) === '~~'
            && selectedText.trim() !== '';

        let changes, selection;

        if (isSelfWrapped) {
            // 移除内部标记
            changes = {
                from: selFrom,
                to: selTo,
                insert: selectedText.slice(2, -2)
            };
            selection = {
                anchor: selFrom,
                head: selTo - 4
            };
        } else if (isSurroundWrapped) {
            // 移除外部标记
            changes = {
                from: selFrom - 2,
                to: selTo + 2,
                insert: selectedText
            };
            selection = {
                anchor: selFrom - 2,
                head: selTo - 2
            };
        } else {
            // 添加外部标记
            changes = {
                from: selFrom,
                to: selTo,
                insert: `~~${selectedText}~~`
            };
            selection = {
                anchor: selFrom + 2,
                head: selTo + 2
            };
        }

        view.dispatch({ changes, selection });
        return true;
    }
}
export const Mod_Shift_greaterThan = {
    key: "Mod-Shift-.",
    run: (view) => {
        const { state } = view;
        const { from: selFrom, to: selTo } = state.selection.main;
        const line = state.doc.lineAt(selFrom);
        const lineStart = line.from;
        const lineEnd = line.to;
        const lineText = state.sliceDoc(lineStart, lineEnd);

        // --------------------------
        // 步骤1：识别多级列表前缀（含缩进）
        // --------------------------
        // 匹配多级列表前缀：
        // - 有序列表：缩进空格 + 数字 + ". "（如 "  3. "）
        // - 无序列表：缩进空格 + "- " 或 "* "（如 "    - "）
        const listPrefixMatch = lineText.match(/^\s*?(?:\d+\. |[-*+] )/);
        const listPrefix = listPrefixMatch ? listPrefixMatch[0] : "";
        const listPrefixLength = listPrefix.length;


        // --------------------------
        // 步骤2：在列表前缀后判断标题标记
        // --------------------------
        // 内容起始位置 = 行首 + 列表前缀长度
        const contentStart = lineStart + listPrefixLength;
        // 提取列表前缀后的内容（可能包含标题标记）
        const contentAfterList = lineText.slice(listPrefixLength);

        // 匹配标题标记（# 后面必须跟空格，且不在列表前缀内）
        const headingMatch = contentAfterList.match(/^> /);
        const currentLevel = !!headingMatch
        const headingMarkerLength = headingMatch ? headingMatch[0].length : 0;

        // --------------------------
        // 步骤3：计算修改范围和选区偏移
        // --------------------------
        let changes, selection;
        const headingMarkerEnd = contentStart + headingMarkerLength; // 标题标记结束位置

        if (headingMatch) {
            // 情况1：当前已是引用 → 移除引用标记
            changes = {
                from: contentStart,
                to: headingMarkerEnd,
                insert: ""
            };
            const offsetDiff = -headingMarkerLength; // 选区向左偏移（减去标题标记长度）
            selection = {
                anchor: Math.max(lineStart, selFrom + offsetDiff), // 防止选区超出行首
                head: Math.max(lineStart, selTo + offsetDiff)
            };
        } else {
            // 情况2：设置为引用
            const newHeadingMarker = "> ";
            // 子情况2.1：无标题 → 在列表前缀后插入标题标记
            changes = {
                from: contentStart,
                to: contentStart,
                insert: newHeadingMarker
            };
            // 选区向右偏移（增加新标题标记长度）
            selection = {
                anchor: selFrom + newHeadingMarker.length,
                head: selTo + newHeadingMarker.length
            };
        }

        view.dispatch({ changes, selection });
        return true;
    },
}
export const Mod_k = {
    key: "Mod-k",
    run: (view) => {
        console.log('Mod-k')
        return true
    }
}
export const Mod_Shift_c = {
    key: "Mod-Shift-c",
    stopPropagation: true,
    preventDefault: true,
    run: (view) => {
        const { state } = view;
        const { from: selFrom, to: selTo } = state.selection.main;
        const selectedText = state.sliceDoc(selFrom, selTo);
        const docLength = state.doc.length;

        // 计算包裹状态（合并重复的sliceDoc调用）
        const isSelfWrapped = selectedText.startsWith('`') && selectedText.endsWith('`') && selectedText.length > 2;
        const hasValidPrefix = selFrom >= 1;
        const hasValidSuffix = selTo + 1 <= docLength;
        const isSurroundWrapped = hasValidPrefix && hasValidSuffix
            && state.sliceDoc(selFrom - 1, selFrom) === '`'
            && state.sliceDoc(selTo, selTo + 1) === '`'
            && selectedText.trim() !== '';

        let changes, selection;
        console.log(isSelfWrapped, isSurroundWrapped)
        if (isSelfWrapped) {
            // 移除内部标记
            changes = {
                from: selFrom,
                to: selTo,
                insert: selectedText.slice(1, -1)
            };
            selection = {
                anchor: selFrom,
                head: selTo - 2
            };
        } else if (isSurroundWrapped) {
            // 移除外部标记
            changes = {
                from: selFrom - 1,
                to: selTo + 1,
                insert: selectedText
            };
            selection = {
                anchor: selFrom - 1,
                head: selTo - 1
            };
        } else {
            // 添加外部标记
            changes = {
                from: selFrom,
                to: selTo,
                insert: `\`${selectedText}\``
            };
            selection = {
                anchor: selFrom + 1,
                head: selTo + 1
            };
        }

        view.dispatch({ changes, selection });
        return true;
    }
}
export const Tab = {
    key: 'Tab',
    run: (_view) => {
        console.log(_view)
        // 尝试自定义的缩进和重编号逻辑
        if (smartIndent(_view, 'increase')) {
            return true; // 已处理，事件结束
        }
        // 否则，执行默认的 indentMore 行为
        return indentMore(_view);
    }
}
export const Shift_Tab = {
    key: 'Shift-Tab',
    run: (_view) => {
        console.log(_view)
        // 尝试自定义的反缩进和重编号逻辑
        if (smartIndent(_view, 'decrease')) {
            return true;
        }
        // 否则，执行默认的 indentLess 行为
        return indentLess(_view);
    },
    shift: true, // 明确标记 Shift 键
}

export const customizeKeymap = [
    Mod_b,
    Mod_Alt_level,
    Mod_i,
    Mod_Shift_x,
    Mod_Shift_greaterThan,
    Mod_k,
    Mod_Shift_c,
    Tab,
    Shift_Tab,
]