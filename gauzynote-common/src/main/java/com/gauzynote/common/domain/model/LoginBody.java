package com.gauzynote.common.domain.model;

import lombok.Data;

@Data
public class LoginBody {
    /**
     * 用户名
     */
    private String username;

    /**
     * 用户密码
     */
    private String password;

}
