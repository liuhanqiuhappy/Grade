# 项目上下文（供 CodeBuddy 自动读取）

## 项目概况

- **项目名称**：高校学生成绩管理系统（Java Web 课程作业）
- **架构**：前后端分离。前端 `frontend/`（Vue 3 + Vite），后端 `backend/`（Spring Boot 3 + MySQL），**所有业务数据与数据处理都在后端**
- **当前任务**：实验一（登录/注册/退出）后端已实现并自测通过（`mvn test` 17 用例），进入前后端联调阶段
- **开发模式**：契约先行，前后端并行

## 必须先读的文件

| 顺序 | 文件 | 作用 |
| --- | --- | --- |
| 1 | `docs/API_CONTRACT.md` | **接口唯一真相源**：通用约定、错误码表、接口定义、数据归属纪律 |
| 2 | `docs/openapi.json` | 机器可读契约，导入 Apifox 查看/调试/Mock |
| 3 | `frontend/CODEBUDDY.md` | 前端技术栈、现有代码盘点、改造清单、纪律 |
| 4 | `backend/CODEBUDDY.md` | 后端技术栈、需实现的接口、统一响应与异常规范 |

## 铁律

1. **改接口先改契约**：`docs/API_CONTRACT.md` 与 `docs/openapi.json` 同步更新，再改代码。禁止单边改字段。
2. **前端不产生业务数据，只展示和提交**：账号、凭据校验、会话全部在后端。
3. **统一响应** `{code, message, data}`，`code=0` 成功；业务失败也返回 HTTP 200。
4. **认证用 Session + Cookie**，开发期前端经 Vite 代理访问 `/api`，后端不放开 CORS。
5. **不考虑样式**：本阶段只做功能与状态流转。

## 本地环境

- 后端 `http://localhost:8080`（Maven 启动；探活 `GET /api/ping`）
- 前端 `http://localhost:5173`（`npm run dev`）
- 数据库密码走环境变量 `DB_PASSWORD`，禁止写进配置文件

## 其他文档

- `docs/联调测试与验收指南.md`：启动步骤、测试用例 TC-01～TC-16、冒烟脚本、Apifox 联调、验收对照、已知风险
- `【实验一】用户故事与验收要求.md`：课程原始需求与验收标准
- `前端开发启动说明.md`：VS Code + CodeBuddy + Apifox 的操作步骤
