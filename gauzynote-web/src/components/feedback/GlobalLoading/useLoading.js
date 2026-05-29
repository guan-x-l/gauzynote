import {ref} from "vue";
import {createUniqueId} from "@/utils/index.js";

const loadingInstances = ref(new Map())

// 添加加载实例
const addLoadingInstances = (config) => {
    const id = createUniqueId();
    const defaultConfig = {
        lock: false,
        background: 'var(--color-spin-layer-bg)',
        ...config
    };

    loadingInstances.value.set(id, defaultConfig);
    return id;
};

// 移除加载实例
const removeLoadingInstances = (id) => {
    if (loadingInstances.value.has(id)) {
        loadingInstances.value.delete(id);
        return true;
    }
    return false;
};

// 清空所有实例
const clearAllLoadingInstances = () => {
    loadingInstances.value.clear();
};
// 提供组合式API
export const useLoading = () => {
    return {
        loadingInstances,
        addLoadingInstances,
        removeLoadingInstances,
        clearAllLoadingInstances,
    };
};