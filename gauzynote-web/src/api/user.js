import request from "@/utils/request/request.js";

export function getUserList() {
    return request({
        url: '/system/user/list',
        method: 'GET',
    })
}
export function addUser(data) {
    return request({
        url: '/system/user',
        method: 'POST',
        data
    })
}
export function updateUserPwd(data) {
    return request({
        url: '/system/user/editPwd',
        method: 'PUT',
        data
    })
}
export function initializePassword(data) {
    return request({
        url: '/system/user/initializePassword',
        method: 'PUT',
        data
    })
}
export function updateUser(data) {
    return request({
        url: '/system/user',
        method: 'PUT',
        data
    })
}
export function updateUserOfAdmin(data) {
    return request({
        url: '/system/user/editOfAdmin',
        method: 'PUT',
        data
    })
}
export function updateUserStatus(data) {
    return request({
        url: '/system/user/updateStatus',
        method: 'PUT',
        data
    })
}
export function deleteUser(id) {
    return request({
        url: `/system/user/${id}`,
        method: 'DELETE',
    })
}
