const KEY_PREFIX = 'forum_view_history'
const LEGACY_KEY = 'forum_view_history'
const MAX = 50

function storageKey(userId) {
  return `${KEY_PREFIX}_${userId}`
}

function removeLegacyHistory() {
  try {
    localStorage.removeItem(LEGACY_KEY)
  } catch (e) {
    // ignore
  }
}

export function getViewHistory(userId) {
  if (userId == null || userId === '') {
    return []
  }
  removeLegacyHistory()
  try {
    const raw = localStorage.getItem(storageKey(userId))
    return raw ? JSON.parse(raw) : []
  } catch (e) {
    return []
  }
}

export function addViewHistory(post, userId) {
  if (!post || !post.id || userId == null || userId === '') return
  const item = {
    id: post.id,
    title: post.title || '未命名帖子',
    viewedAt: new Date().toISOString()
  }
  const list = getViewHistory(userId).filter(p => p.id !== item.id)
  list.unshift(item)
  localStorage.setItem(storageKey(userId), JSON.stringify(list.slice(0, MAX)))
}

export function clearViewHistory(userId) {
  if (userId == null || userId === '') return
  localStorage.removeItem(storageKey(userId))
}

export function getNotifyEnabled() {
  return localStorage.getItem('forum_notify_enabled') !== 'false'
}

export function setNotifyEnabled(enabled) {
  localStorage.setItem('forum_notify_enabled', enabled ? 'true' : 'false')
}

export function getSoundEnabled() {
  return localStorage.getItem('forum_sound_enabled') === 'true'
}

export function setSoundEnabled(enabled) {
  localStorage.setItem('forum_sound_enabled', enabled ? 'true' : 'false')
}
