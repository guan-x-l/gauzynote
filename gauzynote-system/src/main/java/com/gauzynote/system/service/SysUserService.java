package com.gauzynote.system.service;

import com.gauzynote.common.constant.UserConstants;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.domain.entity.SysUser;
import com.gauzynote.common.domain.model.LoginBody;
import com.gauzynote.common.domain.model.LoginUser;
import com.gauzynote.common.domain.model.PasswordUpdateBody;
import com.gauzynote.common.enums.UserTypeEnum;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.RandomPasswordGenerator;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.framework.service.TokenService;
import com.gauzynote.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

import static com.gauzynote.common.utils.SecurityUtils.*;

/**
 * 用户信息表(SysUser)表服务接口
 */
@Service("SysUserService")
public class SysUserService {
    @Resource
    private SysUserMapper userMapper;

    @Autowired
    private TokenService tokenService;

    /**
     * 查询数据
     *
     * @return 实例对象
     */
    public List<SysUser> selectAll() {
        return this.userMapper.selectAll();
    }


    /**
     * 通过ID查询单条数据
     *
     * @param userId 主键
     * @return 实例对象
     */
    public SysUser selectUserById(Long userId) {
        return this.userMapper.selectUserById(userId);
    }

    /**
     * 通过ID查询单条数据
     *
     * @return 实例对象
     */
    public SysUser selectUserByUsername(String username) {
        return this.userMapper.selectUserByUsername(username);
    }

    /**
     * 校验用户名称是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    public boolean checkUserNameUnique(SysUser user) {
        Long userId = ObjectUtils.isEmpty(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkUserNameUnique(user.getUsername());
        if (!ObjectUtils.isEmpty(info) && info.getUserId().longValue() != userId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 新增保存用户信息
     *
     * @param user 用户信息
     * @return 实例对象
     */
    @Transactional
    public AjaxResult insertUser(SysUser user) {
        if (!this.checkUserNameUnique(user)) {
            return AjaxResult.error(MessageUtils.message("account.already.exists"));
        }

        SysUser sysUser = new SysUser();
        sysUser.setUsername(user.getUsername());
        sysUser.setNickname(user.getUsername());
        sysUser.setCreateBy(SecurityUtils.getUsername());
        // 随机密码
        String password = RandomPasswordGenerator.generateSimplePassword();
        sysUser.setPassword(SecurityUtils.encryptPassword(password));
        // 新增用户信息
        int rows = userMapper.insertUser(sysUser);
        if (rows > 0) {
            LoginBody loginBody = new LoginBody();
            loginBody.setUsername(user.getUsername());
            loginBody.setPassword(password);
            return AjaxResult.success(loginBody);
        }
        return AjaxResult.error(MessageUtils.message("operation.failed"));
    }

