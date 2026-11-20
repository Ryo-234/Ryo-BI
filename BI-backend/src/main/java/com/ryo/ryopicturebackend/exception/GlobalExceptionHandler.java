package com.ryo.ryopicturebackend.exception;

import com.ryo.ryopicturebackend.Common.BaseResponse;
import com.ryo.ryopicturebackend.Common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/**
 * 全局异常处理器
 */

@RestControllerAdvice//环绕切面 在这个类里写切点 打上注解
                    // 指定要捕获的异常.class 任何方法抛出了在这个异常 都可以被捕获 然后进行处理
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 请求体格式错误或为空，@RequestBody 无法解析出对象时抛出该异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<?> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.error("HttpMessageNotReadableException", e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请求参数格式错误");
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}

