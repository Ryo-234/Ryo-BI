package com.ryo.ryopicturebackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ryo.ryopicturebackend.Common.CommonConstant;
import com.ryo.ryopicturebackend.exception.BusinessException;
import com.ryo.ryopicturebackend.exception.ErrorCode;
import com.ryo.ryopicturebackend.model.Enum.UserRoleEnum;
import com.ryo.ryopicturebackend.model.dto.UserCreateRequest;
import com.ryo.ryopicturebackend.model.dto.UserQueryRequest;
import com.ryo.ryopicturebackend.model.dto.UserUpdateRequest;
import com.ryo.ryopicturebackend.model.entity.User;
import com.ryo.ryopicturebackend.model.vo.PageResultVO;
import com.ryo.ryopicturebackend.model.vo.UserLoginVO;
import com.ryo.ryopicturebackend.service.UserService;
import com.ryo.ryopicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author admin
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2026-10-25 18:50:27
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{
    //密码加密盐值，与密码拼接后再做 MD5，避免相同密码密文相同
    private static final String SALT = "ryo-picture";
    //允许排序的字段白名单，排序字段由前端传入，必须校验防止 SQL 注入
    private static final Set<String> SORT_FIELDS = new HashSet<>(Arrays.asList(
            "id", "userAccount", "userName", "userAvatar", "userProfile", "userRole", "createTime", "updateTime"));

    //用户注册 先定义逻辑
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1校验参数 用到strUtils校验传入的参数是否为空 为空的话throw new business异常
        if(StrUtil.hasBlank(userAccount,userPassword,checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        if(userAccount.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 2检查用户账号是否和数据库中已有的重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.getOne(queryWrapper);
        if (user != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        // 3密码一定要加密
        String encryptPassword = this.getEncryptPassword(userPassword);

        // 4插入数据库到数据库中
        User newUser = new User();
        newUser.setUserAccount(userAccount);
        newUser.setUserPassword(encryptPassword);
        newUser.setUserName(userAccount);
        newUser.setUserRole(UserRoleEnum.USER.getValue());
        boolean result = this.save(newUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
        return newUser.getId();
    }

    /**
     * 密码加密
     *
     * @param userPassword 原始密码
     * @return 加密后的密文
     */
    @Override
    public String encryptPassword(String userPassword) {
        return this.getEncryptPassword(userPassword);
    }

    /**
     * 用户登录
     *
     * @param userAccount  账号
     * @param userPassword 密码
     * @param request      当前请求，用于获取 session
     * @return 脱敏后的登录用户信息
     */
    @Override
    public UserLoginVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        // 2加密后查询用户是否存在
        String encryptPassword = this.getEncryptPassword(userPassword);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在或密码错误，统一提示，避免暴露账号是否存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        // 3记录用户的登录态
        request.getSession().setAttribute(CommonConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request 当前请求，用于获取 session
     * @return 脱敏后的登录用户信息，未登录时返回 null
     */
    @Override
    public UserLoginVO getLoginUserVO(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        User loginUser = (User) session.getAttribute(CommonConstant.USER_LOGIN_STATE);
        if (loginUser == null || loginUser.getId() == null) {
            return null;
        }
        //从数据库中查询一遍
        loginUser = this.getById(loginUser.getId());
        if (loginUser == null) {
            return null;
        }
        return this.getLoginUserVO(loginUser);
    }

    /**
     * 退出登录，清除 session 中的登录态
     *
     * @param request 当前请求，用于获取 session
     */
    @Override
    public void userLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(CommonConstant.USER_LOGIN_STATE);
        }
    }

    /**
     * 密码加密
     *
     * @param userPassword 原始密码
     * @return 加密后的密文
     */
    private String getEncryptPassword(String userPassword) {
        return DigestUtil.md5Hex(userPassword + SALT);
    }

    /**
     * 把用户实体转换成脱敏后的视图对象，不返回密码等敏感字段
     *
     * @param user 用户实体
     * @return 脱敏后的登录用户信息
     */
    private UserLoginVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setId(user.getId());
        userLoginVO.setUserAccount(user.getUserAccount());
        userLoginVO.setUserName(user.getUserName());
        userLoginVO.setUserAvatar(user.getUserAvatar());
        userLoginVO.setUserProfile(user.getUserProfile());
        userLoginVO.setUserRole(user.getUserRole());
        return userLoginVO;
    }

    /**
     * 管理员创建用户
     *
     * @param userCreateRequest 创建请求参数
     * @return 新用户 id
     */
    @Override
    public long adminCreateUser(UserCreateRequest userCreateRequest) {
        if (userCreateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        String userAccount = userCreateRequest.getUserAccount();
        String userPassword = userCreateRequest.getUserPassword();
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 检查账号是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        if (this.getOne(queryWrapper) != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        // 密码加密
        String encryptPassword = this.getEncryptPassword(userPassword);
        // 插入数据库
        User newUser = new User();
        newUser.setUserAccount(userAccount);
        newUser.setUserPassword(encryptPassword);
        newUser.setUserName(userAccount);
        // 角色没指定时默认为普通用户
        String userRole = userCreateRequest.getUserRole();
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(userRole);
        newUser.setUserRole(userRoleEnum == null ? UserRoleEnum.USER.getValue() : userRoleEnum.getValue());
        boolean result = this.save(newUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建失败");
        }
        return newUser.getId();
    }

    /**
     * 管理员根据 id 删除用户
     *
     * @param id 用户 id
     * @return 是否删除成功
     */
    @Override
    public boolean adminDeleteUser(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return this.removeById(id);
    }

    /**
     * 管理员更新用户信息
     *
     * @param userUpdateRequest 更新请求参数
     * @return 是否更新成功
     */
    @Override
    public boolean adminUpdateUser(UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        User user = this.getById(userUpdateRequest.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        // 只更新允许修改的字段，账号和密码不通过该接口修改
        User updateUser = new User();
        updateUser.setId(userUpdateRequest.getId());
        updateUser.setUserName(userUpdateRequest.getUserName());
        updateUser.setUserAvatar(userUpdateRequest.getUserAvatar());
        updateUser.setUserProfile(userUpdateRequest.getUserProfile());
        return this.updateById(updateUser);
    }

    /**
     * 管理员分页获取用户列表（脱敏）
     *
     * @param userQueryRequest 查询参数
     * @return 分页后的脱敏用户列表
     */
    @Override
    public PageResultVO<UserLoginVO> adminListUser(UserQueryRequest userQueryRequest) {
        // 根据查询参数构造 QueryWrapper，由 MyBatis Plus 生成 SQL
        QueryWrapper<User> queryWrapper = this.getQueryWrapper(userQueryRequest);
        int current = userQueryRequest.getCurrent();
        int pageSize = userQueryRequest.getPageSize();
        Page<User> page = this.page(new Page<>(current, pageSize), queryWrapper);
        List<UserLoginVO> userLoginVOList = page.getRecords().stream()
                .map(this::getLoginUserVO)
                .collect(Collectors.toList());
        return new PageResultVO<>(userLoginVOList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 获取脱敏后的用户列表
     *
     * @param userQueryRequest 查询参数
     * @return 脱敏后的用户列表
     */
    @Override
    public List<UserLoginVO> listUserByPage(UserQueryRequest userQueryRequest) {
        // 根据查询参数构造 QueryWrapper，由 MyBatis Plus 生成 SQL
        QueryWrapper<User> queryWrapper = this.getQueryWrapper(userQueryRequest);
        return this.list(queryWrapper).stream()
                .map(this::getLoginUserVO)
                .collect(Collectors.toList());
    }

    /**
     * 将查询请求转为 QueryWrapper 查询条件，用于构造 SQL
     *
     * @param userQueryRequest 查询参数
     * @return 查询条件
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        // 排序字段来自前端，必须走白名单校验，防止 SQL 注入
        if (StrUtil.isNotEmpty(sortField) && SORT_FIELDS.contains(sortField)) {
            // 常量在前，避免 sortOrder 为 null 时空指针
            boolean isAsc = "ascend".equals(sortOrder);
            queryWrapper.orderBy(true, isAsc, sortField);
        }
        return queryWrapper;
    }

    /**
     * 管理员根据 id 获取用户详情（未脱敏，返回实体）
     *
     * @param id 用户 id
     * @return 用户实体
     */
    @Override
    public User adminGetUserById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return user;
    }

    /**
     * 根据 id 获取用户信息（脱敏）
     *
     * @param id 用户 id
     * @return 脱敏后的用户信息
     */
    @Override
    public UserLoginVO getUserById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return this.getLoginUserVO(user);
    }
}
