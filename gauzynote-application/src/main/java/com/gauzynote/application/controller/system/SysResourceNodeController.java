package com.gauzynote.application.controller.system;


import com.gauzynote.common.annotation.Log;
import com.gauzynote.common.annotation.RequestLimiter;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.enums.BusinessType;
import com.gauzynote.system.domain.entity.Note;
import com.gauzynote.system.domain.entity.SysResourceNode;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.system.service.SysResourceNodeService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 资源节点表(SysResourceNode)表控制层
 */
@RestController
@RequestMapping("/system/resourceNode")
public class SysResourceNodeController {
    /**
     * 服务对象
     */
    @Resource
    private SysResourceNodeService sysResourceNodeService;

    /**
     * 通过userId查询数据
     *
     * @return 查询结果
     */
    @GetMapping
    public AjaxResult getAllByUserId() {
        Long userId = SecurityUtils.getUserId();
        return AjaxResult.success(this.sysResourceNodeService.selectAllByUserId(userId));
    }

    /**
     * 新增数据
     *
     * @param sysResourceNode 实体
     * @return 新增结果
     */
    @Log(operationName = "node", businessType = BusinessType.ADD)
    @RequestLimiter
    @PostMapping
    public AjaxResult add(@RequestBody SysResourceNode sysResourceNode) {
        return this.sysResourceNodeService.insert(sysResourceNode) > 0 ? AjaxResult.success(sysResourceNode) : AjaxResult.error();
    }


    /**
     * 新增数据
     *
     * @param sysResourceNode 实体
     * @return 新增结果
     */
    @Log(operationName = "node", businessType = BusinessType.ADD)
    @RequestLimiter
    @PostMapping("/addNodeAndNote")
    public AjaxResult addNodeAndNote(@RequestBody SysResourceNode sysResourceNode) {
        return AjaxResult.success(this.sysResourceNodeService.insertNodeAndNote(sysResourceNode));
    }

    /**
     * 编辑数据
     *
     * @param sysResourceNode 实体
     * @return 编辑结果
     */
    @Log(operationName = "node", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PutMapping
    public AjaxResult edit(@RequestBody SysResourceNode sysResourceNode) {
        sysResourceNode.setUserId(SecurityUtils.getUserId());
        return this.sysResourceNodeService.update(sysResourceNode) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 编辑数据
     *
     * @param sysResourceNode 实体
     * @return 编辑结果
     */
    @Log(operationName = "updateNodeName", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PutMapping("/updateNodeName")
    public AjaxResult editNodeName(@RequestBody SysResourceNode sysResourceNode) {
        return this.sysResourceNodeService.updateNodeName(sysResourceNode) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 编辑数据
     *
     * @param sysResourceNode 实体
     * @return 编辑结果
     */
    @Log(operationName = "updateNodeParentId", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PutMapping("/updateNodeParentId")
    public AjaxResult updateNodeParentId(@RequestBody SysResourceNode sysResourceNode) {
        return this.sysResourceNodeService.updateNodeParentId(sysResourceNode) > 0 ? AjaxResult.success() : AjaxResult.error();
    }


    /**
     * 编辑数据
     *
     * @param note 实体
     * @return 编辑结果
     */
    @Log(operationName = "updateNodeNameByNote", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PutMapping("/updateNodeNameByNote")
    public AjaxResult editNoteName(@RequestBody Note note) {
        return this.sysResourceNodeService.updateNodeNameByNote(note) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @Log(operationName = "node", businessType = BusinessType.REMOVE)
    @RequestLimiter
    @DeleteMapping(value="/{id}")
    public AjaxResult removeById(@PathVariable Long id) {
        return this.sysResourceNodeService.deleteById(id) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

}

