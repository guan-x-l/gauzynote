import request from "@/utils/request/request.js";

/**
 * 查询字典数据列表（返回系统公共数据 + 当前用户私有数据，包含 canOperate 字段）
 */
export function getDictDataList(params) {
    return request({
        url: '/system/dict/data/list',
        method: 'GET',
        params
    })
}

/**
 * 根据字典数据主键获取详情
 */
export function getDictDataById(dictCode) {
    return request({
        url: `/system/dict/data/${dictCode}`,
        method: 'GET'
    })
}

/**
 * 根据字典类型获取启用状态的字典数据列表（返回全局数据 + 当前登录用户私有数据）
 */
export function getDictDataByType(dictType) {
    return request({
        url: `/system/dict/data/type/${dictType}`,
        method: 'GET'
    })
}

/**
 * 新增字典数据（admin 可新增 userId 为空/本人，非 admin 仅新增本人）
 */
export function addDictData(data) {
    return request({
        url: '/system/dict/data',
        method: 'POST',
        data
    })
}

/**
 * 修改字典数据（admin 可修改公共/本人，非 admin 仅可修改本人）
 */
export function updateDictData(data) {
    return request({
        url: '/system/dict/data',
        method: 'PUT',
        data
    })
}

/**
 * 删除字典数据（admin 可删除公共/本人，非 admin 仅可删除本人）
 */
export function deleteDictData(dictCode) {
    return request({
        url: `/system/dict/data/${dictCode}`,
        method: 'DELETE'
    })
}
