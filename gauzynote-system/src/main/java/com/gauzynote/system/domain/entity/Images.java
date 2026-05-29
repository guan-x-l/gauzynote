package com.gauzynote.system.domain.entity;

import lombok.Data;

import java.util.Date;
import java.io.Serializable;

/**
 * 图片表(Images)实体类
 *
 */
@Data
public class Images implements Serializable {
    private static final long serialVersionUID = -30694568313614477L;
/**
     * 图片唯一标识ID
     */
    private Long imageId;
/**
     * 用户id
     */
    private Long userId;
/**
     * 图片文件名（存在重复）
     */
    private String imageName;
/**
     * 图片在服务器的存储路径
     */
    private String imagePath;
/**
     * 图片文件类型（如jpeg、png）
     */
    private String imageType;
/**
     * 图片文件大小（单位：字节）
     */
    private Long imageSize;
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

