package com.ryo.ryopicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/*
* 用户修改请求*/
@Data
public class UserUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1403462673149905635L;

    //用户 id
    private Long id;
    //昵称
    private String userName;
    //头像
    private String userAvatar;
    //简介
    private String userProfile;
}
