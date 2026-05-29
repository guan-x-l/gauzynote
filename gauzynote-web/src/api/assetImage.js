import request from '@/utils/request/request.js'
/*image*/


export function getAssetImage(noteId) {
    return request({
        url: `/asset/images/${noteId}`,
        method: 'GET',
    })
}

export function updateAssetImage(data) {
    return request({
        url: '/asset/images',
        method: 'PUT',
        data,
    })
}
export function deleteAssetImage(noteId) {
    return request({
        url: `/asset/images/${noteId}`,
        method: 'DELETE',
    })
}
