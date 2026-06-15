# GauzyNote
> 薄纱般轻盈，堡垒般私密

一款开源、私有化部署的 Web 端 Markdown 笔记工具，专为重视数据自主可控的开发者和知识工作者打造。
* ✨ 轻量 Web 体验：无需安装客户端，浏览器直接访问，编辑流畅无负担
* 🔒 私有化部署：支持本地服务器 / 私有云部署，数据完全自主掌控，拒绝公有云依赖
* 📝 原生 Markdown 支持：完整兼容 MD 语法，支持源码编辑、预览、实时预览模式
* 🆓 开源自由定制：基于MIT开源协议，开发者可按需扩展功能，打造个性化笔记工作站

|           | GauzyNote              |
|-------------|------------------------|
| 费用          | 永久免费             |
| 定制化         | 完全开源，MIT授权                |
| 数据隐私    | 自托管，本地或者私有云部署，不同账户数据独立 |

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.1-green.svg)]()
[![Java](https://img.shields.io/badge/Java-8-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.6-brightgreen.svg)]()
[![Vue](https://img.shields.io/badge/Vue-3.5-4fc08d.svg)]()

🌍 [English](./README-EN.md)

## 📌 目录
- 快速开始
- 技术栈
- 项目进度
- 功能特性
- 文件结构
- 开发指南
- 生产环境部署
- 贡献指南
- 常见问题
- 许可证
- 演示图

## 快速开始

> 切换后端服务语言前往文件修改 Constants.DEFAULT_LOCALE （支持中/英），修改前往i18n目录下

### 开发环境要求
* 后端：JDK 8、Maven 3.9+、MySQL 5.7、Redis 6.0+
* 前端：Node.js 16+、npm 7+
* 运行环境：Windows/macOS/Linux（支持私有化部署的操作系统均可）

### 部署环境要求
* 后端：JDK 8、MySQL 5.7、Redis 6.0+
* nginx或其他工具
* 运行环境：Windows/macOS/Linux（支持私有化部署的操作系统均可）

### 后端启动步骤
1. 克隆仓库：`git clone https://github.com/guan-x-l/gauzynote.git`
2. 进入项目根目录，加载 Maven 依赖包（编辑器也可自动加载，命令行可执行 mvn clean install -Dmaven.test.skip=true）
3. 进入后端目录：`cd gauzynote/gauzynote-application`
4. 修改数据库配置：编辑 `src/main/resources/application-dev.yml`，配置 MySQL 和 Redis 连接信息（参考下方「必要配置」）
5. 启动服务：
    - 命令行：`mvn spring-boot:run`
    - 编辑器：直接运行 `GauzynoteApplication.java` 主类
6. 后端运行成功可以通过(http://localhost:8989)访问，页面出现404提示即为正常（后端无静态页面，需继续部署前端）

### 前端启动步骤
1. 进入前端目录：`cd gauzynote/gauzynote-web`
2. 安装依赖：`npm install`
   > 强烈建议不要用直接使用 cnpm 安装，会有各种诡异的 bug，可以通过重新指定 registry 来解决 npm 安装速度慢的问题。
    - 默认源：`npm install`
    - 国内源（推荐）：`npm install --registry=https://registry.npmmirror.com`
3. 启动开发服务器：`npm run dev`
4. 访问前端：浏览器打开 http://localhost:8988，输入账号(admin)密码(1234567)即可登录（默认账号密码可参考项目初始化数据说明）

> [!WARNING]
> 本项目是前后端完全分离架构，必须确保前后端服务均启动成功，才能正常访问和使用。


### 必要配置（必改）
修改运行环境，编辑 `gauzynote-application/src/main/resources/application.yml` 文件：
```yaml
# Spring配置
spring:
  profiles:
    active: dev #运行环境，可直接修改配置文件，也可以通过编辑器的运行配置添加dev环境文件参数更改环境
token:
   secret: wwssadadbaba # 令牌密钥
```


修改数据库和 Redis 连接信息，编辑 `gauzynote-application/src/main/resources/application-dev.yml` 文件：

```yaml
# 数据源配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gauzy_note?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&serverTimezone=GMT%2B8&createDatabaseIfNotExist=true
    username: root  # 你的数据库账号
    password: 123456  # 你的数据库密码
  redis:
    host: localhost  # Redis 地址
    port: 6379       # Redis 端口
    password:        # Redis 密码（无密码则留空）
    database: 0      # Redis 数据库索引
```

初始化数据库配置：

```yaml
spring:
  sql:
    init:
      mode: ALWAYS  # ALWAYS：始终初始化（开发环境）；NEVER：不初始化（生产环境，建议改为 NEVER）
      schema-locations: classpath:sql/schema.sql  # 表结构初始化脚本
      data-locations: classpath:sql/data.sql      # 初始数据脚本
```

### 额外配置（可选）
修改项目核心配置，编辑`gauzynote-application/src/main/resources/application-dev.yml` 文件：

```yaml
app:
  cors:
    allowed-origins: #跨域配置，本地开发建议保留 * ，prod建议设置为指定域名(已配置域名的前提下)
    - "*"
    - "https://www.example.com"
    - "https://example.com"
    allow-credentials: true # 是否允许携带 Cookie
  upload:
    dir: /Users/{用户名}/home/gauzynote/uploads/ # 本地开发时上传资源路径，此处示例为mac系统路径
    #dir: C:/gauzynote/uploads/ # windows路径示例
```

> 至此没有意外的情况下，便可以在本地完整使用。
> 
> 作者本地开发环境为：macOS Sequoia 15.7.2，jdk1.8.0_462，mysql5.7，maven3.9，redis7.2.0，nodejs18.17.1，npm9.6.7



## 技术栈

### 前端
| 技术/框架        | 版本    | 用途                     |
|--------------|-------|--------------------------|
| Vue          | 3.5.x | 前端框架                   |
| Vite         | 7.x   | 构建工具                   |
| CodeMirror 6 | 6.x   | Markdown 编辑器            |
| markdown-it  | 14.x  | Markdown 解析 & 只读预览      |
| highlight.js | 11.x  | 代码块语法高亮（只读预览 & 实时预览）  |

### 后端
| 技术/框架        | 版本    | 用途         |
|--------------|-------|------------|
| Java         | 8     | 开发语言       |
| Spring Boot  | 2.7.6 | 后端框架       |
| MySQL        | 5.7   | 数据库        |
| Redis        | 6.0+  | 缓存 & Token 存储 |
| Maven        | 3.9+  | 项目构建与依赖管理  |

## 项目进度

### 当前版本
**v1.0**（基础功能可用，持续迭代中）

### 阶段目标
* ✅ **第一阶段**：基础编辑功能
  * Markdown 编辑/预览、资源管理、图片上传、账号系统
* ✅ **第二阶段**：体验优化
  * 实时预览（含表格、任务列表、代码块语法高亮）、主题切换（亮色/暗色）、多语言切换
* 📋 **第三阶段**：高级功能（规划中）
  * 双链笔记、知识图谱、分享功能

## 功能特性

### 编辑与预览
* [x] Markdown 源码编辑
* [x] 图片嵌入与上传
* [x] Markdown 只读预览（highlight.js 代码块语法高亮）
* [x] 实时预览（WYSIWYG 风格，所见即所得）
* [x] 图片嵌入与上传（拖拽/粘贴/工具栏）

### 资源管理
* [x] 资源管理器（文件夹/文件创建、编辑、删除、移动、重命名）
* [x] 回收站（目录/笔记/文件移入回收站、放回原处、彻底删除）

### 系统功能
* [x] 账户系统（注册、登录、密码修改）
* [x] 多语言支持（中/英）
* [x] 主题切换（亮色/暗色，支持跟随系统）
* [x] WebAuthn / Passkey 登录

### 高级功能（规划中）
* [ ] 双链笔记与知识图谱
* [ ] 资源/文章分享
* [ ] 流程图和思维导图


## 文件结构

### 后端项目结构
```
├── gauzynote-application      // 主服务模块（入口 & REST 控制器）
├── gauzynote-common           // 通用模块（领域类、枚举、工具类、注解、常量）
├── gauzynote-framework        // 框架模块（Spring Security、JWT、AOP、拦截器、全局异常处理）
├── gauzynote-system           // 业务模块（MyBatis Mapper、Service、实体/VO）
└── gauzynote-web              // 前端模块（Vue 3 / Vite，独立于 Maven 构建）
```

### 前端项目结构
```
├── public                      // 公共静态资源
├── src                         // 源代码核心目录
│   ├── api                     // 接口请求封装
│   ├── assets                  // 样式、图片等静态资源
│   ├── biz                     // 业务逻辑模块
│   ├── components              // 通用组件（全局复用）
│   ├── constants               // 全局常量定义
│   ├── enum                    // 枚举类型定义
│   ├── hooks                   // Vue 3 Hooks 封装
│   ├── layout                  // 页面布局组件
│   ├── locales                 // 国际化语言包
│   ├── plugins                 // 插件封装（编辑器、UI 等）
│   ├── router                  // 路由配置
│   ├── store                   // 状态管理（Vue 3 Composables）
│   ├── utils                   // 全局工具函数
│   ├── views                   // 页面视图组件
│   ├── App.vue                 // 入口根组件
│   ├── context.js              // 应用上下文
│   ├── jsdoc.js                // JSDoc 配置
│   ├── main.js                 // 入口文件
│   └── storage-compatibility.js// 浏览器存储兼容性处理
├── .env.development            // 开发环境配置
├── .env.production             // 生产环境配置
├── .gitignore                  // Git 忽略文件配置
├── index.html                  // HTML 入口模板
├── jsconfig.js                 // JavaScript 配置
├── package.json                // 依赖与脚本配置
└── vite.config.js              // Vite 构建配置
```


## 开发指南
本项目基于 MIT 协议完全开源，不收取任何费用。

项目中的注释均为中文，请见谅

### 本地开发注意事项
- 前后端联调：确保前端.env.development 中的 VITE_API_BASE_URL 与后端服务地址一致
- 依赖安装：前端依赖安装失败时，可尝试删除 node_modules 和 package-lock.json 后重新安装
- 数据库迁移：若修改了数据模型，需同步更新 schema.sql 并重新初始化数据库

## 🚢 生产环境部署
开发环境验证通过后，可参考以下步骤部署到生产环境（以 Linux 服务器为例）：

### 1. 后端打包部署
1. 打包：进入项目根目录，执行`mvn clean package -Dmaven.test.skip=true`，或编辑器完成，打包完成后在 `gauzynote-application/target` 目录生成`gauzynote-application-{version}.jar`
2. 配置环境：前往 `application.yml`，修改环境为prod（设置 spring.profiles.active=prod）
3. 配置生产环境：前往 `application-prod.yml`，修改数据库、Redis、跨域、上传路径等配置（生产环境建议关闭 SQL 自动初始化，设置 spring.sql.init.mode=NEVER）
4. 启动服务： 
   * 后台启动（推荐）：`nohup java -jar gauzynote-application-{version}.jar > /dev/null 2>&1 &`
   * 验证启动：查看日志 app.log 文件

### 2. 前端打包部署
1. 打包：进入 `gauzynote-web` 目录，执行 `npm run build`，打包完成后生成 dist 目录
2. 部署到 Nginx：
   * 将 dist 目录下的文件复制到 Nginx 静态资源目录（如 /usr/share/nginx/html/gauzynote）
   * 配置 Nginx 反向代理（解决跨域）：
    ```
    server {
        listen 80;
        server_name your-domain.com;  # 你的域名
    
        location / {
            root /usr/share/nginx/html/gauzynote;
            index index.html;
            try_files $uri $uri/ /index.html;  # 解决 Vue 路由刷新 404 问题
        }
        
        # 反向代理后端接口
        location /service/gauzynote {
            proxy_pass http://localhost:8989;  # 后端服务地址
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
    ```
    * 重启 Nginx：`nginx -s reload`

### 3. 生产环境建议
- 数据库：开启 MySQL 慢查询日志，定期备份数据
- 安全：设置防火墙，仅开放必要端口（如 80/443）；配置 HTTPS（通过 Nginx 配置 SSL 证书）
- **定期备份数据库**：建议每日自动备份 MySQL，保留最近 7 天
- **关闭 SQL 自动初始化**：设置 `spring.sql.init.mode=NEVER`
- **Nginx Gzip 压缩**：开启静态资源压缩减少传输量
- 详细信息参见 [SECURITY.md](SECURITY.md)


## 🤝 贡献指南
欢迎通过以下方式参与项目贡献：
1. 提交 Issue：在[Issues](https://github.com/guan-x-l/gauzynote/issues) 页面反馈 bug、提出功能建议


## ❓ 常见问题
1. Q：Mac 系统下上传文件失败？
   A：检查 .yml 中 app.upload.dir 路径是否正确（需使用绝对路径），并确保目录有读写权限（执行 chmod 755 目录路径）。
2. Q：前端启动后无法连接后端？
   A：检查后端服务是否正常启动（访问 http://<主机地址>:8989 验证）；检查前端 .env.development 中的 VITE_API_BASE_URL 是否正确；检查后端跨域配置是否包含前端地址。
3. Q：Maven 依赖加载失败？
   A：检查网络环境，可配置 Maven 国内镜像（阿里云）；执行 mvn clean install -U 强制更新依赖。

如遇到其他无法解决的问题，请到 [Issues](https://github.com/guan-x-l/gauzynote/issues) 反馈，会不定时进行解答。


## 📚 技术借鉴
项目部分逻辑和设计参考了以下优秀项目，在此表示感谢：
* **Obsidian**：优秀的界面布局和流畅的交互
* **Arco Design Vue**：UI 组件和设计规范
* **RuoYi-Vue**：前后端分离架构设计


## ⚠️ 致谢与免责
本项目由个人开发者在学习和实践中开发，功能和稳定性可能存在不足。请根据实际需求评估使用风险，作者不承担因使用本项目导致的任何损失。
本项目为web端Markdown编辑器，界面布局与交互逻辑参考行业通用设计规范，视觉与交互细节设计参考个别浏览器和个别开源项目最终独立完成，与任何第三方app/软件无权属关联，未使用任何第三方受保护的设计元素与代码。
本项目采用MIT开源协议，开源内容仅包含本项目独立开发的代码与设计，若因使用本项目产生侵权纠纷，由使用者自行承担责任。
本项目内容包含独立完成的代码与设计。除此之外关于非独立完成的代码与设计来源为网络或AI提供，若存在侵权行为请联系作者进行删除。

## 📜 许可证
本项目基于 [MIT 许可证](https://opensource.org/licenses/MIT) 开源，详见 `LICENSE` 文件。

## 演示图
|         ||
|---------|-|
| 登陆      | ![登陆.png](/.image%2F%E7%99%BB%E9%99%86.png)|
| 预览笔记    | ![预览笔记.png](/.image%2F%E9%A2%84%E8%A7%88%E7%AC%94%E8%AE%B0.png)|
| 修改密码    | ![密码.png](/.image%2F%E5%AF%86%E7%A0%81.png)|
| 深色主题    | ![深色主题.png](/.image%2F%E6%B7%B1%E8%89%B2%E4%B8%BB%E9%A2%98.png)|
| 编辑与创建   | ![编辑与创建.png](/.image%2F%E7%BC%96%E8%BE%91%E4%B8%8E%E5%88%9B%E5%BB%BA.png)|
| 目录      | ![目录.png](/.image%2F%E7%9B%AE%E5%BD%95.png)|
| 切换语言    | ![语言.png](/.image%2F%E8%AF%AD%E8%A8%80.png)|
| 重命名与标签栏 | ![重命名与标签栏.png](/.image%2F%E9%87%8D%E5%91%BD%E5%90%8D%E4%B8%8E%E6%A0%87%E7%AD%BE%E6%A0%8F.png) |
