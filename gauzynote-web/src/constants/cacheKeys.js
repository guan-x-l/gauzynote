/**
 * 缓存key管理中心
 * 规则：
 * 1. 按业务模块分组
 * 2. 使用命名空间避免冲突
 * 3. 动态key通过函数生成
 */
import localStorageUtil from "@/utils/lib/localStorageUtil.js";

// 基础命名空间（可根据项目名称自定义）
export const NAMESPACE = 'gauzy_note_';

export const storageVersion = 4;

export const STORAGE_VERSION = `${NAMESPACE}storage_v`;


// 通用模块
export const commonKeys = {
    // 左侧边栏 宽度
    BOUNDARY_LEFT_WIDTH: `${NAMESPACE}boundary_left_width`,
    // 左侧边栏 显示状态
    BOUNDARY_LEFT_VISIBLE: `${NAMESPACE}boundary_left_visible`,
    // 右侧边栏 宽度
    BOUNDARY_RIGHT_WIDTH: `${NAMESPACE}boundary_right_width`,
    // 右侧边栏 显示状态
    BOUNDARY_RIGHT_VISIBLE: `${NAMESPACE}boundary_right_visible`,
    // 右侧边栏 显示状态
    THEME: `${NAMESPACE}theme`,
    // language
    LANGUAGE: `${NAMESPACE}language`,

    TAB_LIST:  () =>  `${NAMESPACE}table_list_${localStorageUtil.get(userKeys.username)}`,
    ACTIVE_TAB_ID:  () =>  `${NAMESPACE}active_tab_id_${localStorageUtil.get(userKeys.username)}`,

    // 动态key（带参数的key）
    userLastLogin: (userId) => `${NAMESPACE}user_last_login_${userId}`,
};
// 用户模块
export const userKeys = {
    username: `${NAMESPACE}username`,
    INFO: `${NAMESPACE}user_info`,
    TOKEN: `${NAMESPACE}user_token`,
    PERMISSIONS: `${NAMESPACE}user_permissions`,
};


// 生成某模块下所有key的前缀（用于批量操作）
export const getModulePrefix = (module) => {
    const prefixMap = {
        user: `${NAMESPACE}user_`,
        product: `${NAMESPACE}product_`,
        common: `${NAMESPACE}common_`,
    };
    return prefixMap[module] || '';
};


// 批量删除某个模块的所有缓存
// const clearModuleCache = (module) => {
//     const prefix = getModulePrefix(module);
//     if (!prefix) return;
//
//     // 遍历所有key，删除指定前缀的缓存
//     Object.keys(localStorage).forEach(key => {
//         if (key.startsWith(prefix)) {
//             storage.remove(key);
//         }
//     });
// };

// 使用示例：清除所有商品相关缓存
// clearModuleCache('product');

// 清除用户相关缓存（保留token）
// const clearUserCacheExceptToken = () => {
//     const prefix = getModulePrefix('user');
//     Object.keys(localStorage).forEach(key => {
//         if (key.startsWith(prefix) && key !== userKeys.TOKEN) {
//             storage.remove(key);
//         }
//     });
// };
