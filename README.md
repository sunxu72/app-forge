# app-forge · AI 零代码应用生成平台

用一句自然语言描述需求，AI 自动生成一个可访问的 Web 应用：用户对话 → 代码生成 → 文件落盘 → 一键部署 → 浏览器访问。

> 本项目是在开源教程（程序员鱼皮《AI 应用开发》）的基础上学习并实现的，之后做了工程化改造，改造点见文末「我对原项目的改造」。

## 功能

- **三种生成类型**：单文件 HTML、多文件（HTML/CSS/JS）、完整 Vue 工程
- **流式输出**：SSE 实时推送生成过程，前端增量渲染
- **工具调用**：Vue 工程生成时由模型通过 `FileWriteTool` 直接写文件，避免解析不稳定的超长 JSON
- **对话历史**：每次生成的角色、内容、成功/失败状态都落库，可回看
- **一键部署**：生成 `deployKey` → 复制产物到部署目录 → 返回可访问 URL；Vue 工程自动 `npm install && npm run build`
- **权限体系**：注册/登录、`@AuthCheck` + AOP 校验、用户只能操作自己的应用、管理员可管理全部
- **接口文档**：Knife4j（`/api/doc.html`）

## 技术栈

| 层 | 选型 |
|---|---|
| 后端 | Java 21、Spring Boot 3.3、MyBatis-Flex、AOP |
| AI | LangChain4j 1.1、DeepSeek（OpenAI 兼容协议）、Reactor（Flux） |
| 存储 | MySQL 8、HikariCP |
| 前端 | Vue 3、TypeScript、Vite |
| 工具 | Knife4j、Maven、Docker |

## 架构

```
前端 (Vue3)
   │  POST /api/app/chat/gen/code (SSE)
   ▼
AppController ──► AppServiceImpl ──► AiCodeGeneratorFacade
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    ▼                     ▼                     ▼
             HTML 生成            多文件生成              Vue 工程生成
        (AiCodeGenerator)     (AiCodeGenerator)     (VueProjectGenerator + 工具调用)
                    │                     │                     │
                    └────────► CodeParserExecutor ◄──────────────┘
                                          │
                                          ▼
                               CodeFileSaverExecutor
                                          │
                                          ▼
                              tmp/code_output/{type}_{appId}
                                          │
                                   部署 deployApp
                                          │
                                          ▼
                          tmp/code_deploy/{deployKey} → 静态资源访问
```

## 快速开始

### 方式一：Docker Compose（推荐，一条命令跑全栈）

```bash
# 1. 准备环境变量
cp .env.example .env
# 编辑 .env，填入 DEEPSEEK_API_KEY

# 2. 启动 MySQL + 后端 + 前端
docker compose up -d --build

# 3. 访问
# 前端页面     http://localhost
# 接口文档     http://localhost/api/doc.html
# 生成的应用   http://localhost/api/static/{deployKey}/
```

如果 80 端口被占用，改 `docker-compose.yml` 里 frontend 的端口映射即可（如 `"8080:80"`），
同时把 `.env` 中的 `APP_DEPLOY_HOST` 改成 `http://localhost:8080/api/static`。

### 方式二：本地运行

前置：JDK 21、Maven 3.9+、MySQL 8、Node.js 18+

```bash
# 1. 初始化数据库
mysql -uroot -p < sql/create_table.sql

# 2. 配置环境变量（不要写进 application.yml）
export DEEPSEEK_API_KEY=sk-xxxxxx        # Windows PowerShell: $env:DEEPSEEK_API_KEY="sk-xxxxxx"
# 可选：MYSQL_URL / MYSQL_USERNAME / MYSQL_PASSWORD / APP_DEPLOY_HOST

# 3. 启动后端
mvn spring-boot:run

# 4. 启动前端（另开终端）
cd app-forge-frontend && npm install && npm run dev
```

## 环境变量

| 变量 | 必填 | 默认值 | 说明 |
|---|---|---|---|
| `DEEPSEEK_API_KEY` | ✅ | 无 | 模型密钥，未设置时启动会失败 |
| `DEEPSEEK_BASE_URL` | | `https://api.deepseek.com` | 兼容 OpenAI 协议的网关地址 |
| `DEEPSEEK_MODEL` | | `deepseek-v4-pro` | 模型名 |
| `MYSQL_URL` | | `jdbc:mysql://localhost:3306/yu_ai_code_mother` | 数据库地址 |
| `MYSQL_USERNAME` | | `root` | 数据库用户 |
| `MYSQL_PASSWORD` | | `root` | 数据库密码 |
| `APP_DEPLOY_HOST` | | `http://localhost/api/static` | 部署后返回的访问前缀（Docker 下经 Nginx 反代） |

## 目录结构

```
src/main/java/com/sunxu/appforge
├── ai/            # LangChain4j 模型定义与工具（FileWriteTool）
├── aop/           # 权限拦截
├── config/        # CORS、JSON、流式模型配置
├── controller/    # 应用 / 对话 / 用户 / 静态资源
├── core/          # 生成门面、代码解析器、文件保存器、部署
├── mapper/        # MyBatis-Flex Mapper
├── model/         # entity / dto / vo / enums
└── service/       # 业务实现
```

## 关键设计说明

**1. 三种生成类型如何扩展**
用 `CodeGenTypeEnum` 标识类型，解析与保存各自走 `CodeParserExecutor` / `CodeFileSaverExecutor`（策略模式），
保存侧用 `CodeFileSaverTemplate` 定义「创建目录 → 写文件 → 返回目录」的固定流程（模板方法）。
新增一种类型只需实现 Parser 和 Saver，不需要改生成主流程。

**2. 流式响应的边界处理**
生成流结束后需要把完整代码解析并落盘，但落盘失败不能让前端一直等。
因此流尾部统一追加 `__GENERATE_DONE__` / `__GENERATE_ERROR__` 标记；异常用 `onErrorResume` 吞掉后补发错误标记，
保证 SSE 不会中途断裂，同时失败时已生成的部分内容仍会写入对话历史。

**3. Vue 工程为什么用工具调用**
多文件工程的代码量远超单文件，让模型输出超长 JSON 再由后端解析，容易出现漏引号、转义错误。
改为给模型注册 `FileWriteTool`，由模型按文件粒度直接写盘，可靠性明显更高。

## 我对原项目的改造

- 配置外置：数据库、模型密钥、部署域名全部改为环境变量，仓库内不保留任何密钥
- 安全修复：删除一个直接接收 `App` 实体、无鉴权的 `PUT /app/update` 接口（可篡改应用归属）
- 性能修复：`getAppVOList` 先批量查用户后仍会逐条 `getById`，改为直接用批量结果组装 VO，消除 N+1
- 跨平台：部署时执行 `npm install && npm run build` 由硬编码 `cmd.exe` 改为按操作系统选择 `cmd` / `sh`
- 可配置：部署访问域名从常量改为 `app.deploy.host` 配置项
- 清理：移除流式模型配置里「为测试临时修改」的硬编码模型名
- 测试：为两个代码解析器补充 10 个单元测试
- 工程化：补充 `.gitignore`、`.env.example`、`Dockerfile`、`docker-compose.yml`、`.dockerignore`
- 全栈容器化：新增前端 `Dockerfile` + Nginx 配置，一条 `docker compose up` 起 MySQL / 后端 / 前端；
  Nginx 对 `/api` 做同源反代，并对 SSE 关闭 `proxy_buffering`，避免流式输出被缓冲成一次性返回
