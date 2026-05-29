import { ref } from 'vue';
import {useEventListener} from "@/hooks/useEventListener.js";

/**
 * 鼠标控制元素滚动的自定义Hook
 * @param elementRef - 要滚动的元素的ref引用
 * @param sensitivity - 滚动灵敏度，值越大滚动越快，默认1.5
 * @returns 包含元素滚动状态的对象
 * @example
 * const elementRef = ref(null)
 * useMouseScrollControl(elementRef, 1.5)
 */
export function useMouseScrollControl(
    elementRef,
    sensitivity = 1.5
) {
    // 记录上一次鼠标Y坐标
    const lastY = ref(0);
    // 记录是否正在跟踪鼠标移动
    const isTracking = ref(false);
    // 记录元素当前滚动位置
    const scrollTop = ref(0);

    // 鼠标进入元素时开始跟踪
    const handleMouseEnter = (e) => {
        lastY.value = e.clientY;
        isTracking.value = true;

        // init scrollTop
        const trueScrollHeight = elementRef.value.scrollHeight - elementRef.value.offsetHeight
        elementRef.value.scrollTop = e.offsetY /  elementRef.value.offsetHeight * trueScrollHeight
    };

    // 鼠标离开元素时停止跟踪
    const handleMouseLeave = () => {
        isTracking.value = false;
    };

    // 鼠标在元素内移动时处理滚动
    const handleMouseMove = (e) => {
        if (!isTracking.value || !elementRef.value) return;

        const currentY = e.clientY;
        const deltaY = currentY - lastY.value;

        // 根据鼠标移动方向计算滚动距离
        // 鼠标向上移动(deltaY为负)，元素向上滚动(scrollTop减小)
        // 鼠标向下移动(deltaY为正)，元素向下滚动(scrollTop增大)
        const scrollDelta = deltaY * sensitivity;
        const newScrollTop = elementRef.value.scrollTop + scrollDelta;

        // 更新滚动位置并限制在有效范围内
        const clampedScrollTop = Math.max(
            0,
            Math.min(newScrollTop, elementRef.value.scrollHeight - elementRef.value.clientHeight)
        );

        elementRef.value.scrollTop = clampedScrollTop;
        scrollTop.value = clampedScrollTop;

        // 更新上一次鼠标位置
        lastY.value = currentY;
    };

    useEventListener(elementRef, 'mouseenter', handleMouseEnter)
    useEventListener(elementRef, 'mouseleave', handleMouseLeave)
    useEventListener(elementRef, 'mousemove', handleMouseMove)

    return {
        scrollTop,
        isTracking
    };
}
