package com.gauzynote.application.controller.asset;

import com.gauzynote.common.annotation.RequestLimiter;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.system.domain.entity.SysFile;
import com.gauzynote.system.service.FileService;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件表(File)表控制层
 *
 */
@RestController
@RequestMapping("/asset/files")
public class FileResourceController {
    private static final Logger log = LoggerFactory.getLogger(FileResourceController.class);

    /**
     * 服务对象
     */
    @Resource
    private FileService fileService;

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @RequestLimiter
    @GetMapping("/{id}")
    public AjaxResult queryById(@PathVariable("id") Long id) {
        return AjaxResult.success(this.fileService.selectById(id));
    }

    /**
     * 新增数据
     *
     * @param sysFile 实体
     * @return 新增结果
     */
    @RequestLimiter
    @PostMapping
    public AjaxResult add(SysFile sysFile) {
        return this.fileService.insert(sysFile) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 编辑数据
     *
     * @param sysFile 实体
     * @return 编辑结果
     */
    @RequestLimiter
    @PutMapping
    public AjaxResult edit(SysFile sysFile) {
        return this.fileService.update(sysFile) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @RequestLimiter
    @DeleteMapping
    public AjaxResult deleteById(Long id) {
        return this.fileService.deleteById(id) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 下载文件（使用原始文件名）
     *
     * @param id 文件ID
     */
    @GetMapping("/{id}/download")
    public void download(@PathVariable("id") Long id, HttpServletResponse resp) throws IOException {
        SysFile sysFile = this.fileService.selectById(id);
        if (sysFile == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path filePath = Paths.get(sysFile.getFilePath());
        java.io.File diskFile = filePath.toFile();
        if (!diskFile.exists()) {
            log.error("文件不存在:{}", sysFile.getFilePath());
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 用原始文件名作为下载文件名
        String downloadFileName = sysFile.getFileName();
        String encodedFileName = URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20");

        resp.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName
                + "\"; filename*=UTF-8''" + encodedFileName);

        try (InputStream in = Files.newInputStream(filePath)) {
            IOUtils.copy(in, resp.getOutputStream());
        }
    }

}
