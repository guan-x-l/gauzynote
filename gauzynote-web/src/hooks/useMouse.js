import {ref} from "vue";
import {useEventListener} from "@/hooks/useEventListener.js";

/**
 * 跟踪鼠标位置
 * @returns {Object} - 包含鼠标X和Y坐标的响应式对象
 */
export function useMousePosition() {
    const x = ref(0);
    const y = ref(0);

    const updateMouse = (e) => {
        x.value = e.pageX;
        y.value = e.pageY;
    };

    useEventListener(window, 'mousemove', updateMouse);

    return { x, y };
}
