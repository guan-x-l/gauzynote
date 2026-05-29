package com.gauzynote.system.service;

import com.gauzynote.common.enums.SysResourceNodeType;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.AppConfigProperties;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.framework.service.FileUploadTotalSizeService;
import com.gauzynote.system.domain.entity.Images;
import com.gauzynote.system.domain.entity.SysResourceNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
public class ImageUploadService {
    @Resource
    private AppConfigProperties appConfigProperties;
    @Resource
    private ImagesService imagesService;
    @Resource
    private SysResourceNodeService sysResourceNodeService;
    @Resource
    private FileUploadTotalSizeService fileUploadTotalSizeService;
    @Value("${server.servlet.context-path}")
    private String contextPath;

    public Map<String, Object> upload(MultipartFile file, Long parentNodeId) throws IOException {
        // 1. 基础校验（文件空、文件名空）
        checkFileEmpty(file);
        String fileName = file.getOriginalFilename();

        // 2. 校验文件大小（新增：配置文件中设置最大文件大小）
        // 自动验证


        // 3. 校验文件类型（ContentType + 后缀名，双重校验）
        String[] allowedContentTypes = appConfigProperties.getUpload().getImage().getAllowedContentTypes();
//        String[] allowedExtensions = appConfigProperties.getUpload().getImage().getAllowedExtensions(); // 配置项：jpg,png,gif等
        checkImageFileType(file, allowedContentTypes, null);

        // 4. 校验总上传大小
        fileUploadTotalSizeService.checkedTotalSize(file);

        // 5. 计算文件哈希
        String fileHash;
        String extension = getFileExtension(fileName);
        try {
            fileHash = calculateFileHash(file) + "." + extension;
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR.value(), MessageUtils.message("file.hash.calculate.fail"), e);
        }

        // 6. 处理父节点逻辑（下沉到Service层）
        Long userId = SecurityUtils.getUserId();
        Long finalParentId = this.handleParentNode(parentNodeId);

        // 7. 构建上传目录和文件路径
        Path uploadDirPath = Paths.get(appConfigProperties.getUpload().getUploadImageDir()).resolve(userId.toString());
        // 创建目录
        createDirIfNotExist(uploadDirPath);
        // 构建文件路径
        Path filePath = uploadDirPath.resolve(fileHash).normalize();

        // 8. 提前校验路径合法性（关键：在数据库操作前校验）
        if (!filePath.startsWith(uploadDirPath)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("illegal.path"));
        }

        // 9. 数据库操作（插入Images和SysResourceNode，Service层已加事务）
        Images images = imagesService.insertByFile(file, filePath, fileName);
        SysResourceNode node = this.createResourceNode(images, fileName, finalParentId, parentNodeId);

        // 10. 保存文件（此时路径已合法，无无效操作）
        try {
            Files.write(filePath, file.getBytes());
        } catch (IOException e) {
            // 文件写入失败，数据库操作已通过事务自动回滚（Service层事务）
            throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR.value(), MessageUtils.message("file.write.fail"), e);
        }

        // 11. 更新总上传大小
        fileUploadTotalSizeService.addTotalSize(file);

        // 12. 构建返回结果
        Map<String, Object> result = new HashMap<>(3);
        result.put("fileName", fileName);
        result.put("filePath", contextPath + "/image/" + userId + "/" + fileHash);
        result.put("fileSize", file.getSize());
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
    private SysResourceNode createResourceNode(Images images, String fileName, Long finalParentId, Long parentNodeId) {
        SysResourceNode node = new SysResourceNode();
        node.setRelatedId(images.getImageId());
        node.setNodeName(fileName);
        node.setNodeType(SysResourceNodeType.IMAGES.getCode());
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
