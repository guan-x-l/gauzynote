package com.gauzynote.system.service;

import com.gauzynote.common.enums.SysResourceNodeType;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.system.domain.entity.Note;
import com.gauzynote.system.domain.entity.SysResourceNode;
import com.gauzynote.system.mapper.SysResourceNodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 资源节点表(SysResourceNode)表服务接口
 */
@Service("sysResourceNodeService")
public class SysResourceNodeService {

    @Resource
    private SysResourceNodeMapper sysResourceNodeDao;

    @Resource
    private NoteService noteService;
    @Resource
    private ImagesService imageService;

    /**
     * 通过Id查询数据
     *
     * @param nodeId nodeId
     * @return 实例对象
     */
    public SysResourceNode selectById(Long nodeId) {
        if (nodeId == null) return null;
        return this.sysResourceNodeDao.selectById(nodeId);
    }

    /**
     * 通过relatedId查询数据
     *
     * @param relatedId relatedId
     * @return 实例对象
     */
    public SysResourceNode selectByRelatedId(Long relatedId) {
        if (relatedId == null) return null;
        return this.sysResourceNodeDao.selectByRelatedId(relatedId);
    }

    /**
     * @return 实例对象
     */
    public SysResourceNode selectByParentIdAndNodeName(Long parentId, String nodeName, String nodeType) {
        return this.sysResourceNodeDao.selectByParentIdAndNodeName(SecurityUtils.getUserId(), parentId, nodeName, nodeType);
    }

    /**
     * 通过userId查询数据
     *
     * @param userId userId
     * @return 实例对象
     */
    public List<SysResourceNode> selectAllByUserId(Long userId) {
        return this.sysResourceNodeDao.selectAllByUserId(userId);
    }

    /**
     * @return 实例对象
     */
    public SysResourceNode selectAssetsByNodeNameAndDepth(String nodeName, int depth) {
        return this.sysResourceNodeDao.selectAssetsByNodeNameAndDepth(SecurityUtils.getUserId(), nodeName, depth);
    }

