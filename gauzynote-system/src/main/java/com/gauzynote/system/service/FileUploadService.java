package com.gauzynote.system.service;

import com.gauzynote.common.enums.SysResourceNodeType;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.AppConfigProperties;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.framework.service.FileUploadTotalSizeService;
import com.gauzynote.system.domain.entity.SysFile;
import com.gauzynote.system.domain.entity.SysResourceNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static com.gauzynote.common.utils.file.FileUtils.*;

@Service
public class FileUploadService {
    @Resource
    private AppConfigProperties appConfigProperties;
    @Resource
    private FileService fileService;
    @Resource
    private SysResourceNodeService sysResourceNodeService;
    @Resource
    private FileUploadTotalSizeService fileUploadTotalSizeService;
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upload(MultipartFile file, Long parentNodeId) throws IOException {
        // 1. 基础校验（文件空、文件名空）
        checkFileEmpty(file);
        String fileName = file.getOriginalFilename();

        // 2. 校验文件大小（新增：配置文件中设置最大文件大小）
        // 自动验证

        // 3. 计算文件哈希
        String fileHash;
        String extension = getFileExtension(fileName);
        try {
            fileHash = calculateFileHash(file) + "." + extension;
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR.value(), MessageUtils.message("file.hash.calculate.fail"), e);
        }

        // 4. 处理父节点逻辑（下沉到Service层）
        Long userId = SecurityUtils.getUserId();
        Long finalParentId = this.handleParentNode(parentNodeId);

        // 5. 构建上传目录和文件路径
        Path uploadDirPath = Paths.get(appConfigProperties.getUpload().getUploadOtherFileDir()).resolve(userId.toString());
        // 创建目录
        createDirIfNotExist(uploadDirPath);
        // 构建文件路径
        Path filePath = uploadDirPath.resolve(fileHash).normalize();

        // 6. 提前校验路径合法性（关键：在数据库操作前校验）
        if (!filePath.startsWith(uploadDirPath)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("illegal.path"));
        }

        // 7. 数据库操作（插入SysFile和SysResourceNode，Service层已加事务）
        FileService.FileInsertResult insertResult = fileService.insertByFile(file, filePath, fileName);
        SysFile sysFile = insertResult.getSysFile();

        // 8. 仅新建物理文件时校验总上传大小（复用已有物理文件不额外占空间）
        if (insertResult.isNewStorage()) {
            fileUploadTotalSizeService.checkedTotalSize(file);
        }
        SysResourceNode node = this.createResourceNode(sysFile, fileName, finalParentId, parentNodeId);

        // 9. 保存文件（此时路径已合法，无无效操作）
        try {
            Files.write(filePath, file.getBytes());
        } catch (IOException e) {
            // 文件写入失败，数据库操作已通过事务自动回滚（Service层事务）
            throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR.value(), MessageUtils.message("file.write.fail"), e);
        }

        // 10. 更新总上传大小（仅新建物理文件时累加）
        if (insertResult.isNewStorage()) {
            fileUploadTotalSizeService.addTotalSize(file.getSize());
        }

        // 11. 构建返回结果
        Map<String, Object> result = new HashMap<>(3);
        result.put("fileName", fileName);
        result.put("filePath", contextPath + "/file/" + userId + "/" + fileHash);
        result.put("fileSize", file.getSize());
        result.put("fileType", file.getContentType());
        return result;
    }

    // 处理父节点逻辑（asset目录、note目录创建）
    private Long handleParentNode(Long parentNodeId) {
        if (parentNodeId == null) {
            return null;
        }
        SysResourceNode resourceNode = sysResourceNodeService.selectById(parentNodeId);
        if (resourceNode == null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(),  MessageUtils.message("dir.parent.not.exist"));
        }
        // 处理note类型节点
        if (SysResourceNodeType.NOTE.getCode().equals(resourceNode.getNodeType())) {
            // 校验并创建asset目录
            SysResourceNode assets = sysResourceNodeService.selectAssetsByNodeNameAndDepth("assets", 0);
            if (assets == null) {
                assets = new SysResourceNode();
                assets.setNodeName("assets");
                sysResourceNodeService.insert(assets);
            }
            // 创建note对应的asset目录
            return sysResourceNodeService.insertNoteInTargetDir(assets.getNodeId(), resourceNode.getNodeName());
        }
        return parentNodeId;
    }


    // 创建SysResourceNode实体并插入数据库
    private SysResourceNode createResourceNode(SysFile sysFile, String fileName, Long finalParentId, Long parentNodeId) {
        SysResourceNode node = new SysResourceNode();
        node.setRelatedId(sysFile.getFileId());
        node.setNodeName(fileName);
        node.setNodeType(SysResourceNodeType.FILE.getCode());
        if (finalParentId != null) {
            node.setParentId(finalParentId);
            SysResourceNode parentNode = sysResourceNodeService.selectById(parentNodeId);
            if (parentNode != null) {
                node.setNodePath(parentNode.getNodePath());
            }
        }
        sysResourceNodeService.insert(node);
        return node;
    }
}
