# 高校学生成绩管理系统（Grade）

Java Web 课程作业项目。前后端分离架构：**所有业务数据与数据处理都在后端**，前端只负责展示与提交。

- 前端：Vue 3 + Vite + Pinia + Vue Router + Axios（`frontend/`）
- 后端：Spring Boot 3 + MyBatis-Plus + MySQL 8 + Flyway + Session 认证（`backend/`）

当前进度：实验一「登录 / 退出」已完成并通过自动化测试；注册功能已于契约 v0.4 移除，账号由种子数据/管理员创建。

## 目录结构

```
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/.../    # 身份模块（登录/会话/退出）、统一响应与异常处理
│   └── src/test/java/.../    # MockMvc + Mockito 单元测试（不依赖 MySQL）
├── frontend/                 # Vue 3 前端
│   └── src/                  # 登录页、欢迎页、路由守卫、axios 封装
├── docs/
│   ├── API_CONTRACT.md       # 接口契约（唯一真相源）
│   ├── openapi.json          # 机器可读契约（可导入 Apifox）
│   └── 联调测试与验收指南.md  # 启动步骤、测试用例、冒烟脚本
└── 【实验一】用户故事与验收要求.md
```

## 快速开始

### 后端（http://localhost:8080）

```powershell
# 1. 确认本机 MySQL 已启动（默认 3306）
# 2. 设置数据库密码环境变量（禁止写进配置文件）
$env:DB_PASSWORD = "你的MySQL密码"

cd backend
mvn spring-boot:run
```

启动后访问 <http://localhost:8080/api/ping> 探活，应返回 `{"code":0,"message":"ok","data":{"status":"ok"}}`。
首次启动自动建库 `grade_manager`、建表 `sys_user` 并补齐演示账号。

**演示账号**（`app.demo-data.enabled=true` 时自动补齐）：

| 登录名 | 口令 | 角色 |
| --- | --- | --- |
| `teacher01` | `pass1234` | TEACHER |
| `student01` | `pass1234` | STUDENT |

### 前端（http://localhost:5173）

```powershell
cd frontend
npm install
npm run dev
```

开发期前端经 Vite 代理访问 `/api`（同源携带 Session Cookie），后端不放开 CORS。

### 测试

```powershell
cd backend
mvn test        # 单元测试，不需要 MySQL
```

## 接口概览

统一响应 `{code, message, data}`，`code=0` 成功，业务失败也返回 HTTP 200。完整定义见 `docs/API_CONTRACT.md` 与 `docs/openapi.json`（不一致时以 API_CONTRACT.md 为准）。

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/api/ping` | GET | 探活 |
| `/api/identity/login` | POST | 登录（校验凭据 + 建会话） |
| `/api/identity/me` | GET | 查询当前身份（会话恢复） |
| `/api/identity/logout` | POST | 退出（幂等） |

## 分支策略

采用简单的 **main + develop** 两分支模型：

| 分支 | 用途 | 规则 |
| --- | --- | --- |
| `main` | 稳定可交付版本 | 只接受来自 `develop` 的合并/PR，保持每个提交可运行、可验收 |
| `develop` | 日常开发集成 | 功能开发、契约修订、测试调整都在此进行 |

工作流：在 `develop` 上开发 → `mvn test` 与 `npm run build` 通过后合并回 `main` → 打 tag 标记里程碑（如 `exp1-login`）。

提交信息约定（Conventional Commits，中文描述）：

- `feat:` 新功能、`fix:` 缺陷修复
- `docs:` 文档与契约变更（**改接口必须先改契约再改代码**，见下）
- `chore:` 构建、依赖、配置等杂项

## 开发纪律

1. **改接口先改契约**：`docs/API_CONTRACT.md` 与 `docs/openapi.json` 同步更新后再改代码，禁止单边改字段。
2. **前端不产生业务数据**：账号、凭据校验、会话全部在后端。
3. **认证用 Session + Cookie**：开发期走 Vite 代理，后端不放开 CORS。
4. 数据库密码走环境变量 `DB_PASSWORD`，禁止写进配置文件或提交到仓库。
5. 「记住密码」为纯前端本地行为（`localStorage`），不改变接口与会话时长。
