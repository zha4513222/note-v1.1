import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'X-User-Id': 1,
    'Content-Type': 'application/json'
  }
})

export default {
  // 获取分类列表
  getCategories(userId) {
    return api.get('/categories', { params: { userId } })
  },

  // 创建分类
  createCategory(name) {
    return api.post('/categories', { name })
  },

  // 删除分类
  deleteCategory(id) {
    return api.delete(`/categories/${id}`)
  },

  // 按分类获取笔记
  getNotesByCategory(categoryId, page = 1, size = 9) {
    return api.get(`/categories/${categoryId}/notes`, { params: { page, size } })
  }
}
