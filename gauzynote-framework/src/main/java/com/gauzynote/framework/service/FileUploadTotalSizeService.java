package com.gauzynote.framework.service;

import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.AppConfigProperties;
import com.gauzynote.common.utils.MessageUtils;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

// 处理上传总容量
@Service
public class FileUploadTotalSizeService {

    // 使用 AtomicLong 保证并发上传时的计数准确
    private final AtomicLong currentTotalSize = new AtomicLong(0);

    @Resource
    private AppConfigProperties appConfigProperties;

    /**
     * 1. 服务启动时，先计算一下磁盘上已有的文件大小
     */
    @PostConstruct
    public void init() {
        try (Stream<Path> walk = Files.walk(Paths.get(appConfigProperties.getUpload().getDir()))) {
            long total = walk.filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum();
            currentTotalSize.set(total);
            System.out.println("初始化完成，当前已占用空间: " + total + " bytes");
        } catch (IOException e) {
            // 目录可能不存在，忽略或创建
            new File(appConfigProperties.getUpload().getDir()).mkdirs();
        }
    }
    /**
     * 检查是否会超限
     */
    public void checkedTotalSize(MultipartFile file){
        if (currentTotalSize.get() + file.getSize() > appConfigProperties.getUpload().getMaxTotalCapacity()) {
            throw new ServiceException(MessageUtils.message("server.storage.full"));
        }
    }
    /**
     * 增加计数（仅新建物理文件时调用）
     *
     * @param size 文件大小（字节）
     */
    public void addTotalSize(long size){
        currentTotalSize.addAndGet(size);
    }
    /**
     * 减少计数（物理文件引用归零、彻底删除时调用）
     *
     * @param size 文件大小（字节）
     */
    public void subtractTotalSize(long size) {
        currentTotalSize.addAndGet(-size);
    }
    /**
     * 总计数
     */
    public long getTotalSize() {
        return  currentTotalSize.get();
    }
}
