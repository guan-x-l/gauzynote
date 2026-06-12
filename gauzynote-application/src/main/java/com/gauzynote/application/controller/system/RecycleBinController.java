package com.gauzynote.application.controller.system;

import com.gauzynote.common.annotation.Log;
import com.gauzynote.common.annotation.RequestLimiter;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.enums.BusinessType;
import com.gauzynote.system.service.RecycleBinService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 回收站(RecycleBin)表控制层
 */
@RestController
@RequestMapping("/system/recycleBin")
public class RecycleBinController {

    @Resource
    private RecycleBinService recycleBinService;

    /**
     * 获取回收站列表
     *
     * @return 回收站列表
     */
    @GetMapping("/list")
    public AjaxResult list() {
        return AjaxResult.success(this.recycleBinService.listRecycleBin());
    }

    /**
     * 从回收站恢复资源
     *
     * @param recycleId 回收站记录ID
     * @return 恢复结果
     */
    @Log(operationName = "recycleBin", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PostMapping("/restore/{recycleId}")
    public AjaxResult restore(@PathVariable Long recycleId) {
        this.recycleBinService.restore(recycleId);
        return AjaxResult.success();
    }

    /**
     * 彻底删除回收站中的资源
     *
     * @param recycleId 回收站记录ID
     * @return 删除结果
     */
    @Log(operationName = "recycleBin", businessType = BusinessType.REMOVE)
    @RequestLimiter
    @DeleteMapping("/{recycleId}")
    public AjaxResult permanentDelete(@PathVariable Long recycleId) {
        this.recycleBinService.permanentDelete(recycleId);
        return AjaxResult.success();
    }
}
