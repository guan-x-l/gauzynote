import {isArray} from "@/utils/index.js";

/**
 * 扁平化数组
 * @returns {[]}
 */
export function flatten(arr, childrenKey) {
    return arr.reduce((pre, cur) => {
        const item = { ...cur };
        // 移除子数组属性，避免在扁平数组中保留
        if (childrenKey in item) {
            delete item[childrenKey];
        }
        // 递归处理子数组
        return pre.concat(
            isArray(cur[childrenKey]) ? flatten(cur[childrenKey], childrenKey) : [],
            item
        );
    }, []);
}