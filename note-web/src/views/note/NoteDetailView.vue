<template>
  <div class="note-detail" v-loading="loading">
    <div v-if="note">
      <h1>{{ note.title }}</h1>
      <div class="meta">
        <span>创建于: {{ note.createdAt }}</span>
        <el-tag :type="note.status === 2 ? 'success' : 'info'" style="margin-left: 10px;">
          {{ note.status === 2 ? '已发布' : '草稿' }}
        </el-tag>
      </div>
      <div class="content" v-html="note.content"></div>

      <div class="tags" v-if="note.tags && note.tags.length">
        <el-tag v-for="tag in note.tags" :key="tag" style="margin-right: 8px;">
          {{ tag }}
        </el-tag>
      </div>

      <div class="actions">
        <el-button type="primary" @click="$router.push(`/note/${noteId}/edit`)">编辑</el-button>
        <el-button @click="$router.push('/')">返回列表</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import noteApi from '@/api/note'

const route = useRoute()
const noteId = route.params.id
const note = ref(null)
const loading = ref(false)

const fetchNote = async () => {
  loading.value = true
  try {
    const res = await noteApi.getNote(noteId)
    note.value = res.data.data
  } catch (error) {
    ElMessage.error('获取笔记详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchNote()
})
</script>

<style scoped>
.note-detail {
  padding: 20px;
  max-width: 900px;
}
.meta {
  color: #909399;
  margin: 10px 0 20px;
}
.content {
  line-height: 1.8;
  margin-bottom: 20px;
}
.tags {
  margin-bottom: 20px;
}
.actions {
  display: flex;
  gap: 10px;
}
</style>
