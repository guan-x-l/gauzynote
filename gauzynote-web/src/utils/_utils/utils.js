import {isNumber, isString} from "@/utils/index.js";


/**
 * 函数防抖
 * @param fn 回调函数
 * @param delay 等待时长
 * @returns {(function(...[*]): void)}
 */
export function debounce(fn, delay) {
    let timer;
    return function (...args) {
        if (timer) {
            clearTimeout(timer);
        }
        timer = setTimeout(() => {
            fn.apply(this, args);
        }, delay);
    }
}

/**
 * 函数节流
 * @param fn 回调函数
 * @param delay 等待时长
 * @returns {(function(...[*]): void)|*}
 */
export function throttle(fn, delay) {
    let last = 0 // 上次触发时间
    return (...args) => {
        const now = Date.now()
        if (now - last > delay) {
            last = now
            fn.apply(this, args)
        }
    }
}


/**
 * 复制目标元素的内容
 * @param selectors{String} 元素选择
 * @param content{String | Number} 要复制的内容
 * @returns {boolean} 是否复制成功
 */
export function copyContentBySelectors (selectors, content){
    try {
        const tempElement = document.createElement('textarea');
        tempElement.value = content || document.querySelector(selectors).innerText;
        document.body.appendChild(tempElement);

        tempElement.select(); // 选择文本内容
        document.execCommand('copy'); // 复制选中的文本内容

        document.body.removeChild(tempElement); // 移除临时元素

        const range = document.createRange();
        range.selectNode(document.querySelector(selectors));
        window.getSelection().removeAllRanges(); // 清除之前的选区
        window.getSelection().addRange(range); // 选中新的区域
        return true
    } catch (e) {
        return false
    }
}

/**
 * @param {number | string} size
 */
export function getSizeConfig(size) {
    const numberSize = isString(size) ? parseFloat(size) : size;
    let unit = '';

    if (isNumber(size) || String(numberSize) === size) {
        unit = numberSize > 1 ? 'px' : '%';
    } else {
        unit = 'px';
    }

    return {
        size: numberSize,
        unit,
        isPx: unit === 'px',
    };
}

/**
 * 归一化尺寸值，数字自动转为 px。
 */
export function normalizeSize(sizeValue) {
    if (sizeValue == null || sizeValue === '') {
        return undefined
    }
    return  isNumber(sizeValue) ? `${sizeValue}px` : String(sizeValue)
}