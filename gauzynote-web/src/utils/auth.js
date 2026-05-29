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
    // 跳转到登录页
    router.replace({
        path: '/login',
        // query: { redirect: window.location.pathname }
    });
}