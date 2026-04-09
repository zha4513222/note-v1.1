<template>
  <div class="note-list-page">
    <div class="page-header">
      <div class="header-text">
        <h1 class="page-title">我的笔记</h1>
        <p class="page-subtitle">记录思考，留住灵感</p>
      </div>
      <div class="header-stats">
        <div class="stat-item">
          <span class="stat-value">{{ total }}</span>
          <span class="stat-label">笔记</span>
        </div>
      </div>
    </div>

    <div class="page-content">
      <!-- 分类侧边栏 -->
      <aside class="sidebar">
        <CategorySidebar
          ref="categorySidebar"
          @select="handleCategorySelect"
          @drop-note="handleDropNoteToCategory"
        />
      </aside>

      <!-- 主内容区 -->
      <main class="main-content" @dragover.prevent @drop="handleDropOnMain">
        <!-- 批量操作栏 -->
        <div class="batch-actions" v-if="selectedNotes.length > 0">
          <span class="selected-count">已选择 {{ selectedNotes.length }} 篇笔记</span>
          <el-button type="danger" size="small" @click="handleBatchDelete">批量删除</el-button>
          <el-button size="small" @click="selectedNotes = []">取消选择</el-button>
        </div>

        <!-- 笔记卡片列表 -->
        <div class="notes-grid" v-loading="loading">
          <!-- 空状态 -->
          <div v-if="!loading && notes.length === 0" class="empty-state">
            <div class="empty-icon">◇</div>
            <h3 class="empty-title">暂无笔记</h3>
            <p class="empty-desc">开始记录你的第一个想法吧</p>
            <el-button type="primary" class="empty-btn" @click="$router.push('/note/new')">
              创建笔记
            </el-button>
          </div>

          <!-- 笔记卡片 -->
          <div
            v-for="(note, index) in notes"
            :key="note.id"
            class="note-card"
            :class="{ selected: selectedNotes.includes(note.id), dragging: draggingNoteId === note.id }"
            :style="{ animationDelay: `${index * 0.08}s` }"
            draggable="true"
            @click="toggleSelect(note.id)"
            @dragstart="handleDragStart($event, note)"
            @dragend="handleDragEnd"
          >
            <div class="card-checkbox" @click.stop>
              <input
                type="checkbox"
                :checked="selectedNotes.includes(note.id)"
                @change="toggleSelect(note.id)"
              />
            </div>

            <div class="card-content" @click="$router.push(`/note/${note.id}`)">
              <div class="card-header">
                <div class="card-status published">已发布</div>
                <div class="card-date">{{ formatDate(note.createdAt) }}</div>
              </div>

              <h3 class="card-title">{{ note.title }}</h3>
              <p class="card-preview">{{ note.contentPreview || '暂无内容' }}</p>

              <!-- 标签显示 -->
              <div class="card-tags" v-if="note.tags && note.tags.length > 0">
                <span class="card-tag" v-for="tag in note.tags.slice(0, 3)" :key="tag">
                  {{ tag }}
                </span>
                <span class="card-tag-more" v-if="note.tags.length > 3">
                  +{{ note.tags.length - 3 }}
                </span>
              </div>

              <div class="card-footer">
                <div class="card-meta">
                  <span class="meta-item">
                    <span class="meta-icon">◷</span>
                    {{ note.viewCount || 0 }}
                  </span>
                  <span class="meta-item">
                    <span class="meta-icon">♡</span>
                    {{ note.likeCount || 0 }}
                  </span>
                </div>
                <div class="card-actions" @click.stop>
                  <button class="action-btn edit" @click="$router.push(`/note/${note.id}/edit`)" title="编辑">
                    ✎
                  </button>
                  <button class="action-btn delete" @click="handleDelete(note.id)" title="删除">
                    ✕
                  </button>
                </div>
              </div>
            </div>

            <div class="card-decoration"></div>
            <div class="drag-indicator">⋮⋮</div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="notes.length > 0">
          <el-pagination
            v-model:current-page="page"
            :page-size="9"
            :total="total"
            layout="prev, pager, next"
            @current-change="fetchNotes"
            background
          />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import noteApi from '@/api/note'
import CategorySidebar from '@/components/category/CategorySidebar.vue'

const notes = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const selectedNotes = ref([])
const selectedCategoryId = ref(null)
const categorySidebar = ref(null)
const draggingNoteId = ref(null)

const fetchNotes = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: 9 }
    if (selectedCategoryId.value) {
      params.categoryId = selectedCategoryId.value
    }
    const res = await noteApi.getNotes(params)
    const pageData = res.data.data
    notes.value = pageData.records || []
    total.value = pageData.total || 0
  } catch (error) {
    ElMessage.error('获取笔记列表失败')
  } finally {
    loading.value = false
  }
}

const handleCategorySelect = (categoryId) => {
  selectedCategoryId.value = categoryId
  page.value = 1
  selectedNotes.value = []
  fetchNotes()
}

const handleDragStart = (event, note) => {
  draggingNoteId.value = note.id
  event.dataTransfer.setData('noteId', note.id.toString())
  event.dataTransfer.effectAllowed = 'move'
}

const handleDragEnd = () => {
  draggingNoteId.value = null
}

const handleDropNoteToCategory = async (categoryId, noteId) => {
  console.log('handleDropNoteToCategory called:', { categoryId, noteId })
  try {
    await noteApi.updateNoteCategory(noteId, categoryId)
    ElMessage.success('笔记已移动到分类')
    fetchNotes()
    refreshCategorySidebar()
  } catch (error) {
    console.error('移动笔记失败:', error)
    ElMessage.error('移动笔记失败')
  }
}

