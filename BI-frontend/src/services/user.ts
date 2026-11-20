import { request } from '@umijs/max';

/**
 * 后端统一响应结构（对应 Java 侧 BaseResponse）
 * code = 0 表示成功，其余为业务错误码
 */
export type ApiResult<T> = {
  code: number;
  data?: T | null;
  message?: string;
};

/** 登录用户信息（后端 UserLoginVO，已脱敏，不含密码） */
export type UserInfo = {
  id?: number;
  userAccount?: string;
  userName?: string;
  userAvatar?: string;
  userProfile?: string;
  /** 角色：user / admin */
  userRole?: string;
};

export type UserLoginParams = {
  userAccount: string;
  userPassword: string;
};

export type UserRegisterParams = {
  userAccount: string;
  userPassword: string;
  checkPassword: string;
};

/** 后端业务错误码，与 Java 侧 ErrorCode 保持一致 */
export const ErrorCode = {
  SUCCESS: 0,
  PARAMS_ERROR: 40000,
  NOT_LOGIN_ERROR: 40100,
  NO_AUTH_ERROR: 40101,
  NOT_FOUND_ERROR: 40400,
  FORBIDDEN_ERROR: 40300,
  SYSTEM_ERROR: 50000,
} as const;

/** 用户登录 POST /api/user/login */
export async function userLogin(
  params: UserLoginParams,
  options?: { [key: string]: any },
) {
  return request<ApiResult<UserInfo>>('/api/user/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: params,
    ...(options || {}),
  });
}

/** 用户注册 POST /api/user/register */
export async function userRegister(
  params: UserRegisterParams,
  options?: { [key: string]: any },
) {
  return request<ApiResult<number>>('/api/user/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: params,
    ...(options || {}),
  });
}

/** 获取当前登录用户 GET /api/user/login */
export async function getLoginUser(options?: { [key: string]: any }) {
  return request<ApiResult<UserInfo>>('/api/user/login', {
    method: 'GET',
    ...(options || {}),
  });
}

/** 退出登录 POST /api/user/logout */
export async function userLogout(options?: { [key: string]: any }) {
  return request<ApiResult<boolean>>('/api/user/logout', {
    method: 'POST',
    ...(options || {}),
  });
}

/**
 * 从异常中提取后端返回的业务错误信息
 * requestErrorConfig 里的 errorThrower 会把后端的 { code, message } 包装成 BizError
 */
export function getBizErrorMessage(error: any, fallback = '请求失败'): string {
  if (!error) return fallback;
  const errorMessage = error?.info?.errorMessage || error?.message;
  return typeof errorMessage === 'string' && errorMessage
    ? errorMessage
    : fallback;
}
