import {getAssetImage} from "@/api/assetImage.js";
import {Message} from "@/components/index.js";
import {$t} from "@/locales";
/**
 * 根据id获取图片信息,
 * @param id 图片资源id
 * @returns {Promise<Object|null>} 返回对象或者null，为null资源不存在
 */
export async function getImageById(id) {
    const res = await getAssetImage(id)
    if (!res.data) {
        Message.error($t('message.noAsset'))
        return null
    }
    return res.data
}

/**
 * 从完整图片路径中提取相对路径
 * @param {string} fullPath - 完整图片路径
 * @param {string} baseDir - 基础目录（如 '/home/gauzynote/uploads/images/'）
 * @returns {string} 提取的相对路径
 */
export function extractRelativeImagePath(fullPath, baseDir) {
    // 统一路径分隔符为 '/'
    const normalizedPath = fullPath.replace(/[\\/]/g, '/');
    const normalizedBaseDir = baseDir.replace(/[\\/]/g, '/');

    // 确保基础目录以 '/' 结尾
    const baseDirWithSlash = normalizedBaseDir.endsWith('/')
        ? normalizedBaseDir
        : `${normalizedBaseDir}/`;

    // 查找基础目录在路径中的位置
    const index = normalizedPath.indexOf(baseDirWithSlash);
    if (index === -1) {
        // 如果未找到基础目录，返回原始路径或处理错误
        console.warn(`基础目录 ${baseDirWithSlash} 不在路径 ${normalizedPath} 中`);
        return normalizedPath;
    }

    // 提取基础目录之后的部分作为相对路径
    return normalizedPath.slice(index + baseDirWithSlash.length);
}