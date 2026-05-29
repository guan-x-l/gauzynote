package com.gauzynote.framework.service;


import com.gauzynote.common.annotation.Log;
import com.gauzynote.common.domain.entity.OperLog;
import org.aspectj.lang.JoinPoint;
import org.springframework.scheduling.annotation.Async;

import javax.servlet.http.HttpServletRequest;

/**
 * 操作日志表(OperLog)表服务接口
 *
 */
public interface OperLogService {
    @Async(value="OperLogServiceInsertAsync")
    void insertAsync(OperLog operLog);

    /**
     * 获取日志aop的operLog
     * @return operLog
     */
    OperLog getInsertAsyncOperLogByOperLogAspect(JoinPoint joinPoint, Log log, Object result, Long executionTime, HttpServletRequest request, Throwable e);

}
