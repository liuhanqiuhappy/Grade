import request from '@/utils/request'

// ===== 模块三 M3A：课程管理 =====

/**
 * R1 · GET /api/courses 课程列表（分页 + 关键词检索，仅返回当前教师的课程）
 */
export function listCourses(params) {
  return request({
    url: '/courses',
    method: 'get',
    params
  })
}

/**
 * R2 · GET /api/courses/{courseId} 课程详情
 */
export function getCourse(courseId) {
  return request({
    url: `/courses/${courseId}`,
    method: 'get'
  })
}

/**
 * R3 · POST /api/courses 新建课程
 * 表单提交类请求 silent:true，由页面内联展示 message。
 */
export function createCourse(data) {
  return request({
    url: '/courses',
    method: 'post',
    data,
    silent: true
  })
}

/**
 * R4 · PATCH /api/courses/{courseId} 修改课程（部分更新）
 */
export function updateCourse(courseId, data) {
  return request({
    url: `/courses/${courseId}`,
    method: 'patch',
    data,
    silent: true
  })
}

/**
 * R5 · DELETE /api/courses/{courseId} 归档课程（软删除）
 */
export function archiveCourse(courseId) {
  return request({
    url: `/courses/${courseId}`,
    method: 'delete',
    silent: true
  })
}
