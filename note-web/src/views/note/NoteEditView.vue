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
          <QuillEditor
            ref="editorRef"
            theme="snow"
            v-model:content="form.content"
            contentType="html"
            :options="editorOptions"
            placeholder="开始书写你的想法..."
          />
        </div>
      </div>

      <div class="sidebar-section">
        <!-- 置顶设置 -->
        <div class="sidebar-card">
          <div class="card-header">
            <h3 class="card-title">置顶设置</h3>
          </div>
          <div class="pin-options">
            <el-radio-group v-model="form.pinDuration" size="small">
              <el-radio-button :value="0">永久</el-radio-button>
              <el-radio-button :value="7">7天</el-radio-button>
              <el-radio-button :value="3">3天</el-radio-button>
              <el-radio-button :value="1">1天</el-radio-button>
              <el-radio-button :value="-1">不置顶</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- 封面图选择 -->
        <div class="sidebar-card" v-if="form.images.length > 0">
          <div class="card-header">
            <h3 class="card-title">封面图</h3>
            <span class="crop-tip">点击图片裁剪选择区域</span>
          </div>
          <div class="cover-image-selector">
            <div
              v-for="(img, index) in form.images"
              :key="index"
              class="cover-option"
              :class="{ selected: form.coverImage === img }"
              @click="openCropDialog(img)"
            >
              <img :src="img" alt="封面候选" />
              <div class="cover-check" v-if="form.coverImage === img">✓</div>
              <div class="crop-icon">✂</div>
            </div>
          </div>
          <!-- 当前封面预览 -->
          <div class="cover-preview" v-if="form.coverImage">
            <span class="preview-label">当前封面：</span>
            <img :src="form.coverImage" alt="封面预览" class="preview-img" />
          </div>
        </div>

        <!-- 图片裁剪对话框 -->
        <ImageCropper
          :visible="cropDialogVisible"
          :image-src="cropImageSrc"
          @crop="handleCropComplete"
          @close="cropDialogVisible = false"
        />

        <!-- 推荐标签 -->
        <div class="sidebar-card">
          <div class="card-header">
            <h3 class="card-title">推荐标签</h3>
            <div class="tag-actions">
              <el-button size="small" @click="generateTagsFromContent" :loading="generatingTags" class="generate-btn">
                从内容生成
              </el-button>
              <el-button size="small" @click="fetchRecommendations" :loading="recommending" class="refresh-btn">
                刷新推荐
              </el-button>
            </div>
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
              点击生成或刷新获取标签
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
import ImageCropper from '@/components/ImageCropper.vue'

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
  status: 1,
  // 新增字段
  pinDuration: 0,        // 默认永久置顶
  coverImage: null,      // 封面图URL
  images: []             // 图片URL列表
})

// 裁剪相关状态
const cropDialogVisible = ref(false)
const cropImageSrc = ref('')
const currentCropImage = ref(null)  // 记录当前正在裁剪的原图

const selectedTags = ref([])
const availableTags = ref([])
const recommendedTags = ref([])
const newTagName = ref('')
const saving = ref(false)
const recommending = ref(false)
const generatingTags = ref(false)
const publishing = ref(false)
const editorRef = ref(null)

// Quill编辑器配置
const editorOptions = {
  modules: {
    toolbar: {
      container: [
        ['bold', 'italic', 'underline', 'strike'],
        ['blockquote', 'code-block'],
        [{ 'header': 1 }, { 'header': 2 }],
        [{ 'list': 'ordered'}, { 'list': 'bullet' }],
        [{ 'color': [] }, { 'background': [] }],
        ['image'],
        ['clean']
      ],
      handlers: {
        image: imageHandler
      }
    }
  }
}

