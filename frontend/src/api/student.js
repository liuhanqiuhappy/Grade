import request from './request'

export const listStudentsOfClass = (classId, params) =>
  request.get(`/classes/${classId}/students`, { params })
export const createStudent = (classId, data) =>
  request.post(`/classes/${classId}/students`, data)
export const importStudents = (classId, formData) =>
  request.post(`/classes/${classId}/students/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
export const searchStudents = (params) => request.get('/students', { params })
export const updateStudent = (id, data) => request.put(`/students/${id}`, data)
export const deleteStudent = (id) => request.delete(`/students/${id}`)
