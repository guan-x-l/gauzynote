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

export function getImageTab(nodeItem) {
    const path = `/image/${nodeItem.relatedId}`;
    return {
        name: 'image',
        path: path,
        fullPath: path,
        componentName: `image_${nodeItem.relatedId}_${createUUID()}`,
        title: nodeItem.nodeName || `image-${nodeItem.relatedId}`,
        nodeId: nodeItem.nodeId,
        nodeType: nodeItem.nodeType,
        relatedId: nodeItem.relatedId,
    }
}

