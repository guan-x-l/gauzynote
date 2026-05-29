import {createUniqueTimeId} from "@/utils/_utils/IDUtils.js";
import {sleep} from "@/utils/_utils/timeUtils.js";
import {validateFile} from "@/utils/_utils/fileUtils.js";
import {$t, i18n} from "@/locales";

/**
 * 生成下一个未命名节点名称（格式：未命名、未命名1、未命名2...）
 * @param {string[]} existingNames - 已存在的名称列表
 * @returns {string} 新的未命名名称
 */
export function createNextUnnamedNodeName(existingNames) {
    // 过滤并规范化数组，只保留标准"未命名"格式的字符串
    // const regex = /^未命名(\d*)$/;
    const name = $t('empty.untitled')
    const regex = new RegExp('^'+name+'(\\d*)$')
    const existingNumbers = existingNames
        .filter(name => name)
        .map(item => {
            const match = item.match(regex);
            return match ? (match[1] === '' ? 0 : Number(match[1])) : null;
        })
        .filter(num => num !== null);

    // 使用Set加速查找
    const existingNumberSet = new Set(existingNumbers);
    const MAX_ATTEMPTS = 1000;// 最大尝试次数

    // 从0开始查找第一个不存在的数字
    for (let i = 0; i <= MAX_ATTEMPTS; i++) {
        if (!existingNumberSet.has(i)) {
            return i === 0 ? $t('empty.untitled') : `${$t('empty.untitled')}${i}`;
        }
    }

    // 极端情况下返回带时间戳的唯一名称
    return `${$t('empty.untitled')}_${Date.now()}`;
}

/**
 * 通用的轮询检查工具
 * @deprecated
 * @param {Function} checkFn - 检查函数，返回需要检查的值
 * @param {Object} options - 配置选项
 * @param {number} [options.interval=1000] - 检查间隔时间(ms)
 * @param {number} [options.timeout=60000] - 超时时间(ms)
 * @param {Function} [options.successCondition=(value) => Boolean(value)] - 成功条件判断函数
 * @param {string} [options.name='target'] - 检查目标的名称，用于日志
 * @returns {Promise} - 返回检查成功的值
 */
export async function pollUntil(checkFn, options = {}) {
    // 默认配置
    const {
        interval = 1000,
        timeout = 60000,
        successCondition = (value) => Boolean(value),
        name = 'target'
    } = options;

    const startTime = Date.now();
    let checkCount = 0;

    // 检查是否超时
    const isTimeout = () => Date.now() - startTime >= timeout;

    while (true) {
        checkCount++;

        try {
            // 执行检查函数
            const result = await checkFn();

            // 检查是否满足成功条件
            if (successCondition(result)) {
                console.log(`✅ ${name} 检查成功 (第 ${checkCount} 次尝试)`);
                return result;
            }

            // 检查是否超时
            if (isTimeout()) {
                console.error(`❌ ${name} 检查超时 (${timeout}ms内未满足条件)`);
                throw new Error(`${name} 检查超时`);
            }

            // 未满足条件，继续轮询
            console.log(`⏳ ${name} 未满足条件，将在 ${interval}ms 后重试 (第 ${checkCount} 次尝试)`);
            await sleep(interval);

        } catch (error) {
            // 如果检查函数抛出"over"错误，视为主动终止
            if (error === 'over' || error.message === 'over') {
                console.error(`❌ ${name} 检查被终止 (第 ${checkCount} 次尝试)`);
                throw new Error(`${name} 检查被终止`);
            }

            // 其他错误处理
            console.error(`❌ ${name} 检查发生错误:`, error);
            throw error;
        }
    }
}

/**
 * 可控轮询检查工具
 * 支持开始、暂停、继续、重新开始操作
 */
export class ControllablePoller {
    /**
     * 创建轮询器实例
     * @param {Function} checkFn - 检查函数，返回需要检查的值（支持异步）
     * @param {Object} options - 配置选项
     * @param {number} [options.interval=1000] - 检查间隔时间(ms)
     * @param {number} [options.timeout=60000] - 超时时间(ms)
     * @param {Function} [options.successCondition=(value) => Boolean(value)] - 成功条件判断函数
     * @param {string} [options.name='target'] - 检查目标的名称，用于日志
     */
    constructor(checkFn, options = {}) {
        // 配置参数
        this.checkFn = checkFn;
        this.options = {
            interval: 1000,
            timeout: 60000,
            successCondition: (value) => Boolean(value),
            name: 'target',
            showLog: false,
            ...options
        };

        // 状态管理
        this.status = 'idle'; // idle | running | paused
        this.checkCount = 0;
        this.startTime = null;
        this.timer = null;
        this.resolve = null;
        this.reject = null;
        this.promise = null;
    }

