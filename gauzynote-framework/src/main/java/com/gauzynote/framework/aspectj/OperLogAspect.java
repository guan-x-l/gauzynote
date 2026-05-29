package com.gauzynote.framework.aspectj;

import com.gauzynote.common.annotation.Log;
import com.gauzynote.framework.service.OperLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


@Aspect
@Component
public class OperLogAspect {
    @Resource
    private OperLogService operLogService;

    // 定义切入点，使用Log注解作为切入点
    @Pointcut("@annotation(log)")
    public void logPointcut(Log log) {
    }

    // 在方法抛出异常时执行的通知，用于保存日志
    @AfterThrowing(pointcut = "logPointcut(log)", throwing = "e", argNames = "joinPoint,log,e")
    public void saveLogOnException(JoinPoint joinPoint, Log log, Throwable e) {
        operLogService.insertAsync(operLogService.getInsertAsyncOperLogByOperLogAspect(joinPoint, log, null, null, obtainHttpServletRequest(), e));
    }

    @Around(value = "logPointcut(log)", argNames = "joinPoint,log")
    public Object saveLog(ProceedingJoinPoint joinPoint, Log log) throws Throwable {
        long startTime = System.currentTimeMillis(); // 记录方法开始时间
        Object result = joinPoint.proceed();// 执行方法逻辑
        Long executionTime = System.currentTimeMillis() - startTime; // 计算执行时间
        operLogService.insertAsync(operLogService.getInsertAsyncOperLogByOperLogAspect(joinPoint, log, result, executionTime, obtainHttpServletRequest(), null));
        return result;
    }

    private HttpServletRequest obtainHttpServletRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest();
        }
        return null;
    }
}