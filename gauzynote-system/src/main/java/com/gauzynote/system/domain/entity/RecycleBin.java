package com.gauzynote.system.domain.entity;

import lombok.Data;

import java.util.Date;
import java.io.Serializable;

/**
 * 回收站记录(RecycleBin)实体类
 *
 */
@Data
public class RecycleBin implements Serializable {
    private static final long serialVersionUID = -493639542356184578L;
/**
     * 回收站记录唯一ID
     */
    private Long recycleId;
/**
     * 关联资源节点表的node_id，标识被删除的资源
     */
    private Long nodeId;
/**
     * 用户id
     */
    private Long userId;
/**
     * 资源类型，与resource_node表的node_type一致
     */
    private String resourceType;
/**
     * 删除时的资源名称，用于回收站展示
     */
    private String resourceName;
/**
     * 是否已彻底删除：0-在回收站 2-已彻底删除
     */
    private String permanentlyDeleted;
/**
     * 彻底删除的时间
     */
    private Date permanentDeleteTime;
/**
     * 资源被放入回收站的时间
     */
    private Date deleteTime;
}
