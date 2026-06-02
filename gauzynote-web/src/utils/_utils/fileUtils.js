import {isString} from "@/utils/index.js";

/**
 * 文件类型映射表
 */
export const FILE_TYPES = {
    'jpg': 'image/jpeg',
    'jpeg': 'image/jpeg',
    'png': 'image/png',
    'gif': 'image/gif',
    'webp': 'image/webp',
    'pdf': 'application/pdf',
    'mp4': 'video/mp4'
}

/**
 * 获取文件的扩展名
 * @param {string} fileName - 文件名（可以包含路径）
 * @param {boolean} [includeDot=false] - 是否包含点（.）
 * @param {boolean} [lower=false] - 是否小写
 * @returns {string} 文件扩展名（小写），无扩展名则返回空字符串
 */
export function getFileExtension(fileName, includeDot = false, lower = false) {
    if (!isString(fileName)) {
        throw new TypeError('文件名必须是字符串类型');
    }
    if (!fileName || !fileName.includes('.')) {
        return '';
    }
    const extension = lower ? fileName.split('.').pop().toLowerCase() : fileName.split('.').pop().toUpperCase()
    // 返回小写的扩展名（统一格式）
    return includeDot ? '.' + extension : extension
}
/**
 * 获取文件名（不包含扩展名和点）
 * @param {string} fileName - 文件名（可以包含路径）
 * @returns {string} 不含扩展名和点的文件名
 */
export function getFileNameWithoutExtension(fileName) {
    if (!isString(fileName)) {
        throw new TypeError('文件名必须是字符串类型');
    }

    // 处理包含路径的情况（移除路径，只保留文件名）
    const baseName = fileName.split(/[\\/]/).pop();

    // 查找最后一个点的位置
    const lastDotIndex = baseName.lastIndexOf('.');

    // 没有点或点是第一个字符，返回完整文件名
    if (lastDotIndex <= 0) {
        return baseName;
    }

    // 提取点之前的部分作为文件名
    return baseName.substring(0, lastDotIndex);
}
/**
 * 从URL提取文件名
 * @param {string} url - 资源URL
 * @returns {string} 提取的文件名
 */
export function extractFileNameFromUrl(url) {
    try {
        const urlObj = new URL(url);
        const pathSegments = urlObj.pathname.split('/').filter(segment => segment);
        if (pathSegments.length > 0) {
            const lastSegment = pathSegments.pop();
            return lastSegment.includes('.') ? lastSegment : `${lastSegment}_${Date.now()}.unknown`;
        }
    } catch (e) {
        // URL解析失败时的降级处理
        console.warn('URL解析失败，使用默认文件名', e);
    }
    return `file_${Date.now()}.unknown`;
}
/**
 * 将URL转换为File对象
 * @param {string} url - 资源URL
 * @param {Object} [options] - 可选配置
 * @param {string} [options.filename] - 自定义文件名（默认从URL提取）
 * @returns {Promise<File>} File对象
 * @throws {Error} 转换失败时抛出错误
 */
export async function urlToFile(url, options = {}) {
    if (!url) {
        throw new Error('URL不能为空');
    }

    try {
        // 发送请求获取资源
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                // 模拟 referer
                Referer: 'https://i-blog.csdnimg.cn'
            }
        });
        if (!response.ok) {
            throw new Error(`请求失败，状态码：${response.status}`);
        }

        // 转换为Blob
        const blob = await response.blob();

        // 处理文件名
        const fileName = options.filename || extractFileNameFromUrl(url);
        const fileExt = this.getFileExtension(fileName);

        // 生成File对象
        return new File([blob], fileName, {
            type: this.FILE_TYPES[fileExt] || blob.type
        });
    } catch (error) {
        throw new Error(`URL转File失败：${error.message}`);
    }
}


/**
 * 验证文件
 * @param file
 * @param fileTypes
 * @param maxSize
 * @returns {{message: string, status: boolean}}
 */
export function validateFile(file, fileTypes, maxSize) {
    // 检查类型
    if (fileTypes.length && !fileTypes.includes(file.type)) {
        return {
            status: false,
            message:  `不支持的文件类型，仅支持: ${fileTypes.map(type => type.split('/')[1]).join(', ')}`
        }
    }

    // 检查文件大小 (MB转字节)
    if (file.size > maxSize * 1024 * 1024) {
        return {
            status: false,
            message:  `文件大小不能超过${maxSize}MB`
        }
    }

    return {status:true, message: '文件验证通过'};
}

/**
 * 格式化文件大小显示
 * @param {number} bytes - 文件字节数
 * @returns {string} 格式化后的大小字符串（B/KB/MB/GB）
 */
export function formatFileSize(bytes) {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}