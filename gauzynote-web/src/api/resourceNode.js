import request from '@/utils/request/request.js'

/*资源节点*/

export function getResourceNode() {
    return request({
        url: '/system/resourceNode',
        method: 'GET',
    })
}

export function addResourceNode(data) {
    return request({
        url: '/system/resourceNode',
        method: 'POST',
        data,
    })
}

export function addNodeAndNote(data) {
    return request({
        url: '/system/resourceNode/addNodeAndNote',
        method: 'POST',
        data,
    })
}


export function updateResourceNode(data) {
    return request({
        url: '/system/resourceNode',
        method: 'PUT',
        data,
    })
}
export function updateResourceNodeName(data) {
    return request({
        url: '/system/resourceNode/updateNodeName',
        method: 'PUT',
        data,
    })
}
export function updateResourceNodeParentId(data) {
    return request({
        url: '/system/resourceNode/updateNodeParentId',
        method: 'PUT',
        data,
    })
}
export function updateResourceNodeNameByNote(data) {
    return request({
        url: '/system/resourceNode/updateNodeNameByNote',
        method: 'PUT',
        data,
    })
}

export function deleteResourceNode(id) {
    return request({
        url: `/system/resourceNode/${id}`,
        method: 'DELETE',
    })
}
