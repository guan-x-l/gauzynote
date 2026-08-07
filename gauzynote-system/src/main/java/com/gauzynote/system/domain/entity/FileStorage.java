package com.gauzynote.system.domain.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 物理文件存储表(FileStorage)实体类
 */
@Data
public class FileStorage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 物理文件唯一标识ID
     */
    private Long storageId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 内容哈希（含扩展名），与物理文件一一对应
     */
    private String fileHash;

    /**
     * 在服务器的存储路径
     */
    private String filePath;

    /**
     * 文件大小（单位：字节）
     */
    private Long fileSize;

    /**
     * 引用该物理文件的file记录数
     */
    private Integer refCount;

    /**
     * 首次上传时间
     */
    private Date createTime;
}
