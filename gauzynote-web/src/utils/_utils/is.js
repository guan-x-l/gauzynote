export const isArray = (val) => Array.isArray ? Array.isArray(val) : Object.prototype.toString.call(val) === '[object Array]'
export const isFunction = (val) => typeof val === 'function'
export const isString = (val) => typeof val === 'string'
export const isNumber = (val) => typeof val === 'number' && !isNaN(val)
export const isBoolean = (val) => typeof val === 'boolean'
export const isNull = (val) => val === null
export const isUndefined = (val) => val === undefined
export const isObject = (val) => val !== null && typeof val === 'object' && !isArray(val)
export const isElement = (val) => val instanceof Element
export const isValidUrl = (val) => {
    try {
        new URL(val); // 如果无效会抛出错误
        return true;
    } catch (error) {
        return false;
    }
}
/**
 * 严格判断是否为空
 * @param val {*}
 * @returns {boolean} 是否为空
 */
export const isEmpty = (val) => {
    if (val === null || val === undefined) return true;
    if (isArray(val) || isString(val)) return val.length === 0;
    if (isObject(val)) {
        if (val instanceof Map || val instanceof Set) return val.size === 0;
        return Object.keys(val).length === 0;
    }
    return false;
}

/**
 * 判断一个值是否为Vue组件的公开实例（ComponentPublicInstance）
 * 核心依据Vue组件实例的内置标识：存在非undefined的.$属性（Vue3组件实例核心特征）
 * @param {*} value - 待判断的任意值
 * @returns {boolean} 是Vue组件实例返回true，否则返回false
 */
// export const isComponentInstance = (value) => {
//     return value?.$ !== undefined;
// };