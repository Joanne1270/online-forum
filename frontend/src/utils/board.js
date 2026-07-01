export function flattenBoards(tree) {
  const result = []
  if (!tree) return result
  for (const board of tree) {
    result.push(board)
    if (board.children && board.children.length) {
      result.push(...flattenBoards(board.children))
    }
  }
  return result
}

export function findBoardName(tree, id) {
  const flat = flattenBoards(tree)
  const board = flat.find(b => String(b.id) === String(id))
  if (!board) return '版块'
  if (board.parentId) {
    const parent = flat.find(b => b.id === board.parentId)
    return parent ? `${parent.name} · ${board.name}` : board.name
  }
  return board.name
}

function isHomeChild(board, flat) {
  if (board.boardType !== 'NORMAL' || !board.parentId) return false
  const parent = flat.find(p => p.id === board.parentId)
  return parent && parent.boardType === 'HOME'
}

/** 是否属于「首页·官方公告」区域（含其下所有小版块） */
export function isHomeSectionBoard(board, flat) {
  if (!board) return false
  if (board.boardType === 'HOME') return true
  return isHomeChild(board, flat)
}

export function categoryBoards(tree, isAdmin) {
  const flat = flattenBoards(tree)
  const postableIds = new Set(postableBoards(tree, isAdmin).map(b => b.id))
  return (tree || [])
    .filter(b => (b.boardType === 'CATEGORY' || b.boardType === 'HOME') && b.children && b.children.length)
    .map(cat => ({
      ...cat,
      children: cat.children.filter(c => postableIds.has(c.id))
    }))
    .filter(cat => cat.children.length > 0)
}

export function allowsEmptyPostContent(board) {
  return board && (board.name === '互动问答' || board.name === '本地问答')
}

export function isStandalonePostableBoard(board) {
  return board && board.boardType === 'NORMAL' && !board.parentId && board.name === '互动问答'
}

export function postableBoards(tree, isAdmin) {
  const flat = flattenBoards(tree)
  if (isAdmin) {
    return flat.filter(b => isHomeChild(b, flat))
  }
  return flat.filter(b => {
    if (isStandalonePostableBoard(b)) return true
    if (b.boardType !== 'NORMAL' || !b.parentId) return false
    const parent = flat.find(p => p.id === b.parentId)
    if (parent && parent.boardType === 'HOME') {
      return false
    }
    return parent && parent.boardType === 'CATEGORY'
  })
}

export function boardBackRoute(boardId, tree) {
  const flat = flattenBoards(tree)
  const board = flat.find(b => String(b.id) === String(boardId))
  if (!board) return { path: '/' }
  if (board.boardType === 'NORMAL' && board.parentId) {
    const parent = flat.find(p => p.id === board.parentId)
    if (parent && parent.boardType === 'CATEGORY') {
      return { path: `/board/${board.id}` }
    }
  }
  if (isStandalonePostableBoard(board)) {
    return { path: `/board/${board.id}` }
  }
  return { path: '/', query: { boardId: String(board.id) } }
}

export function canPostInBoard(boardId, tree, isAdmin) {
  if (!boardId) return false
  const flat = flattenBoards(tree)
  const board = flat.find(b => String(b.id) === String(boardId))
  if (!board) return false
  if (isAdmin) {
    return isHomeChild(board, flat)
  }
  if (isHomeSectionBoard(board, flat)) return false
  if (board.boardType === 'ALL' || board.boardType === 'CATEGORY') return false
  if (isStandalonePostableBoard(board)) return true
  const parent = flat.find(p => p.id === board.parentId)
  return board.boardType === 'NORMAL' && parent && parent.boardType === 'CATEGORY'
}
