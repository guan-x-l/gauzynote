import {createNextUnnamedNodeName} from "@/utils/index.js";

/**
 * 获取节点名称（为空时生成默认名称）
 * @param {[ResourceNode]} resourceNodeList 节点数组
 * @param {ResourceNode} [parentNode] - 目标节点，也是父节点（可选）
 * @param {string} nodeType - 目标节点类型（可选）
 * @returns {string} 节点名称
 */
export function getNodeName(resourceNodeList, parentNode, nodeType) {
    if (parentNode && parentNode.nodeId && nodeType) {
        // 获取目标节点下的名称列表
        const topNodeNames = resourceNodeList
            .filter(item => item.parentId === parentNode.nodeId && item.nodeType === nodeType)
            .map(item => item.nodeName);
        return createNextUnnamedNodeName(topNodeNames)
    } else {
        // 获取顶级节点（depth=0）的名称列表
        const topNodeNames = resourceNodeList
            .filter(item => item.depth === 0 && item.nodeType === nodeType)
            .map(item => item.nodeName);
        return createNextUnnamedNodeName(topNodeNames)
    }
}

/**
 * 创建资源节点
 * @param {ResourceNode} parentNode 目标节点，也是父节点
 * @param {string} nodeType 节点类型
 * @param {[ResourceNode]} resourceNodeList 节点数组
 * @returns {{nodeName: string, nodeType}}
 */
export function createResourceNode(parentNode, nodeType, resourceNodeList) {
    let resourceNode = {
        nodeType: nodeType,
        nodeName: getNodeName(resourceNodeList, parentNode, nodeType),
    }
    if (parentNode) {
        resourceNode.parentId = parentNode?.nodeId
        resourceNode.nodePath = parentNode?.nodePath
        resourceNode.depth = parentNode.depth + 1
    }
    return resourceNode
}