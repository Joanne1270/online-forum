import request from './request'

export const adminApi = {
  stats: () => request.get('/admin/stats'),
  onlineStats: range => request.get('/admin/online-stats', { params: { range } }),
  users: params => request.get('/admin/users', { params }),
  ban: (id, data) => request.put(`/admin/users/${id}/ban`, data || {}),
  unban: id => request.put(`/admin/users/${id}/unban`),
  setRole: (id, role) => request.put(`/admin/users/${id}/role`, null, { params: { role } }),
  deactivateUser: id => request.delete(`/admin/users/${id}`),
  boards: () => request.get('/admin/boards'),
  createBoard: data => request.post('/admin/boards', data),
  updateBoard: (id, data) => request.put(`/admin/boards/${id}`, data),
  deleteBoard: id => request.delete(`/admin/boards/${id}`),
  posts: params => request.get('/admin/posts', { params }),
  deletePost: id => request.delete(`/admin/posts/${id}`),
  reports: params => request.get('/admin/reports', { params }),
  handleReport: (id, action) => request.put(`/admin/reports/${id}/handle`, null, { params: { action } }),
  profileRequests: params => request.get('/admin/profile-requests', { params }),
  approveProfileRequest: id => request.put(`/admin/profile-requests/${id}/approve`),
  rejectProfileRequest: id => request.put(`/admin/profile-requests/${id}/reject`),
  bannedWords: () => request.get('/admin/banned-words'),
  addBannedWord: data => request.post('/admin/banned-words', data),
  deleteBannedWord: id => request.delete(`/admin/banned-words/${id}`)
}
