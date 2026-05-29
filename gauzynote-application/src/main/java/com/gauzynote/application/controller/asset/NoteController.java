package com.gauzynote.application.controller.asset;


import com.gauzynote.common.annotation.Log;
import com.gauzynote.common.annotation.RequestLimiter;
import com.gauzynote.common.enums.BusinessType;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.system.domain.entity.Note;
import com.gauzynote.system.service.NoteService;
import com.gauzynote.common.domain.AjaxResult;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * notes(Note)表控制层
 */
@RestController
@RequestMapping("/asset/note")
public class NoteController {
    /**
     * 服务对象
     */
    @Resource
    private NoteService noteService;

    /**
     * 通过ID查询数据
     *
     * @return 查询结果
     */
    @Log(operationName = "note", businessType = BusinessType.QUERY)
    @RequestLimiter
    @GetMapping(value = "/{noteId}")
    public AjaxResult get(@PathVariable Long noteId) {
        return AjaxResult.success(this.noteService.selectById(noteId));
    }

    /**
     * 新增数据
     *
     * @param note 实体
     * @return 新增结果
     * @deprecated
     */
    @Log(operationName = "note", businessType = BusinessType.ADD)
    @PostMapping
    public AjaxResult add(@RequestBody Note note) {
        return this.noteService.insert(note) > 0 ? AjaxResult.success(note) : AjaxResult.error();
    }

    /**
     * 编辑数据
     *
     * @param note 实体
     * @return 编辑结果
     */
    @Log(operationName = "note", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PutMapping
    public AjaxResult edit(@RequestBody Note note) {
        return this.noteService.update(note) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 删除数据
     *
     * @param noteId 主键
     * @return 删除是否成功
     */
    @Log(operationName = "note", businessType = BusinessType.REMOVE)
    @RequestLimiter
    @DeleteMapping(value = "/{noteId}")
    public AjaxResult remove(@PathVariable Long noteId) {
        return this.noteService.deleteById(noteId) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

}
