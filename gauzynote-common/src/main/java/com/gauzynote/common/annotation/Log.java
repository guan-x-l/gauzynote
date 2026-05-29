package com.gauzynote.common.annotation;



import com.gauzynote.common.enums.BusinessType;

import java.lang.annotation.*;

/**
 * 自定义操作日志记录注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    /**
     * 操作名称，默认为空字符串
     */
    String operationName() default "";

    /**
     * 业务类型
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 是否根据ip定位，并录入位置信息
     */
//    boolean isIPLocating() default false;

    /**
     * 是否保存请求的参数
     */
    boolean isSaveRequestParams() default true;

    /**
     * 是否保存响应数据
     */
    boolean isSaveResponseParams() default false;
}
