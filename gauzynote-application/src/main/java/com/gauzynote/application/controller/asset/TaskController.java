package com.gauzynote.application.controller.asset;

import com.gauzynote.common.annotation.Log;
import com.gauzynote.common.annotation.RequestLimiter;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.enums.BusinessType;
import com.gauzynote.system.domain.entity.Task;
import com.gauzynote.system.service.TaskService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 任务表(Task)控制层
 */
@RestController
@RequestMapping("/asset/task")
public class TaskController {
    @Resource
    private TaskService taskService;

    /**
     * 查询任务列表
     *
     * @param status 状态（可选）
     * @param startDate 截止日期起始（可选，yyyy-MM-dd）
     * @param endDate 截止日期结束（可选，yyyy-MM-dd）
     * @return 任务列表
     */
    @Log(operationName = "task", businessType = BusinessType.QUERY)
    @RequestLimiter
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String status,
                           @RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate) {
        return AjaxResult.success(taskService.selectList(status, startDate, endDate));
    }

    /**
     * 查询单个任务
     *
     * @param taskId 任务id
     * @return 任务详情
     */
    @Log(operationName = "task", businessType = BusinessType.QUERY)
    @RequestLimiter
    @GetMapping("/{taskId}")
    public AjaxResult get(@PathVariable Long taskId) {
        return AjaxResult.success(taskService.selectById(taskId));
    }

    /**
     * 新增任务
     *
     * @param task 任务实体
     * @return 任务id
     */
    @Log(operationName = "task", businessType = BusinessType.ADD)
    @RequestLimiter
    @PostMapping
    public AjaxResult add(@RequestBody Task task) {
        return AjaxResult.success(taskService.insert(task));
    }

    /**
     * 更新任务
     *
     * @param task 任务实体
     * @return 影响行数
     */
    @Log(operationName = "task", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PutMapping
    public AjaxResult edit(@RequestBody Task task) {
        return AjaxResult.success(taskService.update(task));
    }

    /**
     * 批量保存任务排序。
     *
     * @param taskList 仅包含 taskId 与 sort 的任务集合
     * @return 影响行数
     */
    @Log(operationName = "task", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PutMapping("/sort")
    public AjaxResult sort(@RequestParam(defaultValue = "sort") String sortField, @RequestBody List<Task> taskList) {
        return AjaxResult.success(taskService.updateSortList(taskList, sortField));
    }

    /**
     * 删除任务
     *
     * @param taskId 任务id
     * @return 影响行数
     */
    @Log(operationName = "task", businessType = BusinessType.REMOVE)
    @RequestLimiter
    @DeleteMapping("/{taskId}")
    public AjaxResult remove(@PathVariable Long taskId) {
        return AjaxResult.success(taskService.deleteById(taskId));
    }
}
