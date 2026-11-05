import request from '@/utils/request'

// ===== 模块三 M3B：课程成员 =====

/**
 * M1 · GET /api/courses/{courseId}/members 成员列表（keyword 按学号模糊匹配）
 */
export function listMembers(courseId, params) {
  return request({
    url: `/courses/${courseId}/members`,
    method: 'get',
    params
  })
}

/**
 * M2 · POST /api/courses/{courseId}/members 单个加入（按学号）
 */
export function addMember(courseId, data) {
  return request({
    url: `/courses/${courseId}/members`,
    method: 'post',
    data,
    silent: true
  })
}

/**
 * M3 · POST /api/courses/{courseId}/members/import-class 按班级批量加入（该班全部在读学生）
 */
export function addMembersByClass(courseId, data) {
  return request({
    url: `/courses/${courseId}/members/import-class`,
    method: 'post',
    data,
    silent: true
  })
}

/**
 * M4 · DELETE /api/courses/{courseId}/members/{studentId} 移出成员
 */
export function removeMember(courseId, studentId) {
  return request({
    url: `/courses/${courseId}/members/${studentId}`,
    method: 'delete',
    silent: true
  })
}