    /**
     * 修改保存用户信息
     *
     * @param user 用户信息
     * @return 实例对象
     */
    @Transactional
    public int updateUser(SysUser user) {
        if (user.getUserType() != null) {
            UserTypeEnum.getByCode(user.getUserType());
        }
        if (user.getUserId().equals(getUserId())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("current.user.cannot.operate"));
        }
        user.setUpdateBy(SecurityUtils.getUsername());
        if (UserConstants.USER_DISABLE.equals(user.getStatus())) {
            tokenService.clearUserTokens(user.getUserId());
        }
        return userMapper.updateUser(user);
    }

    /**
     * 修改数据
     *
     * @param user 实例对象
     * @return 实例对象
     */
    public int updateUserOneself(SysUser user) {
        SysUser sysUser = new SysUser();
        LoginUser loginUser = getLoginUser();
        sysUser.setUserId(loginUser.getUserId());
        sysUser.setUpdateBy(loginUser.getUsername());
        loginUser.getSysUser().setUpdateBy(loginUser.getUsername());
        if (user.getNickname() != null) {
            sysUser.setNickname(user.getNickname());
            loginUser.getSysUser().setNickname(user.getNickname());
        }
        int i = userMapper.updateUser(sysUser);
        if (i > 0) {
            tokenService.setLoginUser(loginUser);
            return i;
        }
        throw new ServiceException(MessageUtils.message("user.update.exception"));
    }

    /**
     * 修改用户密码
     *
     * @return 结果
     */
    public int resetUserPwd(String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("password.mismatch"));
        }
        LoginUser loginUser = getLoginUser();

        boolean same = matchesPassword(newPassword, loginUser.getPassword());
        if (same) {
            throw new  ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("password.new.cannot.match.old"));
        }

        String newEncryptPassword = encryptPassword(newPassword);
        int i = userMapper.resetUserPwd(loginUser.getUserId(), newEncryptPassword);
        if (i > 0) {
            // 更新缓存，密码、密码最后更新时间
            loginUser.setPassword(newEncryptPassword);
            loginUser.getSysUser().setPwdUpdateDate(new Date());
            loginUser.getSysUser().setPassword(newEncryptPassword);
            tokenService.setLoginUser(loginUser);
            return i;
        }
        throw new ServiceException(MessageUtils.message("password.update.exception"));
    }

    /**
     * 修改密码
     *
     * @param updateBody 实例对象
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public int updatePwd(PasswordUpdateBody updateBody) {

        LoginUser loginUser = getLoginUser();

        boolean b1 = matchesPassword(updateBody.getCurrentPassword(), loginUser.getPassword());
        if (!b1) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(),MessageUtils.message("password.current.incorrect"));
        }

        return this.resetUserPwd(updateBody.getNewPassword(), updateBody.getConfirmPassword());
        /*boolean same = matchesPassword(updateBody.getNewPassword(), loginUser.getPassword());
        if (same) {
            throw new ServiceException(MessageUtils.message("password.new.cannot.match.old"));
        }

        String newPassword = encryptPassword(updateBody.getNewPassword());
        int i = userMapper.resetUserPwd(loginUser.getUserId(), newPassword);
        if (i > 0) {
            // 更新缓存，密码、密码最后更新时间
            loginUser.setPassword(newPassword);
            loginUser.getSysUser().setPwdUpdateDate(new Date());
            loginUser.getSysUser().setPassword(newPassword);
            tokenService.setLoginUser(loginUser);
            return i;
        }
        throw new ServiceException(MessageUtils.message("password.update.exception"));*/
    }

    /**
     * 初始化用户密码
     *
     * @param passwordUpdateBody  实例对象
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public int initializePassword(PasswordUpdateBody passwordUpdateBody) {
        if (ObjectUtils.isEmpty(passwordUpdateBody.getConfirmPassword())) {
            throw new  ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("password.confirm.not.blank"));
        }
        if (ObjectUtils.isEmpty(passwordUpdateBody.getNewPassword())) {
            throw new  ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("password.new.not.blank"));
        }
        LoginUser loginUser = getLoginUser();

        if (!ObjectUtils.isEmpty(loginUser.getSysUser().getPwdUpdateDate())) {
            throw new  ServiceException(MessageUtils.message("account.password.already.reset"));
        }

        return this.resetUserPwd(passwordUpdateBody.getNewPassword(), passwordUpdateBody.getConfirmPassword());
    }

    /**
     * 重置用户密码
     *
     * @return 结果
     */
    public int resetUserPwd(SysUser user) {
        SysUser sysUser = new SysUser();

        sysUser.setUserId(user.getUserId());
        sysUser.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        sysUser.setUpdateBy(getUsername());
        return userMapper.updateUser(sysUser);
    }


    /**
     * 通过主键删除数据
     *
     * @param userId 主键
     * @return 是否成功
     */
    @Transactional
    public int deleteUserById(Long userId) {
        if (userId.equals(getUserId())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("current.user.cannot.operate"));
        }
        tokenService.clearUserTokens(userId);
        return userMapper.deleteUserById(userId);
    }
}
