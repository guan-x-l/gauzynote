/**
 * @template T
 * @typedef {Object} Result
 * @property {number} code
 * @property {string} msg
 * @property {*|T} data
 */
/**
 * 资源节点
 * @typedef {Object} ResourceNode
 * @property {number} nodeId - 主键
 * @property {number} parentId - 父节点ID
 * @property {number} relatedId - 关联的具体文件ID（仅文件类型节点有效）
 * @property {number} userId - 用户ID
 * @property {string} nodeType - 节点类型（1文件夹）
 * @property {string} nodeName - 节点名称
 * @property {number | null} sort - 排序
 * @property {string} nodePath - 存储完整路径
 * @property {number} depth - 存储层级深度
 * @property {string} createTime
 * @property {string} updateTime
 */

/**
 * TreeNode
 * @template T
 * @typedef {T} TreeNode
 * @property {T[]} [children] - 子节点数组，可选
 */


/**
 * 标签页对象
 * @typedef {Object} Tab
 * @property {number} tabId - 标签页唯一标识
 * @property {string} name - 标签页名称
 * @property {string} path - 标签页路径
 * @property {string} fullPath - 标签页完整路径
 * @property {string} title - 标签页标题
 * @property {number} tabIndex - 下标
 * @property {boolean} isPin - 固定
 * @property {boolean} isOnly - 固定
 * @property {string} componentName - 组件名称
 * @property {number | null} nodeId - 节点id
 * @property {string | null} nodeType - 节点类型
 * @property {number | null} relatedId - 关联资源id
 */

/**
 * @typedef {Object} FileInfo
 * @property {number} id - 唯一标识
 * @property {File} file
 * @property {string} name - 文件名
 * @property {string} type - 文件类型
 * @property {number} size - 文件大小
 * @property {string|null} dataUrl - 文件地址
 * @property {string|null} errorMessage - 异常信息
 */

/**
 * 存储空间
 * @typedef {Object} StorageSpace
 * @property {number} storageSpace - 总存储空间
 * @property {number} usedStorageSpace - 已使用存储空间
 */


/**
 * 任务对象
 * @typedef {Object} AssetTask
 * @property {number} taskId - 任务ID
 * @property {string} taskTitle - 任务标题
 * @property {string | null} [taskContent] - 任务内容
 * @property {string | null} [startTime] - 开始时间
 * @property {string | null} [endTime] - 截止时间
 * @property {string | null} [actualCompletionTime] - 实际完成时间
 * @property {string} status - 任务状态
 * @property {string} priority - 优先级
 * @property {number | null} [sort] - 所有任务列表排序值
 * @property {number | null} [kanbanSort] - 看板排序值
 * @property {number | null} [ganttSort] - 甘特图排序值
 * @property {string} [createTime] - 创建时间
 * @property {string} [updateTime] - 更新时间
 */
