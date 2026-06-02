import {createUUID} from "@/utils/index.js";

/**
 * 生成笔记标签页配置
 * @param {Object} nodeItem - 节点对象
 * @returns {Object} 标签页配置
 */
export function getNoteTab(nodeItem) {
    const path = `/note/${nodeItem.relatedId}`;
    return {
        name: 'note',
        path: path,
        fullPath: path,
        componentName: `note_${nodeItem.relatedId}_${createUUID()}`,
        title: nodeItem.nodeName || `note-${nodeItem.relatedId}`,
        nodeId: nodeItem.nodeId,
        nodeType: nodeItem.nodeType,
        relatedId: nodeItem.relatedId,
    }
}

export function getFileTab(nodeItem) {
    const path = `/file/${nodeItem.relatedId}`;
    return {
        name: 'file',
        path: path,
        fullPath: path,
        componentName: `file_${nodeItem.relatedId}_${createUUID()}`,
        title: nodeItem.nodeName || `file-${nodeItem.relatedId}`,
        nodeId: nodeItem.nodeId,
        nodeType: nodeItem.nodeType,
        relatedId: nodeItem.relatedId,
    }
}

/** @deprecated 使用 getFileTab 替代 */
export function getImageTab(nodeItem) {
    const path = `/file/${nodeItem.relatedId}`;
    return {
        name: 'file',
        path: path,
        fullPath: path,
        componentName: `file_${nodeItem.relatedId}_${createUUID()}`,
        title: nodeItem.nodeName || `file-${nodeItem.relatedId}`,
        nodeId: nodeItem.nodeId,
        nodeType: nodeItem.nodeType,
        relatedId: nodeItem.relatedId,
    }
}

