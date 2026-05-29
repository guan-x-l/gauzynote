
/**
 * 在组件生命周期内管理事件监听器
 * @param {EventTarget} target - 事件目标，可以是DOM元素、window等
 * @param {string} event - 事件名称
 * @param {Function} callback - 事件回调函数
 * @param {Object} options - 事件监听器选项
 */
/*
export function useEventListener(target, event, callback, options = {}) {
    onMounted(() => {
        console.log(event)
        const element = target.value || target;
        console.log(element)
        console.log(document.body.tagName)
        console.log(element.body.tagName)
        element.addEventListener && element?.addEventListener(event, callback, options);
    });

    onUnmounted(() => {
        const element = target.value || target;
        element?.removeEventListener && element.removeEventListener(event, callback, options);
    });
}
*/


import { onMounted, onUnmounted, watch, unref, isRef } from 'vue'

/**
 * 用于在元素上绑定事件监听器的 React 钩子，支持静态和动态目标元素
 *
 * 自动处理事件的绑定与解绑，在组件卸载时清除事件监听，
 * 支持 ref 动态目标和静态 DOM 元素，当动态目标变化时会自动重新绑定事件
 *
 * @template {keyof HTMLElementEventMap} T - 事件类型的泛型，限制为标准 HTML 事件
 * @param {HTMLElement | Document | Window | import('vue').Ref<HTMLElement> | (() => HTMLElement | null) | null} target
 *        事件监听的目标元素，可以是：
 *        - 直接的 DOM 元素
 *        - React RefObject（通过 useRef 创建）
 *        - 返回 DOM 元素的函数
 *        - null（此时不会绑定事件）
 * @param {T | 'visibilitychange'} event - 要监听的事件名称（如 'click'、'scroll' 等）
 * @param {(event: HTMLElementEventMap[T]) => void} callback
 *        事件触发时执行的回调函数，接收事件对象作为参数
 * @param {AddEventListenerOptions} [options={}]
 *        事件监听选项，同 addEventListener 的第三个参数，包含：
 *        - capture: 是否在捕获阶段触发
 *        - once: 是否只触发一次
 *        - passive: 是否不阻止默认行为
 * @returns {{stop: function(),restart:Function}}
 * 包含两个方法的对象：
 *     - stop: 手动停止事件监听
 *     - restart: 重新启动事件监听，可传入新的目标元素（默认使用初始 target）
 *
 * @example
 * // 绑定到静态元素
 * useEventListener(document, 'click', handleClick)
 *
 * @example
 * // 绑定到 ref 目标
 * const divRef = useRef(null)
 * useEventListener(divRef, 'scroll', handleScroll)
 *
 * @example
 * // 绑定到动态目标
 * useEventListener(() => document.querySelector('.active'), 'mouseover', handleMouseOver)
 */
export function useEventListener(
    target,
    event,
    callback,
    options = {}
) {
    let cleanup = () => {}
    let isActive = false

    const setup = (el) => {
        // if (!el || isActive) return

        start(el)
    }
    const start = (el) => {
        if (!el || isActive) return
        const handler = callback
        const realOptions = options

        el.addEventListener(event, handler, realOptions)
        cleanup = () => {
            if (el) {
                el.removeEventListener(event, handler, realOptions)
            }
        }
        isActive = true
    }

    const stop = () => {
        cleanup()
        isActive = false
    }
    const restart = (el = target) => {
        start(el)
    }

    if (isRef(target) || typeof target === 'function') {
        // 动态目标 (ref 或函数)
        const stopWatch = watch(
            () => unref(target),
            (el) => {
                stop()
                setup(el)
            },
            { immediate: true }
        )

        onUnmounted(() => {
            stop()
            stopWatch()
        })
    } else {
        // 静态目标 (直接值)
        onMounted(() => setup(target))
        onUnmounted(stop)
    }

    return {
        stop,
        restart
    }
}