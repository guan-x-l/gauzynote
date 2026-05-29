import MarkdownIt from 'markdown-it'
import lists from 'markdown-it-task-lists'
import anchor from 'markdown-it-anchor'
import footnote from 'markdown-it-footnote'
import abbr from 'markdown-it-abbr'
// import container from 'markdown-it-container'
import deflist from 'markdown-it-deflist'
// import emoji from 'markdown-it-emoji'
import {full as emoji} from 'markdown-it-emoji'
import ins from 'markdown-it-ins'
import mark from 'markdown-it-mark'
import sub from 'markdown-it-sub'
import sup from 'markdown-it-sup'
import mila from 'markdown-it-link-attributes'
import githubAlertsPlugin from "./markdown-it-github-alerts.js";

import hljs from 'highlight.js/lib/core'; // 注意：只导入核心库
// 按需导入需要的语言（只保留项目中实际使用的）
import javascript from 'highlight.js/lib/languages/javascript';
import html from 'highlight.js/lib/languages/xml'; // html 包含在 xml 中
import css from 'highlight.js/lib/languages/css';
import json from 'highlight.js/lib/languages/json';
import python from 'highlight.js/lib/languages/python';
import java from 'highlight.js/lib/languages/java';
import sql from 'highlight.js/lib/languages/sql';
import {createUUID, getAttrsStr} from "@/utils/index.js";

// 注册语言
hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('html', html);
hljs.registerLanguage('css', css);
hljs.registerLanguage('json', json);
hljs.registerLanguage('python', python);
hljs.registerLanguage('java', java);
hljs.registerLanguage('sql', sql);

let markdownIt = new MarkdownIt({
    html: true,
    breaks: true,  // 这个选项会将换行符转换为<br>
    linkify: true, // 是否自动识别链接
    typographer: true, // 是否启用排版优化
    // xhtmlOut: true,
    langPrefix: "locales-",
    // highlight: function(str, locales) {
    //     // 这里可以保留语法高亮逻辑（如使用 prism/highlight.js）
    //     // 示例：直接返回处理后的代码（实际项目需替换为高亮逻辑）
    //     return locales ? `<pre class="language-${locales}"><code>${str}</code></pre>` : str;
    // }
    // quotes: "“”‘’",
    // highlight: function (str, locales) {
    //     console.log(str, locales)
    //     if (locales && hljs.getLanguage(locales)) {
    //         try {
    //           return '<pre><code class="hljs text-break">' +
    //             hljs.highlight(str, { language: locales, ignoreIllegals: true }).value +
    //             '</code></pre>';
    //         } catch (__) {}
    //     }
    //     // 未指定语言或不支持的语言，使用自动检测
    //     return `<pre class="hljs"><code>${hljs.highlightAuto(str).value}</code></pre>`;
    // },
});
let mhn = 0
markdownIt.use(lists)
    .use(anchor, {
        // level: [1, 2, 3, 4, 5, 6], // 为哪些级别标题添加id
        // permalink: true, // 启用链接
        // permalinkClass: 'direct-link', // 设置链接的class

        // slugify: function(str) {
        //   return 'mhn' + ++mhn
        // }
        slugify: (str) => {
            return createUUID()
        },
    })
    // .use(anchor)
    .use(footnote)
    .use(abbr)
    // .use(container)
    // .use(container, "hljs-left")
    // .use(container, "hljs-center")
    // .use(container, "hljs-right")
    .use(deflist)
    .use(emoji)
    .use(ins)
    .use(mark)
    .use(sub)
    .use(sup)
    .use(mila, {
        attrs: {
            target: "_blank",
        },
    })
    .use(githubAlertsPlugin)
    .use((md) => {
        // // 目的：渲染行数
        md.renderer.render = function (tokens, options, env) {
            const rules = this.rules;
            const tokensLen = tokens.length
            let result = ''
            for (let i = 0; i < tokensLen; i++) {
                const type = tokens[i].type;
                if (tokens[i].map !== null && tokens[i].tag !== 'ul') {
                    tokens[i].attrPush(['data-source-line', (tokens[i].map[0] + 1).toString()])
                    tokens[i].meta = {
                        ...tokens[i].meta,
                        dataSourceLine: (tokens[i].map[0] + 1)
                    }
                }
                if (type === 'inline') {
                    result += this.renderInline(tokens[i].children, options, env);
                } else if (typeof rules[type] !== 'undefined') {
                    result += rules[tokens[i].type](tokens, i, options, env, this);
                } else {
                    result += this.renderToken(tokens, i, options, env);
                }
            }

            return result;
        };
        /*md.renderer.rules.fence = function (tokens, idx) {
            const {info, content, attrs} = tokens[idx]
            const attrsStr = getAttrsStr(attrs)
            if (info && hljs.getLanguage(info)) {
                try {
                    return `<pre ${attrsStr}><code class="hljs text-break">` +
                        hljs.highlight(content, { language: info, ignoreIllegals: true }).value +
                        '</code></pre>';
                } catch (__) {}
            }
            // 未指定语言或不支持的语言，使用自动检测
            return `<pre ${attrsStr}><code class="hljs text-break">${hljs.highlightAuto(content).value}</code></pre>`;
        }*/
        // 自定义代码块渲染规则
        md.renderer.rules.fence = function (tokens, idx) {
            const {info, content, attrs, meta} = tokens[idx]
            const attrsStr = getAttrsStr(attrs)
            try {
                let value;
                let className = '';
                if (info && hljs.getLanguage(info)) {
                    value = hljs.highlight(content, {language: info, ignoreIllegals: true}).value
                    className = 'language-' + hljs.getLanguage(info).name.toLowerCase()
                } else {
                    // 未指定语言或不支持的语言，使用自动检测
                    value = hljs.highlightAuto(content).value
                }
                const codeStr = value.split('\n').map((item, i) => {
                    return `<span class="line" data-source-line="${meta.dataSourceLine + i + 1}">${item}</span>`
                }).join('\n')
                return `<pre ${attrsStr} class="${className}"><code class="hljs text-break">${codeStr}</code></pre>`;
            } catch (__) {}
        }
        // 自定义图片渲染规则
        md.renderer.rules.image = function(tokens, idx, options, env, self) {
            const token = tokens[idx];
            const loadingIndex = token.attrIndex('loading');
            if (loadingIndex < 0) {
                token.attrs.push(['loading', 'lazy']);
            }
            // 给img添加onerror
            // const errorIndex = token.attrIndex('onerror');
            // if (errorIndex < 0) {
            //     // 图片加载失败的兜底逻辑，写在这里即可，原生属性，无需编译，立即生效
            //     token.attrs.push([
            //         'onerror',
            //         "if (this.alt==='') {this.removeAttribute('alt')}this.classList.add('img-error')"
            //     ]);
            // }
            return self.renderToken(tokens, idx, options);
        };
    })


export default markdownIt
