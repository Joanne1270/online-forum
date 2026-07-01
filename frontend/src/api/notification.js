import request from './request'

export const notificationApi = {
  list: params => request.get('/notifications', { params }),
  unreadCount: () => request.get('/notifications/unread-count'),
  unreadSummary: () => request.get('/notifications/unread-summary'),
  markRead: id => request.put(`/notifications/${id}/read`),
  markAllRead: category => request.put('/notifications/read-all', null, { params: category ? { category } : {} })
}
