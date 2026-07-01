export function normalizeFileUrl(url) {
  if (!url) return url
  const marker = '/api/files/uploads/'
  const idx = url.indexOf(marker)
  if (idx >= 0) {
    return url.slice(idx)
  }
  return url
}

function escapeAttr(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

const MENTION_ID_RE = /@\[(\d+)\]([^\s@\[]+)/g
const MENTION_LEGACY_RE = /@(?!\[\d+\])(?!#)([^\s@\[][^\s@]{0,49})/g
const MENTION_BOUNDARY = '(?=[\\s@#,，。！？；：、]|$)'
/** Editor: only `#name ` (with confirming space) is a completed tag. */
const HASHTAG_RE_EDITOR = /#([\w\u4e00-\u9fff]{1,50}) /g
/** Render/extract: also accept terminal `#name` when trim removed the trailing space. */
const HASHTAG_RE = /#([\w\u4e00-\u9fff]{1,50})(?: |$)/g
const TERMINAL_HASHTAG_RE = /#[\w\u4e00-\u9fff]{1,50}$/

function escapeRegex(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function sortedMentionNicknames(mentionIdByNickname) {
  return Object.keys(mentionIdByNickname || {}).sort((a, b) => b.length - a.length)
}

function buildMentionHighlightRegex(mentionIdByNickname) {
  const nicknames = sortedMentionNicknames(mentionIdByNickname)
  if (!nicknames.length) return null
  return new RegExp(`@(${nicknames.map(escapeRegex).join('|')})${MENTION_BOUNDARY}`, 'g')
}

export function trimPostContent(text) {
  if (!text) return ''
  const hadTrailingSpace = /\s$/.test(text)
  let trimmed = text.trim()
  if (hadTrailingSpace && TERMINAL_HASHTAG_RE.test(trimmed)) {
    trimmed += ' '
  }
  return trimmed
}

/** 载入编辑器时补回末尾 tag 的确认空格（保存时可能被 trim 掉） */
export function prepareEditorText(text) {
  if (!text) return ''
  let result = text.replace(/^\s+/, '').replace(/\s+$/, '')
  if (TERMINAL_HASHTAG_RE.test(result)) {
    result += ' '
  }
  return result
}

/** 加载/编辑：API 格式 @[id]昵称 → 编辑器里只显示 @昵称，id 单独保存 */
export function parseApiMentions(text) {
  const mentionIdByNickname = {}
  if (!text) {
    return { text: '', mentionIdByNickname }
  }
  const display = text.replace(MENTION_ID_RE, (_, id, name) => {
    mentionIdByNickname[name] = Number(id)
    return `@${name}`
  })
  return { text: display, mentionIdByNickname }
}

/** 提交：把已选中的 @昵称 转回 @[id]昵称 */
export function toApiMentionFormat(text, mentionIdByNickname = {}) {
  if (!text) return ''
  let result = text
  for (const name of sortedMentionNicknames(mentionIdByNickname)) {
    const id = mentionIdByNickname[name]
    if (!id) continue
    const re = new RegExp(`@${escapeRegex(name)}${MENTION_BOUNDARY}`, 'g')
    result = result.replace(re, `@[${id}]${name}`)
  }
  return result
}

export function syncMentionMap(text, mentionIdByNickname = {}) {
  const synced = {}
  for (const name of sortedMentionNicknames(mentionIdByNickname)) {
    const id = mentionIdByNickname[name]
    const re = new RegExp(`@${escapeRegex(name)}${MENTION_BOUNDARY}`)
    if (re.test(text || '')) {
      synced[name] = id
    }
  }
  return synced
}

/** 根据光标位置判断当前是在输入 @ 还是 #（避免正文后面的 tag 抢走 @ 面板） */
export function getEditorPanelContext(text, cursor, mentionIdByNickname = {}) {
  const safeCursor = typeof cursor === 'number' ? Math.max(0, Math.min(cursor, text.length)) : text.length
  const before = (text || '').slice(0, safeCursor)

  const tagTail = before.match(/#([^#\s@]{0,50})$/)
  if (tagTail) {
    return { kind: 'tag', keyword: tagTail[1] || '' }
  }

  const atTail = before.match(/@([^@\s]*)$/)
  if (atTail) {
    const keyword = atTail[1]
    if (keyword.startsWith('#')) {
      const tagKeyword = keyword.slice(1).match(/^[^#\s@]{0,50}/)
      return { kind: 'tag', keyword: tagKeyword ? tagKeyword[0] : '' }
    }
    if (Object.prototype.hasOwnProperty.call(mentionIdByNickname, keyword)) {
      return { kind: 'none', keyword: '' }
    }
    return { kind: 'at', keyword }
  }

  return { kind: 'none', keyword: '' }
}

export function extractMentionNicknames(text) {
  if (!text) return []
  const names = new Set()
  let match
  const idRe = new RegExp(MENTION_ID_RE.source, 'g')
  while ((match = idRe.exec(text)) !== null) {
    if (match[2]) names.add(match[2])
  }
  const legacyRe = new RegExp(MENTION_LEGACY_RE.source, 'g')
  while ((match = legacyRe.exec(text)) !== null) {
    names.add(match[1])
  }
  return [...names]
}

function renderMentionSpan(id, name) {
  const displayName = name || '用户'
  const attrs = [
    'class="mention mention-link"',
    `data-user-id="${id}"`,
    `data-nickname="${escapeAttr(displayName)}"`
  ]
  return `<span ${attrs.join(' ')}>@${escapeHtml(displayName)}</span>`
}

function renderHashtag(name) {
  const tag = escapeAttr(name.toLowerCase())
  return `<a class="hashtag-link" href="#" data-tag="${tag}">#${escapeHtml(name)}</a>`
}

function highlightTagsInPlain(segment) {
  if (!segment) return ''
  let result = ''
  let lastIndex = 0
  const re = new RegExp(HASHTAG_RE_EDITOR.source, 'g')
  let match
  while ((match = re.exec(segment)) !== null) {
    result += escapeHtml(segment.slice(lastIndex, match.index))
    result += `<span class="tag-complete">#${escapeHtml(match[1])} </span>`
    lastIndex = match.index + match[0].length
  }
  result += escapeHtml(segment.slice(lastIndex))
  return result
}

function highlightMentionsInPlain(segment, mentionIdByNickname) {
  if (!segment) return ''
  const mentionRe = buildMentionHighlightRegex(mentionIdByNickname)
  if (!mentionRe) return highlightTagsInPlain(segment)

  let result = ''
  let lastIndex = 0
  let match
  while ((match = mentionRe.exec(segment)) !== null) {
    result += highlightTagsInPlain(segment.slice(lastIndex, match.index))
    result += `<span class="mention-complete">@${escapeHtml(match[1])}</span>`
    lastIndex = mentionRe.lastIndex
  }
  result += highlightTagsInPlain(segment.slice(lastIndex))
  return result
}

/** 编辑器内联高亮：已选 @昵称 深蓝，#标签名+空格 变蓝 */
export function highlightEditorContent(text, mentionIdByNickname = {}) {
  if (!text) return ''
  return highlightMentionsInPlain(text, mentionIdByNickname).replace(/\n/g, '<br/>')
}

export function highlightCompletedTags(text) {
  if (!text) return ''
  return highlightTagsInPlain(text).replace(/\n/g, '<br/>')
}

export function extractCompletedTags(text) {
  if (!text) return []
  const tags = []
  const re = new RegExp(HASHTAG_RE.source, 'g')
  let match
  while ((match = re.exec(text)) !== null) {
    tags.push(match[1])
  }
  return tags
}

const IMAGE_MARKDOWN_RE = /!\[[^\]]*\]\([^)]+\)/g
const VIDEO_TAG_RE = /<video[^>]*>[\s\S]*?<\/video>/gi

export function postContentPreview(text, maxLength = 120) {
  if (!text || !text.trim()) return ''
  const plain = text
    .replace(MENTION_ID_RE, (_, id, name) => `@${name}`)
    .replace(MENTION_LEGACY_RE, (_, name) => `@${name}`)
    .replace(HASHTAG_RE, (_, name) => `#${name} `)
    .replace(IMAGE_MARKDOWN_RE, '[图片]')
    .replace(VIDEO_TAG_RE, '[视频]')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/\s+/g, ' ')
    .trim()
  if (!plain) return ''
  if (plain.length <= maxLength) return plain
  return `${plain.slice(0, maxLength)}…`
}

function replaceHashtags(text, renderTag) {
  if (!text) return ''
  const re = new RegExp(HASHTAG_RE.source, 'g')
  let result = ''
  let lastIndex = 0
  let match
  while ((match = re.exec(text)) !== null) {
    result += text.slice(lastIndex, match.index)
    result += renderTag(match[1])
    lastIndex = match.index + match[0].length
  }
  result += text.slice(lastIndex)
  return result
}

function replaceMentionsInPlain(text, mentionMap = {}) {
  const tokens = []
  const mark = (html) => {
    tokens.push(html)
    return `\u0000M${tokens.length - 1}\u0000`
  }

  let result = text.replace(MENTION_ID_RE, (_, id, name) => mark(renderMentionSpan(id, name)))
  result = result.replace(MENTION_LEGACY_RE, (_, name) => {
    const id = mentionMap[name]
    if (id) {
      return mark(renderMentionSpan(id, name))
    }
    const attrs = [
      'class="mention mention-link"',
      `data-nickname="${escapeAttr(name)}"`
    ]
    return mark(`<span ${attrs.join(' ')}>@${escapeHtml(name)}</span>`)
  })
  return result.replace(/\u0000M(\d+)\u0000/g, (_, index) => tokens[Number(index)])
}

export function renderPostContent(text, mentionMap = {}) {
  if (!text) return ''
  const normalized = text.replace(
    /https?:\/\/[^/\s"']+\/api\/files\/uploads\//g,
    '/api/files/uploads/'
  )
  let html = replaceMentionsInPlain(normalized, mentionMap)
  html = replaceHashtags(html, name => `${renderHashtag(name)} `)
  return html
    .replace(/!\[.*?\]\((.*?)\)/g, (_, url) => {
      const src = escapeAttr(normalizeFileUrl(url))
      return (
        '<span class="content-image-wrap">' +
        `<img class="content-image" src="${src}" alt="图片" loading="lazy"/>` +
        `<button type="button" class="content-image-zoom" data-src="${src}" aria-label="放大">` +
        '<i class="el-icon-zoom-in"></i></button></span>'
      )
    })
    .replace(/\[([^\]]+)\]\((https?:\/\/[^)]+)\)/g, (_, label, url) =>
      `<a href="${escapeAttr(normalizeFileUrl(url))}" target="_blank" rel="noopener noreferrer">${label}</a>`)
    .replace(/\[([^\]]+)\]\((\/api\/files\/uploads\/[^)]+)\)/g, (_, label, url) =>
      `<a href="${escapeAttr(url)}" target="_blank" rel="noopener noreferrer">${label}</a>`)
    .replace(/\n/g, '<br/>')
}
