import {onMounted, onUnmounted, ref, watchEffect} from "vue";
import {useEventListener} from "@/hooks/useEventListener.js";

/**
 * 返回指定元素的尺寸和位置信息
 * @param elementRef - 对DOM元素的引用
 * @returns {Object} - 包含元素尺寸和位置信息的响应式对象
 */
export function useElementSize(elementRef) {
    const width = ref(0);
    const height = ref(0);
    const scrollWidth = ref(0);
    const scrollHeight = ref(0);
    const isInViewport = ref(false);

    const updateSize = () => {
        if (!elementRef.value) return;

        const rect = elementRef.value.getBoundingClientRect();
        width.value = rect.width;
        height.value = rect.height;
        scrollWidth.value = elementRef.value.scrollWidth;
        scrollHeight.value = elementRef.value.scrollHeight;

        // 检查元素是否在视口中
        isInViewport.value = (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
            rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
    };

    // 初始调用一次
    onMounted(updateSize);

    // 使用IntersectionObserver来检测元素是否在视口中
    const useIntersectionObserver = () => {
        if (!elementRef.value || !('IntersectionObserver' in window)) return;

        const observer = new IntersectionObserver(
            (entries) => {
                entries.forEach(entry => {
                    isInViewport.value = entry.isIntersecting;
                });
            },
            { threshold: 0.1 }
        );

        observer.observe(elementRef.value);

        onUnmounted(() => {
            if (elementRef.value) {
                observer.unobserve(elementRef.value);
            }
        });
    };

    // 使用ResizeObserver监听元素尺寸变化
    const useResizeObserver = () => {
        if (!elementRef.value || !('ResizeObserver' in window)) return;

        const resizeObserver = new ResizeObserver(updateSize);
        resizeObserver.observe(elementRef.value);

        onUnmounted(() => {
            resizeObserver.disconnect();
        });
    };

    // 使用scroll事件监听滚动变化
    const useScrollListener = () => {
        if (!elementRef.value) return;

        const handleScroll = () => {
            if (elementRef.value) {
                scrollWidth.value = elementRef.value.scrollWidth;
                scrollHeight.value = elementRef.value.scrollHeight;
                updateSize();
            }
        };

        useEventListener(elementRef, 'scroll', handleScroll);
    };

    // 使用watchEffect确保在元素引用变化时重新初始化
    watchEffect(() => {
        updateSize();
        useIntersectionObserver();
        useResizeObserver();
        useScrollListener();
    });

    return { width, height, scrollWidth, scrollHeight, isInViewport };
}