const handleDropOnMain = async (event) => {
  // 如果拖拽到主内容区但没有落在分类上，可以显示提示
  if (draggingNoteId.value) {
    // 可选：移除分类
  }
}

const toggleSelect = (id) => {
  const index = selectedNotes.value.indexOf(id)
  if (index > -1) {
    selectedNotes.value.splice(index, 1)
  } else {
    selectedNotes.value.push(id)
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}月${day}日`
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇笔记吗？', '提示', {
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await noteApi.deleteNote(id)
    ElMessage.success('删除成功')
    fetchNotes()
    refreshCategorySidebar()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedNotes.value.length} 篇笔记吗？`, '提示', {
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await noteApi.batchDeleteNotes(selectedNotes.value)
    ElMessage.success('批量删除成功')
    selectedNotes.value = []
    fetchNotes()
    refreshCategorySidebar()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

const refreshCategorySidebar = () => {
  if (categorySidebar.value) {
    categorySidebar.value.fetchCategories()
    categorySidebar.value.fetchTotalNotes()
  }
}

onMounted(() => {
  fetchNotes()
})
</script>

<style scoped>
.note-list-page {
  animation: fadeInUp 0.6s ease-out;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-light);
}

.header-text {
  animation: fadeInLeft 0.6s ease-out;
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
  letter-spacing: 2px;
}

.header-stats {
  display: flex;
  gap: 32px;
  animation: fadeInRight 0.6s ease-out;
}

.stat-item {
  text-align: center;
  padding: 16px 24px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-soft);
}

.stat-value {
  display: block;
  font-family: 'Crimson Pro', serif;
  font-size: 32px;
  font-weight: 700;
  color: var(--accent-primary);
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
}

/* 页面布局 */
.page-content {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 32px;
}

.sidebar {
  animation: fadeInLeft 0.5s ease-out;
}

/* 批量操作栏 */
.batch-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  animation: fadeIn 0.3s ease-out;
}

.selected-count {
  font-size: 14px;
  color: var(--text-secondary);
  margin-right: auto;
}

/* 笔记网格 */
.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  min-height: 400px;
}

/* 笔记卡片 */
.note-card {
  position: relative;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.5s ease-out both;
  overflow: hidden;
  display: flex;
}

.note-card.selected {
  outline: 2px solid var(--accent-primary);
  outline-offset: 2px;
}

.note-card.dragging {
  opacity: 0.5;
  transform: rotate(2deg) scale(1.02);
}

.note-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
}

.drag-indicator {
  position: absolute;
  top: 50%;
  left: 8px;
  transform: translateY(-50%);
  color: var(--text-muted);
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
  cursor: grab;
  writing-mode: vertical-rl;
  letter-spacing: 2px;
}

.note-card:hover .drag-indicator {
  opacity: 0.5;
}

.card-checkbox {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 8px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.note-card:hover .card-checkbox,
.note-card.selected .card-checkbox {
  opacity: 1;
}

.card-checkbox input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.card-content {
  flex: 1;
  padding: 24px;
  padding-top: 20px;
}

.card-decoration {
  position: absolute;
  bottom: -30px;
  right: -30px;
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, var(--accent-tertiary), transparent);
  border-radius: 50%;
  opacity: 0.5;
  transition: all 0.4s ease;
}

.note-card:hover .card-decoration {
  bottom: -20px;
  right: -20px;
  transform: scale(1.2);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-status {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 3px 8px;
  border-radius: 20px;
  background: rgba(196, 112, 75, 0.12);
  color: var(--accent-primary);
}

.card-date {
  font-size: 12px;
  color: var(--text-muted);
}

.card-title {
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-preview {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 12px;
}

.card-tag {
  padding: 2px 8px;
  background: var(--accent-tertiary);
  color: var(--accent-primary);
  border-radius: 10px;
  font-size: 10px;
  font-weight: 500;
}

.card-tag-more {
  padding: 2px 8px;
  background: var(--bg-secondary);
  color: var(--text-muted);
  border-radius: 10px;
  font-size: 10px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

.card-meta {
  display: flex;
  gap: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.meta-icon {
  font-size: 11px;
}

.card-actions {
  display: flex;
  gap: 6px;
  opacity: 0;
  transform: translateX(10px);
  transition: all 0.3s ease;
}

.note-card:hover .card-actions {
  opacity: 1;
  transform: translateX(0);
}

.action-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn.edit {
  background: var(--bg-secondary);
  color: var(--text-secondary);
}

.action-btn.edit:hover {
  background: var(--accent-tertiary);
  color: var(--accent-primary);
}

.action-btn.delete {
  background: var(--bg-secondary);
  color: var(--text-muted);
}

.action-btn.delete:hover {
  background: #f8d7da;
  color: #dc3545;
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 80px 40px;
  animation: fadeIn 0.6s ease-out;
}

.empty-icon {
  font-size: 64px;
  color: var(--border-medium);
  margin-bottom: 24px;
}

.empty-title {
  font-family: 'Crimson Pro', serif;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 15px;
  color: var(--text-muted);
  margin-bottom: 32px;
}

.empty-btn {
  background: linear-gradient(135deg, var(--accent-primary), #a85d3d) !important;
  border: none !important;
  padding: 14px 32px !important;
  border-radius: var(--radius-md) !important;
  font-weight: 600 !important;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid var(--border-light);
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: var(--bg-card);
  --el-pagination-hover-color: var(--accent-primary);
}

:deep(.el-pagination.is-background .el-pager li) {
  border-radius: var(--radius-sm);
  margin: 0 4px;
  font-weight: 500;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background: var(--accent-primary) !important;
}

/* 动画 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes fadeInLeft {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes fadeInRight {
  from { opacity: 0; transform: translateX(20px); }
  to { opacity: 1; transform: translateX(0); }
}
</style>