    /**
     * 检查是否超时
     */
    #isTimeout() {
        return Date.now() - this.startTime >= this.options.timeout;
    }

    /**
     * 核心轮询逻辑
     */
    async #poll() {
        if (this.status !== 'running') return;

        this.checkCount++;

        try {
            // 执行检查函数
            const result = await this.checkFn();

            // 检查是否满足成功条件
            if (this.options.successCondition(result)) {
                this.options.showLog && console.log(`✅ ${this.options.name} 检查成功 (第 ${this.checkCount} 次尝试)`);
                this.status = 'idle';
                this.resolve(result);
                return;
            }

            // 检查是否超时
            if (this.#isTimeout()) {
                this.options.showLog && console.error(`❌ ${this.options.name} 检查超时 (${this.options.timeout}ms内未满足条件)`);
                this.status = 'idle';
                this.reject(new Error(`${this.options.name} 检查超时`));
                return;
            }

            // 未满足条件，继续轮询
            this.options.showLog && console.log(`⏳ ${this.options.name} 未满足条件，将在 ${this.options.interval}ms 后重试 (第 ${this.checkCount} 次尝试)`);

            // 设置下一次轮询定时器
            this.timer = setTimeout(() => this.#poll(), this.options.interval);

        } catch (error) {
            // 主动终止标识
            if (error === 'over' || error.message === 'over') {
                this.options.showLog && console.error(`❌ ${this.options.name} 检查被终止 (第 ${this.checkCount} 次尝试)`);
                this.status = 'idle';
                this.reject(new Error(`${this.options.name} 检查被终止`));
                return;
            }

            // 其他错误处理
            this.options.showLog && console.error(`❌ ${this.options.name} 检查发生错误:`, error);
            this.status = 'idle';
            this.reject(error);
        }
    }

    /**
     * 开始轮询
     * @returns {Promise} 返回检查成功的值
     */
    start() {
        if (this.status === 'running') {
            this.options.showLog && console.warn(`${this.options.name} 轮询已在运行中`);
            return this.promise;
        }

        // 重置状态
        this.status = 'running';
        this.startTime = Date.now();


        // 创建新的Promise
        this.promise = new Promise((resolve, reject) => {
            this.resolve = resolve;
            this.reject = reject;
        });

        // 立即开始第一次检查
        this.#poll();
        return this.promise;
    }

    /**
     * 暂停轮询
     */
    pause() {
        if (this.status !== 'running') {
            this.options.showLog && console.warn(`${this.options.name} 轮询未在运行中，无法暂停`);
            return;
        }

        this.status = 'paused';
        if (this.timer) {
            clearTimeout(this.timer);
            this.timer = null;
        }
        this.options.showLog && console.log(`⏸️ ${this.options.name} 轮询已暂停 (已尝试 ${this.checkCount} 次)`);
    }

    /**
     * 继续（从暂停状态恢复或重新启动）
     */
    continueTimer() {
        if (this.status === 'running') {
            this.options.showLog && console.warn(`${this.options.name} 轮询已在运行中`);
            return;
        }

        // 如果是从暂停状态恢复，保持已有计数和开始时间
        if (this.status === 'paused') {
            this.status = 'running';
            this.options.showLog && console.log(`▶️ ${this.options.name} 轮询已恢复 (将继续第 ${this.checkCount + 1} 次尝试)`);
            this.timer = setTimeout(() => this.#poll(), this.options.interval);
        } else {
            this.start()
        }
    }
    /**
     * 重新开始轮询
     */
    restart() {
        if (this.status === 'running') {
            this.pause()
        }
        this.options.showLog && console.log(`${this.options.name} 轮询已重新开始`);
        this.checkCount = 0;
        return this.start();
    }

    /**
     * 强制停止轮询（会触发reject）
     * @param {string} reason - 停止原因
     */
    stop(reason = '手动停止') {
        if (this.status === 'idle') {
            this.options.showLog && console.warn(`${this.options.name} 轮询未在运行中，无需停止`);
            return;
        }

        this.status = 'idle';
        if (this.timer) {
            clearTimeout(this.timer);
            this.timer = null;
        }
        this.options.showLog && console.log(`⏹️ ${this.options.name} 轮询已停止: ${reason}`);
        this.reject(new Error(`${this.options.name} 轮询被停止: ${reason}`));
    }

    /**
     * 获取当前轮询状态
     * @returns {string} 状态：idle | running | paused
     */
    getStatus() {
        return this.status;
    }
}

/**
 * 处理文件，返回图片对象信息
 * @param {File} file - 文件
 * @param {Object} [options]
 * @param {number} [options.maxSize] - 最大文件大小(MB)，默认5MB
 * @param {string[]} [options.allowedTypes] - 允许的图片类型，默认['image/jpeg', 'image/png', 'image/gif', 'image/webp']
 * @returns {Promise.<FileInfo>} 包含文件信息的对象
 */
export async function processFile(file, options = {}) {
    const {
        maxSize = 5,
        allowedTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/gif', 'image/webp'],
    } = options;

    // 检查输入是否有效
    if (!file) {
        return Promise.reject();
    }

    /** @type {FileInfo} */
    let fileInfo = {
        id: createUniqueTimeId(),
        file,
        name: file.name,
        type: file.type,
        size: file.size,
        dataUrl: null,
        errorMessage: null
    }
    let validate = validateFile(file, allowedTypes, maxSize);
    if (validate.status === false) {
        return new Promise((resolve) => {
            fileInfo.errorMessage = validate.message
            resolve(fileInfo);
        })
    }
    return new Promise((resolve) => {
        const reader = new FileReader();
        reader.onload = (e) => {
            fileInfo.dataUrl = e.target.result
            resolve(fileInfo);
        };
        // 读取失败时也返回基础信息，但标记错误状态
        reader.onerror = () => {
            fileInfo.errorMessage = '文件读取失败'
            resolve(fileInfo);
        };
        // 开始读取文件
        reader.readAsDataURL(file);
    });

}
/**
 * 处理文件数组，返回图片对象信息数组
 * @param {FileList} fileList - 图片文件数组
 * @param {Object} [options]
 * @param {number} [options.maxSize] - 最大文件大小(MB)，默认5MB
 * @param {string[]} [options.allowedTypes] - 允许的图片类型，默认['image/jpeg', 'image/png', 'image/gif', 'image/webp']
 * @returns {Promise.<FileInfo[]>} 包含图片信息的对象数组
 */
export async function processFileList(fileList, options = {}) {
    if (!fileList) {
        return Promise.reject();
    }

    if (fileList.length === 0) {
        return Promise.resolve([]);
    }

    // 返回所有处理结果的Promise
    return Promise.all(Array.from(fileList).map(file => processFile(file, options)));
}