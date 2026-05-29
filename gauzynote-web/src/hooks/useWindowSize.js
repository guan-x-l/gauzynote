import {onMounted, ref} from "vue";
import {useEventListener} from "@/hooks";


/**
 * 返回窗口的尺寸信息和可见性状态
 * 可选择性监听窗口大小变化和页面可见性变化事件
 *
 * @param {Object} [options] - 配置选项
 * @param {boolean} [options.watchSize=true] - 是否监听窗口尺寸变化
 * @param {boolean} [options.watchVisibility=true] - 是否监听页面可见性变化
 */
export const useWindowSize = (options) => {
    const {
        watchSize = true,
        watchVisibility = true
    } = options;

    /**
     * 窗口宽度（单位px）的响应式引用
     */
    const width = ref(window.innerWidth);
    /**
     * 窗口高度（单位px）的响应式引用
     */
    const height = ref(window.innerHeight);
    /**
     * 页面可见性状态的响应式引用
     * 基于document.hidden属性，true表示页面可见，false表示页面被隐藏（如切换到其他标签页）
     */
    const domHidden = ref(true);

    /**
     * 更新窗口尺寸的函数
     * 从window对象获取最新的innerWidth和innerHeight并更新响应式引用
     */
    const updateSize = () => {
        width.value = window.innerWidth;
        height.value = window.innerHeight;
    };

    /**
     * 处理页面可见性变化的函数
     * 根据document.hidden属性更新domHidden状态
     */
    const handleVisibilityChange = () => {
        domHidden.value = !document.hidden;
    };

    onMounted(()=>{
        handleVisibilityChange()
    })

    // 条件性添加事件监听
    if (watchSize) {
        useEventListener(window, 'resize', updateSize);
    }
    if (watchVisibility) {
        useEventListener(document, 'visibilitychange', handleVisibilityChange);
    }

    return {width, height, domHidden};
}