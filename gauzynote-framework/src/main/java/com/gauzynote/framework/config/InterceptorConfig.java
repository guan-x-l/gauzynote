package com.gauzynote.framework.config;

import com.gauzynote.framework.interceptor.RequestLimiterInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLimiterInterceptor()).addPathPatterns("/**");
    }

    @Bean
    public RequestLimiterInterceptor requestLimiterInterceptor() {
        // 限流拦截器
        return new RequestLimiterInterceptor();
    }
}
