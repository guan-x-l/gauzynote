/**
 * 浏览器存储兼容性解决方案
 * 支持无痕模式、错误处理和完整的Storage接口
 */
class StorageManager {
    constructor(storageType = 'local') {
        this.storageType = storageType;
        this.isSupported = false;
        this.nativeStorage = null;
        this.init();
    }

    // 初始化存储检测
    init() {
        const storageKey = `__storage_test__${this.storageType}`;
        const testValue = 'test';

        try {
            // 检测原生存储对象是否存在
            const storage = this.storageType === 'local'
                ? window.localStorage
                : window.sessionStorage;

            // 尝试写入并读取测试值，验证存储是否可用
            storage.setItem(storageKey, testValue);
            if (storage.getItem(storageKey) === testValue) {
                storage.removeItem(storageKey);
                this.isSupported = true;
                this.nativeStorage = storage;
            }
        } catch (e) {
            console.warn(`[StorageManager] ${this.storageType}Storage 不可用:`, e);
        }
    }

    // 实现完整的Storage接口
    setItem(key, value) {
        // if (!this.isSupported) return;
        try {
            this.nativeStorage.setItem(key, value);
        } catch (e) {
            this.handleStorageError(e, 'setItem', key);
        }
    }

    getItem(key) {
        if (!this.isSupported) return null;
        try {
            return this.nativeStorage.getItem(key);
        } catch (e) {
            this.handleStorageError(e, 'getItem', key);
            return null;
        }
    }

    removeItem(key) {
        if (!this.isSupported) return;
        try {
            this.nativeStorage.removeItem(key);
        } catch (e) {
            this.handleStorageError(e, 'removeItem', key);
        }
    }

    clear() {
        if (!this.isSupported) return;
        try {
            this.nativeStorage.clear();
        } catch (e) {
            this.handleStorageError(e, 'clear');
        }
    }

    key(index) {
        if (!this.isSupported || index < 0) return null;
        try {
            return this.nativeStorage.key(index);
        } catch (e) {
            this.handleStorageError(e, 'key', index);
            return null;
        }
    }

    get length() {
        if (!this.isSupported) return 0;
        try {
            return this.nativeStorage.length;
        } catch (e) {
            this.handleStorageError(e, 'length');
            return 0;
        }
    }

    // 错误处理与日志记录
    handleStorageError(error, operation, key = '') {
        console.error(`[StorageManager] ${this.storageType}Storage ${operation} 失败`, {
            key,
            error: error.message,
            errorType: error.name,
            storageSize: this.getStorageSize()
        });

        // 触发自定义事件，便于应用监听存储错误
        this.dispatchStorageEvent('error', {
            operation,
            key,
            error: error.message
        });
    }

    // 获取存储使用情况（以KB为单位）
    getStorageSize() {
        if (!this.isSupported) return 0;
        try {
            let size = 0;
            for (let i = 0; i < this.nativeStorage.length; i++) {
                const key = this.nativeStorage.key(i);
                size += this.nativeStorage.getItem(key).length * 2 / 1024; // 转为KB
            }
            return Math.round(size * 100) / 100; // 保留两位小数
        } catch (e) {
            return -1; // 错误时返回-1
        }
    }

    // 触发自定义事件
    dispatchStorageEvent(eventName, detail) {
        const event = new CustomEvent(`storage-${this.storageType}-${eventName}`, {
            detail: {
                storageType: this.storageType,
                ...detail
            }
        });
        window.dispatchEvent(event);
    }
}

// 初始化并替换原生存储对象
function setupStorageCompatibility() {
    // 初始化本地存储
    const localStorageManager = new StorageManager('local');
    Object.defineProperty(window, 'localStorage', {
        value: localStorageManager,
        writable: false,
        configurable: false
    });

    // 初始化会话存储
    const sessionStorageManager = new StorageManager('session');
    Object.defineProperty(window, 'sessionStorage', {
        value: sessionStorageManager,
        writable: false,
        configurable: false
    });

    // 提供全局状态检查
    window.storageStatus = {
        localStorage: {
            supported: localStorageManager.isSupported,
            size: localStorageManager.getStorageSize()
        },
        sessionStorage: {
            supported: sessionStorageManager.isSupported,
            size: sessionStorageManager.getStorageSize()
        }
    };

    console.info('[StorageManager] 存储兼容性初始化完成', window.storageStatus);
}

// 导出工具函数
export const getStorageStatus = () => ({
    localStorage: {
        supported: window.localStorage.isSupported,
        size: window.localStorage.getStorageSize()
    },
    sessionStorage: {
        supported: window.sessionStorage.isSupported,
        size: window.sessionStorage.getStorageSize()
    }
});

// 导出存储管理器类
// export { StorageManager };

// 默认导出初始化函数
export default setupStorageCompatibility;