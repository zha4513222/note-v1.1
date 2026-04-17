<template>
  <div class="note-edit-page">
    <div class="edit-header">
      <button class="back-btn" @click="$router.back()">
        <span class="back-icon">←</span>
        返回
      </button>
      <h1 class="page-title">{{ isEdit ? '编辑笔记' : '新建笔记' }}</h1>
      <div class="header-actions">
        <el-button @click="$router.back()" class="cancel-btn">取消</el-button>
        <el-button v-if="isEdit && form.status === 1" type="warning" @click="handlePublish" :loading="publishing" class="publish-btn">
          发布
        </el-button>
        <el-button type="primary" @click="handleSave" :loading="saving" class="save-btn">
          保存笔记
        </el-button>
      </div>
    </div>

    <div class="edit-content">
      <div class="editor-section">
        <input
          v-model="form.title"
          type="text"
          class="title-input"
          placeholder="输入笔记标题..."
          maxlength="100"
        />
        <div class="editor-wrapper">
          <QuillEditor theme="snow" v-model:content="form.content" contentType="html" placeholder="开始书写你的想法..." />
        </div>
      </div>

      <div class="sidebar-section">
        <!-- 推荐标签 -->
        <div class="sidebar-card">
          <div class="card-header">
            <h3 class="card-title">推荐标签</h3>
            <el-button size="small" @click="fetchRecommendations" :loading="recommending" class="refresh-btn">
              刷新
            </el-button>
          </div>
          <div class="tags-list">
            <span
              v-for="tag in recommendedTags"
              :key="tag.id"
              class="recommended-tag"
              @click="addRecommendedTag(tag)"
            >
              + {{ tag.name }}
            </span>
            <span v-if="recommendedTags.length === 0" class="no-tags">
              点击刷新获取推荐
            </span>
          </div>
        </div>

        <!-- 已选标签 -->
        <div class="sidebar-card" v-if="selectedTags.length > 0">
          <div class="card-header">
            <h3 class="card-title">已选标签</h3>
          </div>
          <div class="tags-list selected">
            <span
              v-for="tag in selectedTags"
              :key="tag.id"
              class="selected-tag"
            >
              {{ tag.name }}
              <button class="remove-tag" @click="removeTag(tag)">×</button>
            </span>
          </div>
        </div>

        <!-- 可选标签 -->
        <div class="sidebar-card">
          <div class="card-header">
            <h3 class="card-title">添加标签</h3>
          </div>
          <div class="tag-input-wrapper">
            <input
              v-model="newTagName"
              type="text"
              class="tag-input"
              placeholder="输入新标签名"
              @keyup.enter="createTag"
            />
            <el-button size="small" @click="createTag" class="add-tag-btn">添加</el-button>
          </div>
          <div class="tags-list available">
            <span
              v-for="tag in availableTags"
              :key="tag.id"
              class="available-tag"
              @click="handleAddTag(tag)"
            >
              {{ tag.name }}
              <button class="delete-tag" @click.stop="deleteTag(tag)">×</button>
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import noteApi from '@/api/note'
import tagApi from '@/api/tag'

const route = useRoute()
const router = useRouter()

const noteId = route.params.id
const isEdit = !!noteId

// 从路由查询参数获取 categoryId（创建新笔记时）
const initialCategoryId = route.query.categoryId ? Number(route.query.categoryId) : null

const form = reactive({
  title: '',
  content: '',
  contentText: '',
  categoryId: initialCategoryId,
  tagIds: [],
  status: 1
})

const selectedTags = ref([])
const availableTags = ref([])
const recommendedTags = ref([])
const newTagName = ref('')
const saving = ref(false)
const recommending = ref(false)
const publishing = ref(false)

const fetchNote = async () => {
  if (!isEdit) return
  try {
    const res = await noteApi.getNote(noteId)
    const note = res.data.data
    form.title = note.title
    form.content = note.content
    form.status = note.status
  } catch (error) {
    ElMessage.error('获取笔记失败')
  }
}

const fetchTags = async () => {
  try {
    const res = await tagApi.getTags(1)
    availableTags.value = res.data.data || []
  } catch (error) {
    console.error('获取标签失败', error)
  }
}

const fetchRecommendations = async () => {
  if (!form.content) {
    ElMessage.warning('请先输入内容')
    return
  }
  recommending.value = true
  try {
    form.contentText = form.content.replace(/<[^>]+>/g, '')
    const res = await tagApi.recommendTags(1, form.contentText, form.tagIds)
    recommendedTags.value = res.data.data || []
  } catch (error) {
    console.error('获取推荐失败', error)
  } finally {
    recommending.value = false
  }
}

const handleAddTag = (tag) => {
  if (!selectedTags.value.find(t => t.id === tag.id)) {
    selectedTags.value.push(tag)
    form.tagIds.push(tag.id)
  }
}

const addRecommendedTag = (tag) => {
  if (!selectedTags.value.find(t => t.id === tag.id)) {
    selectedTags.value.push(tag)
    form.tagIds.push(tag.id)
    recommendedTags.value = recommendedTags.value.filter(t => t.id !== tag.id)
  }
}

