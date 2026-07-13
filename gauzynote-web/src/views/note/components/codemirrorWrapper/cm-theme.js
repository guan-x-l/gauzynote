// 自定义主题
import {EditorView} from "@codemirror/view";

export const codemirrorTheme = EditorView.theme({
    "&": {
        color: "var(--color-text-1)",
        // backgroundColor: "var(--color-bg-1)"
    },
    ".cm-scroller": {
        overflowX: "hidden",
        fontFamily: 'inherit',
        lineHeight: 'inherit',
    },
    ".cm-content": {
        flex: 1,
        minWidth: 0,
        overflow: "hidden",
        lineHeight: 'inherit',
        fontSize: '16px',
        wordBreak: 'break-all',
        padding: '0',
        paddingBottom: '200px'
    },
    "&.cm-focused": {
        outline: '0'
        // borderLeftColor: "#0e9"
    },
    "&.cm-focused .cm-cursor": {
        // borderLeftColor: "#0e9"
    },
    ".cm-selectionLayer .cm-selectionBackground": {
        background: "rgb(var(--primary-2)) !important"
    },
    // "&.cm-focused .cm-selectionBackground, ::selection": {
    //   backgroundColor: "#074"
    // },
    ".cm-gutters": {
        backgroundColor: "var(--color-bg-1)",
        color: "var(--color-text-3)",
        border: "none",
        paddingRight: '24px',
        fontSize: '12px',
        // lineHeight: '32px'
    },
    // ".cm-gutters .cm-activeLineGutter": {
        // backgroundColor: "var(--color-bg-1)",
    // },
    "& .cm-lineNumbers .cm-gutterElement":{
        // alignContent: 'center',
        // paddingTop: '4px'
    },
    ".cm-cursor": {
        borderLeft: "2px solid var(--color-text-1)",
    },
    ".cm-line": {
        position: "relative"
    },
    ".cm-activeLine": {
        // backgroundColor: "#fff",
        backgroundColor: "transparent",
    },
    // ".cm-header .ͼ7":{
    //     textDecoration: 'unset',
    // },
    ".cm-header .ͼ7.ͼ5":{
        color: 'var(--color-text-3)'
    },
    ".ͼ9.ͼ5":{
        color: 'var(--color-text-3)'
    },
    ".ͼ7": {
        textDecoration: 'none'
    },
    // ͼb: 关键字 #708
    // ".ͼb": {
    //   color: '#d73a49'
    // },
    // ".ͼg": {
    //   color: '#24292e'
    // },
    // ".ͼe": {
    //   color: '#032f62'
    // },
    // ".ͼm": {
    //   color: '#6a737d'
    // },
    ".cm-header.cm-header-1":{
        fontSize: '2em'
    },
    ".cm-header.cm-header-2":{
        fontSize: '1.5em'
    },
    ".cm-header.cm-header-3":{
        fontSize: '1.17em'
    },
    ".cm-header.cm-header-4":{
        fontSize: '1em'
    },
    ".cm-header.cm-header-5":{
        fontSize: '1em'
    },
    ".cm-header.cm-header-6":{
        fontSize: '1em'
    },
    // ── Live Preview 样式 ──
    ".cm-live-preview-heading-1": {
        fontSize: "2em",
        fontWeight: "700"
    },
    ".cm-live-preview-heading-2": {
        fontSize: "1.6em",
        fontWeight: "700"
    },
    ".cm-live-preview-heading-3": {
        fontSize: "1.37em",
        fontWeight: "700"
    },
    ".cm-live-preview-heading-4": {
        fontSize: "1.25em",
        fontWeight: "700"
    },
    ".cm-live-preview-heading-5": {
        fontSize: "1.12em",
        fontWeight: "700"
    },
    ".cm-live-preview-heading-6": {
        fontSize: "1.12em",
        fontWeight: "700",
    },
    ".cm-live-preview-bq-border::before": {
        content: "",
        width: "1px",
        borderInlineStart: "3px solid var(--color-border-2)",
        position: "absolute",
        top: 0,
        bottom: 0,
        textIndent: "0px",
        pointerEvents: "none"
    },
    ".cm-live-preview-strong": {
        fontWeight: "bold",
    },
    ".cm-live-preview-emphasis": {
        fontStyle: "italic",
    },
    ".cm-live-preview-strike": {
        textDecoration: "line-through",
    },
    ".cm-live-preview-inline-code": {
        backgroundColor: "var(--color-fill-2)",
        borderRadius: "3px",
        padding: "0.1em 0.3em",
        fontFamily: "ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, monospace",
        fontSize: "0.9em",
        color: "var(--color-text-2)"
    },
    ".cm-live-preview-link-text, .cm-live-preview-link-text > .ͼ6,.cm-live-preview-link-text > .ͼc, .ͼ6.ͼe, .ͼ6.ͼc": {
        color: "#0969da",
        textDecoration: "none",
    },
    ".ͼ6": {
        textDecoration: "none",
    },
    ".cm-live-preview-link-text:hover, .cm-live-preview-link-text > .ͼ6:hover,.cm-live-preview-link-text > .ͼc:hover, .ͼ6.ͼe:hover, .ͼ6.ͼc:hover": {
        textDecoration: "underline",
    },
    ".cm-live-preview-image": {
        maxWidth: "100%",
        verticalAlign: "middle",
    },
    ".cm-live-preview-code-block": {
        // backgroundColor: "var(--color-fill-2)",
        textIndent: "1em",
    },
    ".cm-live-preview-code-block::before": {
        backgroundColor: "var(--color-fill-2)",
        position: 'absolute',
        top: 0,
        bottom: 0,
        left: '6px',
        right: 0,
        zIndex: '-2'
    },
    ".cm-live-preview-code-block-first::before": {
        borderTopLeftRadius: "4px",
        borderTopRightRadius: "4px",
    },
    ".cm-live-preview-code-block-last::before": {
        borderBottomLeftRadius: "4px",
        borderBottomRightRadius: "4px",
    },
    ".cm-line-hr":{
        display: "flex",
        alignItems: "center",
    },
    ".cm-live-preview-hr": {
        border: "none",
        borderTop: "4px solid var(--color-border-2)",
        margin: "0",
        width: "100%",
    },
    ".cm-live-preview-bullet": {
        fontWeight: "700",
        color: "var(--color-text-2)",
        userSelect: "none"
    },
    ".cm-html-embed": {
        display: "inline",
        userSelect: "none",
    },
    // ".cm-embed-block .cm-widgetBuffer:first-child": {
        // height: "0",
        // display: "none",
    // },
    ".cm-embed-block .cm-widgetBuffer": {
        display: "none",
    },
    ".cm-embed-block": {
        display: "block",
        contain: "paint",
        minHeight: "1.6251em"
    },
    ".cm-embed-block tbody td": {
        padding: "6px 13px",
        border: "1px solid #d1d9e0",
    },
    // ── 表格样式 ──
    ".cm-live-preview-table-line":{
        padding: "1em",
    },
    ".cm-live-preview-table-line .cm-widgetBuffer": {
        display: "none",
    },
    ".cm-live-preview-table-wrapper": {
        position: "relative",
        width: "fit-content",
    },
    ".cm-live-preview-table": {
        borderCollapse: "collapse",
        // width: "fit-content",
        // margin: "8px 0",
        // padding: "1em",
        // fontSize: "1em",
    },
    ".cm-live-preview-table th, .cm-live-preview-table td": {
        border: "1px solid var(--color-border-2)",
        padding: "0",
        textAlign: "left",
        minWidth: "60px",
        outline: "none",
    },
    ".cm-table-cell-content": {
        padding: "6px 12px",
        minHeight: "38px"
    },
    ".cm-live-preview-table th": {
        // backgroundColor: "var(--color-fill-2)",
        fontWeight: "600",
    },
    ".cm-live-preview-task-checkbox": {
        display: "inline-flex",
        alignItems: "center",
        marginRight: "4px",
        verticalAlign: "middle"
    },
    ".cm-live-preview-task-checkbox input": {
        margin: "0",
        cursor: "pointer"
    },
    ".heading-highlight .cm-header":{
        backgroundColor: "var(--color-primary-3)",
        borderRadius: 'var(--border-radius-medium)',
        transition: "background-color .3s ease-out"
    },
    '.cm-live-preview-task-checkbox input[type="checkbox"]':{
        position: "relative",
        top: "-0.1em",
        marginInlineStart: ".8em",
        marginInlineEnd: "-.1em",
        width: '1em',
        height: '1em'
    },
    // ".code-line": {
    //     backgroundColor: "#f6f8fa"
    // }
}, {dark: false})
