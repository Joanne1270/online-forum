const HISTORY_KEY = 'forum_search_history'
const ENABLED_KEY = 'forum_search_history_enabled'
const MAX = 10

export function getSearchHistoryEnabled() {
  return localStorage.getItem(ENABLED_KEY) !== 'false'
}

export function setSearchHistoryEnabled(enabled) {
  localStorage.setItem(ENABLED_KEY, enabled ? 'true' : 'false')
}

export function getSearchHistory() {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    const list = raw ? JSON.parse(raw) : []
    return Array.isArray(list) ? list.filter(Boolean) : []
  } catch (e) {
    return []
  }
}

export function addSearchHistory(keyword) {
  const text = (keyword || '').trim()
  if (!text) return
  const list = getSearchHistory().filter(item => item !== text)
  list.unshift(text)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(list.slice(0, MAX)))
}

export function removeSearchHistory(keyword) {
  const list = getSearchHistory().filter(item => item !== keyword)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(list))
}

export function clearSearchHistory() {
  localStorage.removeItem(HISTORY_KEY)
}
