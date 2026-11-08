# 项目上下文（供 CodeBuddy 自动读取）

## 项目概况

- **项目名称**：高校学生成绩管理系统（Java Web 课程作业）
- **架构**：前后端分离。前端 `frontend/`（Vue 3 + Vite + Pinia），后端 `backend/`（Spring Boot 3 + MyBatis-Plus + Flyway + MySQL），**所有业务数据与数据处理都在后端**
- **状态**：2026-11 项目已整体重构（覆盖式重写），旧代码与旧契约文档全部删除，按新接口说明 `docs/API.md` 全新实现四个模块：身份认证、班级与学生（含 Excel 导入）、课程与成员、成绩录入与确认
- **注册功能不存在**：账号由演示数据初始化器创建（`teacher01/2023010101`，口令 `pass1234`）

## 必须先读的文件

| 文件 | 作用 |
| --- | --- |
| `docs/API.md` | 接口唯一真相源：统一响应、错误码表、接口定义 |
| `README.md` | 项目概况、快速启动、约定 |

## 铁律

1. 改接口先改 `docs/API.md`，再改代码。
2. 统一响应 `{code, message, data}`，`code=0` 成功；业务失败也返回 HTTP 200（`BusinessException` + `GlobalExceptionHandler`）。
3. 认证用 Session + Cookie；身份只从 Session 判定（`SessionUser.requireTeacher`）；开发期前端经 Vite 代理访问 `/api`，后端不开 CORS。
4. 数据库密码走环境变量 `DB_PASSWORD`，禁止写进配置文件。
5. 表结构变更一律新增 `backend/src/main/resources/db/migration/V{n}__xxx.sql`，不改已执行过的脚本。

## 本地环境

- 后端 `http://localhost:8080`（探活 `GET /api/ping`）
- 前端 `http://localhost:5173`（`npm run dev`）
- 后端测试（不依赖 MySQL）：`mvn test`
