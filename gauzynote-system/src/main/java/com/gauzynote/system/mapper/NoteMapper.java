package com.gauzynote.system.mapper;

import com.gauzynote.system.domain.entity.Note;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * notes(Note)表数据库访问层
 *
 */
public interface NoteMapper {

    /**
     * 通过ID查询单条数据
     *
     * @param noteId 主键
     * @return 实例对象
     */
    Note selectById(@Param("noteId") Long noteId, @Param("userId") Long userId);

    /**
     * 新增数据
     *
     * @param note 实例对象
     * @return 影响行数
     */
    int insert(Note note);


    /**
     * 修改数据
     *
     * @param note 实例对象
     * @return 影响行数
     */
    int update(Note note);

    /**
     * 修改数据
     *
     * @param note 实例对象
     * @return 影响行数
     */
    int updateNoteName(Note note);

    /**
     * 通过主键删除数据
     *
     * @param noteId 主键
     * @return 影响行数
     */
    int deleteById(@Param("noteId") Long noteId,@Param("userId") Long userId);

    /**
     * 通过主键删除数据
     *
     * @param noteIds 主键集合
     * @return 影响行数
     */
    int deleteByIds(@Param("noteIds") List<Long> noteIds,@Param("userId") Long userId);

}

