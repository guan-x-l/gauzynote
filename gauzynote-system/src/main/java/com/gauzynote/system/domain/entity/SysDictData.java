package com.gauzynote.system.domain.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class SysDictData implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long dictCode;
    private Integer dictSort;
    private String dictLabel;
    private String dictValue;
    private String dictType;
    private Long userId;
    private String cssClass;
    private String listClass;
    private String isDefault;
    private String status;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private String remark;
    /**
     * 当前登录用户是否可对该字典数据执行编辑/删除操作（前端动态按钮控制）
     */
    private Boolean canOperate;
}
