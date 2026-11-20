import type { RequestConfig } from '@umijs/max';
import { getIntl } from '@umijs/max';
import { message, notification } from 'antd';

// 错误处理方案： 错误类型
enum ErrorShowType {
  SILENT = 0,
  WARN_MESSAGE = 1,
  ERROR_MESSAGE = 2,
  NOTIFICATION = 3,
  REDIRECT = 9,
}
// 与后端约定的响应数据格式
interface ResponseStructure {
  success: boolean;
  data: unknown;
  errorCode?: number;
  errorMessage?: string;
  showType?: ErrorShowType;
}

/**
 * 真实后端（Java BaseResponse）的响应结构
 * code = 0 表示成功，非 0 为业务错误码，HTTP 状态码始终是 200
 */
interface BizResponseStructure {
  code: number;
  data?: unknown;
  message?: string;
}

/**
 * 保留大整数精度的响应解析
 *
 * 后端主键用的是雪花 ID（如 chartId、图表 id），是 19 位数字，
 * 超出 JS Number 的安全整数范围（2^53-1），直接 JSON.parse 会丢精度
 * （例如 2120108794238247000 会被舍入成另一个值），
 * 导致后续按 id 查询时后端提示「图表不存在」。
 * 这里把 id、chartId 这类大整数字段按字符串读出，其它字段不受影响。
 */
export const bigIntSafeTransformResponse: RequestConfig['transformResponse'] = [
  (data: unknown) => {
    if (typeof data !== 'string' || !data) {
      return data;
    }
    const patched = data.replace(
      /("(?:chartId|id)"\s*:\s*)(\d{16,})/g,
      '$1"$2"',
    );
    try {
      return JSON.parse(patched);
    } catch {
      // 不是合法 JSON，交给上层按原样处理
      return data;
    }
  },
];

/**
 * @name 错误处理
 * pro 自带的错误处理， 可以在这里做自己的改动
 * @doc https://umijs.org/docs/max/request#配置
 */
export const errorConfig: RequestConfig = {
  // 后端使用 Session 维护登录态，必须携带 Cookie 才能保持登录
  withCredentials: true,

  // 错误处理： umi@3 的错误处理方案。
  errorConfig: {
    // 错误抛出
    errorThrower: (res) => {
      // 非对象响应（如纯字符串）不做业务校验，直接放行
      if (!res || typeof res !== 'object') {
        return;
      }

      // 优先识别后端的 { code, data, message } 结构
      const bizRes = res as unknown as BizResponseStructure;
      if (typeof bizRes.code === 'number') {
        if (bizRes.code !== 0) {
          const error: any = new Error(bizRes.message || '请求失败');
          error.name = 'BizError';
          error.info = {
            errorCode: bizRes.code,
            errorMessage: bizRes.message || '请求失败',
            showType: ErrorShowType.ERROR_MESSAGE,
            data: bizRes.data,
          };
          throw error; // 抛出自制的错误
        }
        return;
      }

      // 兼容 mock 站点返回的 { success, data, errorCode, errorMessage } 结构
      const { success, data, errorCode, errorMessage, showType } =
        res as unknown as ResponseStructure;
      if (success === false) {
        const error: any = new Error(errorMessage);
        error.name = 'BizError';
        error.info = { errorCode, errorMessage, showType, data };
        throw error; // 抛出自制的错误
      }
    },
    // 错误接收及处理
    errorHandler: (error: any, opts: any) => {
      if (opts?.skipErrorHandler) throw error;
      // 我们的 errorThrower 抛出的错误。
      if (error.name === 'BizError') {
        const errorInfo: ResponseStructure | undefined = error.info;
        if (errorInfo) {
          const { errorMessage, errorCode } = errorInfo;
          switch (errorInfo.showType) {
            case ErrorShowType.SILENT:
              // do nothing
              break;
            case ErrorShowType.WARN_MESSAGE:
              message.warning(errorMessage);
              break;
            case ErrorShowType.ERROR_MESSAGE:
              message.error(errorMessage);
              break;
            case ErrorShowType.NOTIFICATION:
              notification.open({
                title: errorCode,
                description: errorMessage,
              });
              break;
            case ErrorShowType.REDIRECT:
              window.location.href = '/user/login';
              break;
            default:
              message.error(errorMessage);
          }
        }
      } else if (error.response) {
        // Axios 的错误
        // 请求成功发出且服务器也响应了状态码，但状态代码超出了 2xx 的范围
        message.error(`Response status:${error.response.status}`);
      } else if (typeof navigator !== 'undefined' && !navigator.onLine) {
        message.error(
          getIntl().formatMessage({
            id: 'app.request.offline',
            defaultMessage:
              'Network unavailable. Please check your connection and try again.',
          }),
        );
      } else if (error.request) {
        message.error('None response! Please retry.');
      } else {
        message.error('Request error, please retry.');
      }
    },
  },

  // 请求拦截器
  requestInterceptors: [],

  // 响应拦截器
  responseInterceptors: [],
};
