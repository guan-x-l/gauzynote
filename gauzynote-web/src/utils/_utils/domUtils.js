import {unref} from "vue";
import {isArray, isElement, isFunction} from "./is.js";

export const NOOP = () => {
    return undefined;
};
export const isServerRendering = (() => {
    try {
        return !(typeof window !== 'undefined' && document !== undefined);
    } catch (e) {
        return true;
    }
})();

/**
 * 根据目标获取DOM元素
 * 支持多种目标选择方式：选择器字符串、DOM元素、函数或响应式引用
 * @inner
 * @param {string|Element|Function|import('vue').Ref|useTemplateRef} target - 目标元素选择器、元素本身、返回元素的函数或Vue ref
 * @returns {Element|null} 获取到的DOM元素，若无效则返回null
 */
function getElement(target) {
    let currentEl = null;

    if (typeof target === 'function') {
        currentEl = target();
    } else if (typeof target === 'string') {
        currentEl = document.querySelector(target);
    } else if (isElement(target)) {
        currentEl = target;
    } else {
        currentEl = unref(target);
    }
    // 验证获取到的元素是否有效
    if (!isElement(currentEl)) {
        return null;
    }
    return currentEl;
}

/**
 * 返回元素的attrs字符串
 * @param {[[string, string]]} attrs
 */
