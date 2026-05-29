import request from '@/utils/request/request.js'
/*notes*/


export function getAssetNote(noteId) {
    return request({
        url: `/asset/note/${noteId}`,
        method: 'GET',
        custom: {
            showLoading: false,
        },
        abortConfig: {
            key: `/asset/note/${noteId}`
        }
    })
}
// export function addAssetNote(data) {
//     return request({
//         url: '/asset/note',
//         method: 'POST',
//         data,
//     })
// }
export function updateAssetNote(data) {
    return request({
        url: '/asset/note',
        method: 'PUT',
        data,
    })
}
export function deleteAssetNote(noteId) {
    return request({
        url: `/asset/note/${noteId}`,
        method: 'DELETE',
    })
}
