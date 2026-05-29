import { HighlightStyle } from "@codemirror/language";
import { tags } from "@lezer/highlight";

/**
 * 将 CodeMirror 代码语言 tag 映射到 highlight.js CSS 类名，
 * 以复用全局已加载的 highlight.js 主题（github.css / github-dark.css）。
 * 未被映射的 tag 回退到 defaultHighlightStyle。
 */
export const codeHighlightStyle = HighlightStyle.define([
  // ── 关键字 ──
  { tag: tags.keyword, class: "hljs-keyword" },
  { tag: tags.controlKeyword, class: "hljs-keyword" },
  { tag: tags.operatorKeyword, class: "hljs-keyword" },
  { tag: tags.definitionKeyword, class: "hljs-keyword" },
  { tag: tags.modifier, class: "hljs-keyword" },
  { tag: tags.moduleKeyword, class: "hljs-keyword" },

  // ── 字符串 ──
  { tag: tags.string, class: "hljs-string" },
  { tag: tags.special(tags.string), class: "hljs-string" },
  { tag: tags.character, class: "hljs-string" },
  { tag: tags.attributeValue, class: "hljs-string" },

  // ── 注释 ──
  { tag: tags.comment, class: "hljs-comment" },
  { tag: tags.lineComment, class: "hljs-comment" },
  { tag: tags.blockComment, class: "hljs-comment" },
  { tag: tags.docComment, class: "hljs-comment" },

  // ── 数字 ──
  { tag: tags.number, class: "hljs-number" },
  { tag: tags.integer, class: "hljs-number" },
  { tag: tags.float, class: "hljs-number" },
  { tag: tags.color, class: "hljs-number" },
  { tag: tags.unit, class: "hljs-number" },

  // ── 类型 ──
  { tag: tags.typeName, class: "hljs-type" },
  { tag: tags.typeOperator, class: "hljs-type" },

  // ── 类名 / 函数名 ──
  { tag: tags.className, class: "hljs-title" },
  { tag: tags.function(tags.variableName), class: "hljs-title" },

  // ── HTML/XML 标签 ──
  { tag: tags.tagName, class: "hljs-name" },
  { tag: tags.attributeName, class: "hljs-attr" },

  // ── 属性 ──
  { tag: tags.propertyName, class: "hljs-property" },

  // ── 运算符 ──
  { tag: tags.operator, class: "hljs-operator" },
  { tag: tags.arithmeticOperator, class: "hljs-operator" },
  { tag: tags.compareOperator, class: "hljs-operator" },
  { tag: tags.logicOperator, class: "hljs-operator" },
  { tag: tags.bitwiseOperator, class: "hljs-operator" },
  { tag: tags.updateOperator, class: "hljs-operator" },
  { tag: tags.controlOperator, class: "hljs-operator" },
  { tag: tags.definitionOperator, class: "hljs-operator" },
  { tag: tags.derefOperator, class: "hljs-operator" },

  // ── 正则 ──
  { tag: tags.regexp, class: "hljs-regexp" },

  // ── 字面量 (true/false/null) ──
  { tag: tags.bool, class: "hljs-literal" },
  { tag: tags.null, class: "hljs-literal" },
  { tag: tags.literal, class: "hljs-literal" },

  // ── 标点 / 括号 ──
  { tag: tags.punctuation, class: "hljs-punctuation" },
  { tag: tags.separator, class: "hljs-punctuation" },
  { tag: tags.bracket, class: "hljs-punctuation" },
  { tag: tags.angleBracket, class: "hljs-punctuation" },
  { tag: tags.squareBracket, class: "hljs-punctuation" },
  { tag: tags.brace, class: "hljs-punctuation" },
  { tag: tags.paren, class: "hljs-punctuation" },
  { tag: tags.contentSeparator, class: "hljs-punctuation" },

  // ── 变量 ──
  { tag: tags.variableName, class: "hljs-variable" },
  { tag: tags.definition(tags.variableName), class: "hljs-variable" },
  { tag: tags.local(tags.variableName), class: "hljs-variable" },
  { tag: tags.special(tags.variableName), class: "hljs-variable" },

  // ── 内置函数/常量 ──
  { tag: tags.standard(tags.variableName), class: "hljs-built_in" },
  { tag: tags.constant(tags.variableName), class: "hljs-built_in" },
  { tag: tags.macroName, class: "hljs-built_in" },

  // ── 元数据 / 预处理器 / 注解 ──
  { tag: tags.meta, class: "hljs-meta" },
  { tag: tags.documentMeta, class: "hljs-meta" },
  { tag: tags.processingInstruction, class: "hljs-meta" },
  { tag: tags.annotation, class: "hljs-meta" },

  // ── 转义序列 ──
  { tag: tags.escape, class: "hljs-subst" },

  // ── 标签 ──
  { tag: tags.labelName, class: "hljs-symbol" },
]);
