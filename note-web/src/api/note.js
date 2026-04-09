import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'X-User-Id': 1, // 默认用户ID
    'Content-Type': 'application/json'
  }
})

export default {
  // 获取笔记列表
  getNotes(params) {
    return api.get('/notes', { params })
  },

  // 获取笔记总数
  getNotesTotal() {
    return api.get('/notes', { params: { page: 1, size: 1 } })
  },

  // 获取笔记详情
  getNote(id) {
    return api.get(`/notes/${id}`)
  },

  // 创建笔记
  createNote(data) {
    return api.post('/notes', data)
  },

  // 更新笔记
  updateNote(id, data) {
    return api.put(`/notes/${id}`, data)
  },

  // 删除笔记
  deleteNote(id) {
    return api.delete(`/notes/${id}`)
  },

  // 批量删除笔记
  batchDeleteNotes(ids) {
    return api.post('/notes/batch-delete', { ids })
  },

  // 更新笔记分类
  updateNoteCategory(id, categoryId) {
    return api.put(`/notes/${id}/category`, { categoryId })
  },

  // 发布笔记
  publishNote(id) {
    return api.post(`/notes/${id}/publish`)
  },

  // 获取关键词
  getKeywords(id) {
    return api.get(`/notes/${id}/keywords`)
  },

  // 提取关键词
  extractKeywords(content) {
    return api.post('/notes/extract-keywords', { content })
  }
}
