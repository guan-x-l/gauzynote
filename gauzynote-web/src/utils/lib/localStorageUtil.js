import {isFunction} from "../index.js";
import {NAMESPACE} from "@/constants/cacheKeys.js";

/**
 * localStorage缓存工具类
 * 支持设置过期时间、自动序列化/反序列化、批量操作等
 */
const localStorageUtil = {
    /**
     * 存储数据到localStorage
     * @param {string | function} key - 存储的键名
     * @param {any} value - 存储的值（支持任意可序列化类型）
     * @param {number} [expireSeconds] - 过期时间（秒），不传则永久有效
     * @returns {boolean} - 是否存储成功
     */
    set(key, value, expireSeconds) {
        try {
            // 优化存储结构：只有设置过期时间时才添加expire字段
            const data = expireSeconds
                ? {
                    v: value,  // 使用短键名减少存储体积
                    e: Date.now() + expireSeconds * 1000
                }
                : { v: value };
            localStorage.setItem(isFunction(key) ? key() : key, JSON.stringify(data));
            return true;
        } catch (error) {
            // 处理存储失败（如存储空间不足）
            console.error('localStorage存储失败:', error);
            return false;
        }
    },

    /**
     * 从localStorage获取数据
     * @param {string | function} key - 要获取的键名
     * @param {any} [defaultValue=null] - 数据不存在或过期时的默认返回值
     * @returns {any} - 存储的值或默认值
     */
    get(key, defaultValue = null) {
        try {
            const item = localStorage.getItem(isFunction(key) ? key() : key);
            if (!item) {
                return defaultValue;
            }

            // 反序列化
            const data = JSON.parse(item);

            // 检查是否过期
            if (data.e && Date.now() > data.e) {
                // 过期数据自动清理
                this.remove(key);
                return defaultValue;
            }

            return data.v;
        } catch (error) {
            console.error('localStorage获取失败:', error);
            // 数据格式错误时清理掉
            this.remove(key);
            return defaultValue;
        }
    },

    /**
     * 从localStorage删除指定数据
     * @param {string | function} key - 要删除的键名
     */
    remove(key) {
        try {
            localStorage.removeItem(isFunction(key) ? key() : key);
        } catch (error) {
            console.error('localStorage删除失败:', error);
        }
    },

    /**
     * 清空所有已NAMESPACE开头的localStorage数据
     * @param {string[]} [keepKeys=[]] - 需要保留的键名数组
     */
    clear(keepKeys = []) {
        try {
            // 获取当前所有键名
            const keys = Object.keys(localStorage);

            // 只删除不需要保留的键
            keys.forEach(key => {
                if (!keepKeys.includes(key) && key.startsWith(NAMESPACE)) {
                    localStorage.removeItem(key);
                }
            });
        } catch (error) {
            console.error('localStorage清空失败:', error);
        }
    },

    /**
     * 批量存储数据
     * @param {Array.<{key: string, value: any, expireSeconds?: number}>} items - 要存储的键值对数组
     * @returns {number} - 成功存储的数量
     */
    batchSet(items) {
        let successCount = 0;
        items.forEach(item => {
            if (this.set(item.key, item.value, item.expireSeconds)) {
                successCount++;
            }
        });
        return successCount;
    },

    /**
     * 批量获取数据
     * @param {string[]} keys - 要获取的键名数组
     * @returns {Object} - 键值对结果
     */
    batchGet(keys) {
        const result = {};
        keys.forEach(key => {
            result[key] = this.get(key);
        });
        return result;
    },

    /**
     * 检查localStorage是否存在指定键
     * @param {string | function} key - 要检查的键名
     * @returns {boolean} - 是否存在且未过期
     */
    has(key) {
        try {
            const item = localStorage.getItem(isFunction(key) ? key() : key);
            if (!item) {
                return false;
            }

            const data = JSON.parse(item);
            return !data.e || Date.now() <= data.e;
        } catch (error) {
            return false;
        }
    },
    versionUpdate(key,version){
        const value = this.get(key)
        if (value && value !== version) {
            this.clear();
        }
        this.set(key, version);
    }
};

export default localStorageUtil;


/**
 * // 引入工具函数
 * import storage from './localStorageUtil.js';
 *
 * // 存储永久数据
 * storage.set('userName', '张三');
 *
 * // 存储带过期时间的数据（30分钟后过期）
 * storage.set('token', 'abc123xyz', 30 * 60);
 *
 * // 获取数据
 * const userName = storage.get('userName', '默认名称');
 * const token = storage.get('token');
 *
 * // 检查数据是否存在
 * if (storage.has('token')) {
 *   console.log('token存在且有效');
 * }
 *
 * // 获取剩余过期时间
 * const remaining = storage.getRemainingTime('token');
 * console.log(`token剩余${remaining}秒过期`);
 *
 * // 批量操作
 * storage.batchSet([
 *   { key: 'a', value: 1 },
 *   { key: 'b', value: 2, expireSeconds: 3600 }
 * ]);
 *
 * const batchData = storage.batchGet(['a', 'b', 'c']);
 *
 * // 删除数据
 * storage.remove('token');
 *
 * // 清空数据，保留某些键
 * storage.clear(['userName']);
 */
