package com.gauzynote.framework.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Objects;

@Configuration
public class ProfileConfig {

    @Resource
    private ApplicationContext context;

    public String getActiveProfile() {
        return context.getEnvironment().getActiveProfiles()[0];
    }
    public Boolean getIsDevProfile() {
        return Objects.equals(context.getEnvironment().getActiveProfiles()[0], "dev");
    }

    /**
     * 是否为正式环境
     * @return boolean
     */
    public Boolean getIsProdProfile() {
        return Objects.equals(context.getEnvironment().getActiveProfiles()[0], "prod");
    }
}