    /**
     * 新增数据
     *
     * @param sysResourceNode 实例对象
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int insert(SysResourceNode sysResourceNode) {
        sysResourceNode.setUserId(SecurityUtils.getUserId());

        int insert = this.sysResourceNodeDao.insert(sysResourceNode);

        if (insert <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("node.creation.failed"));
        }
        Long currentNodeId = sysResourceNode.getNodeId();
        String nodePath = getNodePath(sysResourceNode);

        // 更新 node_path
        SysResourceNode updateSysResourceNode = new SysResourceNode();
        updateSysResourceNode.setUserId(sysResourceNode.getUserId());
        updateSysResourceNode.setNodeId(currentNodeId);
        updateSysResourceNode.setNodePath(nodePath);
        int update = this.sysResourceNodeDao.update(updateSysResourceNode);

        if (update <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("node.path.update.failed"));
        }

        return update;
    }

    /**
     * 新增数据同时新增note
     *
     * @param sysResourceNode 实例对象
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public SysResourceNode insertNodeAndNote(SysResourceNode sysResourceNode) {
        Long userId = SecurityUtils.getUserId();

        // 创建关联笔记
        Note note = new Note();
        note.setUserId(userId);
        note.setNoteName(sysResourceNode.getNodeName());
        int insert = noteService.insert(note);

        // 校验笔记ID是否生成
        Long noteId = note.getNoteId();
        if (noteId == null) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("note.id.generation.failed.cannot.link.node"));
        }

        // 创建节点，赋值noteId
        sysResourceNode.setRelatedId(noteId);
        int row = this.insert(sysResourceNode);

        if (row <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("node.creation.failed"));
        }

        return sysResourceNode;
    }

    /**
     * 创建节点到指定目录下
     *
     * @return 节点ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long insertNoteInTargetDir(Long parentAssetId, String noteName) {
        SysResourceNode noteNodeAsset = selectByParentIdAndNodeName(parentAssetId, noteName, SysResourceNodeType.FOLDER.getCode());
        if (noteNodeAsset == null) {
            noteNodeAsset = new SysResourceNode();
            noteNodeAsset.setNodeName(noteName);
            noteNodeAsset.setParentId(parentAssetId);
            insert(noteNodeAsset);
        }
        return noteNodeAsset.getNodeId();
    }

    /**
     * 修改数据
     *
     * @param sysResourceNode 实例对象
     * @return 影响行数
     */
    public int update(SysResourceNode sysResourceNode) {
        int update = this.sysResourceNodeDao.update(sysResourceNode);
        if (update <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("resource.node.modification.failed"));
        }
        return update;
    }

    /**
     * 修改数据
     *
     * @param sysResourceNode 实例对象
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateNodeName(SysResourceNode sysResourceNode) {
        String nodeName = sysResourceNode.getNodeName();
        Long userId = SecurityUtils.getUserId();

        SysResourceNode node = sysResourceNodeDao.selectById(sysResourceNode.getNodeId());
        if (node == null) {
            throw new ServiceException(HttpServletResponse.SC_BAD_REQUEST, MessageUtils.message("resource.node.not.exists"));
        }

        SysResourceNode duplicationName = sysResourceNodeDao.selectByParentIdAndNodeName(userId, node.getParentId(), nodeName, node.getNodeType());
        if (duplicationName != null) {
            throw new ServiceException(HttpServletResponse.SC_BAD_REQUEST, MessageUtils.message("name.duplicate"));
        }
        SysResourceNode editNode = new SysResourceNode();
        editNode.setNodeId(node.getNodeId());
        editNode.setUserId(userId);
        editNode.setNodeName(nodeName);

        int update = this.sysResourceNodeDao.updateNodeName(editNode);

        if (update <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("resource.node.modification.failed"));
        }

        if (SysResourceNodeType.FOLDER.getCode().equals(node.getNodeType())) {
            return update;
        }

        if (node.getRelatedId() == null) {
            throw new ServiceException(HttpServletResponse.SC_BAD_REQUEST, MessageUtils.message("associated.resource.not.exists"));
        }

        int relatedUpdate = 0;

        if (SysResourceNodeType.NOTE.getCode().equals(node.getNodeType())) {
            Note note = new Note();
            note.setNoteId(node.getRelatedId());
            note.setNoteName(nodeName);
            relatedUpdate = noteService.updateNoteName(note);
        } else if (SysResourceNodeType.IMAGES.getCode().equals(node.getNodeType())) {
            // 图片修改名称流程
            relatedUpdate = imageService.updateImageName(node.getRelatedId(), nodeName);
        }

        return relatedUpdate;
    }

    /**
     * 修改数据
     *
     * @param sysResourceNode 实例对象
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateNodeParentId(SysResourceNode sysResourceNode) {
        List<Long> ids = new ArrayList<>();
        ids.add(sysResourceNode.getNodeId());
        if (sysResourceNode.getParentId() != null) {
            ids.add(sysResourceNode.getParentId());
        }
        List<SysResourceNode> list = sysResourceNodeDao.selectByIds(ids);

        if (list == null || list.size() != ids.size()) {
            throw new ServiceException(HttpServletResponse.SC_BAD_REQUEST, MessageUtils.message("resource.node.not.exists"));
        }
        String nodePath = getNodePath(sysResourceNode);
        SysResourceNode editNode = new SysResourceNode();
        editNode.setNodeId(sysResourceNode.getNodeId());
        editNode.setParentId(sysResourceNode.getParentId());
        editNode.setNodePath(nodePath);
        editNode.setUserId(SecurityUtils.getUserId());
        int update = this.sysResourceNodeDao.updateNodeParentId(editNode);

        if (update <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("resource.node.modification.failed"));
        }
        return update;
    }

    /**
     * 修改数据
     *
     * @param note 实例对象
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateNodeNameByNote(Note note) {
        String noteName = note.getNoteName();

        Note editNote = new Note();
        editNote.setNoteId(note.getNoteId());
        editNote.setNoteName(noteName);

        SysResourceNode node = sysResourceNodeDao.selectByRelatedId(note.getNoteId());

        if (node == null) {
            throw new ServiceException(HttpServletResponse.SC_BAD_REQUEST, MessageUtils.message("resource.node.not.exists"));
        }
        SysResourceNode duplicationName = sysResourceNodeDao.selectByParentIdAndNodeName(SecurityUtils.getUserId(), node.getParentId(), noteName, node.getNodeType());
        if (duplicationName != null) {
            throw new ServiceException(HttpServletResponse.SC_BAD_REQUEST, MessageUtils.message("name.duplicate"));
        }
        noteService.updateNoteName(editNote);

        node.setNodeName(noteName);

        int update = this.sysResourceNodeDao.updateNodeName(node);

        if (update <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("resource.node.modification.failed"));
        }
        return update;
    }

    /**
     * 校验Assets节点，如果不存在则创建
     *
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public SysResourceNode selectAssetsNodeAndInsert(String nodeName, int depth) {
        SysResourceNode assets = this.selectAssetsByNodeNameAndDepth(nodeName, depth);
        if (assets == null) {
            SysResourceNode node = new SysResourceNode();
            node.setNodeName(nodeName);
            this.insert(node);
            return node;
        }
        return assets;
    }


    /**
     * 通过主键删除数据
     *
     * @param nodeId 主键
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteById(Long nodeId) {
        Long userId = SecurityUtils.getUserId();
        // 1.根据nodeid查询所有子节点,包括自身
        List<SysResourceNode> sysResourceNodes = this.sysResourceNodeDao.selectAllChildrenByNodeId(nodeId);

        // node
        List<Long> nodeIdList = new ArrayList<>();
        // note
        List<Long> noteIdList = new ArrayList<>();
        // image
        List<Long> imageIdList = new ArrayList<>();
        for (SysResourceNode node : sysResourceNodes) {
            nodeIdList.add(node.getNodeId());
            if (node.getRelatedId() != null && SysResourceNodeType.NOTE.getCode().equals(node.getNodeType())) {
                noteIdList.add(node.getRelatedId());
            }
            if (node.getRelatedId() != null && SysResourceNodeType.IMAGES.getCode().equals(node.getNodeType())) {
                imageIdList.add(node.getRelatedId());
            }
        }

        int i = this.sysResourceNodeDao.deleteByIds(nodeIdList, userId);
        if (i <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("node.deletion.failed"));
        }

        if (!noteIdList.isEmpty()) {
            noteService.deleteByIds(noteIdList);
        }
        if (!imageIdList.isEmpty()) {
            imageService.deleteByIds(imageIdList);
        }
        // todo 回收站
        return i;
    }

    public String getNodePath(SysResourceNode sysResourceNode) {
        String nodePath = "/" + sysResourceNode.getNodeId();

        if (sysResourceNode.getParentId() != null) {
            SysResourceNode parentNode = this.sysResourceNodeDao.selectById(sysResourceNode.getParentId());
            nodePath = parentNode.getNodePath() + nodePath;
        }
        return nodePath;
    }
}
