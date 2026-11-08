import request from './request'

export const listScores = (courseId) => request.get(`/courses/${courseId}/scores`)
export const saveScores = (courseId, entries) =>
  request.put(`/courses/${courseId}/scores`, { entries })
export const confirmScores = (courseId, classId) =>
  request.post(`/courses/${courseId}/scores/confirm`, classId ? { classId } : {})
export const courseStatistics = (courseId) => request.get(`/courses/${courseId}/statistics`)
export const importScores = (courseId, formData) =>
  request.post(`/courses/${courseId}/scores/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
export const myScores = () => request.get('/my/scores')
