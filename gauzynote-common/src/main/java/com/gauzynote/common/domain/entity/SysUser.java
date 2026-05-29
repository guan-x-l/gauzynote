package com.gauzynote.common.domain.entity;


import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户信息表(SysUser)实体类
 *
 */
@Data
public class SysUser implements Serializable {
    private static final long serialVersionUID = 172099885732216614L;
/**
     * 用户ID
     */
    private Long userId;
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
/**
     * 密码
     */
    private String password;
/**
     * 账号状态（0正常 1停用）
     */
    private String status;
/**
     * 删除标志（0代表存在 2代表删除）
     */
    private String delFlag;
/**
     * 最后登录IP
     */
    private String loginIp;
/**
     * 最后登录时间
     */
    private Date loginDate;
/**
     * 密码最后更新时间
     */
    private Date pwdUpdateDate;
/**
     * 创建者
     */
    private String createBy;
/**
     * 创建时间
     */
    private Date createTime;
/**
     * 更新者
     */
    private String updateBy;
/**
     * 更新时间
     */
    private Date updateTime;
/**
     * 备注
     */
    private String remark;

}