// 图片上传handler
function imageHandler() {
  const input = document.createElement('input')
  input.setAttribute('type', 'file')
  input.setAttribute('accept', 'image/jpeg,image/png,image/gif,image/webp')
  input.click()

  input.onchange = async () => {
    const file = input.files[0]
    if (!file) return

    // 验证文件大小 (5MB)
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.warning('图片大小不能超过5MB')
      return
    }

    // 验证图片数量 (最多10张)
    if (form.images.length >= 10) {
      ElMessage.warning('每篇笔记最多上传10张图片')
      return
    }

    try {
      const res = await noteApi.uploadImage(file)
      const url = res.data.data.url

      // 获取Quill实例并插入图片
      const quill = editorRef.value.getQuill()
      const range = quill.getSelection()
      quill.insertEmbed(range.index, 'image', url)
      quill.setSelection(range.index + 1)

      // 更新图片列表
      form.images.push(url)

      // 如果是第一张图，自动设为封面
      if (form.images.length === 1 && !form.coverImage) {
        form.coverImage = url
      }

      ElMessage.success('图片上传成功')
    } catch (error) {
      console.error('图片上传失败:', error)
      ElMessage.error('图片上传失败')
    }
  }
}

const fetchNote = async () => {
  if (!isEdit) return
  try {
    const res = await noteApi.getNote(noteId)
    const note = res.data.data
    form.title = note.title
    form.content = note.content
    form.status = note.status
    form.pinDuration = note.pinDuration || 0
    form.coverImage = note.coverImage
    form.images = note.images || []
  } catch (error) {
    ElMessage.error('获取笔记失败')
  }
}

// 裁剪相关函数
const openCropDialog = (imageUrl) => {
  currentCropImage.value = imageUrl
  cropImageSrc.value = imageUrl
  cropDialogVisible.value = true
}

const handleCropComplete = (croppedUrl) => {
  // 更新封面图为裁剪后的URL
  form.coverImage = croppedUrl

  // 如果裁剪后的图片URL不在images列表中，添加它
  if (!form.images.includes(croppedUrl)) {
    form.images.push(croppedUrl)
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

// 从内容自动生成标签（最多3个）
const generateTagsFromContent = async () => {
  if (!form.content) {
    ElMessage.warning('请先输入内容')
    return
  }
  generatingTags.value = true
  try {
    form.contentText = form.content.replace(/<[^>]+>/g, '')
    const res = await tagApi.generateTags(form.contentText, 3)
    const generatedTags = res.data.data || []
    // 将生成的标签添加到推荐列表和可用列表
    recommendedTags.value = generatedTags.filter(tag => !selectedTags.value.find(t => t.id === tag.id))
    // 同时更新可用标签列表
    for (const tag of generatedTags) {
      if (!availableTags.value.find(t => t.id === tag.id)) {
        availableTags.value.push(tag)
      }
    }
    ElMessage.success(`已生成 ${generatedTags.length} 个标签`)
  } catch (error) {
    console.error('生成标签失败', error)
    ElMessage.error('生成标签失败')
  } finally {
    generatingTags.value = false
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
  try {
    form.contentText = form.content.replace(/<[^>]+>/g, '')
    if (isEdit) {
      await noteApi.updateNote(noteId, form)
      ElMessage.success('更新成功')
    } else {
      await noteApi.createNote(form)
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

.tag-actions {
  display: flex;
  gap: 8px;
}

.generate-btn {
  font-size: 12px;
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: white !important;
}

/* 置顶设置 */
.pin-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.el-radio-button__inner) {
  padding: 8px 16px !important;
  border-radius: 20px !important;
  border: 1px solid var(--border-medium) !important;
  background: var(--bg-secondary) !important;
  color: var(--text-secondary) !important;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: white !important;
  box-shadow: none !important;
}

/* 封面图选择 */
.cover-image-selector {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.cover-option {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s;
}

.cover-option:hover {
  border-color: var(--accent-tertiary);
}

.cover-option.selected {
  border-color: var(--accent-primary);
}

.cover-option img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-check {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  background: var(--accent-primary);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

/* 裁剪图标 */
.crop-icon {
  position: absolute;
  bottom: 4px;
  left: 4px;
  width: 20px;
  height: 20px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  opacity: 0;
  transition: opacity 0.2s;
}

.cover-option:hover .crop-icon {
  opacity: 1;
}

/* 裁剪提示 */
.crop-tip {
  font-size: 11px;
  color: var(--text-muted);
}

/* 封面预览 */
.cover-preview {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

.preview-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  display: block;
}

.preview-img {
  width: 100%;
  height: 80px;
  object-fit: cover;
  border-radius: var(--radius-md);
  border: 2px solid var(--accent-primary);
}

/* 标签 */
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