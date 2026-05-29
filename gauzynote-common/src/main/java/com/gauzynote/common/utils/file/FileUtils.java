package com.gauzynote.common.utils.file;

import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.MessageUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 文件处理类
 */
public class FileUtils {
    public static String FILENAME_PATTERN = "[^a-zA-Z0-9\\u4e00-\\u9fa5_\\-.]";


    /**
     * 过滤文件名中的非法字符
     * 保留字母、数字、中文、下划线、横线、点，其他替换为下划线
     *
     * @param fileName 文件名称
     * @return 过滤后的文件名称
     */
    public static String sanitizeFileName(String fileName) {
        return fileName.replaceAll(FILENAME_PATTERN, "_");
    }

    /**
     * 文件名称验证
     *
     * @param filename 文件名称
     * @return true 正常 false 非法
     */
    public static boolean isValidFilename(String filename) {
        return filename.matches(FILENAME_PATTERN);
    }

    /**
     * 获取不带后缀文件名称 /home/gauzynote/uploads/filename.png -- filename
     *
     * @param fileName 路径名称
     * @return 没有文件路径和后缀的名称
     */
    public static String getNameNotSuffix(String fileName) {
        if (fileName == null) {
            return null;
        }
        return FilenameUtils.getBaseName(fileName);
    }


    /**
     * 检查文件类型是否允许
     * @param contentType 检查类型
     * @param allowedContentTypes 允许类型
     * @return true 允许 false 不允许
     */
    public static boolean isAllowedContentType(String contentType, String[] allowedContentTypes) {
        if (contentType == null) {
            return false;
        }
        for (String allowed : allowedContentTypes) {
            if (allowed.equalsIgnoreCase(contentType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 严格校验文件类型（ContentType + 文件后缀）
     */
    public static void checkImageFileType(MultipartFile file, String[] allowedContentTypes, String[] allowedExtensions) {
        // 1. ContentType校验
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(allowedContentTypes).contains(contentType)) {
            throw new ServiceException(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                    MessageUtils.message("only.image.files.allowed") + Arrays.toString(allowedContentTypes));
        }
        // 2. 文件后缀校验（补充ContentType的不足，防止篡改）
        String fileName = file.getOriginalFilename();
        String extension = getFileExtension(fileName).toLowerCase();
        if (allowedExtensions != null && !Arrays.asList(allowedExtensions).contains(extension)) {
            throw new ServiceException(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                    MessageUtils.message("file.extension.not.allowed") + Arrays.toString(allowedExtensions));
        }
    }

    /**
     * 安全获取文件扩展名（仅保留最后一个.后的内容）
     * @param fileName 文件名
     * @return 扩展名
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 创建目录（不存在则创建）
     */
    public static void createDirIfNotExist(Path dirPath) {
        try {
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
        } catch (IOException e) {
            throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR.value(), MessageUtils.message("dir.create.fail") + dirPath, e);
        }
    }

    /**
     * 重命名文件或目录（在同一目录下）
     * @param oldPath 原路径
     * @param newName 新名称
     * @return 是否重命名成功
     */
    public static boolean rename(String oldPath, String newName) {
        File oldFile = new File(oldPath);
        if (!oldFile.exists()) {
            throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, MessageUtils.message("file.or.directory.not.exists"));
        }

        String parentPath = oldFile.getParent();
        if (parentPath == null) {
            throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, MessageUtils.message("file.or.directory.not.exists"));
        }

        File newFile = new File(parentPath + File.separator + newName);
        boolean flag = oldFile.renameTo(newFile);
        if (!flag) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("rename.failed"));
        }
        return true;
    }
    /**
     * 删除文件或目录（目录会被递归删除）
     * @param path 要删除的路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return true; // 文件不存在视为删除成功
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!deleteFile(f.getAbsolutePath())) {
                        return false;
                    }
                }
            }
        }

        return file.delete();
    }

    public static String calculateFileHash(MultipartFile file) throws NoSuchAlgorithmException, IOException, IllegalArgumentException  {
        // 验证文件有效性
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空或无效");
        }
        MessageDigest digest;
        try {
            // 获取SHA-256消息摘要实例
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // 明确抛出算法不支持的异常，附带详细信息
            throw new NoSuchAlgorithmException("不支持SHA-256哈希算法", e);
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            // 包装IO异常，添加文件信息
            throw new IOException("读取文件内容时发生IO错误: " + file.getOriginalFilename(), e);
        }
        return bytesToHash(digest.digest());
    }

    /**
     * 转换哈希字节数组为十六进制字符串
     * @param bytes 字节数组
     * @return 哈希值字符串
     */
    public static String bytesToHash(byte[] bytes) throws NoSuchAlgorithmException {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            // %02x 保证单字节输出两位十六进制，不足补0（比如0x0a输出"0a"而非"a"）
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    /**
     * 校验文件是否为空
     */
    public static void checkFileEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("upload.file.cannot.be.empty"));
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("file.name.cannot.be.empty"));
        }
    }
}
