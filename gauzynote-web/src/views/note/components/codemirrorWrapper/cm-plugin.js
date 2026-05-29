// 创建视图插件来检测标题并应用自定义 classname
import {Decoration, EditorView, ViewPlugin} from "@codemirror/view";
import {syntaxTree} from "@codemirror/language";
import {AnnotationType, StateField} from "@codemirror/state";
import {objDeepClone} from "@/utils/index.js";


// 1. 定义状态字段存储
export const versionField = StateField.define({
    create() {
        return {
            v: 0
        };
    },
    update(prev, transaction) {
        const annotationData = transaction.annotation(versionAnnotation);
        if (annotationData) {
            return annotationData;
        }
        return {
            ...prev
        };
    }
});
// 2. 创建注解类型用于更新
export const versionAnnotation = new AnnotationType();

export function setVersionField(view) {
    const currentState = view.state.field(versionField);
    let v = 1 + currentState.v
    view.dispatch({
        annotations: [versionAnnotation.of({
            v
        })]
    });
}


// 标题装饰器
export const headerClassPlugin = ViewPlugin.fromClass(
    class {
        decorations;

        constructor(view) {
            this.decorations = this.getDecorations(view);
        }

        update(update) {
            // console.log(update)
            // console.log(update.state.field(versionField).v !==
            //     update.startState.field(versionField).v)
            // console.log(update.state.field(versionField).v, update.startState.field(versionField).v)
            if (update.docChanged || update.viewportChanged || update.state.field(versionField).v !== update.startState.field(versionField).v) {
                this.decorations = this.getDecorations(update.view);
            }
        }

        // 检测标题并生成装饰
        getDecorations(view) {
            const decorations = [];
            for (const {from, to} of view.visibleRanges) {
                syntaxTree(view.state).iterate({
                    from, to,
                    enter: (node) => {
                        // console.log(objDeepClone(node, 10))
                        // console.log(node.name + '：'+view.state.sliceDoc(node.from, node.to))
                        if (node.name.startsWith('ATXHeading')) {
                            const level = Number(node.name.replace('ATXHeading', ''))
                            decorations.push(
                                // Decoration.mark({class: `cm-header cm-header-${level}`}).range(node.from, node.to)
                                Decoration.line({class: `cm-header cm-header-${level}`}).range(node.from)
                            )
                            // } else if (node.name === 'URL'){
                            //     decorations.push(
                            //         Decoration.mark({class: `cm-mark cm-url`}).range(node.from, node.to)
                            //     )
                            // }
                        }
                        // 选中颜色有问题，覆盖范围有问题
                        /*else if (node.name.startsWith('Code')){
                            let className = 'code-line'
                            if (node.name === 'CodeMark'){
                                className += ' code-mark'
                            }
                            decorations.push(
                                Decoration.mark({class: className}).range(node.from, node.to)
                            );
                        }*/
                    }
                });
            }
            // console.log(decorations)
            return Decoration.set(decorations);
        }
    },
    {
        decorations: v => v.decorations
    }
);


// 1. 定义状态字段存储当前高亮的标题行号和计时器ID
export const highlightedLineField = StateField.define({
    create() {
        return {
            lineNumber: null,
            timerId: null
        };
    },

    update(prev, transaction) {
        // 清除之前的计时器
        if (prev.timerId) {
            clearTimeout(prev.timerId);
        }

        // 检查是否有高亮行更新的注解
        const highlightData = transaction.annotation(highlightAnnotation);
        if (highlightData) {
            return highlightData;
        } else {
            prev.lineNumber = null
        }

        return {
            ...prev,
            timerId: null // 清除计时器ID
        };
    }
});
// 2. 创建注解类型用于更新高亮行
export const highlightAnnotation = new AnnotationType();

// 高亮行装饰器
export const headingHighlightPlugin = ViewPlugin.fromClass(
    class {
        decorations;

        constructor(view) {
            this.decorations = Decoration.none; // 初始化空装饰
        }

        update(update) {
            if (update.docChanged || update.viewportChanged || update.selectionSet || update.state.field(highlightedLineField).lineNumber !==
                update.startState.field(highlightedLineField).lineNumber) {
                this.decorations = this.getDecorations(update.view);
            }
        }

        getDecorations(view) {
            const {lineNumber} = view.state.field(highlightedLineField);
            if (lineNumber === null) {
                return Decoration.none; // 无高亮
            }

            // 检查行号是否有效
            if (lineNumber < 1 || lineNumber > view.state.doc.lines) {
                return Decoration.none;
            }

            // 创建行装饰
            const line = view.state.doc.line(lineNumber);
            const decoration = Decoration.line({
                class: "heading-highlight"
            }).range(line.from);

            return Decoration.set([decoration]);
        }
    },
    {
        decorations: v => v.decorations
    }
);

