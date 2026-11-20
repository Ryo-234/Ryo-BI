# Ryo-BI · 基于 React 和 SpringBoot 的智能数据分析平台

南昌交通学院毕业设计项目。一个面向业务数据的智能分析平台：后端提供图表管理、
Excel 导入导出、AI 图表生成与异步消息处理能力，前端提供数据看板与图表可视化界面。

## 技术栈

| 模块 | 技术选型 |
| --- | --- |
| 后端 | Spring Boot 3.5.3、Java 21、MyBatis-Plus、MySQL 8、RabbitMQ、Knife4j、DeepSeek API |
| 前端 | React 18、TypeScript、Ant Design、UmiJS、Tailwind CSS、Biome |
| 工程 | Maven / npm、Git |

## 目录结构

```
Ryo-BI/
├── BI-backend/          # Spring Boot 后端服务
│   ├── src/main/java/com/ryo/ryopicturebackend/
│   │   ├── Controller/  # Chart / User / AsyncMq 接口
│   │   ├── bizmq/       # RabbitMQ 消息生产与消费
│   │   ├── Config/      # 数据源、MQ、AI 模型配置
│   │   └── service/     # 业务逻辑
│   ├── sql/             # 数据库建表脚本
│   └── pom.xml
├── BI-frontend/         # React 前端应用
│   ├── src/pages/       # 页面与路由
│   ├── src/components/  # 可复用组件
│   ├── src/services/    # 接口请求封装
│   └── package.json
└── docs/                # 课程与设计文档（不纳入版本管理）
```

## 本地运行

### 1. 数据库

执行 `BI-backend/sql/` 下的建表脚本，创建本地数据库。

### 2. 后端

```bash
cd BI-backend
mvn spring-boot:run
```

服务默认监听 `8123` 端口，接口根路径为 `/api`（Knife4j 文档：<http://localhost:8123/api/doc.html>）。

**敏感配置说明**：数据库密码与 DeepSeek API Key 不写在仓库里。仓库中的
`application.yml` 使用占位符 `${DB_USERNAME:root}` / `${DB_PASSWORD:123456}`，
真实值写在 `src/main/resources/application-local.yml`（已被 `.gitignore` 忽略）。
首次使用可从 `application-local.yml.example` 复制生成。

### 3. 前端

```bash
cd BI-frontend
npm install
npm run dev
```

## 开发规范

- 后端遵循 Spring Boot 官方分层约定，数据库脚本变更同步更新 `BI-backend/sql/`。
- 前端使用 Biome 做格式与静态检查，提交前会执行 lint-staged 与 husky 钩子。
- 提交信息遵循 Conventional Commits 约定。
