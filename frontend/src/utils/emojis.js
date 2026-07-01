const RECENT_KEY = 'forum_emoji_recent'
const MAX_RECENT = 16

export const EMOJI_LIST = [
  '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃',
  '😉', '😊', '😇', '🥰', '😍', '🤩', '😘', '😗', '😚', '😙',
  '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔',
  '🤐', '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '🤥',
  '😌', '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕', '🤢', '🤮',
  '🥵', '🥶', '🥴', '😵', '🤯', '🤠', '🥳', '😎', '🤓', '🧐',
  '😕', '😟', '🙁', '☹️', '😮', '😯', '😲', '😳', '🥺', '😦',
  '😧', '😨', '😰', '😥', '😢', '😭', '😱', '😖', '😣', '😞',
  '😓', '😩', '😫', '🥱', '😤', '😡', '😠', '🤬', '😈', '👿',
  '💀', '☠️', '💩', '🤡', '👹', '👺', '👻', '👽', '👾', '🤖',
  '👋', '🤚', '🖐️', '✋', '🖖', '👌', '🤌', '🤏', '✌️', '🤞',
  '🤟', '🤘', '🤙', '👈', '👉', '👆', '👇', '☝️', '👍', '👎',
  '✊', '👊', '🤛', '🤜', '👏', '🙌', '👐', '🤲', '🤝', '🙏',
  '💪', '🦾', '🦿', '🦵', '🦶', '👂', '👃', '🧠', '🫀', '🫁',
  '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔',
  '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '♥️',
  '🌹', '🥀', '🌷', '🌸', '💐', '🌺', '🌻', '🌼', '🌱', '🌿',
  '🍀', '🍎', '🍊', '🍋', '🍉', '🍇', '🍓', '🍒', '🍑', '🥭',
  '🍍', '🥥', '🥝', '🍅', '🍆', '🥑', '🌽', '🌶️', '🥒', '🥕',
  '🍞', '🥐', '🥖', '🧀', '🍳', '🥞', '🍔', '🍟', '🍕', '🌭',
  '🍿', '🧁', '🍰', '🎂', '🍩', '🍪', '🍫', '🍬', '🍭', '🍺',
  '🍻', '🥂', '🍷', '🍸', '🍹', '☕', '🍵', '🧋', '🍼', '🥤',
  '⚽', '🏀', '🏈', '⚾', '🎾', '🏐', '🎱', '🏓', '🏸', '🥅',
  '🎯', '🎮', '🎲', '🎭', '🎨', '🎬', '🎤', '🎧', '🎼', '🎹',
  '🎁', '🎈', '🎉', '🎊', '🎀', '🏆', '🥇', '🥈', '🥉', '🎖️',
  '⭐', '🌟', '✨', '💫', '🔥', '💥', '💢', '💦', '💨', '🌈',
  '☀️', '🌤️', '⛅', '🌥️', '☁️', '🌧️', '⛈️', '🌩️', '🌨️', '❄️',
  '🌙', '🌛', '🌜', '🌞', '🌍', '🌎', '🌏', '🪐', '💫', '🌠',
  '🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼', '🐨', '🐯',
  '🦁', '🐮', '🐷', '🐸', '🐵', '🐔', '🐧', '🐦', '🐤', '🦆',
  '🦅', '🦉', '🦇', '🐺', '🐗', '🐴', '🦄', '🐝', '🐛', '🦋',
  '🐌', '🐞', '🐜', '🦟', '🦗', '🕷️', '🐢', '🐍', '🦎', '🐙',
  '🐠', '🐟', '🐬', '🐳', '🦈', '🐊', '🐅', '🐆', '🦓', '🦍'
]

export function getRecentEmojis() {
  try {
    const raw = localStorage.getItem(RECENT_KEY)
    if (!raw) return []
    const list = JSON.parse(raw)
    return Array.isArray(list) ? list.filter(e => EMOJI_LIST.includes(e)) : []
  } catch (e) {
    return []
  }
}

export function addRecentEmoji(emoji) {
  if (!emoji) return
  const list = getRecentEmojis().filter(e => e !== emoji)
  list.unshift(emoji)
  localStorage.setItem(RECENT_KEY, JSON.stringify(list.slice(0, MAX_RECENT)))
}

export function insertTextAtCursor(textarea, text, currentValue, selection) {
  const value = currentValue || ''
  let start
  let end
  if (selection && typeof selection.start === 'number') {
    start = selection.start
    end = typeof selection.end === 'number' ? selection.end : selection.start
  } else if (textarea && typeof textarea.selectionStart === 'number') {
    start = textarea.selectionStart
    end = textarea.selectionEnd
  } else {
    start = value.length
    end = value.length
  }
  const next = value.slice(0, start) + text + value.slice(end)
  const pos = start + text.length
  return { value: next, pos }
}

export function focusTextarea(textarea, pos) {
  if (!textarea) return
  textarea.focus()
  if (typeof pos === 'number') {
    textarea.setSelectionRange(pos, pos)
  }
}
