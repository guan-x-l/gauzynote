package com.gauzynote.framework.service;

import com.gauzynote.common.domain.model.LoginUser;
import com.gauzynote.common.domain.entity.SysUser;
import com.gauzynote.common.enums.UserStatus;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final static Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private UserAuthService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userService.selectUserByUsername(username);
        if (ObjectUtils.isEmpty(user)) {
            log.info("登录用户：{} 不存在", username);
            throw new ServiceException(MessageUtils.message("user.not.exist"));
        }/* else if (UserStatus.DELETED.getCode().equals(user.getDelFlag())) {
            // sql有限制查不出这种类型的数据,无法到达此处
            log.info("登录用户：{} 已被删除", username);
            throw new ServiceException(MessageUtils.message("user.not.exist"));
        }*/ else if (!UserStatus.NORMAL.getCode().equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用", username);
            throw new ServiceException(MessageUtils.message("user.suspended"));
        }
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getUsername(), user.getPassword(), LoginUser.LOGIN_METHOD_PASSWORD, user);
        return loginUser;
    }
}