// 高亮行控制函数
export function setHighlightedLine(view, lineNumber, duration = 1000) {
    // 清除任何已存在的计时器
    const currentState = view.state.field(highlightedLineField);
    if (currentState.timerId) {
        clearTimeout(currentState.timerId);
    }

    // 设置新的计时器
    const timerId = setTimeout(() => {
        // 1秒后清除高亮
        view.dispatch({
            annotations: [highlightAnnotation.of({
                lineNumber: null,
                timerId: null
            })]
        });
    }, duration);

    // 设置高亮行并存储计时器ID
    view.dispatch({
        annotations: [highlightAnnotation.of({
            lineNumber,
            timerId
        })]
    });

}

/**
 * 内容行追加行号
 */
export const lineAttrsFieldPlugin = ViewPlugin.fromClass(
    class {
        decorations;

        constructor(view) {
            this.decorations = this.getDecorations(view);
        }

        update(update) {
            if (update.docChanged || update.viewportChanged) {
                this.decorations = this.getDecorations(update.view);

            }
        }

        //行数
        getDecorations(view) {
            // console.log(view)
            const decorations = [];
            // 获取可视区域的范围
            const visibleRanges = view.visibleRanges;
            // 遍历每个可视范围
            for (const range of visibleRanges) {
                // 获取可视范围的起始行和结束行
                const startLine = view.state.doc.lineAt(range.from);
                const endLine = view.state.doc.lineAt(range.to);

                // 遍历可视区域内的每一行
                for (let lineNum = startLine.number; lineNum <= endLine.number; lineNum++) {
                    const line = view.state.doc.line(lineNum);
                    // console.log(line)

                    decorations.push(
                        Decoration.line({
                            attributes: {"data-source-line": `${lineNum}`}
                        }).range(line.from)
                    )
                }
            }
            return Decoration.set(decorations);
        }
    }, {
        decorations: v => v.decorations
    }
);

/**
 * 设置行号与内容行水平位置对应
 */
export const syncLineHeightPlugin = ViewPlugin.fromClass(class {
    constructor(view) {
        this.sync(view);
    }

    update(update) {
        // 只有在文档改变、滚动或布局改变时才重新同步
        if (update.docChanged || update.viewportChanged || update.geometryChanged) {
            // 使用 requestAnimationFrame 确保在浏览器渲染帧中执行，避免抖动
            requestAnimationFrame(() => this.sync(update.view));
        }
    }

    sync(view) {
        // 1. 获取所有的槽位容器（通常是 lineNumbers 所在的那个）
        const gutters = view.dom.querySelectorAll('.cm-gutters.cm-gutters-before .cm-gutterElement');
        if (!gutters.length) return;
        const decorations = [];
        // 获取可视区域的范围
        const visibleRanges = view.visibleRanges;
        view.requestMeasure({
            read(e){
                // 遍历每个可视范围
                for (const range of visibleRanges) {
                    // 获取可视范围的起始行和结束行
                    const startLine = view.state.doc.lineAt(range.from);
                    const endLine = view.state.doc.lineAt(range.to);

                    // 遍历可视区域内的每一行
                    for (let lineNum = startLine.number; lineNum <= endLine.number; lineNum++) {
                        const line = view.state.doc.line(lineNum);
                        let lineHeight = "";
                        // try {
                            let targetEl = view.domAtPos(line.from).node
                            while (targetEl.parentElement && targetEl.parentElement !== view.contentDOM) {
                                targetEl = targetEl.parentElement;
                            }
                            lineHeight = window.getComputedStyle(targetEl).lineHeight
                            gutters.forEach(gutter => {
                                if (gutter.textContent == lineNum) {
                                    gutter.style.lineHeight = lineHeight;
                                }
                            });
                        // } catch (e) {}
                    }
                }
            }
        })

        return Decoration.set(decorations);
    }
});
