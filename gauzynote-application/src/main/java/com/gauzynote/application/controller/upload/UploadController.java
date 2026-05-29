package com.gauzynote.application.controller.upload;

import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.AppConfigProperties;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.framework.service.FileUploadTotalSizeService;
import com.gauzynote.system.service.FileUploadService;
import com.gauzynote.system.service.ImageUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static com.gauzynote.common.utils.file.NetworkFileConverter.convertUrlToMultipartFile;

@RestController
@RequestMapping("/upload")
public class UploadController {
    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    @Resource
    private FileUploadTotalSizeService fileUploadTotalSizeService;
    @Resource
    private AppConfigProperties appConfigProperties;
    @Resource
    private ImageUploadService imageUploadService;
    @Resource
    private FileUploadService fileUploadService;

    @GetMapping("/getStorageSpace")
    public AjaxResult getStorageSpace() {
        Map<String, Long> storage = new HashMap<>();
        storage.put("storageSpace", appConfigProperties.getUpload().getMaxTotalCapacity());
        storage.put("usedStorageSpace", fileUploadTotalSizeService.getTotalSize());
        return AjaxResult.success(storage);
    }

    @PostMapping("/image")
    public AjaxResult uploadImage(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "parentNodeId", required = false) Long parentNodeId) {
        try {
            // 调用业务Service处理上传逻辑
            Map<String, Object> result = imageUploadService.upload(file, parentNodeId);
            return AjaxResult.success(result);
        } catch (ServiceException e) {
            log.error(MessageUtils.message("image.upload.fail"), e.getMessage(), e);
            return AjaxResult.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error(MessageUtils.message("image.upload.exception"), e.getMessage(), e);
            return AjaxResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), MessageUtils.message("image.upload.fail"));
        }
    }

    @PostMapping("/networkImage")
    public AjaxResult uploadNetworkImage(@RequestParam("fileUrl") String fileUrl,
                                  @RequestParam(value = "parentNodeId", required = false) Long parentNodeId) {
        try {
            // 调用业务Service处理上传逻辑
            Map<String, Object> result = imageUploadService.upload(convertUrlToMultipartFile(fileUrl), parentNodeId);
            return AjaxResult.success(result);
        } catch (ServiceException e) {
            log.error(MessageUtils.message("image.upload.fail"), e.getMessage(), e);
            return AjaxResult.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error(MessageUtils.message("image.upload.exception"), e.getMessage(), e);
            return AjaxResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), MessageUtils.message("image.upload.fail"));
        }
    }

//    @PostMapping("/file")
    /**
     * @deprecated
     */
    public AjaxResult uploadFile(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "parentNodeId", required = false) Long parentNodeId) {
        try {
            // 调用业务Service处理上传逻辑
            Map<String, Object> result = fileUploadService.upload(file, parentNodeId);
            return AjaxResult.success(result);
        } catch (ServiceException e) {
            log.error(MessageUtils.message("file.upload.fail"), e.getMessage(), e);
            return AjaxResult.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error(MessageUtils.message("file.upload.exception"), e.getMessage(), e);
            return AjaxResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), MessageUtils.message("file.upload.fail"));
        }
    }
}
