package com.gauzynote.system.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务表(Task)实体类
 */
@Data
public class Task implements Serializable {
    private static final long serialVersionUID = 6815826386355301693L;
    /**
     * 自增id
     */
    private Long taskId;
    /**
     * 所属用户id
     */
    private Long userId;
    /**
     * 任务标题
     */
    private String taskTitle;
    /**
     * 任务内容
     */
    private String taskContent;
    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startTime;
    /**
     * 截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endTime;
    /**
     * 实际完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date actualCompletionTime;
    /**
     * 状态 0: 未开始, 1: 进行中, 2: 已完成, 3: 已停滞
     */
    private String status;
    /**
     * 优先级 0: 低, 1: 中, 2: 高
     */
    private String priority;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 看板排序
     */
    private Integer kanbanSort;
    /**
     * 甘特图排序
     */
    private Integer ganttSort;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 删除标志（0代表存在 2代表删除）
     */
    private String delFlag;
}
