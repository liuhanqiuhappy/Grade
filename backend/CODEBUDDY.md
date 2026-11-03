# 后端上下文（供 CodeBuddy 自动读取）

## 项目概况

- **位置**：仓库根目录下的 `backend/`（当前状态：**已初始化**，登录/注册/退出/me/ping 已实现，契约 v0.3 已定稿，待端到端联调）
- **技术栈**：Spring Boot 3.x + JDK 17（Temurin）+ Maven 3.9.x + MyBatis-Plus（**boot3 版 starter**）+ Flyway + MySQL 8.4
- **开发工具**：IntelliJ IDEA（免费功能足够；数据库用 DBeaver，接口测试用 Apifox）
- **运行**：Maven 启动 → `http://localhost:8080`；探活 `GET /api/ping`

## 必须先读的文件

| 顺序 | 文件 | 作用 |
| --- | --- | --- |
| 1 | `docs/API_CONTRACT.md` | **接口唯一真相源**：错误码表、接口定义、数据归属纪律、待确认事项 |
| 2 | `docs/openapi.json` | 机器可读契约，保持同步 |
| 3 | 根目录 `CODEBUDDY.md` | 项目级铁律 |
| 4 | `docs/联调测试与验收指南.md` | 启动步骤、TC-01～TC-16 测试用例、Apifox 联调、验收对照 |

## 初始化步骤（**已完成**，保留作为流程记录）

1. IDEA 新建 Spring Boot 项目到 `backend/`，JDK 17，依赖：Spring Web、Validation、MyBatis-Plus（boot3 starter）、Flyway、MySQL Driver
2. `application.yml` 数据库密码用环境变量 `DB_PASSWORD`：`${DB_PASSWORD}`。**密码禁止写进配置文件、禁止提交 Git**（`.gitignore` 排除本地环境配置）
3. 包结构按模块划分，身份模块：`com.xxx.identity`（controller / service / entity / mapper）
4. 先实现 `GET /api/ping` 并跑通，再写业务接口

## 已实现的接口（契约 v0.3，Identity 模块）

| 接口 | 要点 | 错误码 |
| --- | --- | --- |
| `GET /api/ping` | 探活，无需登录 | — |
| `POST /api/identity/login` | 凭据校验 + 建会话；成功返回 `userId/username/displayName/role`；`Set-Cookie: JSESSIONID`（HttpOnly） | `4001` 缺字段；`1001` 凭据错（不区分账号不存在/密码错） |
| `GET /api/identity/me` | 从会话取当前身份，用于前端恢复登录态 | `1002` 未登录/过期 |
| `POST /api/identity/logout` | 使会话失效；**幂等**：会话已失效也返回 `code=0` | — |
| `POST /api/identity/register` | **契约 §3.5（已定稿）**：仅学生自助注册，不接收 `role`，成功不自动登录 | `4001` 字段非法；`1004` 用户名已存在 |

## 统一响应与异常规范

1. 所有接口返回 `{code, message, data}`；`code=0` 成功。
2. **业务失败也返回 HTTP 200**（凭据错、未登录等都是 200 + 错误码）。HTTP 非 200 只留给基础设施故障。
3. 实现：自定义 `BusinessException(code, message)` + 全局 `@RestControllerAdvice` 捕获，统一包装；参数校验失败（`MethodArgumentNotValidException` 等）统一转 `4001`，message 指出缺失字段。
4. 兜底：未知异常转 `5000`，日志记完整堆栈，message 不暴露内部细节。

## 安全与数据纪律

1. **身份只从 Session 判定**，不采信请求体/参数里的任何角色自述（学生伪造教师 → `1003`）。
2. 账号数据只存后端数据库；前端不保存、不校验凭据。
3. 不放开 CORS（前端走 Vite 代理，同源）。
4. 改接口先改契约（`API_CONTRACT.md` + `openapi.json`），禁止单边改字段。

## 测试要求

1. 自动化回归：`mvn test`（**18 用例**，不依赖 MySQL）
   - `IdentityServiceImplTest`：口令校验、1001 统一提示、注册落 BCrypt 哈希并强制 STUDENT、重名 1004。
   - `IdentityControllerTest`：HTTP 一律 200、4001/1001/1002/1004、会话建立与失效、退出幂等、注册不自动登录。
2. Apifox 导入 `docs/openapi.json`，按 `docs/联调测试与验收指南.md` §3 的 TC-01～TC-16 逐条跑。
3. DBeaver 验证数据真实落库——**返回 200 不代表数据对了**：`password_hash` 应为 60 字符 `$2a$` 开头，库中无明文。
4. 每条验收都要有可复现的操作步骤（契约 §3 的"对应验收"列 + 指南 §6 对照表）。

## 待确认事项

契约 §4 的 Q1-Q11 已于 2026-11-03 **定稿**（Q5 注册保留、Q9 记住密码为前端本地实现），后端实现与契约一致；如课程官方契约另有要求，按「改契约 → 改 DTO/校验注解/`application.yml`」的顺序调整，不动表结构。

## 本地运行

```powershell
$env:DB_PASSWORD = "你的 MySQL 密码"   # 必需，配置文件里没有默认值
mvn spring-boot:run                     # Flyway 自动建表，首次启动补齐演示账号
```

- 演示账号：`teacher01` / `student01`，口令均为 `pass1234`（`app.demo-data.enabled=false` 可关闭）
- 表结构变更一律新增 `src/main/resources/db/migration/V{n}__xxx.sql`，**不改已执行过的脚本**
