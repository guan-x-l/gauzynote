package com.gauzynote.application.controller.system;

import com.gauzynote.common.annotation.Log;
import com.gauzynote.common.annotation.RequestLimiter;
import com.gauzynote.common.constant.Constants;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.system.domain.vo.UserInfoVO;
import com.gauzynote.common.domain.model.LoginBody;
import com.gauzynote.common.domain.model.LoginUser;
import com.gauzynote.common.enums.BusinessType;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.framework.service.TokenService;
import com.gauzynote.common.domain.entity.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;

@RestController
public class SysLoginController {
    @Resource
    private AuthenticationManager authenticationManager;


    @Autowired
    private TokenService tokenService;

    /**
     * 登录方法
     *
     * @param loginBody 登录信息
     * @return 结果
     */
    @Log(operationName = "login", businessType = BusinessType.LOGIN, isSaveResponseParams = true)
    @RequestLimiter
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody) {
        // 登录前置校验
        loginPreCheck(loginBody.getUsername(), loginBody.getPassword());
        // 用户验证
        Authentication authentication;
        try {
            // 用户验证
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginBody.getUsername(), loginBody.getPassword());
            // 该方法会去调用 UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
        // 从security认证信息中获取用户
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 生成 Token
        String token = tokenService.createToken(loginUser);

        AjaxResult ajax = AjaxResult.success();
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser sysUser = loginUser.getSysUser();
        UserInfoVO user = new UserInfoVO();
        user.setAvatar(sysUser.getAvatar());
        user.setEmail(sysUser.getEmail());
        user.setNickname(sysUser.getNickname());
        user.setPhonenumber(sysUser.getPhonenumber());
        user.setUsername(sysUser.getUsername());
        user.setUserType(sysUser.getUserType());

        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("isDefaultModifyPwd", initPasswordIsModify(sysUser.getPwdUpdateDate()));
        return ajax;
    }



    // 检查初始密码是否提醒修改
    public boolean initPasswordIsModify(Date pwdUpdateDate) {
        return pwdUpdateDate == null;
    }

    /**
     * 登录前置校验
     *
     * @param username 用户名
     * @param password 用户密码
     */
    public void loginPreCheck(String username, String password) {
        // 用户名或密码为空 错误
        if (ObjectUtils.isEmpty(username) || ObjectUtils.isEmpty(password)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST);
        }
    }
}
