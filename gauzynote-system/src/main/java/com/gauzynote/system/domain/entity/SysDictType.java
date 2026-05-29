package com.gauzynote.system.domain.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class SysDictType implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long dictId;
    private String dictName;
    private String dictType;
    private Long userId;
    private String status;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private String remark;
    /**
     * 当前登录用户是否可对该字典类型执行编辑/删除操作（前端动态按钮控制）
     */
    private Boolean canOperate;
}
