import {uploadFile, uploadImage} from "@/api/upload.js";
import {processFileList} from "@/utils/index.js";
import {Message} from "@/components/index.js";
import {$t} from "@/locales";
/**
 * 上传图片列表
 * @param {FileList|Array} files - 图片文件列表
 * @param {string|number} [parentNodeId] - 可选的父节点ID
 * @returns {Promise<Array>} 上传结果数组
 */
export async function handleUploadFileList(files, parentNodeId) {
    // 输入验证
    if (!files || (files.length === 0 && !files[0])) {
        Message.warning($t('upload.file.noFile'));
        return [];
    }

    try {
        // 处理文件列表（验证、格式化等）
        const fileInfos = await processFileList(files, {allowedTypes: [], maxSize: 100});
        if (!fileInfos || !fileInfos.length) {
            Message.info($t('upload.file.noMatch'));
            return [];
        }
        console.log(fileInfos)
        const uploadResults = [];
        // 遍历文件信息，逐个上传
        for (const fileInfo of fileInfos) {
            // 处理文件验证错误
            if (fileInfo.errorMessage) {
                Message.error(`${fileInfo.name}：${fileInfo.errorMessage}`);
                uploadResults.push({
                    name: fileInfo.name,
                    success: false,
                    error: fileInfo.errorMessage
                });
                continue;
            }

            try {
                // 构建表单数据
                const formData = new FormData();
                formData.append('file', fileInfo.file);
                if (parentNodeId) {
                    formData.append('parentNodeId', parentNodeId);
                }

                const result = await uploadFile(formData);
                if (result.code === 200) {
                    // 上传成功反馈
                    Message.success($t('upload.success', {fileName: fileInfo.name}));

                    uploadResults.push({
                        name: fileInfo.name,
                        success: true,
                        result
                    });
                }
            } catch (e) {
                // 单个文件上传失败处理
            }
        }

        return uploadResults;
    } catch (e) {
        // 整体流程错误处理
        const errorMsg = e?.message;
        Message.error($t('upload.error', {msg: errorMsg}));
        throw e; // 继续抛出错误供上层处理
    }
}

/** @deprecated 使用 handleUploadFileList 替代 */
export async function handleUploadImageList(files, parentNodeId) {
    // 输入验证
    if (!files || (files.length === 0 && !files[0])) {
        Message.warning($t('upload.image.noFile'));
        return [];
    }

    try {
        // 处理文件列表（验证、格式化等）
        const fileInfos = await processFileList(files);
        if (!fileInfos || !fileInfos.length) {
            Message.info($t('upload.image.noMatch'));
            return [];
        }

        const uploadResults = [];
        // 遍历文件信息，逐个上传
        for (const fileInfo of fileInfos) {
            // 处理文件验证错误
            if (fileInfo.errorMessage) {
                Message.error(`${fileInfo.name}：${fileInfo.errorMessage}`);
                uploadResults.push({
                    name: fileInfo.name,
                    success: false,
                    error: fileInfo.errorMessage
                });
                continue;
            }

            try {
                // 构建表单数据
                const formData = new FormData();
                formData.append('file', fileInfo.file);
                if (parentNodeId) {
                    formData.append('parentNodeId', parentNodeId);
                }

                const result = await uploadImage(formData);
                if (result.code === 200) {
                    // 上传成功反馈
                    Message.success($t('upload.success', {fileName: fileInfo.name}));

                    uploadResults.push({
                        name: fileInfo.name,
                        success: true,
                        result
                    });
                }
            } catch (e) {
                // 单个文件上传失败处理
                // Message.error(`${fileInfo.name} 上传失败：${e?.msg || '未知错误'}`);

                /*uploadResults.push({
                    name: fileInfo.name,
                    success: false,
                    error: errorMsg
                });*/
            }
        }

        // 全部上传完成后的汇总反馈
        /*const successCount = uploadResults.filter(r => r.success).length;
        if (successCount > 0 && successCount < uploadResults.length) {
            Message.info(`部分图片上传成功，共 ${successCount}/${uploadResults.length} 个`);
        } else if (successCount === uploadResults.length && successCount > 1) {
            Message.success(`全部 ${successCount} 个图片上传成功`);
        }*/

        return uploadResults;
    } catch (e) {
        // 整体流程错误处理
        const errorMsg = e?.message;
        Message.error($t('upload.error', {msg: errorMsg}));
        throw e; // 继续抛出错误供上层处理
    }
}
