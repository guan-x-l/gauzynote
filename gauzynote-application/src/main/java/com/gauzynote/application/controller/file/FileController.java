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

    @GetMapping("/image/**")
    public void getImage(HttpServletRequest request, HttpServletResponse resp) throws IOException {
        // 路径处理
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = URLDecoder.decode(path, "UTF-8");
        String fullImagePath = appConfigProperties.getUpload().getUploadImageDir() + path.replaceFirst("/image/", "");

        checkImageFile(fullImagePath);

        File folder = new File(fullImagePath);
        if (!folder.exists()) {
            log.error("文件不存在:{}", fullImagePath);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path imagePath = Paths.get(fullImagePath);

        // 自动关闭输入流
        try (InputStream in = Files.newInputStream(imagePath)) {
            // 设置正确的Content-Type（根据文件扩展名）
            String contentType = Files.probeContentType(imagePath);
            resp.setContentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);


            // 缓存控制设置
            // 1. 设置过期时间
            resp.setHeader("Cache-Control", "public, max-age=" + appConfigProperties.getUpload().getImage().getMaxAge());

            // 2. 设置过期日期（与max-age配合使用，兼容旧浏览器）
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, appConfigProperties.getUpload().getImage().getMaxAge());
            resp.setDateHeader("Expires", calendar.getTimeInMillis());

            // 3. 添加ETag支持（基于文件最后修改时间和大小）
            FileTime lastModifiedTime = Files.getLastModifiedTime(imagePath);
            long fileSize = Files.size(imagePath);
            String etag = "\"" + lastModifiedTime.toMillis() + "-" + fileSize + "\"";
            resp.setHeader("ETag", etag);

            // 4. 设置最后修改时间
            resp.setDateHeader("Last-Modified", lastModifiedTime.toMillis());

            // 检查If-None-Match头，如果匹配则返回304 Not Modified
            String ifNoneMatch = request.getHeader("If-None-Match");
            if (etag.equals(ifNoneMatch)) {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }

            // 检查If-Modified-Since头，如果未修改则返回304 Not Modified
            long ifModifiedSince = request.getDateHeader("If-Modified-Since");
            if (ifModifiedSince != -1 && lastModifiedTime.toMillis() <= ifModifiedSince) {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }


            IOUtils.copy(in, resp.getOutputStream());
        }
    }

    private void checkImageFile(String imagePath) {
        if (ObjectUtils.isEmpty(imagePath) || imagePath.isEmpty()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST);
        }
        // 是否为配置的前缀，后续改到配置文件中
        if (!imagePath.startsWith(appConfigProperties.getUpload().getUploadImageDir())) {
            log.error("非法路径:{}", imagePath);
            throw new ServiceException(HttpStatus.BAD_REQUEST);
        }
        String fileExt = "image/" + getFileExtension(imagePath);
        if (!isAllowedContentType(fileExt, appConfigProperties.getUpload().getImage().getAllowedContentTypes())) {
            log.error("图片扩展名非法:{}", imagePath);
            throw new ServiceException(HttpStatus.BAD_REQUEST);
        }
        if (!imagePath.startsWith("/")) {
            log.error("该路径不是绝对路径:{}", imagePath);
            throw new ServiceException(HttpStatus.BAD_REQUEST);
        }
    }
}
