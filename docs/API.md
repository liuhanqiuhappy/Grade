# 接口说明

统一响应 `{code, message, data}`；`code=0` 成功，业务失败同样返回 HTTP 200。除 `ping` 与 `login` 外均需登录（Session + Cookie）。

## 错误码表

| 错误码 | 含义 |
| --- | --- |
| 1001 | 用户名或密码错误（不区分账号不存在/密码错） |
| 1002 | 未登录或会话已过期 |
| 1003 | 无权执行该操作（角色不符） |
| 1004 | 口令变更未完成：必须先修改初始口令，除 `me` / `logout` / 修改口令外一律拒绝 |
| 2001 | 班级不存在 |
| 2002 | 班级名称已存在 |
| 2003 | 班级下仍有学生，无法删除 |
| 2004 | 学生不存在 |
| 2005 | 学号已存在 |
| 2006 | Excel 文件解析失败 |
| 2007 | 学生已加入课程或存在成绩，无法删除 |
| 3001 | 课程不存在 |
| 3002 | 同名同学期课程已存在 |
| 3003 | 成绩已确认发布，数据不可再修改 |
| 3004 | 该学生已在课程成员中 |
| 3005 | 课程成员不存在 |
| 3006 | 成绩必须在 0~100 之间 |
| 3007 | 成绩尚未确认发布 |
| 3008 | 存在未录入成绩的学生，无法确认发布 |
| 3010 | 成绩保存冲突：数据已被他人修改，请刷新后重试 |
| 3011 | 通知不存在或已撤回 |
| 4001 | 参数缺失或非法 |
| 5000 | 系统繁忙 |

## 身份模块

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/ping` | 探活，无需登录 |
| POST | `/api/identity/login` | 登录，body `{username, password}`，成功返回 `userId/username/displayName/role` 并建立会话 |
| GET | `/api/identity/me` | 恢复当前登录态 |
| POST | `/api/identity/logout` | 退出（幂等） |
| POST | `/api/identity/password` | 修改本人口令，body `{oldPassword, newPassword}`，仅需登录。原口令不符返回 1001；新口令不合规返回 4001（message 指明字段）；成功后清除强制改密标记并刷新会话身份，不需重新登录 |

## 班级与学生模块（仅教师）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/classes?page&size&keyword` | 班级分页列表（含学生数） |
| POST | `/api/classes` | 新建班级 `{name}` |
| PUT | `/api/classes/{id}` | 修改班级名 |
| DELETE | `/api/classes/{id}` | 删除班级（班级下有学生时拒绝） |
| GET | `/api/classes/{classId}/students?page&size&keyword` | 班级学生分页列表 |
| POST | `/api/classes/{classId}/students` | 新增学生 `{studentNo, name, gender}`，班级取自路径（请求体不必带 classId，带了也以路径为准） |
| POST | `/api/classes/{classId}/students/import` | Excel(.xlsx) 批量导入，form-data 字段 `file`；返回 `{successCount, failures:[{row, reason}]}` |
| GET | `/api/students?classId&page&size&keyword` | 全库学生搜索 |
| PUT | `/api/students/{id}` | 修改学生（含换班，`classId` 必填；缺省返回 4001） |
| DELETE | `/api/students/{id}` | 删除学生 |
| POST | `/api/students/{id}/password/reset` | 教师重置单个学生口令：服务端生成一次性临时口令，返回 `{studentId, studentNo, studentName, temporaryPassword}`；库中只存哈希并置 `must_change_password = 1`。临时口令**仅此一次**返回，不落库、不打日志、不可二次查询；重置不主动踢下线，下次登录生效 |

Excel 模板：第 1 列学号、第 2 列姓名、第 3 列性别；首行表头自动跳过；逐行校验并汇报失败行，合法行正常入库。

## 课程与成员模块（仅教师）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/courses?keyword` | 课程列表（含教师名/成员数/状态） |
| POST | `/api/courses` | 新建课程 `{name, term}`（同名同学期唯一，初始 DRAFT） |
| PUT | `/api/courses/{id}` | 修改课程（CONFIRMED 后拒绝） |
| DELETE | `/api/courses/{id}` | 删除课程（CONFIRMED 后拒绝） |
| GET | `/api/courses/{courseId}/members` | 成员列表（className/classId 为**加入课程时的班级快照**，BR05；含 `groupConfirmed` 标记） |
| POST | `/api/courses/{courseId}/members` | 批量加入 `{studentIds:[]}`，加入时快照学生当前班级；课程已确认后拒绝。返回 `{added, skipped}`：`skipped` 为已在课程中而被跳过的人数（不中断整批，3004 不再用于批量场景） |
| DELETE | `/api/courses/{courseId}/members/{studentId}` | 移出成员（所在分组已确认发布时拒绝；同时清理其暂存成绩） |

