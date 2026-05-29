package com.gauzynote.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * web配置
 * @deprecated
 */
//@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 用于跨域请求处理
     *
     * @param registry 1
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        //所有资源允许跨域
        registry.addMapping("/**")
                //设置放行哪些原始域
                .allowedOriginPatterns("*")
                //是否发送Cookie 允许携带信息
                .allowCredentials(true)
                //允许的请求
                .allowedMethods("POST", "GET", "PUT", "OPTIONS", "DELETE")
                //最大响应时间
                .maxAge(3600)
                //放行哪些原始请求头部信息
                .allowedHeaders("*")
                //暴露哪些原始请求头部信息
                .exposedHeaders("*");
    }

    /**
     * 跨域配置
     */
    @Bean
    public CorsFilter corsFilter()
    {
        CorsConfiguration config = new CorsConfiguration();
        // 设置访问源地址
        config.addAllowedOriginPattern("*");
        // 设置访问源请求头
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // 允许携带凭证
//        config.setAllowCredentials(true);
        // 有效期 1800秒
        config.setMaxAge(1800L);
        // 添加映射路径，拦截一切请求
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有接口都有效
        source.registerCorsConfiguration("/**", config);
        // 返回新的CorsFilter
        return new CorsFilter(source);
    }
}
