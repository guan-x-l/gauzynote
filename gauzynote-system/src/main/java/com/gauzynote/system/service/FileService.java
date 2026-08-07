package com.gauzynote.system.service;

import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.framework.service.FileUploadTotalSizeService;
import com.gauzynote.system.domain.entity.FileStorage;
import com.gauzynote.system.domain.entity.SysFile;
import com.gauzynote.system.mapper.FileStorageMapper;
import com.gauzynote.system.mapper.FileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gauzynote.common.utils.file.FileUtils.deleteFile;

/**
 * 文件表(File)表服务实现类
 *
 */
@Service("fileService")
public class FileService {
    @Resource
    private FileMapper fileDao;

    @Resource
    private FileStorageMapper fileStorageDao;

    @Resource
    private FileUploadTotalSizeService fileUploadTotalSizeService;

    /**
     * 通过ID查询单条数据
     *
     * @param fileId 主键
     * @return 实例对象
     */
    public SysFile selectById(Long fileId) {
        Long userId = SecurityUtils.getUserId();
        return this.fileDao.selectById(fileId, userId);
    }

    /**
     * 新增数据
     *
     * @param sysFile 实例对象
     * @return 实例对象
     */
    public int insert(SysFile sysFile) {
        sysFile.setUserId(SecurityUtils.getUserId());
        int insert = this.fileDao.insert(sysFile);
        if (insert <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("file.creation.failed"));
        }
        return insert;
    }

    public FileInsertResult insertByFile(MultipartFile file, Path filePath, String fileName) {
        Long userId = SecurityUtils.getUserId();
        String path = filePath.toString();
        String fileHash = filePath.getFileName().toString();
        // 同用户同哈希视为同一个物理文件，引用计数+1；1表示新建，2表示复用
        int upsert = this.fileStorageDao.upsertIncrement(userId, fileHash, path, file.getSize());
        FileStorage storage = this.fileStorageDao.selectByUserIdAndFileHash(userId, fileHash);

        SysFile sysFile = new SysFile();
        sysFile.setFileName(fileName);
        sysFile.setStorageId(storage.getStorageId());
        sysFile.setFileType(file.getContentType());
        sysFile.setFileSize(file.getSize());
        insert(sysFile);
        return new FileInsertResult(sysFile, upsert == 1);
    }

    /**
     * 插入文件记录的结果
     */
    public static class FileInsertResult {
        private final SysFile sysFile;
        private final boolean newStorage;

        FileInsertResult(SysFile sysFile, boolean newStorage) {
            this.sysFile = sysFile;
            this.newStorage = newStorage;
        }

        public SysFile getSysFile() {
            return sysFile;
        }

        public boolean isNewStorage() {
            return newStorage;
        }
    }

    /**
     * 修改数据
     *
     * @param sysFile 实例对象
     * @return 实例对象
     */
    public int update(SysFile sysFile) {
        sysFile.setUserId(SecurityUtils.getUserId());
        int update = this.fileDao.update(sysFile);
        if (update <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("file.modification.failed"));
        }
        return update;
    }

    @Transactional(rollbackFor = Exception.class)
    public int updateFileName(Long fileId, String fileName) {
        Long userId = SecurityUtils.getUserId();
        SysFile oldFile = this.fileDao.selectById(fileId, userId);
        if (oldFile == null) {
            throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, MessageUtils.message("resource.not.exists"));
        }

        SysFile sysFile = new SysFile();
        sysFile.setFileId(fileId);
        sysFile.setFileName(fileName);

        return this.fileDao.update(sysFile);
    }

    /**
     * 通过主键删除数据（移入回收站，不删物理文件）
     *
     * @param fileId 主键
     * @return 是否成功
     */
    public int deleteById(Long fileId) {
        Long userId = SecurityUtils.getUserId();
        int i = this.fileDao.deleteById(fileId, userId);
        if (i <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("file.deletion.failed"));
        }
        return i;
    }

    /**
     * 通过主键删除数据（移入回收站，不删物理文件）
     *
     * @param fileIds 主键
     */
    public void deleteByIds(List<Long> fileIds) {
        Long userId = SecurityUtils.getUserId();
        int i = this.fileDao.deleteByIds(fileIds, userId);
        if (i <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("file.deletion.failed"));
        }
    }

    /**
     * 彻底删除：删除file记录并释放物理文件引用，引用计数归零时物理删除磁盘文件
     *
     * @param fileIds 文件ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void permanentDeleteFiles(List<Long> fileIds) {
        Long userId = SecurityUtils.getUserId();
        List<SysFile> files = this.fileDao.selectByIdsIgnoreDelFlag(fileIds, userId);
        if (files.isEmpty()) {
            return;
        }

        // 按物理文件统计本批需要释放的引用数
        Map<Long, Integer> releaseCountMap = new HashMap<>();
        for (SysFile sysFile : files) {
            if (sysFile.getStorageId() != null) {
                releaseCountMap.merge(sysFile.getStorageId(), 1, Integer::sum);
            }
        }
        if (releaseCountMap.isEmpty()) {
            return;
        }

        // 锁定物理文件记录，防止并发彻底删除时引用计数互相覆盖
        List<FileStorage> storages = this.fileStorageDao.selectByIdsForUpdate(new ArrayList<>(releaseCountMap.keySet()));

        // 删除file记录，释放引用
        this.fileDao.deleteRowsByIds(fileIds, userId);

        for (FileStorage storage : storages) {
            int released = releaseCountMap.getOrDefault(storage.getStorageId(), 0);
            this.fileStorageDao.decrementRefCount(storage.getStorageId(), released);
            if (storage.getRefCount() - released <= 0) {
                if (storage.getFilePath() != null) {
                    deleteFile(storage.getFilePath());
                }
                // 物理文件引用归零，从已用空间计数中扣除
                if (storage.getFileSize() != null) {
                    this.fileUploadTotalSizeService.subtractTotalSize(storage.getFileSize());
                }
                this.fileStorageDao.deleteById(storage.getStorageId());
            }
        }
    }

    /**
     * 批量恢复（将del_flag从'2'改回'0'）
     *
     * @param fileIds 文件ID列表
     */
    public void restoreByIds(List<Long> fileIds) {
        Long userId = SecurityUtils.getUserId();
        this.fileDao.restoreByIds(fileIds, userId);
    }
}
