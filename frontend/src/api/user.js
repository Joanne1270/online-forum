import request from './request'

export const authApi = {
  login: data => request.post('/auth/login', data),
  register: data => request.post('/auth/register', data),
  sendResetCode: data => request.post('/auth/forgot/send-code', data),
  resetPassword: data => request.post('/auth/forgot/reset', data)
}

export const userApi = {
  me: () => request.get('/users/me'),
  heartbeat: () => request.post('/users/me/heartbeat'),
  updateMe: data => request.put('/users/me', data),
  updatePrivacy: data => request.put('/users/me/privacy', data),
  publicProfile: id => request.get(`/users/${id}/profile`),
  getUser: id => request.get(`/users/${id}`),
  search: keyword => request.get('/users/search', { params: { keyword, limit: 10 } }),
  batchByNicknames: nicknames => request.post('/users/batch', nicknames),
  following: () => request.get('/users/me/following'),
  followers: () => request.get('/users/me/followers'),
  follow: id => request.post(`/users/${id}/follow`),
  unfollow: id => request.delete(`/users/${id}/follow`)
}
