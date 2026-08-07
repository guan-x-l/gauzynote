package com.gauzynote.system.mapper;

import com.gauzynote.system.domain.entity.SysFile;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件表(File)表数据库访问层
 *
 */
public interface FileMapper {

    /**
     * 通过ID查询单条数据
     *
     * @param fileId 主键
     * @return 实例对象
     */
    SysFile selectById(@Param("fileId") Long fileId, @Param("userId") Long userId);

    /**
     * 新增数据
     *
     * @param sysFile 实例对象
     * @return 影响行数
     */
    int insert(SysFile sysFile);

    /**
     * 修改数据
     *
     * @param sysFile 实例对象
     * @return 影响行数
     */
    int update(SysFile sysFile);

    /**
     * 修改文件名
     *
     * @param sysFile 实例对象
     * @return 影响行数
     */
    int updateFileName(SysFile sysFile);

    /**
     * 通过主键删除数据
     *
     * @param fileId 主键
     * @param userId 主键
     * @return 影响行数
     */
    int deleteById(@Param("fileId") Long fileId, @Param("userId") Long userId);

    /**
     * 通过主键删除数据
     *
     * @param fileIds 主键集合
     * @param userId 主键
     * @return 影响行数
     */
    int deleteByIds(@Param("fileIds") List<Long> fileIds, @Param("userId") Long userId);

    /**
     * 彻底删除：物理移除file记录（释放对物理文件的引用）
     *
     * @param fileIds 主键集合
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteRowsByIds(@Param("fileIds") List<Long> fileIds, @Param("userId") Long userId);

    /**
     * 批量恢复（将del_flag从'2'改回'0'）
     *
     * @param fileIds 主键集合
     * @param userId 用户ID
     * @return 影响行数
     */
    int restoreByIds(@Param("fileIds") List<Long> fileIds, @Param("userId") Long userId);

    /**
     * 通过ID列表查询数据（不过滤del_flag，用于彻底删除时查找文件路径）
     *
     * @param fileIds 主键集合
     * @param userId 用户ID
     * @return 实例对象列表
     */
    List<SysFile> selectByIdsIgnoreDelFlag(@Param("fileIds") List<Long> fileIds, @Param("userId") Long userId);

}
