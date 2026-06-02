package com.gauzynote.system.service;

import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.system.domain.entity.SysFile;
import com.gauzynote.system.mapper.FileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.util.List;

import static com.gauzynote.common.utils.file.FileUtils.deleteFile;

/**
 * 文件表(File)表服务实现类
 *
 */
@Service("fileService")
public class FileService {
    @Resource
    private FileMapper fileDao;

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

    public SysFile insertByFile(MultipartFile file, Path filePath, String fileName) {
        SysFile sysFile = new SysFile();
        sysFile.setFileName(fileName);
        sysFile.setFilePath(filePath.toString());
        sysFile.setFileType(file.getContentType());
        sysFile.setFileSize(file.getSize());
        insert(sysFile);
        return sysFile;
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
     * 通过主键删除数据
     *
     * @param fileId 主键
     * @return 是否成功
     */
    public int deleteById(Long fileId) {
        Long userId = SecurityUtils.getUserId();
        SysFile sysFile = this.fileDao.selectById(fileId, userId);
        if (sysFile == null) {
            throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, MessageUtils.message("resource.not.exists"));
        }
        boolean b = deleteFile(sysFile.getFilePath());
        if (!b) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("file.deletion.failed"));
        }
        int i = this.fileDao.deleteById(fileId, userId);
        if (i <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("file.deletion.failed"));
        }
        return i;
    }

    /**
     * 通过主键删除数据
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
}
