# 高校学生成绩管理系统

Java Web 课程作业，前后端分离。**所有业务数据与数据处理都在后端**，前端只负责展示与提交。

## 技术栈

- **后端** `backend/`：Spring Boot 3.3 + JDK 17 + Maven + MyBatis-Plus（boot3 starter）+ Flyway + MySQL 8/9 + Session 认证 + Apache POI（Excel 导入）
- **前端** `frontend/`：Vue 3 + Vite 5 + Pinia + Vue Router + Axios + Bootstrap 5

## 功能模块（全新实现）

| 模块 | 内容 |
| --- | --- |
| 身份认证 | `ping` / `login` / `me` / `logout`，Session + Cookie，不提供自助注册 |
| 班级与学生 | 班级增删改查、学生增删改查、Excel(.xlsx) 批量导入 |
| 课程与成员 | 课程增删改查（草稿/已确认两态）、课程成员批量加入与移出 |
| 成绩 | 教师批量录入成绩、确认发布（发布后锁定）；学生自助查已发布成绩 |

## 快速启动

```powershell
# 后端（需先设置数据库密码环境变量，禁止写进配置文件）
cd backend
$env:DB_PASSWORD = "你的 MySQL 密码"
mvn spring-boot:run          # Flyway 自动建库建表（grade_manager），首次启动补齐演示数据

# 前端
cd frontend
npm install
npm run dev                  # http://localhost:5173，/api 经 Vite 代理到 8080
```

- 演示账号：`teacher01 / pass1234`（教师），`student01 / pass1234`（学生）
- 演示数据开关：`app.demo-data.enabled=true`（默认开，置 false 可关闭）
- 后端测试（不依赖 MySQL）：`mvn test`

## 约定

1. 统一响应 `{code, message, data}`，`code=0` 成功；业务失败也返回 HTTP 200。
2. 认证用 Session + Cookie；开发期前端走 Vite 代理访问 `/api`，后端不开 CORS。
3. 身份只从 Session 判定，不采信请求体中的角色自述。
4. 「记住密码」为纯前端 localStorage 行为，不新增接口字段。
5. 数据库表结构变更一律新增 `src/main/resources/db/migration/V{n}__xxx.sql`，不改已执行过的脚本。

## 接口说明

见 [docs/API.md](docs/API.md)。

## Git 分支策略

- `main`：稳定可交付版本，只接受来自 `develop` 的合并/PR
- `develop`：日常开发集成
- 提交信息遵循 Conventional Commits 中文描述（feat / fix / docs / chore）
