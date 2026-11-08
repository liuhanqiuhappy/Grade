import request from './request'

export const listCourses = (params) => request.get('/courses', { params })
export const createCourse = (data) => request.post('/courses', data)
export const updateCourse = (id, data) => request.put(`/courses/${id}`, data)
export const deleteCourse = (id) => request.delete(`/courses/${id}`)

export const listMembers = (courseId) => request.get(`/courses/${courseId}/members`)
export const addMembers = (courseId, studentIds) =>
  request.post(`/courses/${courseId}/members`, { studentIds })
export const removeMember = (courseId, studentId) =>
  request.delete(`/courses/${courseId}/members/${studentId}`)
