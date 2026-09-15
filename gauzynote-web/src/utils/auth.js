import Cookies from 'js-cookie'
import {router} from "@/router/index.js";

const TokenKey = 'Auth-Token'

export function getToken() {
    return Cookies.get(TokenKey)
}

export function setToken(token) {
    return Cookies.set(TokenKey, token)
}

export function removeToken() {
    return Cookies.remove(TokenKey)
}

// 处理未授权状态
export function handleUnauthorized() {
    // 清除过期令牌
    removeToken()
    const event = new CustomEvent('gauzynote:unauthorized', {cancelable: true})
    if (!window.dispatchEvent(event)) {
        return
    }
    const currentRoute = router.currentRoute.value
    if (currentRoute.path === '/login' || currentRoute.path === '/m/login') {
        return
    }
    const isMobileRoute = currentRoute.path === '/m' || currentRoute.path.startsWith('/m/')
    // 跳转到登录页
    router.replace({
        path: isMobileRoute ? '/m/login' : '/login',
        query: isMobileRoute ? {redirect: currentRoute.fullPath} : undefined
    });
}
