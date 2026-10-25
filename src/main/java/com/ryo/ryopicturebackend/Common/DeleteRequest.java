package com.ryo.ryopicturebackend.Common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用的删除请求类 实现序列化接口 让对象方便传输 ，封装这个类方便接受参数 对常用的接受参数进行封装
 * */

@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
