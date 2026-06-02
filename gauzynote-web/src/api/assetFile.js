import request from '@/utils/request/request.js'
/*file*/


export function getAssetFile(fileId) {
    return request({
        url: `/asset/files/${fileId}`,
        method: 'GET',
    })
}

export function updateAssetFile(data) {
    return request({
        url: '/asset/files',
        method: 'PUT',
        data,
    })
}
export function deleteAssetFile(fileId) {
    return request({
        url: `/asset/files/${fileId}`,
        method: 'DELETE',
    })
}
