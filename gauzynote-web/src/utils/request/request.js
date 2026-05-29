import axios from "axios";
import {getMessageByCode} from "@/utils/request/errorMessageMap.js";
import controllerManager from "@/utils/request/RequestControllerManager.js";
import {getToken, handleUnauthorized} from "@/utils/auth.js";
import {Message, MLoading} from "@/components/index.js";

// 创建 axios 实例
const serviceAxios = axios.create({
    baseURL: import.meta.env.VITE_BASE_URL,
    timeout: 10000,
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json'
    },
    custom: {
        // 是否跳过状态码处理
        skipStatusCodeHandling: false,
        showLoading: true,
        // 添加默认配置：是否需要令牌
        requireToken: true
    }
});

// 请求拦截器
serviceAxios.interceptors.request.use(
    (config) => {
        // console.log(config)
        const { key: abortKey } = config.abortConfig || {};

        // 处理请求取消逻辑
        if (abortKey) {
            const controller = controllerManager.add(abortKey);
            if (controller) {
                config.signal = controller.signal;
            }
        }

        // 添加 Authorization 令牌
        if (config.custom?.requireToken) {
            const token = getToken(); // 从存储中获取令牌
            if (token) {
                config.headers['Authorization'] = `Bearer ${token}`;
            }
        }

        // 设置请求头信息
        // config.headers['Current-Url'] = window.location.href;
        // config.headers['Referer'] = document.referrer;

        // if (location?.ancestorOrigins?.length) {
        //     try {
        //         config.headers['Current-AncestorOrigins'] = Array.from(location.ancestorOrigins).toString();
        //     } catch (e) {
        //         config.headers['Current-AncestorOrigins'] = JSON.stringify(location.ancestorOrigins) || '-';
        //     }
        // }

        // 添加应用标识和版本信息
        // config.headers['Current-Uuid'] = window.UUID || '';
        // config.headers['Current-V'] = window.version || '';

        // 显示加载状态
        if (config.custom?.showLoading) {
            config.custom.loadTimer = setTimeout(()=>{
                const load =  MLoading.show()
                config.custom.showLoadingId = load.id
            }, 100)
        }

        return config;
    },
    (error) => {
        console.error(error);
        return Promise.reject(error);
    }
);

// 响应拦截器
serviceAxios.interceptors.response.use(
    /**
     * @param {import('axios').AxiosResponse<Result>} response
     * @returns {Result}
     */
    (response) => {
        // console.log(response)
        const { key: abortKey } = response.config?.abortConfig || {};
        controllerManager.remove(abortKey);

        // 隐藏加载状态
        if (response.config?.custom?.loadTimer) {
            clearTimeout(response.config?.custom?.loadTimer)
        }

        // 隐藏加载状态
        if (response.config?.custom?.showLoadingId) {
            MLoading.close(response.config?.custom?.showLoadingId)
        }

        // 处理业务状态码
        const { code, msg } = response.data || {};
        if (code !== 200 && !response.config?.custom?.skipStatusCodeHandling) {
            // 处理 401 未授权状态码
            if (code === 401) {
                handleUnauthorized(); // 处理未授权状态
            }
            Message.error(getMessageByCode(code, msg), {duration: 10000})
        }
        return response.data;
    },
    (error) => {
        console.error(error)
        const { key: abortKey } = error.config?.abortConfig || {};
        controllerManager.remove(abortKey);

        // 隐藏加载状态
        if (error.config?.custom?.loadTimer) {
            clearTimeout(error.config?.custom?.loadTimer)
        }

        // 隐藏加载状态
        if (error.config?.custom?.showLoadingId) {
            MLoading.close(error.config?.custom?.showLoadingId)
        }

        // 处理取消请求
        if (axios.isCancel(error)) {
            console.error(error.message);
            error.isCanceled = true;
            return Promise.reject(error);
        }

        // 跳过错误处理
        if (error.config?.custom?.skipStatusCodeHandling) {
            return Promise.reject(error);
        }


        // 处理 HTTP 状态码
        if (error.response && error.response.status === 401) {
            MLoading.closeAll()
            // 处理 401 未授权状态
            Message.error(getMessageByCode(error.response.status), {duration: 10000})
            handleUnauthorized();
        }
        // 处理网络错误
        else if (error.code === 'ERR_NETWORK' &&
            error.request?.readyState === 4 &&
            error.request?.status === 0) {
            Message.error(getMessageByCode(0), {duration: 10000})
        }
        // 处理超时错误
        else if (error.code === 'ECONNABORTED') {
            Message.error(getMessageByCode(408), {duration: 10000})
        }
        // 未知错误
        else {
            Message.error(getMessageByCode(error.response?.status), {duration: 10000})
        }

        // 处理生产环境错误跳转
        // if (import.meta.env.MODE === 'production') {
        //     router.replace('/500');
        // }

        return Promise.reject(error);
    }
);

// 提供取消所有请求的方法
serviceAxios.cancelAllRequests = () => {
    controllerManager.clear();
};

export default serviceAxios;