export function getAttrsStr(attrs) {
    return attrs?.map(([key, value]) => {
        // 对属性值进行转义（避免特殊字符导致 HTML 错误）
        const escapedValue = String(value).replace(/"/g, '&quot;');
        return `${key}="${escapedValue}"`;
    }).join(' ');
}

/**
 * @param {string|Element|Function|import('vue').Ref|useTemplateRef} target
 */
export const domUtils = (target) => {
    const el = getElement(target);


    const context = {
        /**
         * 添加类名
         * @param {string|Array.<string>} classNames - 类名或类名数组
         */
        addClass(classNames) {
            if (el) {
                const classesToAdd = isArray(classNames) ? classNames : [classNames];
                el.classList.add(...classesToAdd);
            }
            return context
        },
        /**
         * 移除类名
         * @param {string|Array.<string>} classNames - 类名或类名数组
         */
        removeClass(classNames) {
            if (el) {
                const classesToRemove = isArray(classNames) ? classNames : [classNames];
                el.classList.remove(...classesToRemove);
            }
            return context
        },
        /**
         * 切换类名
         * @param {string} className - 类名
         * @param {boolean} [force] - 可选参数，强制添加或移除类名
         */
        toggleClass(className, force) {
            if (el) {
                el.classList.toggle(className, force);
            }
            return context
        },
        /**
         * 检查元素是否包含某个类名
         * @param {string} className - 类名
         * @returns {boolean} 如果包含返回true，否则返回false
         */
        hasClass(className) {
            return el?.classList.contains(className) || false;
        },
        /**
         * 设置元素样式
         * @param {import('vue').CSSProperties} styles - 样式对象
         */
        setStyle(styles) {
            if (el) {
                Object.entries(styles).forEach(([key, value]) => {
                    el.style[key] = value;
                });
            }
            return context
        },
        /**
         * 移除元素的指定样式
         * @param {string|Array.<string>} styleNames - 样式名称或样式名称数组
         */
        removeStyle(styleNames) {
            if (el) {
                const stylesToRemove = isArray(styleNames) ? styleNames : [styleNames];
                stylesToRemove.forEach((styleName) => {
                    el.style.removeProperty(styleName);
                    // el.style.removeProperty(stringUtils.camelToKebab(styleName));
                });
            }
            return context
        },
        /**
         * 同 Element setAttribute
         * @param name
         * @param value
         */
        setAttribute(name, value) {
            el.setAttribute(name, value);
            return context
        },
        /**
         * 后续查询
         * @param {string|Element|Function|import('vue').Ref|useTemplateRef} target
         */
        query(target) {
            return domUtils(el.querySelector(target));
        },
        /**
         * 后续查询
         * @param {string|Element|Function|import('vue').Ref|useTemplateRef} target
         */
        queryAll(target) {
            return domUtils(el.querySelectorAll(target));
        },
        /**
         * 获取原生元素
         * @param [callback] 回调
         * @returns {*|Element|null}
         */
        get(callback) {
            return isFunction(callback) ? callback(el) : el
        },
        /**
         * 获取原生元素getBoundingClientRect
         * @returns {DOMRect|{}}
         */
        getBounding() {
            return el?.getBoundingClientRect() || {}
        },
        // 判断元素是否有效
        isValid() {
            return isElement(el);
        }
    }
    return context
}

/**
 * DOM事件绑定方法
 * @param {HTMLElement | Window} element - 事件绑定的目标元素（HTMLElement/Window）
 * @param {keyof HTMLElementEventMap} event - 要绑定的事件名（如click/input/scroll，遵循原生事件类型）
 * @param {(ev: HTMLElementEventMap[keyof HTMLElementEventMap])=>void} handler - 事件处理函数，参数为原生事件对象（匹配对应事件的类型）
 * @param {boolean | AddEventListenerOptions} [options=false] - 事件监听配置项，可选，默认false
 * @returns {void}
 */
export const on = (() => {
    if (isServerRendering) {
        return NOOP;
    }
    return (element, event, handler, options = false) => {
        element.addEventListener(event, handler, options);
    };
})();


/**
 * DOM事件解绑方法
 * @param {HTMLElement | Window} element - 事件解绑的目标元素（与绑定时一致）
 * @param {keyof HTMLElementEventMap} type - 要解绑的事件名（与绑定时一致）
 * @param {Function} handler - 要解绑的事件处理函数（与绑定时为同一个引用）
 * @param {boolean | EventListenerOptions} [options=false] - 事件监听配置项（与绑定时一致）
 * @returns {void}
 */
export const off = (() => {
    if (isServerRendering) {
        return NOOP;
    }
    return (element, type, handler, options = false) => {
        element.removeEventListener(type, handler, options);
    };
})();

/**
 * 判断【单行文本】是否被 CSS 省略（显示 ...）
 * @param {HTMLElement|string} el - DOM 元素 或 选择器字符串
 * @returns {boolean} true=已省略，false=未省略
 */
export function isTextEllipsis(el) {
    // 支持传入选择器
    if (typeof el === 'string') {
        el = document.querySelector(el);
    }
    if (!el || !(el instanceof HTMLElement)) return false;

    const style = getComputedStyle(el);

    // 校验是否配置了单行省略 CSS
    const isEllipsisStyle =
        style.whiteSpace === 'nowrap' &&
        style.overflow === 'hidden' &&
        style.textOverflow === 'ellipsis';

    if (!isEllipsisStyle) return false;

    // 精确判断：内容宽度 > 可视宽度（解决小数精度问题）
    const viewWidth = el.getBoundingClientRect().width;
    const contentWidth = el.scrollWidth;

    // 加 0.5px 容错，避免四舍五入误判
    return contentWidth > viewWidth + 0.5;
}

/**
 * 判断【多行文本】是否被 CSS 省略（显示 ...）
 * @param {HTMLElement|string} el - DOM 元素 或 选择器字符串
 * @returns {boolean} true=已省略，false=未省略
 */
export function isMultiLineEllipsis(el) {
    if (typeof el === 'string') {
        el = document.querySelector(el);
    }
    if (!el || !(el instanceof HTMLElement)) return false;

    const style = getComputedStyle(el);

    // 校验是否配置了多行省略 CSS
    const hasLineClamp = style.webkitLineClamp !== 'none';
    if (!hasLineClamp) return false;

    // 精确判断：内容高度 > 可视高度
    const viewHeight = el.getBoundingClientRect().height;
    const contentHeight = el.scrollHeight;

    return contentHeight > viewHeight + 0.5;
}