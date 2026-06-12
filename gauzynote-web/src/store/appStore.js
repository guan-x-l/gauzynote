import {onMounted, ref} from "vue";
import {getResourceNode} from "@/api/resourceNode.js";

/**
 * 节点列表数据
 * @type {import('vue').Ref<[ResourceNode]>}
 */
const resourceNodeList = ref([])
const resourceNodeListVersion = ref(1)

/**
 * 回收站变更版本号，删除/恢复/彻底删除时递增，回收站页面监听此值刷新
 * @type {import('vue').Ref<number>}
 */
const recycleBinChangeVersion = ref(0)

/**
 * 需要聚焦
 * @type {import('vue').Ref<number[]>}
 */
const needFocusAssetNameIdList = ref([])

/**
 * 资源加载状态
 * @type {import('vue').Ref<boolean>}
 */
const assetLoadingStatus = ref(false)

// 大纲选择行号，仅用于codemirrorWrapper组件
const tocSelectLineEnd = ref(-1)

// 跳转行号，仅用于codemirrorWrapper组件
const tocLineNumber = ref(-1)

// 被拖拽的目标节点id；用于处理图片拖拽时，鼠标指针样式的切换;和拖拽至note中释放时，全局的id
const dragNodeId = ref(null)


function initAppData() {
    resourceNodeList.value = []
    resourceNodeListVersion.value = 1
    needFocusAssetNameIdList.value = []
    assetLoadingStatus.value = false
}

export const useAppStore = () => {

    /**
     * 初始化资源节点数据
     */
    async function initResourceNode() {
        return await getResourceNode().then(response => {
            let arr = response?.data || []
            arr.sort((a, b) => (b.nodeType === '1' ? 1 : 0) - (a.nodeType === '1' ? 1 : 0))
            resourceNodeList.value = arr.map((item, index) => {
                    return {
                        ...item,
                        // extension: item.nodeName.substring(fileName.lastIndexOf('.') + 1)
                        // sort: item.sort || Infinity
                        // sort:item.createTime ? +new Date(item.createTime) : Infinity
                        // sort: item.nodeType === '1' ? item.nodeId : item.nodeId + 1
                        sort: index
                    }
            });
            resourceNodeListVersion.value++
        })
    }

    function pushResourceNodeList(node) {
        resourceNodeList.value.push(node)
    }

    /**
     * 查询节点，根据节点id
     * @param nodeId
     * @returns {ResourceNode}
     */
    function getResourceNodeByNodeId(nodeId) {
        return resourceNodeList.value.find(item=>item.nodeId === nodeId)
    }

    /**
     * 查询节点，根据资源id
     * @param nodeType
     * @param {Number} relatedId
     * @returns {ResourceNode}
     */
    function getResourceNodeByRelatedId( relatedId, nodeType) {
        return resourceNodeList.value.find(item=>item.relatedId === relatedId && item.nodeType === nodeType)
    }

    /**
     * 根据节点id更新节点名称
     * @param nodeId 节点id
     * @param nodeName
     */
    function updateResourceNodeNameByNodeId(nodeId, nodeName) {
        for (let i = 0; i < resourceNodeList.value.length; i++) {
            if (resourceNodeList.value[i].nodeId === nodeId) {
                resourceNodeList.value[i].nodeName = nodeName
                resourceNodeListVersion.value++
            }
        }
    }

    /**
     * 根据资源id更新节点名称
     * @param nodeType
     * @param relatedId 资源id
     * @param nodeName
     */
    function updateResourceNodeNameByRelatedId(relatedId, nodeType, nodeName) {
        for (let i = 0; i < resourceNodeList.value.length; i++) {
            if (resourceNodeList.value[i].relatedId === relatedId && resourceNodeList.value[i].nodeType === nodeType) {
                resourceNodeList.value[i].nodeName = nodeName
                resourceNodeListVersion.value++
            }
        }
    }

    onMounted(() => {

    })

    function notifyRecycleBinChange() {
        recycleBinChangeVersion.value++
    }

    return {
        initAppData,
        resourceNodeListVersion,
        resourceNodeList,
        recycleBinChangeVersion,
        initResourceNode,
        notifyRecycleBinChange,
        pushResourceNodeList,
        getResourceNodeByNodeId,
        getResourceNodeByRelatedId,
        updateResourceNodeNameByRelatedId,
        updateResourceNodeNameByNodeId,
        needFocusAssetNameIdList,
        assetLoadingStatus,
        tocSelectLineEnd,
        tocLineNumber,
        dragNodeId,
    }
}