<template>
  <div class="category-sidebar">
    <div class="sidebar-header">
      <h3 class="sidebar-title">分类</h3>
      <el-button size="small" text @click="showAddInput = !showAddInput">+</el-button>
    </div>

    <!-- 添加分类输入框 -->
    <div v-if="showAddInput" class="add-category">
      <input
        v-model="newCategoryName"
        type="text"
        class="category-input"
        placeholder="分类名称"
        @keyup.enter="handleCreateCategory"
      />
      <el-button size="small" type="primary" @click="handleCreateCategory">添加</el-button>
    </div>

    <!-- 分类列表 -->
    <div class="category-list">
      <div
        class="category-item"
        :class="{ active: selectedCategoryId === null }"
        @click="selectCategory(null)"
        @dragover.prevent
        @drop="(e) => handleDropOnCategory(null, e)"
      >
        <span class="category-icon">◇</span>
        <span class="category-name">全部笔记</span>
        <span class="category-count">{{ totalNotes }}</span>
      </div>

      <div
        v-for="category in categories"
        :key="category.id"
        class="category-item"
        :class="{ active: selectedCategoryId === category.id, dragOver: dragOverCategoryId === category.id }"
        @click="selectCategory(category.id)"
        @dragover.prevent="handleDragOver(category.id)"
        @dragleave="handleDragLeave"
        @drop="(e) => handleDropOnCategory(category.id, e)"
      >
        <span class="category-icon">▸</span>
        <span class="category-name">{{ category.name }}</span>
        <span class="category-count">{{ category.noteCount }}</span>
        <button class="delete-btn" @click.stop="handleDeleteCategory(category.id)">×</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import categoryApi from '@/api/category'
import noteApi from '@/api/note'

const emit = defineEmits(['select', 'dropNote'])

const categories = ref([])
const selectedCategoryId = ref(null)
const showAddInput = ref(false)
const newCategoryName = ref('')
const totalNotes = ref(0)
const dragOverCategoryId = ref(null)

const fetchCategories = async () => {
  try {
    const res = await categoryApi.getCategories(1)
    categories.value = res.data.data || []
  } catch (error) {
    console.error('获取分类失败', error)
  }
}

const fetchTotalNotes = async () => {
  try {
    const res = await noteApi.getNotesTotal()
    totalNotes.value = res.data.data.total || 0
  } catch (error) {
    console.error('获取笔记总数失败', error)
  }
}

const handleCreateCategory = async () => {
  if (!newCategoryName.value.trim()) return
  try {
    const res = await categoryApi.createCategory(newCategoryName.value)
    categories.value.push(res.data.data)
    newCategoryName.value = ''
    showAddInput.value = false
    ElMessage.success('分类创建成功')
  } catch (error) {
    ElMessage.error('创建分类失败')
  }
}

const handleDeleteCategory = async (id) => {
  try {
    await categoryApi.deleteCategory(id)
    categories.value = categories.value.filter(c => c.id !== id)
    if (selectedCategoryId.value === id) {
      selectCategory(null)
    }
    ElMessage.success('分类已删除')
  } catch (error) {
    ElMessage.error('删除分类失败')
  }
}

const selectCategory = (id) => {
  selectedCategoryId.value = id
  emit('select', id)
}

const handleDragOver = (categoryId) => {
  dragOverCategoryId.value = categoryId
}

const handleDragLeave = () => {
  dragOverCategoryId.value = null
}

const handleDropOnCategory = (categoryId, event) => {
  dragOverCategoryId.value = null
  const noteId = event.dataTransfer.getData('noteId')
  console.log('CategorySidebar drop:', { categoryId, noteId, data: event.dataTransfer })
  if (noteId) {
    emit('dropNote', categoryId, parseInt(noteId))
  }
}

const setTotalNotes = (count) => {
  totalNotes.value = count
  fetchTotalNotes()
}

defineExpose({ fetchCategories, fetchTotalNotes })

onMounted(() => {
  fetchCategories()
  fetchTotalNotes()
})
</script>

<style scoped>
.category-sidebar {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-soft);
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.sidebar-title {
  font-family: 'Crimson Pro', serif;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.add-category {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.category-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-sm);
  font-size: 13px;
  outline: none;
}

.category-input:focus {
  border-color: var(--accent-primary);
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.category-item:hover {
  background: var(--bg-secondary);
}

.category-item.active {
  background: rgba(196, 112, 75, 0.1);
  color: var(--accent-primary);
}

.category-item.dragOver {
  background: rgba(196, 112, 75, 0.2);
  outline: 2px dashed var(--accent-primary);
  outline-offset: -2px;
}

.category-icon {
  font-size: 12px;
  color: var(--text-muted);
}

.category-name {
  flex: 1;
  font-size: 14px;
  color: var(--text-secondary);
}

.category-item.active .category-name {
  color: var(--accent-primary);
  font-weight: 500;
}

.category-count {
  font-size: 12px;
  color: var(--text-muted);
  background: var(--bg-secondary);
  padding: 2px 8px;
  border-radius: 10px;
}

.delete-btn {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.2s;
}

.category-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: #dc3545;
}
</style>
