import { createRouter, createWebHistory } from 'vue-router'
import NoteListView from '@/views/note/NoteListView.vue'
import NoteEditView from '@/views/note/NoteEditView.vue'
import NoteDetailView from '@/views/note/NoteDetailView.vue'
import SearchView from '@/views/search/SearchView.vue'
import StatsView from '@/views/stats/StatsView.vue'

const routes = [
  { path: '/', name: 'NoteList', component: NoteListView },
  { path: '/note/new', name: 'NoteNew', component: NoteEditView },
  { path: '/note/:id/edit', name: 'NoteEdit', component: NoteEditView },
  { path: '/note/:id', name: 'NoteDetail', component: NoteDetailView },
  { path: '/search', name: 'Search', component: SearchView },
  { path: '/stats', name: 'Stats', component: StatsView },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
