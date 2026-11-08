import request from './request'

// 教师端：挂科名单（派生查询）+ 补考通知
export const failedStudents = (courseId, classId) =>
  request.get(`/courses/${courseId}/failed-students`, { params: classId ? { classId } : {} })
export const sendNotice = (courseId, payload) => request.post(`/courses/${courseId}/notices`, payload)
export const listNotices = (courseId) => request.get(`/courses/${courseId}/notices`)
export const revokeNotice = (courseId, noticeId) =>
  request.post(`/courses/${courseId}/notices/${noticeId}/revoke`)

// 学生端：站内信下拉
export const myNotices = () => request.get('/my/notices')
export const markNoticeRead = (id) => request.post(`/my/notices/${id}/read`)
export const markAllNoticesRead = () => request.post('/my/notices/read-all')
