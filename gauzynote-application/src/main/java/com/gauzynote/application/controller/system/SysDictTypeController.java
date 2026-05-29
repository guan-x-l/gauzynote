package com.gauzynote.application.controller.system;

import com.gauzynote.common.annotation.Log;
import com.gauzynote.common.annotation.RequestLimiter;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.enums.BusinessType;
import com.gauzynote.system.domain.entity.SysDictType;
import com.gauzynote.system.service.SysDictTypeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/system/dict/type")
public class SysDictTypeController {
    @Resource
    private SysDictTypeService dictTypeService;

    /**
     * 获取字典类型列表（返回系统公共数据 + 当前用户私有数据）
     */
    @RequestLimiter
    @GetMapping("/list")
    public AjaxResult list(SysDictType dictType) {
        return AjaxResult.success(dictTypeService.selectDictTypeList(dictType));
    }

    /**
     * 获取启用状态的字典类型列表（用于下拉选项）
     */
    @RequestLimiter
    @GetMapping("/optionselect")
    public AjaxResult optionselect() {
        return AjaxResult.success(dictTypeService.selectDictTypeAll());
    }

    /**
     * 获取字典类型详情（按当前用户可见范围返回）
     */
    @RequestLimiter
    @GetMapping("/{dictId}")
    public AjaxResult getInfo(@PathVariable Long dictId) {
        return AjaxResult.success(dictTypeService.selectDictTypeById(dictId));
    }

    /**
     * 新增字典类型（管理员可新增公共/本人数据，非管理员仅新增本人数据）
     */
    @Log(operationName = "dict_type", businessType = BusinessType.ADD)
    @RequestLimiter
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDictType dictType) {
        return dictTypeService.insertDictType(dictType);
    }

    /**
     * 修改字典类型（管理员可改公共/本人数据，非管理员仅可改本人数据）
     */
    @Log(operationName = "dict_type", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDictType dictType) {
        return dictTypeService.updateDictType(dictType) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 删除字典类型（管理员可删公共/本人数据，非管理员仅可删本人数据）
     */
    @Log(operationName = "dict_type", businessType = BusinessType.REMOVE)
    @RequestLimiter
    @DeleteMapping("/{dictId}")
    public AjaxResult remove(@PathVariable Long dictId) {
        return dictTypeService.deleteDictTypeById(dictId) > 0 ? AjaxResult.success() : AjaxResult.error();
    }
}
