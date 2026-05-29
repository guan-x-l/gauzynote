package com.gauzynote.application.controller.system;

import com.gauzynote.common.annotation.Log;
import com.gauzynote.common.annotation.RequestLimiter;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.enums.BusinessType;
import com.gauzynote.system.domain.entity.SysDictData;
import com.gauzynote.system.service.SysDictDataService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/system/dict/data")
public class SysDictDataController {
    @Resource
    private SysDictDataService dictDataService;

    /**
     * 获取字典数据列表（返回系统公共数据 + 当前用户私有数据）
     */
    @RequestLimiter
    @GetMapping("/list")
    public AjaxResult list(SysDictData dictData) {
        return AjaxResult.success(dictDataService.selectDictDataList(dictData));
    }

    /**
     * 根据字典类型获取启用状态的字典数据列表（仅返回全局数据 + 当前用户私有数据）
     */
    @RequestLimiter
    @GetMapping("/type/{dictType}")
    public AjaxResult getByType(@PathVariable String dictType) {
        return AjaxResult.success(dictDataService.selectDictDataByType(dictType));
    }

    /**
     * 获取字典数据详情（按当前登录用户进行数据权限过滤）
     */
    @RequestLimiter
    @GetMapping("/{dictCode}")
    public AjaxResult getInfo(@PathVariable Long dictCode) {
        return AjaxResult.success(dictDataService.selectDictDataById(dictCode));
    }

    /**
     * 新增字典数据（管理员可新增公共/本人数据，非管理员仅新增本人数据）
     */
    @Log(operationName = "dict_data", businessType = BusinessType.ADD)
    @RequestLimiter
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDictData dictData) {
        return dictDataService.insertDictData(dictData);
    }

    /**
     * 修改字典数据（管理员可改公共/本人数据，非管理员仅可改本人数据）
     */
    @Log(operationName = "dict_data", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDictData dictData) {
        return dictDataService.updateDictData(dictData) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 删除字典数据（管理员可删公共/本人数据，非管理员仅可删本人数据）
     */
    @Log(operationName = "dict_data", businessType = BusinessType.REMOVE)
    @RequestLimiter
    @DeleteMapping("/{dictCode}")
    public AjaxResult remove(@PathVariable Long dictCode) {
        return dictDataService.deleteDictDataById(dictCode) > 0 ? AjaxResult.success() : AjaxResult.error();
    }
}
