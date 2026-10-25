package com.ryo.ryopicturebackend.Common;

import com.ryo.ryopicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;
//返回一个接口 不再返回裸string了 在controller里 继承这个baseresponse封装一层 返回new <>baseresponse（code,"hello"+name）
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}




