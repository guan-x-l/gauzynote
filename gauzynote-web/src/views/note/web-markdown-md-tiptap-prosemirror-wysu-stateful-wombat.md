# 实时阅览 Markdown 编辑器 — 详细设计文档

## 目标

基于 CodeMirror 6 实现 Obsidian 风格的「实时阅览」（Live Preview）Markdown 编辑器。

**核心理念**：数据层始终是 Markdown 原文；渲染层通过 CodeMirror Decoration 叠加视觉效果，语法标记默认不可见，光标/选区进入和处于标记首尾时恢复。

---

## 一、标记分类与显示策略

语法标记按语义分为三类，处理策略不同：

| 分类 | 包含语法 | 默认状态 | 光标/选区进入和处于标记首尾时 |
|------|---------|---------|---------------|
| **格式化标记** | `**` `*` `~~` `` ` `` `#` | 完全不可见（replace widget） | 完全可见 |
| **结构标记** | `- ` `1. ` | 替换为ul和ol列表 | 始终为html |
| **引用标记** | `> ` | 替换为`<blockquote></blockquote>` | 恢复原文 |
| **链接语法标记** | `[]()` URL LinkTitle | 完全不可见 | 完全可见 |
| **代码块围栏** | ` ``` ` | 完全不可见 | 完全可见 |
| **分割线** | `---` `***` `___` | 替换分割线 `<hr/>` widget | 恢复原文 |
| **任务复选框** | `- [ ] ` `- [x] ` | 替换为 `<input type="checkbox">` | 处于标记首尾时恢复原文，其余始终为checkbox |

---

## 二、各语法元素详细规则

### 2 标记的首尾示意
**已粗体示意，用`_`代表光标**

| 模拟示意                | 是否显示标记 |
| ------------------- | :----: |
| `**bl_od**`         |   是    |
| `*_*blod**`         |   是    |
| `**blod*_*`         |   是    |
| `text**_blod**text` |   是    |
| `text**blod**_text` |   是    |
| `text_**blod**text` |   是    |
| `tex_t**blod**text` |   否    |
| `text**blod**te_xt` |   否    |

### 2.1 标题 `# H1` ~ `###### H6`
**语法规则:**
要创建标题，请在单词或短语前面添加井号 (`#`) 。`#` 的数量代表了标题的级别。例如，添加三个 `#` 表示创建一个三级标题 (`<h3>`) (例如：`### My Header`)。

| Markdown语法               | HTML                       |
| ------------------------ | -------------------------- |
| `# Heading level 1`      | `<h1>Heading level 1</h1>` |
| `## Heading level 2`     | `<h2>Heading level 2</h2>` |
| `### Heading level 3`    | `<h3>Heading level 3</h3>` |
| `#### Heading level 4`   | `<h4>Heading level 4</h4>` |
| `##### Heading level 5`  | `<h5>Heading level 5</h5>` |
| `###### Heading level 6` | `<h6>Heading level 6</h6>` |
**可选语法:**
还可以在文本下方添加任意数量的 == 号来标识一级标题，或者 -- 号来标识二级标题。

| Markdown语法                          | HTML                       |
| ----------------------------------- | -------------------------- |
| `Heading level 1   ===============` | `<h1>Heading level 1</h1>` |
| `Heading level 2   ---------------` | `<h2>Heading level 2</h2>` |
**最佳实践:**
不同的 Markdown 应用程序处理 `#` 和标题之间的空格方式并不一致。为了兼容考虑，请用一个空格在 `#` 和标题之间进行分隔。

| ✅  Do this           | ❌  Don't do this    |
| -------------------- | ------------------- |
| `# Here's a Heading` | `#Here's a Heading` |

**展示规则**：
- `#` 标记完全不可见且没有宽度
- 标题文本按级别应用字号+加粗：H1=2em, H2=1.6em, H3=1.37em, H4=1.25em,h5-H6=1.12em
- 实现方式：`Decoration.line({ class: 'cm-live-preview-heading-N' })` + `Decoration.replace(InvisibleWidget)` 隐藏 HeaderMark

**编辑规则**：
- 光标在标题行任意位置时，`#` 标记恢复可见
- 用户可正常编辑标题文本和 `#` 数量
- 删除所有 `#` 后，该行自动变为普通段落（CodeMirror 原生行为）
- 在非标题行首输入 `# ` 后，自动变为标题（CodeMirror 原生行为）

**标记显隐规则**：
- 默认：`#` 隐藏
- 光标在标题行内任意位置：`#` 显示
- 选区与标题行有交集：`#` 显示
- 选区离开标题行后：`#` 重新隐藏

---

