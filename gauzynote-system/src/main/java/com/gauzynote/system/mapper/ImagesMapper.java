package com.gauzynote.system.mapper;

import com.gauzynote.system.domain.entity.Images;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 图片表(Images)表数据库访问层
 *
 */
public interface ImagesMapper {

    /**
     * 通过ID查询单条数据
     *
     * @param imageId 主键
     * @return 实例对象
     */
    Images selectById(@Param("imageId")Long imageId, @Param("userId") Long userId);

    /**
     * 新增数据
     *
     * @param images 实例对象
     * @return 影响行数
     */
    int insert(Images images);

    /**
     * 修改数据
     *
     * @param images 实例对象
     * @return 影响行数
     */
    int update(Images images);
    /**
     * 修改数据
     *
     * @param images 实例对象
     * @return 影响行数
     */
    int updateImageName(Images images);

    /**
     * 通过主键删除数据
     *
     * @param imageId 主键
     * @param userId 主键
     * @return 影响行数
     */
    int deleteById(@Param("imageId")Long imageId,@Param("userId") Long userId);

    /**
     * 通过主键删除数据
     *
     * @param imageIds 主键集合
     * @param userId 主键
     * @return 影响行数
     */
    int deleteByIds(@Param("imageIds")List<Long> imageIds,@Param("userId") Long userId);

}

