# 高校学生成绩管理系统（Grade）

Java Web 课程作业项目。前后端分离架构：**所有业务数据与数据处理都在后端**，前端只负责展示与提交。

- 前端：Vue 3 + Vite + Pinia + Vue Router + Axios（`frontend/`）
- 后端：Spring Boot 3 + MyBatis-Plus + MySQL 8 + Flyway + Session 认证（`backend/`）

当前进度：模块一「登录 / 退出」、模块二「班级与学生（含 Excel 批量导入）」、模块三「课程管理与课程成员」、模块四「成绩录入与确认」均已完成并联调通过。注册功能已于契约 v0.4 移除，账号由种子数据/管理员创建。

## 目录结构

```
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/.../    # 身份（登录/会话/退出）、班级与学生、课程与课程成员、成绩录入与确认，及统一响应与异常处理
│   ├── src/main/resources/db/migration/  # Flyway 迁移 V1–V6（账号/班级/学生/课程/课程成员/成绩）
│   └── src/test/java/.../    # MockMvc + Mockito 单元测试（不依赖 MySQL）
├── frontend/                 # Vue 3 前端
│   └── src/                  # 登录/欢迎页、班级与学生页、课程/成员/成绩页，路由守卫与 axios 封装
├── docs/
│   ├── API_CONTRACT.md       # 接口契约（唯一真相源）
│   ├── openapi.json          # 机器可读契约（可导入 Apifox）
│   ├── 模块二-班级与学生-接口契约.md
│   └── 联调测试与验收指南.md  # 启动步骤、测试用例、冒烟脚本
└── 【实验一】用户故事与验收要求.md
```

## 快速开始

### 后端（http://localhost:8080）

```powershell
# 1. 确认本机 MySQL 已启动（默认 3306，可通过 DB_HOST/DB_PORT/DB_NAME/DB_USERNAME 覆盖）
# 2. 设置数据库密码环境变量（禁止写进配置文件）
$env:DB_PASSWORD = "你的MySQL密码"

cd backend
mvn clean package -DskipTests
java -jar target\grade-manager-backend-0.0.1-SNAPSHOT.jar
```

> **注意**：如果项目所在路径包含中文（例如 `D:\Workbuddy\项目开发`），`mvn spring-boot:run` 会因类路径含中文而报 `ClassNotFoundException`，请使用上面的 `package` + `java -jar` 方式启动。

启动后访问 <http://localhost:8080/api/ping> 探活，应返回 `{"code":0,"message":"ok","data":{"status":"ok"}}`。
首次启动自动建库 `grade_manager`，并由 Flyway 依次执行 V1–V6 建立 `sys_user`、`school_class`、`student`、`course`、`course_member`、`grade` 六张表，同时补齐演示账号。

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

| 模块 | 接口 | 方法 | 说明 |
| --- | --- | --- | --- |
| 基础 | `/api/ping` | GET | 探活 |
| 一 · 登录 | `/api/identity/login`、`/api/identity/me`、`/api/identity/logout` | POST / GET / POST | 登录、会话恢复、退出 |
| 二 · 班级 | `/api/classes` | GET / POST | 班级列表（分页）、新建班级 |
| 二 · 班级 | `/api/classes/{classId}` | PATCH / DELETE | 修改班级、归档班级（存在未发布成绩时返回 2005） |
| 二 · 学生 | `/api/classes/{classId}/students` | GET / POST | 班级学生名单、新增学生 |
| 二 · 学生 | `/api/students`、`/api/students/{studentId}` | GET / GET / PATCH | 学生查询、详情、修改 |
| 二 · 学生 | `/api/students/{studentId}/reset-password` | POST | 重置学生密码 |
| 二 · 批量导入 | `/api/classes/{classId}/students/import` | POST（multipart，字段 `file`） | Excel 批量导入学生，逐行独立事务 |
| 三 · 课程管理 | `/api/courses`、`/api/courses/{courseId}` | GET / POST / GET / PATCH / DELETE | 课程列表、新建、详情、修改、归档 |
| 三 · 课程成员 | `/api/courses/{courseId}/members` | GET / POST | 成员列表（按学号模糊搜索）、按学号加入 |
| 三 · 课程成员 | `/api/courses/{courseId}/members/import-class` | POST | 按班级批量加入，自动跳过已在课程的成员 |
| 三 · 课程成员 | `/api/courses/{courseId}/members/{studentId}` | DELETE | 移出课程成员 |
| 四 · 成绩 | `/api/courses/{courseId}/grades` | GET / PUT | 成绩看板、批量保存草稿（分数可留空、0–100） |
| 四 · 成绩 | `/api/courses/{courseId}/grades/confirm` | POST | 确认发布成绩，之后整门课锁定只读 |

错误码按模块分段：`1001–1003` 身份与权限、`2001–2007` 班级与学生、`3001–3008` 课程/成员/成绩、`4001` 参数错误、`5000` 系统繁忙。业务失败同样返回 HTTP 200，以 `code` 判定。

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
