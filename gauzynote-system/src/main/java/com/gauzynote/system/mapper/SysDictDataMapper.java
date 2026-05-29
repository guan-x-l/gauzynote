package com.gauzynote.system.mapper;

import com.gauzynote.system.domain.entity.SysDictData;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysDictDataMapper {
    /**
     * 查询字典数据列表（支持条件过滤）
     */
    List<SysDictData> selectDictDataList(@Param("dictData") SysDictData dictData, @Param("currentUserId") Long currentUserId);

    /**
     * 根据字典类型查询启用状态的数据列表（用于下拉选项）
     */
    List<SysDictData> selectDictDataByType(@Param("dictType") String dictType, @Param("currentUserId") Long currentUserId);

    /**
     * 根据主键查询字典数据
     */
    SysDictData selectDictDataById(@Param("dictCode") Long dictCode, @Param("currentUserId") Long currentUserId);

    /**
     * 根据 dictType + dictValue 查询字典数据（用于唯一性校验）
     */
    SysDictData selectDictDataByTypeAndValue(@Param("dictType") String dictType, @Param("dictValue") String dictValue, @Param("userId") Long userId);

    /**
     * 统计某字典类型下的字典数据数量（用于删除字典类型前校验）
     */
    int countDictDataByType(@Param("dictType") String dictType);

    /**
     * 新增字典数据
     */
    int insertDictData(SysDictData dictData);

    /**
     * 修改字典数据
     */
    int updateDictData(@Param("dictData") SysDictData dictData, @Param("currentUserId") Long currentUserId, @Param("isAdmin") boolean isAdmin);

    /**
     * 根据主键删除字典数据
     */
    int deleteDictDataById(@Param("dictCode") Long dictCode, @Param("currentUserId") Long currentUserId, @Param("isAdmin") boolean isAdmin);
}
