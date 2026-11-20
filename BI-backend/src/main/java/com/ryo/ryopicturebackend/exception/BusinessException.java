package com.ryo.ryopicturebackend.exception;


import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {//继承运行时异常

    /**
     * 错误码
     */
    private final int code;//定义错误码

    public BusinessException(int code, String message) {
        super(message);//继承父类
        this.code = code;//给本函数的code赋值
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();//把ec里的code复制给我们的方法
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

}
