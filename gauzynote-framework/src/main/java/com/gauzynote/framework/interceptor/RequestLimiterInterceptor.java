package com.gauzynote.framework.interceptor;

import com.alibaba.fastjson2.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import com.gauzynote.common.annotation.RequestLimiter;
import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 请求限流器
 */
public class RequestLimiterInterceptor implements HandlerInterceptor {
    /**
     * 不同的方法存放不同的令牌桶
     */
    private final Map<String, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();
    /**
     * 自动过期缓存，3 分钟未访问自动移除
     */
    private final Cache<String, RateLimiter> rateLimiterCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES) //自动过期
            .maximumSize(100000) // 最大限制
            .concurrencyLevel(32)
            .build();
    private static final Logger logger = LoggerFactory.getLogger(RequestLimiterInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        try {
            boolean result = preFilter(request, response, handler);
            if (result) {
                return true;
            }
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/json; charset=UTF-8");
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequestLimiter rateLimit = handlerMethod.getMethodAnnotation(RequestLimiter.class);
            AjaxResult serverBusy = new AjaxResult(HttpServletResponse.SC_SERVICE_UNAVAILABLE, rateLimit.msg());

            // 创建新的HttpServletRequest对象并复制原始请求的数据
            HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request);


            OutputStreamWriter ow = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
            ow.write(JSONObject.toJSONString(serverBusy));
            ow.flush();
            ow.close();
            return false;
        } catch (Exception e) {
            logger.error("request limiter preHandle catch a exception:" + e.getMessage());
            throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean preFilter(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                RequestLimiter rateLimit = handlerMethod.getMethodAnnotation(RequestLimiter.class);
                //判断是否有注解
                if (rateLimit != null) {
                    // 获取请求url
                    String key = request.getMethod() + request.getRequestURI();
                    // 从缓存获取或新建
                    RateLimiter rateLimiter = rateLimiterCache.getIfPresent(key);
                    if (rateLimiter == null) {
                        rateLimiter = RateLimiter.create(rateLimit.QPS());
                        rateLimiterCache.put(key, rateLimiter);
                    }
//                    RateLimiter rateLimiter;
//                    // 判断map集合中是否有创建好的令牌桶
//                    if (!rateLimiterMap.containsKey(key)) {
//                        // 创建令牌桶,以n r/s往桶中放入令牌
//                        rateLimiter = RateLimiter.create(rateLimit.QPS());
//                        rateLimiterMap.put(key, rateLimiter);
//                    }
//                    rateLimiter = rateLimiterMap.get(key);


                    // 获取令牌
//                    boolean acquire = rateLimiter.tryAcquire(rateLimit.timeout(), rateLimit.timeunit());
                    boolean acquire = rateLimiter.tryAcquire(rateLimit.timeout(), rateLimit.timeunit());
                    if (acquire) {
                        //获取令牌成功
                        return true;
                    }
                    logger.warn("请求被限流 url:{}", request.getServletPath());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
