package com.gauzynote.system.domain.entity;

import lombok.Data;

import java.util.Date;
import java.io.Serializable;

/**
 * 资源节点表(SysResourceNode)实体类
 */
@Data
public class SysResourceNode implements Serializable {
    private static final long serialVersionUID = -49161782044897349L;
/**
     * 主键
     */
    private Long nodeId;
/**
     * 父节点ID
     */
    private Long parentId;
/**
     * 关联的具体文件ID（仅文件类型节点有效）
     */
    private Long relatedId;
/**
     * 用户ID
     */
    private Long userId;
/**
     * 节点类型（1文件夹）
     */
    private String nodeType;
/**
     * 节点名称
     */
    private String nodeName;
/**
     * 排序
     */
    private Integer sort;
/**
     * 存储完整路径
     */
    private String nodePath;
/**
     * 存储层级深度
     */
    private Integer depth;

    private Date createTime;

    private Date updateTime;

}

