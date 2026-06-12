package com.gauzynote.system.mapper;

import com.gauzynote.system.domain.entity.RecycleBin;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 回收站记录(RecycleBin)表数据库访问层
 *
 */
public interface RecycleBinMapper {

    /**
     * 新增数据
     *
     * @param recycleBin 实例对象
     * @return 影响行数
     */
    int insert(RecycleBin recycleBin);

    /**
     * 通过用户ID查询回收站列表
     *
     * @param userId 用户ID
     * @return 实例对象列表
     */
    List<RecycleBin> selectByUserId(@Param("userId") Long userId);

    /**
     * 通过ID查询单条数据
     *
     * @param recycleId 主键
     * @param userId 用户ID
     * @return 实例对象
     */
    RecycleBin selectById(@Param("recycleId") Long recycleId, @Param("userId") Long userId);

    /**
     * 通过主键删除数据（恢复时调用）
     *
     * @param recycleId 主键
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteById(@Param("recycleId") Long recycleId, @Param("userId") Long userId);

    /**
     * 标记为已彻底删除
     *
     * @param recycleId 主键
     * @param userId 用户ID
     * @return 影响行数
     */
    int markPermanentlyDeleted(@Param("recycleId") Long recycleId, @Param("userId") Long userId);
}
