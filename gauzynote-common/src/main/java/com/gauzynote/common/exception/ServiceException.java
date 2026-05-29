package com.gauzynote.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常
 */
public final class ServiceException extends RuntimeException {
    private Integer code;

    public ServiceException() {
        super();
    }
    public ServiceException(String message) {
        super(message);
    }
    public ServiceException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public ServiceException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ServiceException(HttpStatus httpStatus) {
        super(httpStatus.toString());
        this.code = httpStatus.value();
    }

    public Integer getCode() {
        return code;
    }
    @Override
    public String getMessage()
    {
        return super.getMessage();
    }
    @Override
    public Throwable getCause()
    {
        return super.getCause();
    }
}