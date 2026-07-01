import request from './request'

export const messageApi = {
  conversations: () => request.get('/users/me/messages/conversations'),
  unreadCount: () => request.get('/users/me/messages/unread-count'),
  listWith: (peerId, params) => request.get(`/users/me/messages/with/${peerId}`, { params }),
  send: (peerId, data) => request.post(`/users/me/messages/with/${peerId}`, data),
  markRead: peerId => request.put(`/users/me/messages/with/${peerId}/read`)
}
