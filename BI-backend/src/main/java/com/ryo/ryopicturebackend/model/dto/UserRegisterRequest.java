package com.ryo.ryopicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/*
* 用户注册请求*/
@Data
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1403462673149905632L;
    //更方便的在网络中传输或序列化 给每个定义唯一序列化ID 为了标识对象在序列化 比如到硬盘上 检查对象是否一致

    //账号
    private String userAccount;
    //密码
    private String userPassword;
    //确认密码
    private String checkPassword;
}
