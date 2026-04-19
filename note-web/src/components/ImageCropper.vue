<template>
  <el-dialog
    v-model="dialogVisible"
    title="裁剪封面图"
    width="650px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="cropper-wrapper">
      <Cropper
        ref="cropperRef"
        :src="imageSrc"
        :stencil-props="{
          aspectRatio: 16/9,
          movable: true,
          resizable: false
        }"
        :stencil-component="CircleStencil"
        class="cropper-container"
        image-restriction="fit-area"
      />
      <div class="cropper-tip">
        <span>拖动裁剪框选择封面显示区域（16:9比例）</span>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleCrop" :loading="uploading">
          确认裁剪
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, defineProps, defineEmits } from 'vue'
import { Cropper, CircleStencil } from 'vue-advanced-cropper'
import 'vue-advanced-cropper/dist/style.css'
import { ElMessage } from 'element-plus'
import noteApi from '@/api/note'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  imageSrc: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['crop', 'close', 'update:visible'])

const dialogVisible = ref(false)
const cropperRef = ref(null)
const uploading = ref(false)

// 同步visible状态
watch(() => props.visible, (val) => {
  dialogVisible.value = val
})

watch(dialogVisible, (val) => {
  emit('update:visible', val)
})

const handleCrop = async () => {
  if (!cropperRef.value) return

  uploading.value = true

  try {
    // 获取裁剪后的Canvas
    const { canvas } = cropperRef.value.getResult()

    if (!canvas) {
      ElMessage.warning('请先选择裁剪区域')
      return
    }

    // 将Canvas转换为Blob
    const blob = await new Promise((resolve) => {
      canvas.toBlob(resolve, 'image/jpeg', 0.9)
    })

    if (!blob) {
      ElMessage.error('裁剪失败')
      return
    }

    // 创建File对象
    const file = new File([blob], 'cover_cropped.jpg', { type: 'image/jpeg' })

    // 上传裁剪后的图片
    const res = await noteApi.uploadImage(file)
    const croppedUrl = res.data.data.url

    // 发送裁剪完成事件
    emit('crop', croppedUrl)
    ElMessage.success('封面图裁剪成功')

    handleClose()
  } catch (error) {
    console.error('裁剪上传失败:', error)
    ElMessage.error('裁剪上传失败')
  } finally {
    uploading.value = false
  }
}

const handleClose = () => {
  dialogVisible.value = false
  emit('close')
}
</script>

<style scoped>
.cropper-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cropper-container {
  width: 100%;
  height: 350px;
  background: #f5f5f5;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.cropper-tip {
  text-align: center;
  color: #666;
  font-size: 13px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>