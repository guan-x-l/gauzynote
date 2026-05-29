package com.gauzynote.application.controller.asset;

import com.gauzynote.common.annotation.RequestLimiter;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.system.domain.entity.Images;
import com.gauzynote.system.service.ImagesService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 图片表(Images)表控制层
 *
 */
@RestController
@RequestMapping("/asset/images")
public class ImagesController {
    /**
     * 服务对象
     */
    @Resource
    private ImagesService imagesService;

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @RequestLimiter
    @GetMapping("/{id}")
    public AjaxResult queryById(@PathVariable("id") Long id) {
        return AjaxResult.success(this.imagesService.selectById(id));
    }

    /**
     * 新增数据
     *
     * @param images 实体
     * @return 新增结果
     */
    @RequestLimiter
    @PostMapping
    public AjaxResult add(Images images) {
        return this.imagesService.insert(images) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 编辑数据
     *
     * @param images 实体
     * @return 编辑结果
     */
    @RequestLimiter
    @PutMapping
    public AjaxResult edit(Images images) {
        return this.imagesService.update(images) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @RequestLimiter
    @DeleteMapping
    public AjaxResult deleteById(Long id) {
        return this.imagesService.deleteById(id) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

}

