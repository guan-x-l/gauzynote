package com.gauzynote.common.domain.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * App用户操作日志表(AppOperLog)实体类
 *
 */
@Data
public class OperLog implements Serializable {
    private static final long serialVersionUID = 244211437058017706L;
    /**
     * 主键ID
     */
    private Long operId;

    /**
     * 来源
     */
    private String origin;

    /**
     * 操作名称
     */
    private String operationName;
    /**
     * 操作类型（0其他 1新增 2删除 3修改 4查询 5登录 6注册 7注销）
     */
    private Integer businessType;
    /**
     * 请求方法
     */
    private String requestMethod;
    /**
     * 请求URL
     */
    private String requestUrl;
    /**
     * uuid
     */
    private String uuid;
    /**
     * 请求参数
     */
    private String requestParams;
    /**
     * 响应状态（200代表成功，500代表失败）
     */
    private String responseStatus;
    /**
     * 执行时长
     */
    private Long executionTime;
    /**
     * 响应数据
     */
    private String responseData;
    /**
     * 用户IP地址
     */
    private String ipAddress;
    /**
     * 状态码，0为正常,
     * 310请求参数信息有误，
     * 311Key格式错误,
     * 306请求有护持信息请检查字符串,
     * 110请求来源未被授权
     */
    private Integer addressStatus;
    /**
     * 对status的描述
     */
    private String addressMessage;
    /**
     * 纬度
     */
    private String lat;
    /**
     * 经度
     */
    private String lng;
    /**
     * 国家
     */
    private String nation;
    /**
     * 省
     */
    private String province;
    /**
     * 市
     */
    private String city;
    /**
     * 区
     */
    private String district;
    /**
     * 行政区划代码
     */
    private Integer adcode;
    /**
     * url地址
     */
    private String currentUrl;
    /**
     * host
     */
    private String host;
    /**
     * 请求来源
     */
    private String requestOrigin;
    /**
     * document.referrer
     */
    private String referrer;
    /**
     * location.ancestorOrigins
     */
    private String ancestorOrigins;

    /**
     * 版本
     */
    private String clientVersion;
    /**
     * 用户代理信息
     */
    private String userAgent;
    /**
     * 操作时间
     */
    private Date createTime;
}

