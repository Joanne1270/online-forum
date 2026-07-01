import request from './request'

export const fileApi = {
  upload: (file, category = 'media') => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/files/upload', form, {
      params: { category },
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
