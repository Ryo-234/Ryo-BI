package com.ryo.ryopicturebackend.AOP;


import com.ryo.ryopicturebackend.annotation.AuthCheck;
import com.ryo.ryopicturebackend.exception.BusinessException;
import com.ryo.ryopicturebackend.exception.ErrorCode;
import com.ryo.ryopicturebackend.model.Enum.UserRoleEnum;
import com.ryo.ryopicturebackend.model.vo.UserLoginVO;
import com.ryo.ryopicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect//表示这是个切面
@Component//bean 让spring来识别加载
public class AuthInsterceptor {

    @Resource
    private UserService userService;//权限校验要提取用户信息
    //切点 要在哪个位置执行这个方法
    @Around("@annotation(authCheck)")//既可以前面也可以后面 @annotation对这个注解进行拦截 再通过参数能获取到值
    //pjp明确对哪个方法进行拦截 ac我们自己定义的注解 在这个方法中获取在注解上约定的值（这个就是role）
    public Object doInterceptor(ProceedingJoinPoint pjp, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();//等下改成枚举类
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();//全局的上下文 当前这个请求所有属性
        //获得请求里的参数 得从请求里面取 httpservletrequest 下面那个是强转成子类去getrequest
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        //获取当前用户
        UserLoginVO loginUserVO = userService.getLoginUserVO(request);
        //未登录 直接拦截
        if (loginUserVO == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //如果不需要指定角色 放行（已登录即可）
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        if (mustRoleEnum == null) {
            return pjp.proceed();
        }
        //以下代码必须有权限才通过
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUserVO.getUserRole());
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        if (!userRoleEnum.equals(mustRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return pjp.proceed();
    }
}
