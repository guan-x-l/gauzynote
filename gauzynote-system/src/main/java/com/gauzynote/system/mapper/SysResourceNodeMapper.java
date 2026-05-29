package com.gauzynote.system.mapper;

import com.gauzynote.system.domain.entity.SysResourceNode;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资源节点表(SysResourceNode)表数据库访问层
 *
 */
public interface SysResourceNodeMapper {

    /**
     * 通过ID查询单条数据
     *
     * @param nodeId 主键
     * @return 实例对象
     */
    SysResourceNode selectById(Long nodeId);

    /**
     * 查询数据
     *
     * @param nodeIds 主键
     * @return 实例对象
     */
    List<SysResourceNode> selectByIds(List<Long> nodeIds);

    /**
     * 通过ID查询单条数据
     *
     * @param relatedId 主键
     * @return 实例对象
     */
    SysResourceNode selectByRelatedId(Long relatedId);

    /**
     * @return 实例对象
     */
    SysResourceNode selectByParentIdAndNodeName(@Param("userId") Long userId,@Param("parentId") Long parentId, @Param("nodeName")String nodeName,@Param("nodeType")String nodeType);

    /**
     * 通过userId查询数据
     *
     * @param userId userId
     * @return 实例对象
     */
    List<SysResourceNode> selectAllByUserId(Long userId);

    /**
     * 根据nodeId模糊查询nodePath，返回所有符合条件的子集，包含nodeId自身
     * @param nodeId 主键
     */
    List<SysResourceNode> selectAllChildrenByNodeId(@Param("nodeId")Long nodeId);


    SysResourceNode selectAssetsByNodeNameAndDepth(@Param("userId") Long userId, @Param("nodeName")String nodeName, @Param("depth") int depth);

    /**
     * 查询指定行数据
     *
     * @param sysResourceNode 查询条件
     * @param pageable         分页对象
     * @return 对象列表
     */
//    List<SysResourceNode> selectAllByLimit(SysResourceNode sysResourceNode, @Param("pageable") Pageable pageable);

    /**
     * 统计总行数
     *
     * @param sysResourceNode 查询条件
     * @return 总行数
     */
//    long count(SysResourceNode sysResourceNode);

    /**
     * 新增数据
     *
     * @param sysResourceNode 实例对象
     * @return 影响行数
     */
    int insert(SysResourceNode sysResourceNode);

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<SysResourceNode> 实例对象列表
     * @return 影响行数
     */
//    int insertBatch(@Param("entities") List<SysResourceNode> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<SysResourceNode> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
//    int insertOrUpdateBatch(@Param("entities") List<SysResourceNode> entities);

    /**
     * 修改数据
     *
     * @param sysResourceNode 实例对象
     * @return 影响行数
     */
    int update(SysResourceNode sysResourceNode);

    /**
     * updateNodeName
     *
     * @param sysResourceNode 实例对象
     * @return 影响行数
     */
    int updateNodeName(SysResourceNode sysResourceNode);
    /**
     * 修改数据
     *
     * @param sysResourceNode 实例对象
     * @return 影响行数
     */
    int updateNodeParentId(SysResourceNode sysResourceNode);

    /**
     * 通过主键删除数据
     *
     * @param nodeId 主键
     * @return 影响行数
     */
    int deleteById(@Param("nodeId")Long nodeId,@Param("userId") Long userId);

    /**
     * 通过主键删除数据
     *
     * @param nodeIds 主键集合
     * @return 影响行数
     */
    int deleteByIds(@Param("nodeIds") List<Long> nodeIds,@Param("userId") Long userId);

}

