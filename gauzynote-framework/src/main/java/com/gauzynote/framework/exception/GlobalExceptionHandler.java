package com.gauzynote.framework.exception;

import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.ObjectUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.BindException;
import java.util.Objects;


/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;
    /**
     * 处理业务异常
     */
    @ExceptionHandler(value = ServiceException.class)
    @ResponseBody
    public AjaxResult handleServiceException(ServiceException e) {
        log.error("业务异常！");
        log.error(e.getMessage(), e);
        Integer code = e.getCode();
        return ObjectUtils.isEmpty(code) ? AjaxResult.error(e.getMessage()) : AjaxResult.error(code, e.getMessage());
    }


    /**
     * 处理请求体解析异常
     *
     * @return 通用返回结果
     */
    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public AjaxResult messageExceptionHandler(HttpRequestMethodNotSupportedException e) {
        log.error("请求参数不符合规则！原因是：{}", e.getMessage());
        return AjaxResult.error(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }

    /**
     * 请求路径中缺少必需的路径变量
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public AjaxResult handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求路径中缺少必需的路径变量'{}',", requestURI, e);
        return AjaxResult.error(HttpServletResponse.SC_BAD_REQUEST, String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()));
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e) {
        log.error(e.getMessage(), e);
        return AjaxResult.error(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }

    /**
     * 请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String value = Objects.requireNonNull(e.getValue(), "获取参数失败").toString();
        String typeName = Objects.requireNonNull(e.getRequiredType(), "获取参数类型失败").getName();
        log.error("请求参数类型不匹配'{}',", requestURI, e);
        return AjaxResult.error(String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'", e.getName(), typeName, value));
    }
    /**
     * Http请求消息序列化异常
     *
     * @return 通用返回结果
     */
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    @ResponseBody
    public AjaxResult messageExceptionHandler(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.error("请求参数不符合规则'{}'", request.getRequestURI(), e);
        return AjaxResult.error(HttpServletResponse.SC_BAD_REQUEST, String.format("请求参数不符合规则，[%s]", e.getMessage()));
    }


    // 专门捕获文件大小超限异常
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public AjaxResult handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("上传文件大小超过限制: {}", e.getMessage());
        return AjaxResult.error(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, String.format("上传文件过大，（最大为%s）", maxFileSize));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request)
    {
        log.error("请求参数不符合规则'{}'", request.getRequestURI(), e);
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return AjaxResult.error(message);
    }
    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(e.getMessage());
    }

}
