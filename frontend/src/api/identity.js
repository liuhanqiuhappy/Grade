import request from './request'

export const login = (data) => request.post('/identity/login', data)
export const me = () => request.get('/identity/me')
export const logout = () => request.post('/identity/logout')
export const changePassword = (data) => request.post('/identity/password', data)
export const ping = () => request.get('/ping')
