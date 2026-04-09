import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'X-User-Id': 1,
    'Content-Type': 'application/json'
  }
})

export default {
  // 获取标签列表
  getTags(userId) {
    return api.get('/tags', { params: { userId } })
  },

  // 获取热门标签
  getPopularTags(limit = 10) {
    return api.get('/tags/popular', { params: { limit } })
  },

  // 创建标签
  createTag(name) {
    return api.post('/tags', { name })
  },

  // 删除标签
  deleteTag(id) {
    return api.delete(`/tags/${id}`)
  },

  // 推荐标签
  recommendTags(userId, content, tagIds) {
    return api.get('/tags/recommend', { params: { userId, content, tagIds } })
  }
}
