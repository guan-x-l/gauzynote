package com.gauzynote.system.domain.entity;

import lombok.Data;

import java.util.Date;
import java.io.Serializable;

/**
 * 文件表(SysFile)实体类
 *
 */
@Data
public class SysFile implements Serializable {
    private static final long serialVersionUID = -30694568313614477L;
/**
     * 文件唯一标识ID
     */
    private Long fileId;
    /**
     * 用户id
     */
    private Long userId;
/**
     * 关联file_storage表的storage_id
     */
    private Long storageId;
/**
     * 文件名（存在重复）
     */
    private String fileName;
/**
     * 物理文件路径（来自file_storage关联查询，仅查询返回）
     */
    private String filePath;
/**
     * 文件类型
     */
    private String fileType;
/**
     * 文件大小（单位：字节）
     */
    private Long fileSize;
/**
     * 存储引擎（local：本地存储，oss：对象存储等）
     */
    private String storageEngine;
/**
     * 上传时间
     */
    private Date uploadTime;
/**
     * 删除标志（0代表存在 2代表删除）
     */
    private String delFlag;
}
