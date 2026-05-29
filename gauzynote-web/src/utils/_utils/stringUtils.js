import {isString} from "./is.js";

/**
 * 字符串首字母大写
 * @param {string} str - 输入字符串
 * @returns {string} 首字母大写后的字符串
 */
export function capitalize(str) {
    if (!str || !isString(str)) return ''
    return str.charAt(0).toUpperCase() + str.slice(1);
}

/**
 * 字符串转驼峰格式
 * @param {string} str - 输入字符串，支持 kebab-case、snake_case 等
 * @returns {string} 驼峰格式字符串
 */
export function camelCase(str) {
    if (!str || !isString(str)) return ''
    return str
        .replace(/[-_]+(.)?/g, (match, char) => char ? char.toUpperCase() : '')
        .replace(/^(.)/, (match) => match.toLowerCase());
}

/**
 * 驼峰式命名转换为短横线连接式命名
 * @param {string} str
 * @returns {string}
 */
export function camelToKebab(str) {
    if (!str || !isString(str)) return ''

    // 处理首字母大写的情况
    let result = str.replace(/^[A-Z]/, function (match) {
        return match.toLowerCase();
    });

    // 处理后续的大写字母
    result = result.replace(/[A-Z]/g, function (match) {
        return '-' + match.toLowerCase();
    });

    return result;
}

/**
 * 截断字符串并添加省略号
 * @param {string} str - 输入字符串
 * @param {number} maxLength - 最大长度
 * @param {string} [ellipsis='...'] - 省略号字符，可选
 * @returns {string} 截断后的字符串
 */
export function truncate(str, maxLength, ellipsis = '...') {
    if (!str || !isString(str) || str.length <= maxLength) return str;
    return str.substring(0, maxLength) + ellipsis;
}

/**
 * 转义HTML特殊字符
 * @param {string} str - 输入字符串
 * @returns {string} 转义后的字符串
 */
export function escapeHTML(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

/**
 * 去除换行符、制表符和空格
 */
export function removeWhitespace(str) {
    // 使用正则表达式匹配并去除换行符、制表符和空格
    return str.replace(/[\n\t\s]+/g, '');
}

/**
 * 去除换行符和制表符，但保留单个空格
 */
export function removeWhitespaceExceptSpace(str) {
    // 使用正则表达式匹配并去除换行符和制表符，但保留单个空格
    return str.replace(/[\n\t]+/g, '');
}

/**
 * 判断目标是否json字符串
 * @param {string} str 目标
 * @returns {boolean}
 */
export function isJsonString(str) {
    // 快速排除非字符串或空字符串
    if (typeof str !== 'string' || str.trim().length === 0) {
        return false;
    }

    const trimmed = str.trim();
    const len = trimmed.length;

    // 检查首尾字符是否为 JSON 对象/数组的边界
    if (!((trimmed[0] === '{' && trimmed[len - 1] === '}') ||
        (trimmed[0] === '[' && trimmed[len - 1] === ']'))) {
        return false;
    }

    // 最终通过解析验证
    try {
        JSON.parse(trimmed);
        return true;
    } catch (e) {
        return false;
    }
}