## 成绩模块（确认粒度 = 课程 + 班级分组）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/courses/{courseId}/scores` | 成绩表（教师；含未录入行，score 为 null；className 为加入时班级快照）。每行含 `version`（成绩行当前版本号，未录入为 null，保存时按行回传）与 `groupConfirmed`（该行所属分组是否已确认发布；已确认行前端锁定只读） |
| PUT | `/api/courses/{courseId}/scores` | 批量保存 `{entries:[{studentId, score, expectedVersion}]}`；score 可空、0~100。**已确认分组的成员行跳过而非中断**：返回 `{saved, skipped, skippedStudents[]}`，`saved` 为实际保存行数，`skipped` 为因分组已确认而跳过的行数，`skippedStudents` 为被跳过学生的「姓名（学号）」列表；同批其他行正常保存（3003 不再用于批量保存，仅用于单点写操作）。`expectedVersion` 为该行读取时的 `version`，**新录入行传 null**（新插入行版本即 0，0 为合法版本值）；与库中不一致即判定并发冲突，返回 3010 并在 message 中指出具体学生 |
| POST | `/api/courses/{courseId}/scores/import` | Excel(.xlsx) 批量导入成绩，form-data 字段 `file`；返回 `{successCount, failures:[{row, reason}]}`。模板：第 1 列学号、第 2 列分数（0~100），首行表头自动跳过。逐行校验，**失败行只记录不中断整批**：学号不在本课程成员名单、分数非数字或越界、文件内学号重复、该行所属分组已确认发布，均记为该行失败；合法行按「课程 + 学号」新增或覆盖。课程整体已 CONFIRMED 时整体拒绝（3003）；空文件/非 .xlsx/无数据行按 2006 处理 |
| POST | `/api/courses/{courseId}/scores/confirm` | 确认发布。body 可选 `{classId}`：按「课程+班级」分组确认，其他分组不受影响；缺省时确认全部分组。分组内缺一人即拒绝并指名（3008）；全部分组确认后课程转 CONFIRMED |
| GET | `/api/courses/{courseId}/statistics` | 统计（总人数/未录入/不及格）；groups 按班级快照分组，每组含 `classId`（确认时回传用）、`className`、`confirmed`。`classId` 为加入课程时的班级快照 ID，同一课程内唯一 |
| GET | `/api/my/scores` | 学生自助查分：仅「本人所在分组已确认」的课程返回分数，未确认统一「未公布」（score 恒 null） |

> 学生登录账号：新建/导入学生时自动创建（用户名 = 学号，初始口令 `pass1234`，角色 STUDENT），并绑定 `student.user_id`；新账号一律置 `must_change_password = 1`，首次登录后除 `me` / `logout` / 修改口令外所有接口返回 1004，必须改密后才能继续。
>
> 口令存储：BCrypt 加盐哈希，库中不存明文；校验只走 `PasswordEncoder.matches`。强制改密标记随会话内 `SessionUser` 走，不进每次请求的数据库查询。V4 迁移之前已存在的学生账号保持原状态（不批量置位），避免升级瞬间把存量账号全部锁进改密页。
>
> 并发保护口径（§6-4e）：`GET scores` 返回每行的 `version`；保存时逐行回传为 `expectedVersion`。若期间他人已保存，该行版本已推进，本次保存被拒绝（3010）并指名学生，不再静默覆盖。两窗口同时首次录入同一行时，由唯一键冲突兜底同样返回 3010。
>
> 成绩导入口径：导入是教师显式覆盖操作，直接写库并推进该行 `version`，**不校验 `expectedVersion`**（不参与乐观锁比对）；导入完成后前端必须重新拉取 `GET scores` 取得新版本号，页面未暂存的草稿会被导入结果覆盖。

## 补考通知模块（站内信）

> 一期只做「补考通知」一种类型（`type = MAKEUP_EXAM`），不做通用通知、不接短信/邮件。
> 挂科名单是**派生查询**，不落表：口径与统计一致——`score IS NOT NULL AND score < 60`（0 分算挂科），且**只取已确认发布的分组**（`course_member.group_confirmed = 1`），分组名一律用加入课程时的班级快照。
> 发送即固化收件人（`notice_recipient` 一人一行），之后名单变化不影响已发通知。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/courses/{courseId}/failed-students?classId` | 挂科名单（教师）。`classId` 可选，缺省为全课程；**范围内没有任何已确认分组时返回 3007**（成绩尚未确认发布）。返回 `{items:[{studentId, studentNo, studentName, className, score, hasAccount, notified}], total, noAccount}`；`hasAccount=false` 表示该生未绑定登录账号、站内信发不达，`notified` 表示该课程已有未撤回通知发给过他 |
| POST | `/api/courses/{courseId}/notices` | 发送补考通知（教师）。body `{classId?, title, content, examTime?, examPlace?, examScope?}`；`title` 必填 ≤100 字，`content` 必填 ≤1000 字，`examTime` 为 `yyyy-MM-ddTHH:mm` 本地时间。收件人由服务端按挂科名单算出，**不接受前端传学生列表**；该范围挂科人数为 0 时返回 4001。返回 `{noticeId, sent, noAccount}` |
| GET | `/api/courses/{courseId}/notices` | 已发通知列表（教师，含已撤回）。每条含 `id/classId/className/title/content/examTime/examPlace/examScope/createdAt/revoked/totalRecipients/readCount/noAccountCount` |
| POST | `/api/courses/{courseId}/notices/{noticeId}/revoke` | 撤回通知（幂等）。撤回后学生端不再展示，已读记录保留；不支持编辑已发通知 |
| GET | `/api/my/notices` | 学生端通知列表（按登录身份过滤，最多 20 条，按发送时间倒序）。返回 `{unreadCount, items:[{id(收件记录 id), noticeId, courseName, title, content, examTime, examPlace, examScope, createdAt, read}]}`；`unreadCount` 不含已撤回通知 |
| POST | `/api/my/notices/{id}/read` | 标记单条已读（幂等）。`id` 为收件记录 id，服务端校验归属，越权或不存在返回 3011 |
| POST | `/api/my/notices/read-all` | 全部标为已读 |

> 已读口径：**学生点击单条才标已读**，展开下拉不自动已读；教师端「已读 X/Y」只反映真实点击，Y 为收件人数。
> 撤回保留已读记录，因此教师端 `readCount` 可能大于当前可见通知数，此属预期。
