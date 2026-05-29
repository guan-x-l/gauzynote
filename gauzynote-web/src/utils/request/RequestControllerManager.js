// 检查浏览器是否支持 AbortController
import axios from "axios";

if (!window.AbortController) {
    // 不支持时使用 axios.CancelToken 替代
    console.warn('当前浏览器不支持 AbortController，使用 axios.CancelToken 替代');

    // 为不支持 AbortController 的环境提供兼容实现
    window.AbortController = class AbortController {
        constructor() {
            const cancelTokenSource = axios.CancelToken.source();
            this.signal = { aborted: false };
            this.cancel = (message) => {
                this.signal.aborted = true;
                cancelTokenSource.cancel(message);
            };
            this.token = cancelTokenSource.token;
        }
    };
}
// 请求控制器管理器
class RequestControllerManager {
    constructor() {
        this.controllers = new Map();
    }

    add(key) {
        if (key) {
            // 取消相同 key 的旧请求
            this.cancel(key);
            const controller = new AbortController();
            this.controllers.set(key, controller);
            return controller;
        }
        return null;
    }

    cancel(key) {
        if (key && this.controllers.has(key)) {
            this.controllers.get(key).abort();
            this.controllers.delete(key);
        }
    }

    remove(key) {
        if (key) {
            this.controllers.delete(key);
        }
    }

    clear() {
        this.controllers.forEach(controller => controller.abort());
        this.controllers.clear();
    }
}

// 导出单例实例（核心：整个应用中只会创建一次）
export default new RequestControllerManager();
