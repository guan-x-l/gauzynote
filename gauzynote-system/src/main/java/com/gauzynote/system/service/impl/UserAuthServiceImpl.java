package com.gauzynote.system.service.impl;

import com.gauzynote.common.domain.entity.SysUser;
import com.gauzynote.framework.service.UserAuthService;
import com.gauzynote.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("UserAuthService")
public class UserAuthServiceImpl implements UserAuthService {

    @Autowired
    private SysUserService sysUserService;


    @Override
    public SysUser selectUserByUsername(String username) {
        return sysUserService.selectUserByUsername(username);
    }
}
