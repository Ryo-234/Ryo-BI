package com.ryo.ryopicturebackend.model.dto;

import com.ryo.ryopicturebackend.Common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/*
* 用户列表查询请求，继承通用分页请求类，额外支持按条件筛选*/
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends PageRequest {
    private static final long serialVersionUID = 1403462673149905639L;

    //用户 id
    private Long id;
    //账号，模糊查询
    private String userAccount;
    //昵称，模糊查询
    private String userName;
    //简介，模糊查询
    private String userProfile;
    //角色，精确查询
    private String userRole;
}
