import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'X-User-Id': 1
  }
})

export default {
  // 获取统计概览
  getOverview(userId) {
    return api.get('/stats/overview', { params: { userId } })
  },

  // 获取笔记趋势
  getNotesTrend(userId, days = 7) {
    return api.get('/stats/notes/trend', { params: { userId, days } })
  },

  // 获取标签使用统计
  getTagsUsage(userId) {
    return api.get('/stats/tags/usage', { params: { userId } })
  },

  // 获取分类分布
  getCategoryDist(userId) {
    return api.get('/stats/notes/category-dist', { params: { userId } })
  }
}
