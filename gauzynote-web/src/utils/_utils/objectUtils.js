
/**
 * 带深度限制的深拷贝对象或数组
 * @param  source - 源对象或数组
 * @param  depth - 克隆深度，默认值为 Infinity（无限制）
 * @returns {*} 克隆后的对象或数组
 */
export function objDeepClone(source, depth = Infinity) {
    // 处理null、基本类型或达到指定深度时进行浅拷贝
    if (source === null || typeof source !== 'object' || depth <= 0) {
        return source;
    }

    let target;

    // 处理日期
    if (source instanceof Date) {
        target = new Date(source);
        return target;
    }

    // 处理正则
    if (source instanceof RegExp) {
        target = new RegExp(source.source, source.flags);
        return target;
    }

    // 处理数组
    if (Array.isArray(source)) {
        target = [];
        for (let i = 0; i < source.length; i++) {
            // 递归克隆，深度减1
            target[i] = objDeepClone(source[i], depth - 1);
        }
        return target;
    }

    // 处理普通对象
    if (source instanceof Object) {
        target = {};
        // 拷贝所有自有属性
        Reflect.ownKeys(source).forEach(key => {
            // 递归克隆，深度减1
            target[key] = objDeepClone(source[key], depth - 1);
        });
        return target;
    }

    return source;
}
/**
 * 合并多个对象
 * @param {...Object} objects - 要合并的对象列表
 * @returns {Object} 合并后的新对象
 */
export function mergeObjects(...objects){
    return Object.assign({}, ...objects);
}