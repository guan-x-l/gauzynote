import {ref, onUnmounted, watch, onMounted} from 'vue'
import {useEventListener} from "@/hooks/useEventListener.js";

export function useDrag(targetRef, options = {}) {
    const {
        axis = 'both', // 'x', 'y', 'both'
        onDragStart = () => {},
        onDrag = () => {},
        onDragEnd = () => {},
        disabled = ref(false)
    } = options

    const isDragging = ref(false)
    const startX = ref(0)
    const startY = ref(0)
    const offsetX = ref(0)
    const offsetY = ref(0)
    const lastX = ref(0)
    const lastY = ref(0)

    const mousemoveEv = useEventListener(document, 'mousemove', handleMouseMove)
    const mouseupEv = useEventListener(document, 'mouseup', handleMouseUp)

    onMounted(()=>{
        mousemoveEv.stop()
        mouseupEv.stop()
    })
    function handleMouseDown (e) {
        if (disabled.value) return
        if (e.button !== 0) return // 只处理左键

        const target = targetRef.value || targetRef
        if (!target) return

        isDragging.value = true
        const rect = target.getBoundingClientRect()
        startX.value = e.clientX - rect.left
        startY.value = e.clientY - rect.top
        lastX.value = e.clientX
        lastY.value = e.clientY

        mousemoveEv.restart()
        mouseupEv.restart()

        onDragStart({
            x: e.clientX,
            y: e.clientY,
            target: e.target
        })
    }

    function handleMouseMove (e) {
        if (!isDragging.value) return

        const deltaX = e.clientX - lastX.value
        const deltaY = e.clientY - lastY.value

        if (axis === 'x' || axis === 'both') {
            offsetX.value += deltaX
        }

        if (axis === 'y' || axis === 'both') {
            offsetY.value += deltaY
        }

        lastX.value = e.clientX
        lastY.value = e.clientY

        onDrag({
            x: e.clientX,
            y: e.clientY,
            deltaX,
            deltaY,
            offsetX: offsetX.value,
            offsetY: offsetY.value,
            target: targetRef.value || targetRef
        })
    }

    function handleMouseUp (e) {
        if (!isDragging.value) return

        isDragging.value = false

        mousemoveEv.stop()
        mouseupEv.stop()

        onDragEnd({
            x: e.clientX,
            y: e.clientY,
            offsetX: offsetX.value,
            offsetY: offsetY.value,
            target: targetRef.value || targetRef
        })
    }

    useEventListener(targetRef, 'mousedown', handleMouseDown)

    // 监听禁用状态变化
    watch(disabled, (newVal) => {
        if (newVal && isDragging.value) {
            handleMouseUp({
                clientX: lastX.value,
                clientY: lastY.value
            })
        }
    })

    return {
        isDragging,
        offsetX,
        offsetY,
        stopDrag: () => {
            if (isDragging.value) {
                handleMouseUp({
                    clientX: lastX.value,
                    clientY: lastY.value
                })
            }
        }
    }
}