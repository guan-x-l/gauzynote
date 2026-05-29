package com.gauzynote.framework.config;

import com.gauzynote.common.utils.AppConfigProperties;
import com.gauzynote.framework.filter.AuthenticationTokenFilter;
import com.gauzynote.framework.handle.AuthenticationEntryPointImpl;
import com.gauzynote.framework.handle.LogoutSuccessHandlerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.annotation.Resource;
import java.util.Arrays;

@Configuration
public class SecurityConfig {
    @Autowired
    private AuthenticationTokenFilter authenticationTokenFilter;
    /**
     * 自定义用户认证逻辑
     */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * 自定义用户认证逻辑
     */
    @Autowired
    private AuthenticationEntryPointImpl unauthorizedHandler;

    /**
     * 自定义用户认证逻辑
     */
    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    @Resource
    private AppConfigProperties appConfigProperties;
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(daoAuthenticationProvider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // 禁用CSRF保护（通常用于前后端分离项目）
                .csrf().disable()
                // 配置响应头安全策略
                .headers(headers -> headers
                        .cacheControl().disable()// 禁用缓存
                        .frameOptions().sameOrigin()// 允许同源页面通过iframe加载
                )
                // 会话管理配置, 使用无状态会话（不创建HTTP Session）
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 异常处理配置，自定义未授权的处理类
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler)
                .and()
                // 配置请求授权规则
                .authorizeHttpRequests(requests -> requests
                        // 允许匿名访问的路径
                        .antMatchers(HttpMethod.POST,"/login").permitAll()
                        .antMatchers(HttpMethod.GET,"/login").permitAll()
                        .antMatchers(HttpMethod.POST, "/webauthn/auth/options", "/webauthn/auth/verify").permitAll()
                        // 允许匿名访问的GET请求
                        .antMatchers(HttpMethod.GET, "/", "/*.html", "/assets/**/*.html", "/assets/**/*.css", "/assets/**/*.js", "/assets/profile/**").permitAll()
//                        .antMatchers("/swagger-ui.html", "/swagger-resources/**", "/webjars/**", "/*/api-docs", "/druid/**").permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )
                // 配置登出、登出地址、处理类
                .logout().logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler)
                .and()
                // 自定义过滤器，令牌认证处理类
                .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // 添加CORS过滤器
                .addFilterBefore(corsFilter(), ChannelProcessingFilter.class);
        return httpSecurity.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 使用BCrypt加密密码
    }

    // CORS配置
    @Bean
    public CorsFilter corsFilter( ) {
        CorsConfiguration config = new CorsConfiguration();
        // 批量添加允许的域名
        appConfigProperties.getCors().getAllowedOrigins().forEach(config::addAllowedOriginPattern);
        config.addAllowedHeader("*");
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowCredentials(appConfigProperties.getCors().getAllowCredentials());
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
