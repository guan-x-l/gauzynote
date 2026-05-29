import {ref, useTemplateRef} from "vue";
import localStorageUtil from "@/utils/lib/localStorageUtil.js";
import {domUtils, getSizeConfig} from "@/utils/index.js";

/**
 * 侧边栏拖拽/显隐通用Hook
 */
export const useSidebar = ({
                               minWidth,
                               maxWidth,
                               sizeKey,
                               visibleKey,
                               defaultSize = 0,
                               defaultVisible = true,
                               splitRefKey,
                               handleMoving = () => {
                               },
                               handleMoveStart = () => {
                               },
                               handleMoveEnd = () => {
                               },
                               handleSidebarVisibleChange = () => {
                               },
                           }) => {
    const sidebarVisible = ref(localStorageUtil.get(visibleKey, defaultVisible))
    const size = ref(localStorageUtil.get(sizeKey, defaultSize))
    if (!sidebarVisible.value) {
        size.value = minWidth
    }
    const splitRef = useTemplateRef(splitRefKey)


    function onMoving(e) {
        handleMoving(e)
        localStorageUtil.set(visibleKey, sidebarVisible.value)
        localStorageUtil.set(sizeKey, getSizeConfig(size.value).size)
    }

    function onMoveStart() {
        handleMoveStart()

        domUtils('.layout').setStyle({'user-select': 'none', 'pointer-events': 'none'})
        domUtils(document.body).setStyle({cursor: 'e-resize'})
    }

    function onMoveEnd() {
        handleMoveEnd()
        domUtils('.layout').removeStyle(['user-select', 'pointer-events'])
        domUtils(document.body).removeStyle('cursor')
        localStorageUtil.set(sizeKey, getSizeConfig(size.value).size)
    }

    function onSidebarVisibleChange() {
        sidebarVisible.value = !sidebarVisible.value
        handleSidebarVisibleChange(sidebarVisible.value)
        localStorageUtil.set(visibleKey, sidebarVisible.value)
        if (!sidebarVisible.value) {
            size.value = minWidth
        } else {
            size.value = Math.max(localStorageUtil.get(sizeKey, defaultSize), defaultSize)
            localStorageUtil.set(sizeKey, getSizeConfig(size.value).size)
        }
    }

    return {
        minWidth,
        maxWidth,
        defaultSize,
        sidebarVisible,
        size,
        splitRef,
        onMoving,
        onMoveStart,
        onMoveEnd,
        onSidebarVisibleChange
    }
}
