# 智能BI

基于 Umi Max + React + Ant Design Pro 的智能 BI 分析前端：上传 CSV/Excel 原始数据，调用后端 AI 生成 ECharts 图表与分析结论，并提供「我的图表」列表（搜索、分页、图表预览）。

## 功能

- `/add_chart` 智能分析：填写分析目标 / 图表名称 / 图表类型，上传 CSV（含 Excel）数据，生成图表与结论（后端线程池异步，前端轮询任务状态）
- `/add_chart_mq` 智能分析MQ：同上，走 RabbitMQ 消息队列异步生成，前端轮询任务状态
- `/add_chart_sync` 智能分析同步：同上，走阻塞式同步接口，提交后直接返回图表与结论
- `/my_chart` 我的图表：分页浏览当前账号生成的图表，支持按名称搜索
- `/user/login` 登录：后端 Session 登录（未登录访问业务页面会跳转到登录页）

## 环境要求

- Node >= 20
- 后端服务（Spring Boot）默认运行在 `http://localhost:8123`，接口前缀 `/api`
- MySQL（后端依赖）

## 常用命令

```bash
npm install          # 安装依赖
npm run dev          # 本地开发（不启用 mock，直接联调后端）
npm run build        # 生产构建
npm run preview      # 预览构建产物
npm run lint         # Biome 检查 + TypeScript 类型检查
npm run test         # 单元测试
npm run openapi      # 依据 config/oneapi.json 重新生成 src/services/api 下的请求代码
```

## 接口代码生成

后端使用 Knife4j（Swagger2）暴露文档，生成的请求代码位于 `src/services/api`。

```bash
# 1. 拉取后端文档并转换为 OpenAPI 3（需要后端已启动）
node scripts/sync-openapi.js

# 2. 生成 TypeScript 请求代码
npm run openapi
```

`config/config.ts` 的 `openAPI.projectName` 决定生成目录，`config/oneapi.json` 是转换后的文档。

## 目录结构

```
src/pages/AddChart    智能分析页（表单 + 图表渲染）
src/pages/MyChart     我的图表页（列表 + 搜索 + 分页）
src/pages/user        登录 / 注册
src/services/api      openapi 生成的请求代码
src/services/user.ts  登录相关的手写请求封装
src/utils/chart.ts    ECharts 配置解析工具（兼容 JS 字面量写法）
```