const removeTag = (tag) => {
  selectedTags.value = selectedTags.value.filter(t => t.id !== tag.id)
  form.tagIds = form.tagIds.filter(id => id !== tag.id)
}

const createTag = async () => {
  if (!newTagName.value.trim()) return
  try {
    const res = await tagApi.createTag(newTagName.value)
    const newTag = res.data.data
    availableTags.value.push(newTag)
    handleAddTag(newTag)
    newTagName.value = ''
    ElMessage.success('标签创建成功')
  } catch (error) {
    ElMessage.error('创建标签失败')
  }
}

const deleteTag = async (tag) => {
  try {
    await tagApi.deleteTag(tag.id)
    availableTags.value = availableTags.value.filter(t => t.id !== tag.id)
    selectedTags.value = selectedTags.value.filter(t => t.id !== tag.id)
    form.tagIds = form.tagIds.filter(id => id !== tag.id)
    ElMessage.success('标签已删除')
  } catch (error) {
    ElMessage.error('删除标签失败')
  }
}

const handleSave = async () => {
  if (!form.title) {
    ElMessage.warning('请输入标题')
    return
  }
  saving.value = true
  console.log('Saving note, isEdit:', isEdit, 'noteId:', noteId)
  console.log('Form data:', form)
  try {
    form.contentText = form.content.replace(/<[^>]+>/g, '')
    if (isEdit) {
      console.log('Calling updateNote API...')
      await noteApi.updateNote(noteId, form)
      console.log('Update successful')
      ElMessage.success('更新成功')
    } else {
      console.log('Calling createNote API...')
      await noteApi.createNote(form)
      console.log('Create successful')
      ElMessage.success('创建成功')
    }
    router.push('/')
  } catch (error) {
    console.error('Save error:', error)
    ElMessage.error(isEdit ? '更新失败' : '创建失败')
  } finally {
    saving.value = false
  }
}

const handlePublish = async () => {
  try {
    await noteApi.publishNote(noteId)
    ElMessage.success('发布成功')
    form.status = 2
  } catch (error) {
    ElMessage.error('发布失败')
  }
}

onMounted(() => {
  fetchNote()
  fetchTags()
})
</script>

<style scoped>
.note-edit-page {
  animation: fadeIn 0.5s ease-out;
}

.edit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-light);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.back-icon {
  font-size: 18px;
}

.page-title {
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.cancel-btn {
  padding: 10px 20px !important;
  border-radius: var(--radius-md) !important;
}

.save-btn {
  background: linear-gradient(135deg, var(--accent-primary), #a85d3d) !important;
  border: none !important;
  padding: 10px 24px !important;
  border-radius: var(--radius-md) !important;
  font-weight: 600 !important;
}

.publish-btn {
  background: linear-gradient(135deg, #67c23a, #5daf34) !important;
  border: none !important;
  padding: 10px 24px !important;
  border-radius: var(--radius-md) !important;
  font-weight: 600 !important;
}

.edit-content {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 32px;
}

.editor-section {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 32px;
  box-shadow: var(--shadow-soft);
}

.title-input {
  width: 100%;
  border: none;
  outline: none;
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 32px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 24px;
  padding: 0;
  background: transparent;
}

.title-input::placeholder {
  color: var(--text-muted);
}

.editor-wrapper {
  min-height: 400px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}

:deep(.ql-toolbar) {
  border: none !important;
  border-bottom: 1px solid var(--border-light) !important;
  background: var(--bg-secondary);
}

:deep(.ql-container) {
  border: none !important;
  font-family: 'DM Sans', 'Noto Serif SC', sans-serif;
  font-size: 16px;
}

:deep(.ql-editor) {
  min-height: 350px;
  line-height: 1.8;
}

:deep(.ql-editor.ql-blank::before) {
  color: var(--text-muted);
  font-style: normal;
}

/* 侧边栏 */
.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.sidebar-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-soft);
}

.sidebar-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  font-family: 'Crimson Pro', serif;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.refresh-btn {
  font-size: 12px;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.recommended-tag {
  padding: 6px 12px;
  background: rgba(196, 112, 75, 0.1);
  color: var(--accent-primary);
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.recommended-tag:hover {
  background: var(--accent-primary);
  color: white;
}

.selected-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--accent-tertiary);
  color: var(--accent-primary);
  border-radius: 20px;
  font-size: 13px;
}

.remove-tag {
  background: none;
  border: none;
  color: var(--accent-primary);
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  padding: 0;
  opacity: 0.6;
}

.remove-tag:hover {
  opacity: 1;
}

.available-tag {
  padding: 6px 12px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.available-tag:hover {
  background: var(--accent-tertiary);
  color: var(--accent-primary);
}

.delete-tag {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  padding: 0 0 0 4px;
  opacity: 0.5;
}

.delete-tag:hover {
  opacity: 1;
  color: #dc3545;
}

.no-tags {
  font-size: 13px;
  color: var(--text-muted);
}

.tag-input-wrapper {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.tag-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-sm);
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease;
}

.tag-input:focus {
  border-color: var(--accent-primary);
}

.add-tag-btn {
  background: var(--bg-secondary) !important;
  border: 1px solid var(--border-medium) !important;
  color: var(--text-secondary) !important;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
