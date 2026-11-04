import request from '@/utils/request'

// ===== 模块二：学生与账号（契约 S1–S6）=====

/**
 * S1 · GET /api/classes/{classId}/students 班级学生名单（分页）
 */
export function listClassStudents(classId, params) {
  return request({
    url: `/classes/${classId}/students`,
    method: 'get',
    params
  })
}

/**
 * S2 · POST /api/classes/{classId}/students 添加学生（建账号 + 建档案）
 * 响应含一次性 initialPassword，前端必须模态展示、不可遮罩关闭、不落持久化。
 */
export function addStudent(classId, data) {
  return request({
    url: `/classes/${classId}/students`,
    method: 'post',
    data,
    silent: true
  })
}

/**
 * S3 · GET /api/students 学生查询（跨班检索，按学号/姓名/班级）
 */
export function searchStudents(params) {
  return request({
    url: '/students',
    method: 'get',
    params
  })
}

/**
 * S4 · GET /api/students/{studentId} 学生详情
 */
export function getStudent(studentId) {
  return request({
    url: `/students/${studentId}`,
    method: 'get'
  })
}

/**
 * S5 · PATCH /api/students/{studentId} 修改学生（姓名/学号/转班/停用恢复）
 */
export function updateStudent(studentId, data) {
  return request({
    url: `/students/${studentId}`,
    method: 'patch',
    data,
    silent: true
  })
}

/**
 * S6 · POST /api/students/{studentId}/reset-password 重置初始口令
 * 响应含一次性 initialPassword，同 S2 安全约束。
 */
export function resetStudentPassword(studentId) {
  return request({
    url: `/students/${studentId}/reset-password`,
    method: 'post',
    silent: true
  })
}

/**
 * 批量导入学生（Excel，契约 v1.0 之外的扩展）
 * 提交 multipart/form-data（字段名 file）；部分成功语义，返回 {totalCount,successCount,failedCount,failures}
 */
export function importStudents(classId, formData) {
  return request({
    url: `/classes/${classId}/students/import`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    silent: true
  })
}
