declare module 'slash2';
declare module '*.css';
declare module '*.less';
declare module '*.scss';
declare module '*.sass';
declare module '*.svg';
declare module '*.png';
declare module '*.jpg';
declare module '*.jpeg';
declare module '*.gif';
declare module '*.bmp';
declare module '*.tiff';
declare module '*.md' {
  const content: string;
  export default content;
}
declare module 'omit.js';
declare module 'mockjs';

declare const __APP_VERSION__: string;
declare const __UMI_VERSION__: string;
declare const __UTOO_VERSION__: string;

declare namespace API {
  /** 当前登录用户（后端 UserLoginVO + 前端展示字段） */
  type CurrentUser = {
    /** 用户展示名 */
    name?: string;
    /** 头像地址 */
    avatar?: string;
    /** 头像地址（后端 UserLoginVO 字段名） */
    userAvatar?: string;
    /** 用户 id */
    userid?: string;
    /** 登录账号 */
    userAccount?: string;
    /** 角色：user / admin */
    userRole?: string;
    /** 角色（access 插件使用） */
    access?: string;
    /** 个性签名 */
    signature?: string;
    /** 头衔 */
    title?: string;
    /** 邮箱 */
    email?: string;
    /** 用户组 */
    group?: string;
    /** 标签 */
    tags?: { key: string; label: string }[];
    /** 通知数量 */
    notifyCount?: number;
    /** 未读数量 */
    unreadCount?: number;
    /** 国家 */
    country?: string;
    /** 地区 */
    geographic?: {
      province: { label: string; key: string };
      city: { label: string; key: string };
    };
    /** 地址 */
    address?: string;
    /** 电话 */
    phone?: string;
  };

  /** 图表分页查询条件（对应后端 ChartQueryRequest） */
  type ChartQueryRequest = {
    /** 当前页，从 1 开始 */
    current?: number;
    /** 每页条数 */
    pageSize?: number;
    /** 图表名称，模糊搜索 */
    name?: string;
    /** 图表类型 */
    chartType?: string;
    /** 分析目标 */
    goal?: string;
    /** 图表 id */
    id?: number;
    /** 归属用户 id */
    userId?: number;
    /** 原始数据 */
    chartData?: string;
    /** 图表配置 */
    genChart?: string;
    /** 分析结论 */
    genResult?: string;
    /** 排序字段 */
    sortField?: string;
    /** 排序方式 */
    sortOrder?: string;
  };

  /** 查询表格页的请求参数 */
  type PageParams = {
    current?: number;
    pageSize?: number;
  };

  /** 查询表格页的列表项 */
  type RuleListItem = {
    key?: number;
    disabled?: boolean;
    href?: string;
    avatar?: string;
    name?: string;
    owner?: string;
    desc?: string;
    callNo?: number;
    status?: number;
    updatedAt?: string;
    createdAt?: string;
    progress?: number;
  };

  /** 查询表格页的列表响应 */
  type RuleList = {
    data?: RuleListItem[];
    /** 列表的内容总数 */
    total?: number;
    success?: boolean;
  };
}
