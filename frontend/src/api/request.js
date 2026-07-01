import axios from 'axios'
import store from '../store'
import { Message } from 'element-ui'

function resolveErrorMessage(data, fallback = '请求失败') {
  if (!data) return fallback
  if (typeof data === 'string') return data
  if (data.message) return data.message
  if (data.error) return data.error
  return fallback
}

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = store.state.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code !== 200) {
      if (data.code === 4221) {
        Message({ message: data.message, type: 'warning', duration: 2500, showClose: false })
        return Promise.reject(data)
      }
      if (data.code !== 403) {
        Message.error(resolveErrorMessage(data))
      }
      return Promise.reject(data)
    }
    return data
  },
  err => {
    const data = err.response && err.response.data
    if (data && typeof data === 'object' && data.code != null) {
      if (data.code === 403) {
        return Promise.reject(data)
      }
      if (err.response.status === 401) {
        store.dispatch('logout')
        Message.error('登录已过期，请重新登录')
        return Promise.reject(data)
      }
      Message.error(resolveErrorMessage(data))
      return Promise.reject(data)
    }
    if (err.response && err.response.data) {
      Message.error(resolveErrorMessage(err.response.data))
      return Promise.reject(err.response.data)
    }
    if (err.response && err.response.status === 401) {
      store.dispatch('logout')
      Message.error('登录已过期，请重新登录')
    } else {
      Message.error(err.message || '网络错误')
    }
    return Promise.reject(err)
  }
)

export default request
