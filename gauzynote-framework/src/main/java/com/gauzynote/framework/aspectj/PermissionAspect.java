package com.gauzynote.framework.aspectj;

import com.gauzynote.common.annotation.RequiredPermission;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.enums.UserTypeEnum;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(com.gauzynote.common.annotation.RequiredPermission)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequiredPermission requiredPermission = method.getAnnotation(RequiredPermission.class);
        UserTypeEnum[] allowedTypes = requiredPermission.value();

        String currentUserType = SecurityUtils.getLoginUser().getSysUser().getUserType();
        UserTypeEnum currentEnum = UserTypeEnum.getByCode(currentUserType);

        if (!isTypeAllowed(currentEnum, allowedTypes)) {
            return AjaxResult.error(HttpServletResponse.SC_FORBIDDEN, MessageUtils.message("no.permission.for.current.operation"));
        }
        return joinPoint.proceed();
    }

    private boolean isTypeAllowed(UserTypeEnum current, UserTypeEnum[] allowed) {
        for (UserTypeEnum type : allowed) {
            if (type == current) {
                return true;
            }
        }
        return false;
    }
}