### 2.2 粗体 `**text**` / `__text__`
**语法规则:**
要加粗文本，请在单词或短语的前后各添加两个星号（`*`）或下划线（`_`）。如需加粗一个单词或短语的中间部分用以表示强调的话，请在要加粗部分的两侧各添加两个星号（`*`）。

| Markdown语法                   | HTML                                      |
| ---------------------------- | ----------------------------------------- |
| `I just love **bold text**.` | `I just love <strong>bold text</strong>.` |
| `I just love __bold text__.` | `I just love <strong>bold text</strong>.` |
| `Love**is**bold`             | `Love<strong>is</strong>bold`             |

**最佳实践**
Markdown 应用程序在如何处理单词或短语中间的下划线上并不一致。为兼容考虑，在单词或短语中间部分加粗的话，请使用星号（`*`）。

| ✅  Do this       | ❌  Don't do this |
| ---------------- | ---------------- |
| `Love**is**bold` | `Love__is__bold` |

**展示规则**：
- `**` / `__` 标记完全不可见
- 中间文本以粗体字重呈现
- 实现方式：`Decoration.replace(InvisibleWidget)` 隐藏 EmphasisMark + `Decoration.mark({ class: 'bold' })` 或依赖 CodeMirror 原生粗体语法高亮类

**编辑规则**：
- 光标在粗体区间内和标记的首尾时，`**` 恢复可见
- 选区与粗体区间有交集时，`**` 恢复可见
- 用户可直接编辑标记字符
- 删除任一侧 `**` 后，粗体效果消失（CodeMirror 原生行为，装饰跟随）

**标记显隐规则**：
- 默认：`**` 隐藏
- 光标在粗体区间内和标记的首尾时：`**` 显示
- 选区与粗体区间有交集（包括全选）：`**` 显示

---

### 2.3 斜体 `*text*` / `_text_`
**语法规则:**
要用斜体显示文本，请在单词或短语前后添加一个星号（`*`）或下划线（`_`）。要斜体突出单词的中间部分，请在字母前后各添加一个星号，中间不要带空格。

| Markdown语法                             | HTML                                          |
| -------------------------------------- | --------------------------------------------- |
| `Italicized text is the *cat's meow*.` | `Italicized text is the <em>cat's meow</em>.` |
| `Italicized text is the _cat's meow_.` | `Italicized text is the <em>cat's meow</em>.` |
| `A*cat*meow`                           | `A<em>cat</em>meow`                           |

**最佳实践**
要同时用粗体和斜体突出显示文本，请在单词或短语的前后各添加三个星号或下划线。要加粗并用斜体显示单词或短语的中间部分，请在要突出显示的部分前后各添加三个星号，中间不要带空格。

| ✅  Do this   | ❌  Don't do this |
| ------------ | ---------------- |
| `A*cat*meow` | `A_cat_meow`     |

**展示规则**：
- `*` / `_` 标记完全不可见
- 中间文本以斜体呈现（依赖 CodeMirror 原生斜体语法高亮类）

**编辑规则**：同粗体

**标记显隐规则**：同粗体

---
###  2.4 粗体（Bold）和斜体（Italic）
**语法规则:**
要同时用粗体和斜体突出显示文本，请在单词或短语的前后各添加三个星号或下划线。要加粗并用斜体显示单词或短语的中间部分，请在要突出显示的部分前后各添加三个星号，中间不要带空格。

| Markdown语法                                | HTML                                                          |
| ----------------------------------------- | ------------------------------------------------------------- |
| `This text is ***really important***.`    | `This text is <strong><em>really important</em></strong>.`    |
| `This text is ___really important___.`    | `This text is <strong><em>really important</em></strong>.`    |
| `This text is __*really important*__.`    | `This text is <strong><em>really important</em></strong>.`    |
| `This text is **_really important_**.`    | `This text is <strong><em>really important</em></strong>.`    |
| `This is really***very***important text.` | `This is really<strong><em>very</em></strong>important text.` |
**最佳实践:**
Markdown 应用程序在处理单词或短语中间添加的下划线上并不一致。为了实现兼容性，请使用星号将单词或短语的中间部分加粗并以斜体显示，以示重要。

| ✅  Do this                                | ❌  Don't do this                          |
| ----------------------------------------- | ----------------------------------------- |
| `This is really***very***important text.` | `This is really___very___important text.` |
**展示规则**：
- `*` / `_` 标记完全不可见

**编辑规则**：同粗体

**标记显隐规则**：同粗体

### 2.5 删除线 `~~text~~`
**语法规则:**
您可以通过在单词中心放置一条水平线来删除单词。此功能使您可以指示某些单词是一个错误，要从文档中删除。若要删除单词，请在单词前后使用两个波浪号`~~`。

