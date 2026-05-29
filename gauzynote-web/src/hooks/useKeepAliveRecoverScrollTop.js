import {onActivated} from "vue";
import {useEventListener} from "@/hooks/useEventListener.js";

export function useKeepAliveRecoverScrollTop(recoverScrollRef) {
    let scrollTop = 0

    onActivated(() => {
        recoverScrollRef.value.scrollTop = scrollTop
    })

    useEventListener(recoverScrollRef, 'scroll', (e) => scrollTop = e.target.scrollTop)

    return {recoverScrollRef};
}
