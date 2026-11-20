package com.ryo.ryopicturebackend.service;

import com.ryo.ryopicturebackend.model.dto.UserCreateRequest;
import com.ryo.ryopicturebackend.model.dto.UserQueryRequest;
import com.ryo.ryopicturebackend.model.dto.UserUpdateRequest;
import com.ryo.ryopicturebackend.model.entity.User;
import com.ryo.ryopicturebackend.model.vo.PageResultVO;
import com.ryo.ryopicturebackend.model.vo.UserLoginVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author admin
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-10-25 18:50:27
*/
public interface UserService extends IService<User> {
    long userRegister(String username, String password,String checkPassword);

    /**
     * 密码加密
     *
     * @param userPassword 原始密码
     * @return 加密后的密文
     */
    String encryptPassword(String userPassword);

    /**
     * 用户登录，登录成功后把用户登录态记录到 session 中
     *
     * @param userAccount  账号
     * @param userPassword 密码
     * @param request      当前请求，用于获取 session
     * @return 脱敏后的登录用户信息
     */
    UserLoginVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request 当前请求，用于获取 session
     * @return 脱敏后的登录用户信息，未登录时返回 null
     */
    UserLoginVO getLoginUserVO(HttpServletRequest request);

    /**
     * 退出登录，清除 session 中的登录态
     *
     * @param request 当前请求，用于获取 session
     */
    void userLogout(HttpServletRequest request);

    /**
     * 管理员创建用户
     *
     * @param userCreateRequest 创建请求参数
     * @return 新用户 id
     */
    long adminCreateUser(UserCreateRequest userCreateRequest);

    /**
     * 管理员根据 id 删除用户
     *
     * @param id 用户 id
     * @return 是否删除成功
     */
    boolean adminDeleteUser(Long id);

    /**
     * 管理员更新用户信息
     *
     * @param userUpdateRequest 更新请求参数
     * @return 是否更新成功
     */
    boolean adminUpdateUser(UserUpdateRequest userUpdateRequest);

    /**
     * 管理员分页获取用户列表（脱敏）
     *
     * @param userQueryRequest 查询参数
     * @return 分页后的脱敏用户列表
     */
    PageResultVO<UserLoginVO> adminListUser(UserQueryRequest userQueryRequest);

    /**
     * 管理员根据 id 获取用户详情（未脱敏，返回实体）
     *
     * @param id 用户 id
     * @return 用户实体
     */
    User adminGetUserById(Long id);

    /**
     * 获取脱敏后的单个用户信息
     *
     * @param id 用户 id
     * @return 脱敏后的用户信息
     */
    UserLoginVO getUserById(Long id);

    /**
     * 获取脱敏后的用户列表
     *
     * @param userQueryRequest 查询参数
     * @return 脱敏后的用户列表
     */
    List<UserLoginVO> listUserByPage(UserQueryRequest userQueryRequest);

    /**
     * 将查询请求转为 QueryWrapper 查询条件，用于构造 SQL
     *
     * @param userQueryRequest 查询参数
     * @return 查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