| Markdown语法                 | HTML                              |
| -------------------------- | --------------------------------- |
| `~~世界是平坦的。~~ 我们现在知道世界是圆的。` | `<del>世界是平坦的。</del> 我们现在知道世界是圆的。` |

**展示规则**：
- `~~` 标记完全不可见
- 中间文本应用删除线样式 `text-decoration: line-through`
- 实现方式：`Decoration.replace(InvisibleWidget)` + `Decoration.mark({ class: 'cm-live-preview-strike' })`

**编辑规则**：同粗体

**标记显隐规则**：同粗体

---

### 2.6 行内代码 `` `code` ``
**语法规则:**
要将单词或短语表示为代码，请将其包裹在反引号 (`` ` ``) 中。

| Markdown语法                              | HTML                                             |
| --------------------------------------- | ------------------------------------------------ |
| ``At the command prompt, type `nano`.`` | `At the command prompt, type <code>nano</code>.` |
**转义反引号**
如果你要表示为代码的单词或短语中包含一个或多个反引号，则可以通过将单词或短语包裹在双反引号(` `` `)中。

| Markdown语法                                    | HTML                                               |
| --------------------------------------------- | -------------------------------------------------- |
| ``` ``Use `code` in your Markdown file.`` ``` | ``<code>Use `code` in your Markdown file.</code>`` |


**展示规则**：
- 反引号标记完全不可见
- 代码内容应用代码背景色（`background: var(--color-fill-2)`, 等宽字体）
- 实现方式：`Decoration.replace(InvisibleWidget)` 隐藏 CodeMark + `Decoration.mark({ class: 'cm-live-preview-inline-code' })`

**编辑规则**：
- 光标在行内代码区间内和标记的首尾时，反引号恢复可见
- 选区与行内代码区间有交集时，反引号恢复可见

**标记显隐规则**：同斜体

---

### 2.7 链接 `[text](url)` / `[text](url "title")`
**语法规则:**
链接文本放在中括号内，链接地址放在后面的括号中，链接title可选。
超链接Markdown语法代码：`[超链接显示名](超链接地址 "超链接title")`
对应的HTML代码：`<a href="超链接地址" title="超链接title">超链接显示名</a>`

| Markdown语法                                      | HTML                                                      |
| ----------------------------------------------- | --------------------------------------------------------- |
| `这是一个链接 [Markdown语法](https://markdown.com.cn)。` | `这是一个链接 <a href="https://markdown.com.cn">Markdown语法</a>` |

**给链接增加 Title:**
链接title是当鼠标悬停在链接上时会出现的文字，这个title是可选的，它放在圆括号中链接地址后面，跟链接地址之间以空格分隔。

| Markdown语法                                                      | HTML                                                                            |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| `这是一个链接 [Markdown语法](https://markdown.com.cn "最好的markdown教程")。` | `这是一个链接 <a href="https://markdown.com.cn" title="最好的markdown教程">Markdown语法</a>` |

**网址和Email地址:**
使用尖括号可以很方便地把URL或者email地址变成可点击的链接。

| Markdown语法                                    | HTML                                                                                                                  |
| --------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| `<https://markdown.com.cn><fake@example.com>` | `<a href="https://markdown.com.cn">https://markdown.com.cn</a><a href="mailto:fake@example.com">fake@example.com</a>` |

**带格式化的链接:**
强调链接, 在链接语法前后增加星号。 要将链接表示为代码，请在方括号中添加反引号。
```
I love supporting the **[EFF](https://eff.org)**.
This is the *[Markdown Guide](https://www.markdownguide.org)*.
See the section on [`code`](#code).
```
渲染效果如下：
`I love supporting the <strong><a href="https://eff.org">EFF</a></strong>.`
`This is the <em><a href="https://www.markdownguide.org">Markdown Guide</a></em>.`
`See the section on <a href="#code"><code>code</code></a>.`

**引用类型链接:**
引用样式链接是一种特殊的链接，它使URL在Markdown中更易于显示和阅读。参考样式链接分为两部分：与文本保持内联的部分以及存储在文件中其他位置的部分，以使文本易于阅读。

**链接的第一部分格式**
引用类型的链接的第一部分使用两组括号进行格式设置。第一组方括号包围应显示为链接的文本。第二组括号显示了一个标签，该标签用于指向您存储在文档其他位置的链接。

尽管不是必需的，可以在第一组和第二组括号之间包含一个空格。第二组括号中的标签不区分大小写，可以包含字母，数字，空格或标点符号。

以下示例格式对于链接的第一部分效果相同：
- `[hobbit-hole][1]`
- `[hobbit-hole] [1]`

**链接的第二部分格式:**
引用类型链接的第二部分使用以下属性设置格式：

1. 放在括号中的标签，其后紧跟一个冒号和至少一个空格（例如`[label]:`）。
2. 链接的URL，可以选择将其括在尖括号中。
3. 链接的可选标题，可以将其括在双引号，单引号或括号中。

以下示例格式对于链接的第二部分效果相同：

- `[1]: https://en.wikipedia.org/wiki/Hobbit#Lifestyle`
- `[1]: https://en.wikipedia.org/wiki/Hobbit#Lifestyle "Hobbit lifestyles"`
- `[1]: https://en.wikipedia.org/wiki/Hobbit#Lifestyle 'Hobbit lifestyles'`
- `[1]: https://en.wikipedia.org/wiki/Hobbit#Lifestyle (Hobbit lifestyles)`
- `[1]: <https://en.wikipedia.org/wiki/Hobbit#Lifestyle> "Hobbit lifestyles"`
- `[1]: <https://en.wikipedia.org/wiki/Hobbit#Lifestyle> 'Hobbit lifestyles'`
- `[1]: <https://en.wikipedia.org/wiki/Hobbit#Lifestyle> (Hobbit lifestyles)`

可以将链接的第二部分放在Markdown文档中的任何位置。有些人将它们放在出现的段落之后，有些人则将它们放在文档的末尾（例如尾注或脚注）。

**最佳实践:**
不同的 Markdown 应用程序处理URL中间的空格方式不一样。为了兼容性，请尽量使用%20代替空格。

| ✅  Do this                                          | ❌  Don't do this                                |
| --------------------------------------------------- | ----------------------------------------------- |
| `[link](https://www.example.com/my%20great%20page)` | `[link](https://www.example.com/my great page)` |

**展示规则**：
- 所有语法字符 `[` `]` `(` `)` 和 URL、title文本 完全不可见
- 链接文本显示链接色文本（`color: var(--color-primary-5)`） 
- 链接文本鼠标悬浮显示下划线（`text-decoration: underline`）
- 实现方式：`Decoration.replace(InvisibleWidget)` 隐藏 LinkMark/URL/LinkTitle 设置title(如有的情况)到标签中

**编辑规则**：
- 光标在链接区间内和所有语法字符的首尾时，所有语法字符恢复可见
- 选区与链接区间有交集时，恢复可见
- Ctrl/Cmd+点击链接文本 → 打开 URL

**标记显隐规则**：
- 默认：`[]()` 及 URL、title 隐藏
- 光标/选区在链接区间内和所有语法字符的首尾时：全部显示

---

### 2.8 图片 `![alt](url)` / `![alt](url "title")`
**语法规则:**
要添加图像，请使用感叹号 (`!`), 然后在方括号增加替代文本，图片链接放在圆括号里，括号里的链接后可以增加一个可选的图片标题文本。

插入图片Markdown语法代码：`![图片alt](图片链接 "图片title")`。

对应的HTML代码：`<img src="图片链接" alt="图片alt" title="图片title">`

```
![这是图片](/assets/img/philly-magic-garden.jpg "Magic Gardens")
```

渲染效果如下：
`<img alt="这是图片" src="../images/philly-magic-garden.jpg" title="Magic Gardens">`

**链接图片:**
给图片增加链接，请将图像的Markdown 括在方括号中，然后将链接添加在圆括号中。

```
[![沙漠中的岩石图片](/assets/img/shiprock.jpg "Shiprock")](https://markdown.com.cn)
```

渲染效果如下：
`<a href="https://markdown.com.cn"><img alt="沙漠中的岩石图片" src="../images/shiprock.jpg" title="Shiprock"></a>`


**展示规则**：
- 源码隐藏
- 实现方式：需要渲染图片img标签，图片始终显示，当点击图片或者在选区范围以及光标位于源码首尾时，在图片的前面显示源码。图片和源码都是行内元素

**编辑规则**：同链接

**标记显隐规则**：同链接

---

### 2.9 块引用 `> text`
**语法规则:**
要创建块引用，请在段落前添加一个 `>` 符号。

```
> Dorothy followed her through many of the beautiful rooms in her castle.
```

渲染效果如下所示：
```
<blockquote>
<p>Dorothy followed her through many of the beautiful rooms in her castle.</p>
</blockquote>
```
**多个段落的块引用:**
块引用可以包含多个段落。为段落之间的空白行添加一个 `>` 符号。
```
> Dorothy followed her through many of the beautiful rooms in her castle.
>
> The Witch bade her clean the pots and kettles and sweep the floor and keep the fire fed with wood.
```
渲染效果如下：
```
<blockquote>
<p>Dorothy followed her through many of the beautiful rooms in her castle.</p>
<p>The Witch bade her clean the pots and kettles and sweep the floor and keep the fire fed with wood.</p>
</blockquote>
```
**嵌套块引用:**
块引用可以嵌套。在要嵌套的段落前添加一个 `>>` 符号。
```
> Dorothy followed her through many of the beautiful rooms in her castle.
>
>> The Witch bade her clean the pots and kettles and sweep the floor and keep the fire fed with wood.
```
渲染效果如下：
```
<blockquote>
<p>Dorothy followed her through many of the beautiful rooms in her castle.</p>
<blockquote>
<p>The Witch bade her clean the pots and kettles and sweep the floor and keep the fire fed with wood.</p>
</blockquote>
</blockquote>
```
**带有其它元素的块引用:**
块引用可以包含其他 Markdown 格式的元素。并非所有元素都可以使用，你需要进行实验以查看哪些元素有效。

```
> #### The quarterly results look great!
>
> - Revenue was off the chart.
> - Profits were higher than ever.
>
>  *Everything* is going according to **plan**.
```

渲染效果如下：
```
<blockquote>
<p></p><h4>The quarterly results look great!</h4><p></p>
<ul>
<li>Revenue was off the chart.</li>
<li>Profits were higher than ever.</li>
</ul>
<p><em>Everything</em> is going according to <strong>plan</strong>.</p>
</blockquote>
```


**展示规则**：
- 语法字符 `>` 完全不可见
- 实现方式：`Decoration.mark({ class: 'cm-live-preview-marker' })` 而非 replace

**编辑规则**：
- 光标在引用行：`>` 完全可见
- 输入 `>` 在行首自动创建引用（CodeMirror 原生 + autoBlockquote 快捷键）

**标记显隐规则**：
- 默认：`>` 隐藏
- 光标/选区在引用行：`>` 显示

---

### 2.10 无序列表 `- item` / `* item` / `+ item`

**展示规则**：
- 列表标记 `- ` / `* ` / `+ ` 完全不可见
- 渲染规则：源码标记不显示，但是需要渲染ul li的marker的样式

**编辑规则**：
- 输入 `- ` 在行首自动渲染为列表
- 回车自动延续`下一项（CodeMirror 原生行为）
- Tab 缩进嵌套列表（CodeMirror 原生行为）

**标记显隐规则**：
- 默认：标记隐藏
- 选区在列表区间内，或者光标在语法字符的首尾时`- ` / `* ` / `+ `：显示为标记源码

---

### 2.11 有序列表 `1. item`

**展示规则**：
- 列表标记 `1. ` / `2. ` 始终可见，不做任何装饰

**编辑规则**：同无序列表

**标记显隐规则**：
- 始终可见

---

### 2.12 任务列表 `- [ ] item` / `- [x] item`

**展示规则**：
- `- ` 列表标记始终可见
- `[ ]` / `[x]` 渲染为实际 `<input type="checkbox">`（已勾选/未勾选）
- 实现方式：`Decoration.replace(widget: TaskCheckboxWidget)` 替换 `[ ]` / `[x]`

**编辑规则**：
- 点击 checkbox 切换 `[ ]` ↔ `[x]` → 修改文档内容
- 光标/选区在任务行时，checkbox 始终渲染（不恢复为原文）
- （可选）删除 checkbox → 自动删除整行任务标记，变为普通列表

**标记显隐规则**：
- `[ ]` / `[x]` 始终渲染为 checkbox widget（cursor 进入也不例外，点击行为由 widget 处理）
- `- ` 列表标记始终可见

---

### 2.13 分割线 `---` / `***` / `___`
**语法规则:**
要创建分隔线，请在单独一行上使用三个或多个星号 (`***`)、破折号 (`---`) 或下划线 (`___`) ，并且不能包含其他内容。
```
***

---

_________________
```
以上三个分隔线的渲染效果看起来都一样

**最佳实践**
为了兼容性，请在分隔线的前后均添加空白行。

|✅  Do this|❌  Don't do this|
|---|---|
|`Try to put a blank line before...      ---      ...and after a horizontal rule.`|`Without blank lines, this would be a heading.   ---   Don't do this!`|

**展示规则**：
- 整行替换为视觉分割线（`<hr>` widget）
- 实现方式：`Decoration.replace(widget: HorizontalRuleWidget)`

**编辑规则**：
- 光标移到分割线行 → 恢复为原文，可编辑
- 光标离开分割线行 → 渲染为 `<hr>`
- 输入 `---` 在空行 → 自动变为分割线（CodeMirror 原生或自定义）

**标记显隐规则**：
- 默认：`---` 整行替换为 `<hr>` widget
- 光标/选区在分割线行：恢复原文

---
```
1
```
### 2.14 代码块 ` ```lang\ncode\n``` `

**展示规则**：
- 开闭 ` ``` ` 透明度为0.4,始终显示，但是需要独占一行
- 整个代码块需要添加底色
- 代码内容正常显示（CodeMirror 原生语法高亮）
- 实现方式：`Decoration.mark({ class: 'cm-live-preview-marker' })` 淡化 CodeMark + CodeInfo

**编辑规则**：无

**标记显隐规则**：无

---

### 2.15 表格（GFM）

**展示规则**（Phase 2）：
- `|` 分隔符和 `---` 对齐线淡化显示
- 内容正常呈现
- （可选）自动对齐列宽

**编辑规则**：
- 光标在表格内：`|` 完全可见，方便对齐编辑

---

### 2.16 内嵌 HTML 标签

**语法规则:**
Markdown 允许直接嵌入任意 HTML 标签，无需额外声明。

- **行级/内联标签**：`<span>`、`<cite>`、`<del>`、`<em>`、`<strong>`、`<code>`、`<mark>`、`<sub>`、`<sup>`、`<u>`、`<small>`、`<abbr>`、`<a>`、`<img>`、`<br>`、`<kbd>`、`<samp>`、`<b>`、`<i>` 等可自由混合在段落中使用。内联标签范围内 Markdown 语法可正常解析。
- **块级标签**：`<div>`、`<table>`、`<pre>`、`<p>` 等需前后空行。块级标签内部 Markdown 语法不会被处理。
- **自闭合标签**：`<br/>`、`<hr/>`、`<input/>` 等直接书写。

**展示规则**：
- **行内 HTML 标签**：默认可见，内容正常渲染
- **`<br>` / `<br/>`**：默认渲染为实际换行 widget（复用 `BrWidget`）
- **块级 HTML**：默认渲染块级实际html
- **HTML 注释 `<!-- -->`**：默认完全不可见

**编辑规则**：
- 光标/选区在 HTML 标签区域内时，标签恢复为正常文本样式
- 光标/选区在 HTML 块区域内时，恢复正常文本样式
- 光标/选区在注释区域内时，注释恢复可见

**标记显隐规则**：
- **行内 HTML 标签**：光标/选区重叠 → 恢复正常样式
- **`<br>`**：默认渲染为换行 widget；光标重叠 → 恢复原文 `<br>`
- **块级 HTML**：光标进入 → 恢复正常原文
- **注释**：默认隐藏；光标进入 → 显示注释原文

---

### 2.17 转义字符 `\*`

**语法规则:**
要显示原本用于格式化 Markdown 文档的字符，请在字符前面添加反斜杠 `\`。

| Markdown 语法 | 渲染效果 |
|-------------|---------|
| `\* Without the backslash` | * Without the backslash |

可转义字符：`\` `` ` `` `*` `_` `{` `}` `[` `]` `(` `)` `#` `+` `-` `.` `!` `|`

**展示规则**：
- 反斜杠 `\` 完全不可见
- 转义后的字符正常显示
- 实现方式：`Decoration.replace(InvisibleWidget)` 隐藏 Escape 节点第 1 个字符（`\`），第 2 个字符正常显示

**编辑规则**：光标/选区在 Escape 区间或所在行 → 反斜杠恢复可见

**标记显隐规则**：默认 `\` 隐藏；光标/选区重叠或所在行 → 恢复

---

## 三、光标/选区感知统一规则

### 3.1 粗体、斜体、删除线、行内代码、链接、标题、块引用、代码块

> 选区与格式化节点有交集 → 该节点的标记装饰不生效 → 标记恢复可见

实现：`selFrom < node.to && selTo > node.from` （overlap 判断）
### 3.2 结构标记类（列表 `-` / `1.`、任务复选框 `[ ]`）

> 始终不做隐藏处理。列表标记始终可见；任务复选框始终为 widget。

### 3.3 分割线

> 光标/选区在分割线行 → 恢复原文；否则渲染为 `<hr>` widget

### 3.4 HTML 标签

> **行内 HTML 标签**：光标/选区重叠 → 恢复正常
>
> **`<br>` / `<br/>`**：默认换行 widget；光标重叠 → 恢复原文
>
> **块级 HTML**：默认渲染块级实际html；光标进入 → 恢复正常
>
> **HTML 注释**：默认隐藏；光标进入 → 显示原文

### 3.5 转义字符

> 默认反斜杠 `\` 隐藏；光标/选区与 Escape 节点重叠或所在行 → 反斜杠恢复可见

---

## 四、技术实现要点

### 4.1 架构

```
CodeMirror 6 EditorView
  └─ livePreviewPlugin (ViewPlugin)
       ├─ buildDecorations(view) → DecorationSet
       │    ├─ syntaxTree(view.state) → Lezer AST
       │    ├─ for each visibleRange → tree.iterate
       │    └─ match node type → add decoration to RangeSetBuilder
       └─ update(update) → rebuild on docChanged | viewportChanged | selectionSet
```

### 4.2 性能约束

- 仅重建装饰当：docChanged || viewportChanged || selectionSet
- 仅遍历可视区域：`view.visibleRanges`
- IME 输入期间（`view.composing`）跳过装饰重建
- 使用 `RangeSetBuilder` 批量构建

### 4.3 Lezer Markdown 语法树节点名参考

| Markdown 语法 | Lezer 节点名 |
|--------------|-------------|
| `# H1`~`###### H6` | `ATXHeading1` ~ `ATXHeading6` |
| `#` 标记 | `HeaderMark` |
| `H1\n===` | `SetextHeading1` / `SetextHeading2` |
| `**bold**` | `StrongEmphasis` |
| `*italic*` | `Emphasis` |
| `*` / `**` 标记 | `EmphasisMark` |
| `~~strike~~` | `Strikethrough` |
| `~~` 标记 | `StrikethroughMark` |
| `` `code` `` | `InlineCode` |
| 反引号标记 | `CodeMark` |
| `[text](url)` | `Link` |
| `![alt](url)` | `Image` |
| `[]()` 标记 | `LinkMark` |
| URL | `URL` |
| Title | `LinkTitle` |
| `> quote` | `Blockquote` |
| `>` 标记 | `QuoteMark` |
| `- item` `* item` | `BulletList` > `ListItem` |
| `1. item` | `OrderedList` > `ListItem` |
| 列表标记 | `ListMark` |
| `---` `***` | `HorizontalRule` |
| ` ``` ` code | `FencedCode` |
| 代码语言 | `CodeInfo` |
| `[ ]` `[x]` | `TaskMarker`（GFM 启用时，但当前版本可能不生效） |
| `<span>` `<br>` 等行内 HTML | `HTMLTag` |
| `<div>` `<table>` 等块级 HTML | `HTMLBlock` |
| `<!-- -->` | `CommentBlock` |
| `\*` | `Escape` |

### 4.4 Widget 类清单

| Widget | 用途 | ignoreEvent |
|--------|------|-------------|
| `InvisibleWidget` | 完全隐藏语法标记，渲染空 span | true |
| `BrWidget` | 渲染 `<br>` 换行 | true |
| `HorizontalRuleWidget` | 渲染 `<hr>` 分割线 | true |
| `ImageWidget` | 渲染 `<img>` 图片 | false |
| `BlockquoteBorderWidget` | 渲染 `>` 引用边框 | true |
| `BulletWidget` | 渲染 `•` 无序列表符号 | true |
| `TaskCheckboxWidget` | 渲染 `<input type="checkbox">`，mousedown 切换 | false |

---

## 五、与你现有代码的关系

### 保留的基础设施
- `note.vue` 三态 viewMode（readonly / livePreview / sourcecode）
- `ToolbarNote.vue` 三态循环按钮
- `codemirrorWrapper.vue` 的 `livePreview` prop 和 `getExtensions()` 扩展函数
- `cm-theme.js` 中的直播阅览 CSS 样式
- `zh-cn.js` / `en-us.js` 中的 `livePreview` i18n 键

---

## 六、验收用例

### 6.1 默认状态（无光标/选区）
- [x] `# 标题` → `#` 不可见，标题文本按级别加粗（H1=2em, H2=1.6em, H3=1.37em, H4=1.25em, H5-H6=1.12em）
- [x] `**粗体**` / `__粗体__` → `**` `__` 不可见，文本粗体
- [x] `*斜体*` / `_斜体_` → `*` `_` 不可见，文本斜体
- [x] `***粗斜体***` / `___粗斜体___` → `***` `___` 不可见，文本粗斜体
- [x] `~~删除~~` → `~~` 不可见，文本删除线
- [x] `` `代码` `` → 反引号不可见，文本等宽字体+代码背景色
- [x] `[链接](url)` → `[]()` 和 url 不可见，仅显示链接色文本，悬浮下划线
- [x] `[链接](url "title")` → 同上，title 保留在 DOM 上
- [x] `![图片alt](url)` → 语法字符不可见，显示缩略图或 alt 文本
- [x] `> 引用` → `>` 不可见，引用内容正常显示，带左边框
- [x] `> > 嵌套引用` → 多层边框 widget 叠加
- [x] `---` / `***` / `___` → 整行渲染为 `<hr>` 分割线 widget
- [x] `- 列表` / `* 列表` / `+ 列表` → 标记符替换为 `•`，内容正常显示
- [x] ` ```\ncode\n``` ` → 代码块底色+圆角，代码内容语法高亮

### 6.2 光标进入或处于标记首尾（标记显隐）
- [x] 光标在 `**bl_od**`（标记内侧）→ `**` 可见
- [x] 光标在 `text_**blod**text`（标记首部外侧）→ `**` 可见
- [x] 光标在 `text**blod**_text`（标记尾部外侧）→ `**` 可见
- [x] 光标在 `tex_t**blod**text`（标记外部，不相邻）→ `**` 不可见
- [x] 光标在 `# 标题` 行任意位置（行首/行中/行尾）→ `#` 恢复可见
- [x] 光标在 `[链接](url)` 内和语法字符首尾 → `[]()` url 全部可见
- [x] 光标在 `> 引用` 行 → `>` 恢复可见（仅当前行）
- [x] 光标在 `` `code` `` 标记首尾 → 反引号恢复可见
- [x] 光标在 `---` 行 → 恢复原文 `---`
- [x] 光标在列表项所在行 → 原始标记符 `- ` 恢复可见

### 6.3 选区
- [x] 全选（Cmd+A）→ 所有隐藏标记恢复可见（标题#、粗体**、斜体*、删除线~~、行内代码`、引用>、列表-）
- [x] 选区与粗体部分交集 → 该粗体的 `**` 可见，其他仍隐藏
- [x] 选区与链接有交集 → 该链接的 `[]()` url 可见
- [x] 选区与标题行有交集 → `#` 可见
- [x] 选区离开后 → 标记重新隐藏（selectionSet 触发重建）

### 6.4 编辑
- [x] 在粗体/斜体/删除线/行内代码内编辑文本 → 装饰实时跟随
- [x] 删除一侧 `**` → 粗体效果消失，另一侧 `**` 变为普通文本（CM 原生行为）
- [x] 删除一侧 `*` → 斜体效果消失（CM 原生行为）
- [x] 在标题行首输入 `# ` → 自动变为标题（CodeMirror 原生）
- [x] 删除标题全部 `#` → 自动变为普通段落
- [x] Ctrl/Cmd+点击链接文本 → 在新标签页打开 URL
- [x] 点击 `<hr>` 分割线 widget → 不触发编辑（ignoreEvent: true）

### 6.5 模式切换
- [x] 实时阅览 → 源码模式：所有装饰消失，显示纯 Markdown 原文
- [x] 源码模式 → 实时阅览：装饰恢复，标记重新隐藏
- [x] 实时阅览 → 阅读视图：markdown-it 渲染静态 HTML
- [x] 阅读视图 → 实时阅览：Codmirror 编辑器恢复，装饰正确
- [x] 三态循环切换正常（readonly → livePreview → sourcecode → readonly）

### 6.6 边界与稳定性
- [x] IME 输入中文（拼音中间态）→ 不闪烁，不崩溃
- [x] 撤销/重做（Ctrl+Z / Ctrl+Shift+Z）→ 装饰正确更新
- [x] 长文档（5000+ 行）滚动 → 装饰仅重建可视区域，滚动流畅
- [x] 多个标签页各开 note → KeepAlive 切换后装饰状态不串
- [x] 控制台零错误、零警告

### 6.7 内嵌 HTML 标签
- [x] `<span style="...">红色</span>` → 内容正常显示
- [x] `<cite>`、`<del>`、`<em>` 等行内标签 → 同 span
- [x] `<br>` / `<br/>` → 默认渲染为实际换行 widget
- [x] `<br>` → 光标/选区重叠 → 恢复原文 `<br>`
- [x] `<div>块级内容</div>` → 默认渲染块级实际html
- [x] `<!-- 注释 -->` → 默认隐藏，光标进入可见
- [x] 光标/选区与 HTML 标签重叠 → 恢复正常文本样式（可编辑）
- [x] 光标/选区在块级 HTML 内 → 块级渲染撤销，恢复正常
- [x] 行内 HTML 标签内 Markdown 可正常解析（如 `<span>**粗体**</span>`）
- [x] 实时阅览 ↔ 阅读视图切换：HTML 由 markdown-it 原生渲染
- [x] 源码模式 ↔ 实时阅览切换：装饰正确恢复
- [x] 控制台零错误、零警告

### 6.8 转义字符
- [x] `\* Without the backslash` → 反斜杠 `\` 不可见，`*` 正常显示
- [x] `\_underscore` → 反斜杠不可见，`_` 正常显示
- [x] `\# heading` → 反斜杠不可见，`#` 正常显示，不解析为标题
- [x] 光标/选区进入 Escape 区间或所在行 → 反斜杠恢复可见
- [x] 选区离开 Escape 区间或所在行 → 反斜杠重新隐藏
- [x] 控制台零错误、零警告
