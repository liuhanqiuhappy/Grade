import request from './request'

export const listClasses = (params) => request.get('/classes', { params })
export const createClass = (data) => request.post('/classes', data)
export const updateClass = (id, data) => request.put(`/classes/${id}`, data)
export const deleteClass = (id) => request.delete(`/classes/${id}`)
