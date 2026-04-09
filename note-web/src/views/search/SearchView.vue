<template>
  <div class="search-page">
    <div class="page-header">
      <h1 class="page-title">搜索笔记</h1>
      <p class="page-subtitle">找到你想要的内容</p>
    </div>

    <div class="search-box-wrapper">
      <div class="search-box">
        <span class="search-icon">◈</span>
        <input
          v-model="keyword"
          type="text"
          class="search-input"
          placeholder="输入关键词搜索..."
          @keyup.enter="handleSearch"
        />
        <button class="search-btn" @click="handleSearch">搜索</button>
      </div>
    </div>

    <div class="filter-section">
      <span class="filter-label">标签筛选:</span>
      <div class="filter-tags">
        <span
          v-for="tag in allTags"
          :key="tag.id"
          class="filter-tag"
          :class="{ active: selectedTags.includes(tag.id) }"
          @click="toggleTag(tag.id)"
        >
          {{ tag.name }}
        </span>
      </div>
    </div>

    <div class="results-section" v-loading="loading">
      <div v-if="!searched" class="search-hint">
        <div class="hint-icon">◇</div>
        <p>输入关键词开始搜索</p>
      </div>

      <div v-else-if="results.length === 0" class="empty-results">
        <div class="empty-icon">◇</div>
        <h3>未找到相关笔记</h3>
        <p>换个关键词试试吧</p>
      </div>

      <div v-else class="results-grid">
        <div
          v-for="(note, index) in results"
          :key="note.id"
          class="result-card"
          :style="{ animationDelay: `${index * 0.06}s` }"
          @click="$router.push(`/note/${note.id}`)"
        >
          <h3 class="result-title">{{ note.title }}</h3>
          <p class="result-preview">{{ note.contentText || note.content }}</p>
          <div class="result-meta">
            <span class="result-date">{{ formatDate(note.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const keyword = ref('')
const selectedTags = ref([])
const allTags = ref([])
const results = ref([])
const loading = ref(false)
const searched = ref(false)

const toggleTag = (tagId) => {
  const idx = selectedTags.value.indexOf(tagId)
  if (idx > -1) {
    selectedTags.value.splice(idx, 1)
  } else {
    selectedTags.value.push(tagId)
  }
}

const handleSearch = async () => {
  if (!keyword.value && selectedTags.value.length === 0) {
    ElMessage.warning('请输入关键词或选择标签')
    return
  }
  loading.value = true
  searched.value = true
  try {
    const res = await axios.get('/api/search/notes', {
      params: {
        keyword: keyword.value,
        tagIds: selectedTags.value.join(',')
      }
    })
    results.value = res.data.data || []
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}月${day}日`
}

const fetchTags = async () => {
  try {
    const res = await axios.get('/api/tags')
    allTags.value = res.data.data || []
  } catch (error) {
    console.error('获取标签失败', error)
  }
}

onMounted(() => {
  fetchTags()
})
</script>

<style scoped>
.search-page {
  animation: fadeIn 0.5s ease-out;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 42px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 4px;
  margin-bottom: 8px;
}

.page-subtitle {
  font-size: 16px;
  color: var(--text-muted);
}

.search-box-wrapper {
  max-width: 700px;
  margin: 0 auto 32px;
}

.search-box {
  display: flex;
  align-items: center;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 8px;
  box-shadow: var(--shadow-medium);
  transition: box-shadow 0.3s ease;
}

.search-box:focus-within {
  box-shadow: var(--shadow-hover), 0 0 0 2px var(--accent-tertiary);
}

.search-icon {
  padding: 0 16px;
  font-size: 20px;
  color: var(--text-muted);
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 18px;
  padding: 12px 0;
  background: transparent;
  color: var(--text-primary);
}

.search-input::placeholder {
  color: var(--text-muted);
}

.search-btn {
  padding: 14px 28px;
  background: linear-gradient(135deg, var(--accent-primary), #a85d3d);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.search-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(196, 112, 75, 0.3);
}

.filter-section {
  max-width: 700px;
  margin: 0 auto 40px;
}

.filter-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-right: 16px;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.filter-tag {
  padding: 6px 14px;
  background: var(--bg-card);
  border: 1px solid var(--border-medium);
  border-radius: 20px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-tag:hover {
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}

.filter-tag.active {
  background: var(--accent-primary);
  border-color: var(--accent-primary);
  color: white;
}

.results-section {
  min-height: 300px;
}

.search-hint,
.empty-results {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-muted);
}

.hint-icon,
.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-results h3 {
  font-family: 'Crimson Pro', serif;
  font-size: 20px;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.result-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-soft);
  cursor: pointer;
  transition: all 0.3s ease;
  animation: fadeInUp 0.4s ease-out both;
}

.result-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
}

.result-title {
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-preview {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-muted);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
