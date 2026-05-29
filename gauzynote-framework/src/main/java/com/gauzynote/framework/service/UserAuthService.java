package com.gauzynote.framework.service;

import com.gauzynote.common.domain.entity.SysUser;

public interface UserAuthService {
    /**
     * 通过ID查询单条数据
     *
     * @return 实例对象
     */
    SysUser selectUserByUsername(String username);

}
