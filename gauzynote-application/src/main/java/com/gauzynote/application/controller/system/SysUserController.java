package com.gauzynote.application.controller.system;

import com.gauzynote.common.annotation.Log;
import com.gauzynote.common.annotation.RequestLimiter;
import com.gauzynote.common.annotation.RequiredPermission;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.domain.model.PasswordUpdateBody;
import com.gauzynote.common.enums.BusinessType;
import com.gauzynote.common.enums.UserTypeEnum;
import com.gauzynote.common.domain.entity.SysUser;
import com.gauzynote.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/system/user")
public class SysUserController {

    @Autowired
    private SysUserService userService;

    /**
     * 根据用户编号获取详细信息
     */
    @RequiredPermission(value = UserTypeEnum.ADMIN)
    @RequestLimiter
    @GetMapping("/list")
    public AjaxResult getUserList() {
        return AjaxResult.success(userService.selectAll());
    }

    /**
     * 根据用户编号获取详细信息
     */
//    @Log(operationName = "根据用户编号获取详细信息", businessType = BusinessType.QUERY)
//    @RequestLimiter(QPS = 4, timeout = 5000)
    @RequiredPermission(value = UserTypeEnum.ADMIN)
    @RequestLimiter
    @GetMapping("/{userId}")
    public AjaxResult getUserById(@PathVariable Long userId) {
        return AjaxResult.success(userService.selectUserById(userId));
    }

    /**
     * 新增用户
     */
    @Log(operationName = "user", businessType = BusinessType.ADD)
    @RequiredPermission(value = UserTypeEnum.ADMIN)
    @RequestLimiter
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUser user) {
        return userService.insertUser(user);
    }

    /**
     * 修改用户
     */
    @Log(operationName = "user", businessType = BusinessType.EDIT)
    @RequiredPermission(value = UserTypeEnum.ADMIN)
    @RequestLimiter
    @PutMapping("/editOfAdmin")
    public AjaxResult editOfAdmin(@Validated @RequestBody SysUser user) {
        return userService.updateUser(user) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 修改用户
     */
    @Log(operationName = "userOneself", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysUser user) {
        return userService.updateUserOneself(user) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 修改密码
     */
    @Log(operationName = "pwd", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PutMapping("/editPwd")
    public AjaxResult editPwd(@Validated @RequestBody @NotNull PasswordUpdateBody updateBody) {
        return userService.updatePwd(updateBody) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 删除用户
     */
    @Log(operationName = "user", businessType = BusinessType.REMOVE)
    @RequiredPermission(value = UserTypeEnum.ADMIN)
    @RequestLimiter
    @DeleteMapping("/{userId}")
    public AjaxResult remove(@PathVariable Long userId) {
        return userService.deleteUserById(userId) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 重置用户密码
     * @deprecated 是否需要有管理员来重置用户密码的场景
     */
    @Log(operationName = "resetPwd", businessType = BusinessType.EDIT)
    @RequiredPermission(value = UserTypeEnum.ADMIN)
    @RequestLimiter
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody SysUser user) {

        return userService.resetUserPwd(user) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 初始重置密码
     */
    @Log(operationName = "initializePassword", businessType = BusinessType.EDIT)
    @RequestLimiter
    @PutMapping("/initializePassword")
    public AjaxResult initializePassword(@RequestBody PasswordUpdateBody passwordUpdateBody) {
        return userService.initializePassword(passwordUpdateBody) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 状态修改
     */
    @Log(operationName = "updateStatus", businessType = BusinessType.EDIT)
    @RequiredPermission(value = UserTypeEnum.ADMIN)
    @RequestLimiter
    @PutMapping("/updateStatus")
    public AjaxResult updateStatus(@RequestBody SysUser user) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(user.getUserId());
        sysUser.setStatus(user.getStatus());
        return userService.updateUser(sysUser) > 0 ? AjaxResult.success() : AjaxResult.error();
    }

}
