package com.gauzynote.system.service;

import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.system.domain.entity.Note;
import com.gauzynote.system.mapper.NoteMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * notes(Note)表服务接口
 *
 */
@Service("noteService")
public class NoteService {
    @Resource
    private NoteMapper noteDao;

    /**
     * 通过ID查询单条数据
     *
     * @param noteId 主键
     * @return 实例对象
     */
    public Note selectById(Long noteId) {
        Long userId = SecurityUtils.getUserId();
        return this.noteDao.selectById(noteId, userId);
    }


    /**
     * 新增数据
     *
     * @param note 实例对象
     * @return 实例对象
     */
    public int insert(Note note) {
        note.setUserId(SecurityUtils.getUserId());
        int insert = this.noteDao.insert(note);
        if (insert <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("note.creation.failed"));
        }
        return insert;
    }

    /**
     * 修改数据
     *
     * @param note 实例对象
     * @return 实例对象
     */
    public int update(Note note) {
        note.setUserId(SecurityUtils.getUserId());
        int update = this.noteDao.update(note);
        if (update <= 0){
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("note.modification.failed"));
        }
        return update;
    }

    public int updateNoteName(Note note) {
        note.setUserId(SecurityUtils.getUserId());
        int update = this.noteDao.updateNoteName(note);
        if (update <= 0){
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("note.modification.failed"));
        }
        return update;
    }

    /**
     * 通过主键删除数据
     *
     * @param noteId 主键
     * @return 是否成功
     */
    public int deleteById(Long noteId) {
        Long userId = SecurityUtils.getUserId();
        int i = this.noteDao.deleteById(noteId, userId);
        if (i <= 0){
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("note.deletion.failed"));
        }
        return i;
    }
    /**
     * 通过主键删除数据
     *
     * @param noteIds 主键
     */
    public void deleteByIds(List<Long> noteIds) {
        Long userId = SecurityUtils.getUserId();
        int i = this.noteDao.deleteByIds(noteIds, userId);
        if (i <= 0){
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("note.deletion.failed"));
        }
    }
}
