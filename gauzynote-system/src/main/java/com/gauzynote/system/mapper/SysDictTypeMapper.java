package com.gauzynote.system.mapper;

import com.gauzynote.system.domain.entity.SysDictType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysDictTypeMapper {
    /**
     * 查询字典类型列表（支持条件过滤）
     */
    List<SysDictType> selectDictTypeList(@Param("dictType") SysDictType dictType, @Param("currentUserId") Long currentUserId);

    /**
     * 查询启用状态的全部字典类型（用于下拉选项）
     */
    List<SysDictType> selectDictTypeAll(@Param("currentUserId") Long currentUserId);

    /**
     * 根据主键查询字典类型
     */
    SysDictType selectDictTypeById(@Param("dictId") Long dictId, @Param("currentUserId") Long currentUserId);

    /**
     * 根据 dictType 查询字典类型（用于唯一性校验/关联校验）
     */
    SysDictType selectDictTypeByType(@Param("dictType") String dictType, @Param("currentUserId") Long currentUserId);

    /**
     * 根据 dictType + userId 查询字典类型（用于同归属范围唯一性校验）
     */
    SysDictType selectDictTypeByTypeAndUserId(@Param("dictType") String dictType, @Param("userId") Long userId);

    /**
     * 新增字典类型
     */
    int insertDictType(SysDictType dictType);

    /**
     * 修改字典类型
     */
    int updateDictType(@Param("dictType") SysDictType dictType, @Param("currentUserId") Long currentUserId, @Param("isAdmin") boolean isAdmin);

    /**
     * 根据主键删除字典类型
     */
    int deleteDictTypeById(@Param("dictId") Long dictId, @Param("currentUserId") Long currentUserId, @Param("isAdmin") boolean isAdmin);
}
