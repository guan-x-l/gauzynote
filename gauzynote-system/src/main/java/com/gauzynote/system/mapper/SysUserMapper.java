package com.gauzynote.system.mapper;

import com.gauzynote.common.domain.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户信息表(SysUser)表数据库访问层
 */
public interface SysUserMapper {
    /**
     * 查询数据
     *
     * @return 实例对象
     */
    List<SysUser> selectAll();

    /**
     * 通过ID查询单条数据
     *
     * @param userId 主键
     * @return 实例对象
     */
    SysUser selectUserById(Long userId);

    /**
     * 通过ID查询单条数据
     *
     * @return 实例对象
     */
    SysUser selectUserByUsername(String username);

    /**
     * 通过ID查询单条数据
     *
     * @return 实例对象
     */
    SysUser checkUserNameUnique(String username);

    /**
     * 新增数据
     *
     * @param sysUser 实例对象
     * @return 影响行数
     */
    int insertUser(SysUser sysUser);

    /**
     * 修改数据
     *
     * @param sysUser 实例对象
     * @return 影响行数
     */
    int updateUser(SysUser sysUser);

    /**
     * 重置用户密码
     *
     * @param userId   用户ID
     * @param password 密码
     * @return 结果
     */
    int resetUserPwd(@Param("userId") Long userId, @Param("password") String password);

    /**
     * 通过主键删除数据
     *
     * @param userId 主键
     * @return 影响行数
     */
    int deleteUserById(@Param("userId") Long userId);

}

