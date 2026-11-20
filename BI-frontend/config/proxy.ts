/**
 * @name 代理的配置
 * @see 在生产环境 代理是无法生效的，所以这里没有生产环境的配置
 * -------------------------------
 * The agent cannot take effect in the production environment
 * so there is no configuration of the production environment
 * For details, please see
 * https://pro.ant.design/docs/deploy
 *
 * @doc https://umijs.org/docs/guides/proxy
 */

/** 真实后端地址（Spring Boot，server.port=8123，context-path=/api） */
export const BACKEND_TARGET = 'http://localhost:8123';

export default {
  /**
   * @name 本地开发代理
   * localhost:8000/api/** -> http://localhost:8123/api/**
   * 后端使用 Session 维护登录态，走同源代理可以正常带上 JSESSIONID Cookie
   */
  dev: {
    '/api/': {
      target: BACKEND_TARGET,
      changeOrigin: true,
    },
  },
  /**
   * @name 详细的代理配置
   * @doc https://github.com/chimurai/http-proxy-middleware
   */
  test: {
    // localhost:8000/api/** -> http://localhost:8123/api/**
    '/api/': {
      target: BACKEND_TARGET,
      changeOrigin: true,
    },
  },
  pre: {
    '/api/': {
      target: BACKEND_TARGET,
      changeOrigin: true,
    },
  },
};
