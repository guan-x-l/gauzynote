import request from '@/utils/request/request.js'

export function getRecycleBinList() {
    return request({
        url: '/system/recycleBin/list',
        method: 'GET'
    })
}

export function restoreRecycleBin(recycleId) {
    return request({
        url: `/system/recycleBin/restore/${recycleId}`,
        method: 'POST'
    })
}

export function permanentDeleteRecycleBin(recycleId) {
    return request({
        url: `/system/recycleBin/${recycleId}`,
        method: 'DELETE'
    })
}
