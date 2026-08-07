package com.gauzynote.system.mapper;

import com.gauzynote.system.domain.entity.FileStorage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 物理文件存储表(FileStorage)表数据库访问层
 */
public interface FileStorageMapper {

    /**
     * 通过用户和文件哈希查询物理文件（不存在返回null）
     *
     * @param userId 用户ID
     * @param fileHash 文件哈希（含扩展名）
     * @return 实例对象
     */
    FileStorage selectByUserIdAndFileHash(@Param("userId") Long userId, @Param("fileHash") String fileHash);

    /**
     * 新增物理文件；同用户同哈希已存在时引用计数+1
     *
     * @param userId 用户ID
     * @param fileHash 文件哈希（含扩展名）
     * @param filePath 物理存储路径
     * @param fileSize 文件大小（字节）
     * @return 影响行数（1表示新增，2表示引用计数+1）
     */
    int upsertIncrement(@Param("userId") Long userId, @Param("fileHash") String fileHash, @Param("filePath") String filePath, @Param("fileSize") Long fileSize);

    /**
     * 锁定并查询物理文件，用于彻底删除时安全扣减引用计数
     *
     * @param storageIds 主键集合
     * @return 实例对象列表
     */
    List<FileStorage> selectByIdsForUpdate(@Param("storageIds") List<Long> storageIds);

    /**
     * 扣减引用计数
     *
     * @param storageId 物理文件ID
     * @param count 扣减数量
     * @return 影响行数
     */
    int decrementRefCount(@Param("storageId") Long storageId, @Param("count") int count);

    /**
     * 删除引用计数为0的物理文件记录
     *
     * @param storageId 物理文件ID
     * @return 影响行数
     */
    int deleteById(@Param("storageId") Long storageId);
}
