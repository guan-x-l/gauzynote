package com.gauzynote.application.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 耗时日志
 */
//@Aspect
//@Component
public class RequestTimerAspect {
    private static final Logger logger = LoggerFactory.getLogger(RequestTimerAspect.class);

    // 定义切入点，拦截所有Controller中的方法
    @Pointcut("execution(* com.gauzynote.application.controller..*.*(..))")
    public void controllerPointcut() {}
    @Pointcut("execution(* com.gauzynote.system.mapper..*.*(..))")
    private void pointCutMethod() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String uri = request.getRequestURI();
        String method = request.getMethod();

        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 输出请求耗时日志
            logger.info("请求路径: {}, 请求方法: {}, 执行耗时: {}ms", uri, method, duration);

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            logger.error("请求路径: {}, 请求方法: {}, 执行耗时: {}ms, 异常信息: {}",
                    uri, method, duration, e.getMessage());
            throw e;
        }
    }

    @Around("pointCutMethod()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        Object result = pjp.proceed();
        long end = System.nanoTime();
        logger.info("方法: {}，参数: {}，耗时: {} 毫秒",
                pjp.getSignature(), Arrays.toString(pjp.getArgs()), (end - start) / 1_000_000);
        return result;
    }
}