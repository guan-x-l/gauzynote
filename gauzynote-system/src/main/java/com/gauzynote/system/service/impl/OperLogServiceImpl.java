package com.gauzynote.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.gauzynote.common.annotation.Log;
import com.gauzynote.common.domain.entity.OperLog;
import com.gauzynote.framework.service.OperLogService;
import com.gauzynote.system.mapper.OperLogMapper;
import org.aspectj.lang.JoinPoint;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import com.gauzynote.common.utils.IpUtil;

/**
 * 操作日志表(OperLog)表服务实现类
 *
 */
@Service("operLogService")
public class OperLogServiceImpl implements OperLogService {
    @Resource
    private OperLogMapper operLogDao;

    /**
     * 获取AOP参数，并处理AppOperLog实体类再新增AppOperLog数据
     * 异步方法
     */
    @Override
    @Async(value="OperLogServiceInsertAsync")
    public void insertAsync(OperLog operLog) {
        this.operLogDao.insert(operLog);
    }
    @Override
    public OperLog getInsertAsyncOperLogByOperLogAspect(JoinPoint joinPoint, Log log, Object result, Long executionTime, HttpServletRequest request, Throwable e) {
        OperLog appOperLog = new OperLog();
        try {
            appOperLog.setOperationName(log.operationName()); // 操作名称
            appOperLog.setBusinessType(log.businessType().ordinal()); // 业务类型
            // 如果需要保存请求参数
            if (log.isSaveRequestParams() && joinPoint != null) {
                // 获取方法参数
                Object[] args = joinPoint.getArgs();
                StringBuilder sb = new StringBuilder();
                for (Object arg : args) {
                    sb.append(arg).append(",");
                }
                if (sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                // 设置请求参数
                appOperLog.setRequestParams(sb.toString());
            }

            appOperLog.setRequestMethod(request.getMethod()); // 请求方法
            appOperLog.setRequestUrl(getUrl(request)); // 请求URI
            appOperLog.setCurrentUrl(request.getHeader("Current-Url"));
            appOperLog.setUuid(request.getHeader("Current-Uuid"));
            appOperLog.setClientVersion(request.getHeader("Current-V"));
            appOperLog.setRequestOrigin(request.getHeader("Origin"));
            appOperLog.setHost(request.getHeader("Host"));
            appOperLog.setIpAddress(IpUtil.getIpAddr(request));
            appOperLog.setReferrer(request.getHeader("Current-Referrer"));
            appOperLog.setAncestorOrigins(request.getHeader("Current-AncestorOrigins"));
            appOperLog.setUserAgent(request.getHeader("User-Agent")); // 用户代理信息
            appOperLog.setCreateTime(new Date()); // 操作时间
            appOperLog.setResponseStatus(String.valueOf(HttpServletResponse.SC_OK));
            if (log.isSaveResponseParams()){
                appOperLog.setResponseData(JSON.toJSONString(result));
            }
            appOperLog.setExecutionTime(executionTime);
        } catch (Exception exception) {
            appOperLog.setResponseStatus(String.valueOf(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            String resultMsg = "处理日志错误";
            resultMsg += exception.getStackTrace()[0].toString();
            resultMsg += exception.getStackTrace()[1].toString();
            appOperLog.setResponseData(resultMsg);
        }
        if (e != null) {
            // 如果发生异常，设置响应状态为失败
            appOperLog.setResponseStatus(String.valueOf(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            appOperLog.setResponseData(e.getMessage());
        }

        return appOperLog;
    }
    public static String getUrl(HttpServletRequest request) {
        String url= request.getRequestURL().toString();
        String queryUrl = request.getQueryString();
        if(null!=queryUrl){
            url+="?"+queryUrl;
        }

        return url;
    }
}
