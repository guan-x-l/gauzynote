import request from '@/utils/request/request.js'


/**
 * @returns {Promise<Result<StorageSpace>>}
 */
export function getStorageSpace() {
    return request({
        url: '/upload/getStorageSpace',
        method: 'GET',
    })
}

export function uploadImage(data) {
    return request({
        url: '/upload/image',
        method: 'POST',
        data: data,
        headers: {
            'Content-Type': 'multipart/form-data'
        },
    })
}

export function uploadNetworkImage(data) {
    return request({
        url: '/upload/networkImage',
        method: 'POST',
        data: data,
        headers: {
            'Content-Type': 'multipart/form-data'
        },
    })
}


