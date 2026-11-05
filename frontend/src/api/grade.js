import request from '@/utils/request'

// ===== 模块四：成绩录入与确认 =====

/**
 * G1 · GET /api/courses/{courseId}/grades 成绩看板（成员 + 分数 + 确认状态）
 */
export function getGrades(courseId) {
  return request({
    url: `/courses/${courseId}/grades`,
    method: 'get'
  })
}

/**
 * G2 · PUT /api/courses/{courseId}/grades 批量保存草稿（score 传 null 表示留空）
 */
export function saveGrades(courseId, data) {
  return request({
    url: `/courses/${courseId}/grades`,
    method: 'put',
    data,
    silent: true
  })
}

/**
 * G3 · POST /api/courses/{courseId}/grades/confirm 确认发布（之后只读）
 */
export function confirmGrades(courseId) {
  return request({
    url: `/courses/${courseId}/grades/confirm`,
    method: 'post',
    silent: true
  })
}
