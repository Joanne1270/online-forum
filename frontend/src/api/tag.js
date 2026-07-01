import request from './request'

export const tagApi = {
  search: params => request.get('/tags/search', { params })
}
