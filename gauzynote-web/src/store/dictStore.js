import {ref} from "vue";
import {getDictDataByType} from "@/api/dictData.js";

/** @type {import('vue').Ref<Record<string, Array<Object>>>} */
const dictCacheMap = ref({});
/** @type {Map<string, Promise<Array<Object>>>} */
const pendingDictRequestMap = new Map();

/**
 * 统一格式化字典类型，避免大小写或空白导致缓存命中失败。
 */
function normalizeDictType(dictType) {
    return (dictType || '-').trim()
}

/**
 * 获取指定字典类型的缓存列表。
 */
function getDictItems(dictType) {
    const normalizedType = normalizeDictType(dictType)
    if (!normalizedType) {
        return []
    }
    return dictCacheMap.value[normalizedType] || []
}

/**
 * 根据字典值查找对应字典项。
 */
function findDictItem(dictType, dictValue) {
    const currentValue = String(dictValue ?? '')
    return getDictItems(dictType).find(item => String(item?.dictValue ?? '') === currentValue)
}

/**
 * 根据字典值获取展示标签。
 */
function getDictLabel(dictType, dictValue, fallbackLabel = '-') {
    const dictItem = findDictItem(dictType, dictValue)
    return dictItem?.dictLabel || fallbackLabel
}

/**
 * 加载指定字典类型并写入缓存，支持强制刷新。
 */
async function loadDictByType(dictType, forceRefresh = false) {
    const normalizedType = normalizeDictType(dictType)
    if (!normalizedType) {
        return []
    }

    if (!forceRefresh && dictCacheMap.value[normalizedType]?.length) {
        return dictCacheMap.value[normalizedType]
    }

    if (!forceRefresh && pendingDictRequestMap.has(normalizedType)) {
        return pendingDictRequestMap.get(normalizedType)
    }

    const requestPromise = getDictDataByType(normalizedType)
        .then(res => {
            const dictItems = Array.isArray(res?.data) ? res.data : []
            dictCacheMap.value[normalizedType] = dictItems
            return dictItems
        })
        .finally(() => {
            pendingDictRequestMap.delete(normalizedType)
        })

    pendingDictRequestMap.set(normalizedType, requestPromise)
    return requestPromise
}

/**
 * 批量预加载多个字典类型，减少页面切换时重复请求。
 */
async function loadDictList(dictTypeList = [], forceRefresh = false) {
    const taskList = dictTypeList
        .map(item => normalizeDictType(item))
        .filter(Boolean)
        .map(dictType => loadDictByType(dictType, forceRefresh))
    return Promise.all(taskList)
}

/**
 * 清理指定字典类型缓存，或清空全部缓存。
 */
function clearDictCache(dictType) {
    const normalizedType = normalizeDictType(dictType)
    if (!normalizedType) {
        dictCacheMap.value = {}
        pendingDictRequestMap.clear()
        return
    }
    if (dictCacheMap.value[normalizedType]) {
        delete dictCacheMap.value[normalizedType]
    }
    pendingDictRequestMap.delete(normalizedType)
}

export const useDictStore = () => {
    return {
        dictCacheMap,
        getDictItems,
        findDictItem,
        getDictLabel,
        loadDictByType,
        loadDictList,
        clearDictCache,
    }
}
