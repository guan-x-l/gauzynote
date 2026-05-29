import request from "@/utils/request/request.js";

/**
 * 查询字典类型列表（返回系统公共数据 + 当前用户私有数据，包含 canOperate 字段）
 */
export function getDictTypeList(params) {
    return request({
        url: '/system/dict/type/list',
        method: 'GET',
        params
    })
}

/**
 * 根据字典类型主键获取详情
 */
export function getDictTypeById(dictId) {
    return request({
        url: `/system/dict/type/${dictId}`,
        method: 'GET'
    })
}

/**
 * 获取启用状态字典类型列表（返回系统公共数据 + 当前用户私有数据）
 */
export function getDictTypeOptions() {
    return request({
        url: '/system/dict/type/optionselect',
        method: 'GET'
    })
}

/**
 * 新增字典类型（admin 可新增 userId 为空/本人，非 admin 仅新增本人）
 */
export function addDictType(data) {
    return request({
        url: '/system/dict/type',
        method: 'POST',
        data
    })
}

/**
 * 修改字典类型（admin 可修改公共/本人，非 admin 仅可修改本人）
 */
export function updateDictType(data) {
    return request({
        url: '/system/dict/type',
        method: 'PUT',
        data
    })
}

/**
 * 删除字典类型（admin 可删除公共/本人，非 admin 仅可删除本人）
 */
export function deleteDictType(dictId) {
    return request({
        url: `/system/dict/type/${dictId}`,
        method: 'DELETE'
    })
}
