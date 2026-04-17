import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useCategoryStore = defineStore('category', () => {
  const selectedCategoryId = ref(null)

  const setSelectedCategory = (categoryId) => {
    selectedCategoryId.value = categoryId
  }

  const clearSelectedCategory = () => {
    selectedCategoryId.value = null
  }

  return {
    selectedCategoryId,
    setSelectedCategory,
    clearSelectedCategory
  }
})