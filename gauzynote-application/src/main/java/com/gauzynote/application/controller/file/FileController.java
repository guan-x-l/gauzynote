package com.gauzynote.application.controller.file;

import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.AppConfigProperties;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Calendar;

import static com.gauzynote.common.utils.file.FileUtils.getFileExtension;
import static com.gauzynote.common.utils.file.FileUtils.isAllowedContentType;

@RestController
@RequestMapping
public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Resource
    private AppConfigProperties appConfigProperties;

    /**
     * 通用文件服务端点
     */
    @GetMapping("/file/**")
    public void getFile(HttpServletRequest request, HttpServletResponse resp) throws IOException {
        // 路径处理
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = URLDecoder.decode(path, "UTF-8");
        String fullFilePath = appConfigProperties.getUpload().getUploadOtherFileDir() + path.replaceFirst("/file/", "");

        checkFilePath(fullFilePath, false);

        serveFile(fullFilePath, request, resp);
    }

    /**
     * 图片文件服务端点（向后兼容，保留旧 /image/** 路径）
     */
    @GetMapping("/image/**")
    public void getImage(HttpServletRequest request, HttpServletResponse resp) throws IOException {
        // 路径处理
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = URLDecoder.decode(path, "UTF-8");
        String fullImagePath = appConfigProperties.getUpload().getUploadImageDir() + path.replaceFirst("/image/", "");

        checkFilePath(fullImagePath, true);

        serveFile(fullImagePath, request, resp);
    }

    private void serveFile(String fullPath, HttpServletRequest request, HttpServletResponse resp) throws IOException {
        java.io.File folder = new java.io.File(fullPath);
        if (!folder.exists()) {
            log.error("文件不存在:{}", fullPath);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path filePath = Paths.get(fullPath);

        // 自动关闭输入流
        try (InputStream in = Files.newInputStream(filePath)) {
            // 设置正确的Content-Type（根据文件扩展名）
            String contentType = Files.probeContentType(filePath);
            resp.setContentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);

            // 缓存控制设置
            int maxAge = appConfigProperties.getUpload().getImage().getMaxAge();
            resp.setHeader("Cache-Control", "public, max-age=" + maxAge);

            // 设置过期日期
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, maxAge);
            resp.setDateHeader("Expires", calendar.getTimeInMillis());

            // 添加ETag支持
            FileTime lastModifiedTime = Files.getLastModifiedTime(filePath);
            long fileSize = Files.size(filePath);
            String etag = "\"" + lastModifiedTime.toMillis() + "-" + fileSize + "\"";
            resp.setHeader("ETag", etag);

            // 设置最后修改时间
            resp.setDateHeader("Last-Modified", lastModifiedTime.toMillis());

            // 检查If-None-Match头
            String ifNoneMatch = request.getHeader("If-None-Match");
            if (etag.equals(ifNoneMatch)) {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }

            // 检查If-Modified-Since头
            long ifModifiedSince = request.getDateHeader("If-Modified-Since");
            if (ifModifiedSince != -1 && lastModifiedTime.toMillis() <= ifModifiedSince) {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }

            IOUtils.copy(in, resp.getOutputStream());
        }
    }

    private void checkFilePath(String filePath, boolean imageOnly) {
        if (ObjectUtils.isEmpty(filePath) || filePath.isEmpty()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST);
        }

        String baseDir = imageOnly
                ? appConfigProperties.getUpload().getUploadImageDir()
                : appConfigProperties.getUpload().getUploadOtherFileDir();

        if (!filePath.startsWith(baseDir)) {
            log.error("非法路径:{}", filePath);
            throw new ServiceException(HttpStatus.BAD_REQUEST);
        }

        // 仅对图片路径做扩展名校验
        if (imageOnly) {
            String fileExt = "image/" + getFileExtension(filePath);
            if (!isAllowedContentType(fileExt, appConfigProperties.getUpload().getImage().getAllowedContentTypes())) {
                log.error("图片扩展名非法:{}", filePath);
                throw new ServiceException(HttpStatus.BAD_REQUEST);
            }
        }

        if (!filePath.startsWith("/")) {
            log.error("该路径不是绝对路径:{}", filePath);
            throw new ServiceException(HttpStatus.BAD_REQUEST);
        }
    }
}
