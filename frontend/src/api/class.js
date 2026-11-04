import request from '@/utils/request'

// ===== 模块二：班级管理（契约 C1–C4）=====

/**
 * C1 · GET /api/classes 班级列表（分页 + 关键词/年级/状态检索）
 * 列表/详情类请求可走全局 alert，这里不传 silent。
 */
export function listClasses(params) {
  return request({
    url: '/classes',
    method: 'get',
    params
  })
}

/**
 * C2 · POST /api/classes 新建班级
 * 表单提交类请求必须 silent:true，由页面内联展示 message。
 */
export function createClass(data) {
  return request({
    url: '/classes',
    method: 'post',
    data,
    silent: true
  })
}

/**
 * C3 · PATCH /api/classes/{classId} 重命名/改年级（部分更新）
 */
export function updateClass(classId, data) {
  return request({
    url: `/classes/${classId}`,
    method: 'patch',
    data,
    silent: true
  })
}

/**
 * C4 · DELETE /api/classes/{classId} 归档班级（软删除）
 * 归档是表单操作，silent:true，由页面二次确认并内联展示结果。
 */
export function archiveClass(classId) {
  return request({
    url: `/classes/${classId}`,
    method: 'delete',
    silent: true
  })
}
