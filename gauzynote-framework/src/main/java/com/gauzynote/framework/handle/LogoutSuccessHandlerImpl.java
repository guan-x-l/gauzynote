package com.gauzynote.framework.handle;


import com.alibaba.fastjson2.JSON;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.domain.model.LoginUser;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.ResponseUtils;
import com.gauzynote.framework.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 自定义退出处理类 返回成功
 */
@Configuration
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {
    @Autowired
    private TokenService tokenService;

    /**
     * 退出处理
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String uuid = tokenService.getUUIDByRequest(request);
        if (uuid != null) {
            try {
                LoginUser loginUser = tokenService.getLoginUser(uuid);
                if (!ObjectUtils.isEmpty(loginUser)) {
                    // 删除用户缓存记录
                    tokenService.delLoginUser(loginUser.getToken());
                    ResponseUtils.writeJson(response, HttpServletResponse.SC_OK, JSON.toJSONString(AjaxResult.success(MessageUtils.message("user.logout.success"))));
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
