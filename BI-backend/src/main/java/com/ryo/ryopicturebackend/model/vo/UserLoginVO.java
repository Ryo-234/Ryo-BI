package com.ryo.ryopicturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/*
* 登录用户信息视图对象，脱敏后的用户信息，不含密码等敏感字段*/
@Data
public class UserLoginVO implements Serializable {
    private static final long serialVersionUID = 1403462673149905634L;

    //用户 id
    private Long id;
    //账号
    private String userAccount;
    //昵称
    private String userName;
    //头像
    private String userAvatar;
    //简介
    private String userProfile;
    //角色
    private String userRole;
}
