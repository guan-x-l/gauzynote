package com.gauzynote.system.domain.vo;

import lombok.Data;

@Data
public class UserInfoVO {
    /**
     * 用户账号
     */
    private String username;
    /**
     * 用户昵称
     */
    private String nickname;
    /**
     * 用户类型: （0管理员 1普通用户 2只读用户）
     */
    private String userType;
    /**
     * 用户邮箱
     */
    private String email;
    /**
     * 手机号码
     */
    private String phonenumber;
    /**
     * 头像地址
     */
    private String avatar;
}
