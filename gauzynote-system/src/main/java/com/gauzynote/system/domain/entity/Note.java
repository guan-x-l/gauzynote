package com.gauzynote.system.domain.entity;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * notes(Note)实体类
 *
 */
@ToString(exclude = "content")
@Data
public class Note implements Serializable {
    private static final long serialVersionUID = 660900575914289544L;
/**
     * 自增id
     */
    private Long noteId;
/**
     * 用户id
     */
    private Long userId;
/**
     * 名称
     */
    private String noteName;
/**
     * 笔记内容
     */
    private String content;
/**
     * html内容
     */
    private String contentHtml;
/**
     * 创建时间
     */
    private Date createTime;
/**
     * 更新时间
     */
    private Date updateTime;
/**
     * 删除标识（0代表存在 1代表删除）默认0
     */
    private String delFlag;

}

