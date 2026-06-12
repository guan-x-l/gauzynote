package com.gauzynote.system.service;

import com.gauzynote.common.enums.SysResourceNodeType;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.system.domain.entity.RecycleBin;
import com.gauzynote.system.domain.entity.SysResourceNode;
import com.gauzynote.system.mapper.RecycleBinMapper;
import com.gauzynote.system.mapper.SysResourceNodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 回收站(RecycleBin)表服务实现类
 */
@Service("recycleBinService")
public class RecycleBinService {

    @Resource
    private RecycleBinMapper recycleBinDao;

    @Resource
    private SysResourceNodeMapper sysResourceNodeDao;

    @Resource
    private NoteService noteService;

    @Resource
    private FileService fileService;

    /**
     * 查询当前用户的回收站列表
     *
     * @return 回收站记录列表
     */
    public List<RecycleBin> listRecycleBin() {
        Long userId = SecurityUtils.getUserId();
        return this.recycleBinDao.selectByUserId(userId);
    }

    /**
     * 从回收站恢复资源
     *
     * @param recycleId 回收站记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long recycleId) {
        Long userId = SecurityUtils.getUserId();
        RecycleBin recycleBin = this.recycleBinDao.selectById(recycleId, userId);
        if (recycleBin == null) {
            throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, MessageUtils.message("recycle.bin.record.not.found"));
        }

        Long rootNodeId = recycleBin.getNodeId();
        // 查询所有已删除的子孙节点（含自身）
        List<SysResourceNode> deletedNodes = this.sysResourceNodeDao.selectAllChildrenByNodeIdIncludeDeleted(rootNodeId);

        List<Long> nodeIdList = new ArrayList<>();
        List<Long> noteIdList = new ArrayList<>();
        List<Long> fileIdList = new ArrayList<>();
        for (SysResourceNode node : deletedNodes) {
            nodeIdList.add(node.getNodeId());
            if (node.getRelatedId() != null && SysResourceNodeType.NOTE.getCode().equals(node.getNodeType())) {
                noteIdList.add(node.getRelatedId());
            }
            if (node.getRelatedId() != null && SysResourceNodeType.FILE.getCode().equals(node.getNodeType())) {
                fileIdList.add(node.getRelatedId());
            }
        }

        // 恢复资源节点
        if (!nodeIdList.isEmpty()) {
            this.sysResourceNodeDao.restoreByIds(nodeIdList, userId);
        }
        // 恢复笔记
        if (!noteIdList.isEmpty()) {
            noteService.restoreByIds(noteIdList);
        }
        // 恢复文件
        if (!fileIdList.isEmpty()) {
            fileService.restoreByIds(fileIdList);
        }

        // 删除回收站记录
        this.recycleBinDao.deleteById(recycleId, userId);
    }

    /**
     * 从回收站彻底删除资源
     *
     * @param recycleId 回收站记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void permanentDelete(Long recycleId) {
        Long userId = SecurityUtils.getUserId();
        RecycleBin recycleBin = this.recycleBinDao.selectById(recycleId, userId);
        if (recycleBin == null) {
            throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, MessageUtils.message("recycle.bin.record.not.found"));
        }

        Long rootNodeId = recycleBin.getNodeId();
        // 查询所有已删除的子孙节点（含自身）
        List<SysResourceNode> deletedNodes = this.sysResourceNodeDao.selectAllChildrenByNodeIdIncludeDeleted(rootNodeId);

        // 收集文件ID并物理删除磁盘文件
        List<Long> fileIdList = new ArrayList<>();
        for (SysResourceNode node : deletedNodes) {
            if (node.getRelatedId() != null && SysResourceNodeType.FILE.getCode().equals(node.getNodeType())) {
                fileIdList.add(node.getRelatedId());
            }
        }
        if (!fileIdList.isEmpty()) {
            fileService.permanentDeleteFiles(fileIdList);
        }

        // 标记回收站记录为已彻底删除
        this.recycleBinDao.markPermanentlyDeleted(recycleId, userId);
    }
}
