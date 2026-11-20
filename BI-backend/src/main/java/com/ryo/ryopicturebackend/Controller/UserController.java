package com.ryo.ryopicturebackend.Controller;

import com.ryo.ryopicturebackend.Common.BaseResponse;
import com.ryo.ryopicturebackend.Common.ResultUtils;
import com.ryo.ryopicturebackend.annotation.AuthCheck;
import com.ryo.ryopicturebackend.exception.ErrorCode;
import com.ryo.ryopicturebackend.exception.ThrowUtils;
import com.ryo.ryopicturebackend.model.dto.UserCreateRequest;
import com.ryo.ryopicturebackend.model.dto.UserLoginRequest;
import com.ryo.ryopicturebackend.model.dto.UserQueryRequest;
import com.ryo.ryopicturebackend.model.dto.UserRegisterRequest;
import com.ryo.ryopicturebackend.model.dto.UserUpdateRequest;
import com.ryo.ryopicturebackend.model.entity.User;
import com.ryo.ryopicturebackend.model.vo.PageResultVO;
import com.ryo.ryopicturebackend.model.vo.UserLoginVO;
import com.ryo.ryopicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户接口
 */
@Api(tags = "用户管理")
@RestController//自动把每个接口的返回值转换成json格式 restful的api
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 注册请求参数
     * @return 新用户 id
     */
    @ApiOperation("用户注册")
    @PostMapping("/register")//拼接
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {//接受的参数是一个对象 打上requestbody前端就可以传对象的格式了
        ThrowUtils.throwIf(userRegisterRequest==null, ErrorCode.PARAMS_ERROR);
        //userRegisterRequest.allget就能得到下面代码
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 登录请求参数
     * @param request          当前请求
     * @return 脱敏后的登录用户信息
     */
    @ApiOperation("用户登录")
    @PostMapping("/login")
    public BaseResponse<UserLoginVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest==null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        UserLoginVO userLoginVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(userLoginVO);
    }

    /**
     * 获取当前登录用户信息
     *
     * @param request 当前请求
     * @return 脱敏后的登录用户信息
     */
    @ApiOperation("获取当前登录用户")
    @GetMapping("/login")
    public BaseResponse<UserLoginVO> getLoginUser(HttpServletRequest request) {
        UserLoginVO userLoginVO = userService.getLoginUserVO(request);
        ThrowUtils.throwIf(userLoginVO==null, ErrorCode.NOT_LOGIN_ERROR);
        return ResultUtils.success(userLoginVO);
    }

    /**
     * 退出登录
     *
     * @param request 当前请求
     * @return 退出结果
     */
    @ApiOperation("退出登录")
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        userService.userLogout(request);
        return ResultUtils.success(true);
    }

    /**
     * 【管理员】创建用户
     *
     * @param userCreateRequest 创建请求参数
     * @return 新用户 id
     */
    @ApiOperation("管理员创建用户")
    @AuthCheck(mustRole = "admin")
    @PostMapping("/admin/create")
    public BaseResponse<Long> adminCreateUser(@RequestBody UserCreateRequest userCreateRequest) {
        long result = userService.adminCreateUser(userCreateRequest);
        return ResultUtils.success(result);
    }

    /**
     * 【管理员】根据 id 删除用户
     *
     * @param id 用户 id
     * @return 是否删除成功
     */
    @ApiOperation("管理员删除用户")
    @AuthCheck(mustRole = "admin")
    @DeleteMapping("/admin/delete/{id}")
    public BaseResponse<Boolean> adminDeleteUser(@PathVariable("id") Long id) {
        boolean result = userService.adminDeleteUser(id);
        return ResultUtils.success(result);
    }

    /**
     * 【管理员】更新用户
     *
     * @param userUpdateRequest 更新请求参数
     * @return 是否更新成功
     */
    @ApiOperation("管理员更新用户")
    @AuthCheck(mustRole = "admin")
    @PutMapping("/admin/update")
    public BaseResponse<Boolean> adminUpdateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        boolean result = userService.adminUpdateUser(userUpdateRequest);
        return ResultUtils.success(result);
    }

    /**
     * 【管理员】分页获取用户列表（脱敏）
     *
     * @param userQueryRequest 查询参数
     * @return 分页后的脱敏用户列表
     */
    @ApiOperation("管理员分页获取用户列表")
    @AuthCheck(mustRole = "admin")
    @GetMapping("/admin/list")
    public BaseResponse<PageResultVO<UserLoginVO>> adminListUser(UserQueryRequest userQueryRequest) {
        PageResultVO<UserLoginVO> pageResultVO = userService.adminListUser(userQueryRequest);
        return ResultUtils.success(pageResultVO);
    }

    /**
     * 【管理员】根据 id 获取用户（未脱敏）
     *
     * @param id 用户 id
     * @return 用户实体
     */
    @ApiOperation("管理员获取用户详情")
    @AuthCheck(mustRole = "admin")
    @GetMapping("/admin/get/{id}")
    public BaseResponse<User> adminGetUserById(@PathVariable("id") Long id) {
        User user = userService.adminGetUserById(id);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取用户（脱敏）
     *
     * @param id 用户 id
     * @return 脱敏后的用户信息
     */
    @ApiOperation("根据id获取用户")
    @GetMapping("/get/{id}")
    public BaseResponse<UserLoginVO> getUserById(@PathVariable("id") Long id) {
        UserLoginVO userLoginVO = userService.getUserById(id);
        return ResultUtils.success(userLoginVO);
    }

    /**
     * 获取脱敏后的用户列表
     *
     * @param userQueryRequest 查询参数
     * @return 脱敏后的用户列表
     */
    @ApiOperation("获取脱敏后的用户列表")
    @GetMapping("/list")
    public BaseResponse<List<UserLoginVO>> listUserByPage(UserQueryRequest userQueryRequest) {
        List<UserLoginVO> userLoginVOList = userService.listUserByPage(userQueryRequest);
        return ResultUtils.success(userLoginVOList);
    }
}
