import request from '@/utils/request/request.js'

// 登录方法
export function login(data) {
    return request({
        url: '/login',
        method: 'POST',
        data: data,
        custom: {
            requireToken: false,
            showLoading: false,
        }
    })
}

// 获取用户详细信息
export function getInfo() {
    return request({
        url: '/getInfo',
        method: 'GET'
    })
}

// 退出方法
export function logout() {
    return request({
        url: '/logout',
        method: 'POST',
    })
}
