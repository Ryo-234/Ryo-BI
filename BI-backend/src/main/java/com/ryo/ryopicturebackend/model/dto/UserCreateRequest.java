package com.ryo.ryopicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/*
* 管理员创建用户请求*/
@Data
public class UserCreateRequest implements Serializable {
    private static final long serialVersionUID = 1403462673149905638L;

    //账号
    private String userAccount;
    //密码
    private String userPassword;
    //角色
    private String userRole;
}
