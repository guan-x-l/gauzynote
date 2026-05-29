import request from '@/utils/request/request.js'

export function getAssetTaskList(params) {
    return request({
        url: '/asset/task/list',
        method: 'GET',
        params
    })
}

export function getAssetTask(taskId) {
    return request({
        url: `/asset/task/${taskId}`,
        method: 'GET'
    })
}

export function addAssetTask(data) {
    return request({
        url: '/asset/task',
        method: 'POST',
        data
    })
}

export function updateAssetTask(data) {
    return request({
        url: '/asset/task',
        method: 'PUT',
        data
    })
}

export function updateAssetTaskSort(data, sortField = 'sort') {
    return request({
        url: '/asset/task/sort',
        method: 'PUT',
        params: {
            sortField
        },
        data
    })
}

export function deleteAssetTask(taskId) {
    return request({
        url: `/asset/task/${taskId}`,
        method: 'DELETE'
    })
}
