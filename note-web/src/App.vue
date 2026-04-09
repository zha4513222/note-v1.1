<template>
  <div class="app-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>

    <!-- 顶部导航 -->
    <header class="app-header">
      <div class="header-content">
        <div class="logo">
          <span class="logo-icon">✎</span>
          <span class="logo-text">拾光</span>
          <span class="logo-subtitle">Note Master</span>
        </div>
        <nav class="main-nav">
          <router-link to="/" class="nav-item" :class="{ active: $route.path === '/' }">
            <span class="nav-icon">◇</span>
            笔记
          </router-link>
          <router-link to="/search" class="nav-item" :class="{ active: $route.path === '/search' }">
            <span class="nav-icon">◈</span>
            搜索
          </router-link>
          <router-link to="/stats" class="nav-item" :class="{ active: $route.path === '/stats' }">
            <span class="nav-icon">◆</span>
            统计
          </router-link>
        </nav>
        <div class="header-actions">
          <el-button type="primary" class="create-btn" @click="$router.push('/note/new')">
            <span class="btn-icon">+</span>
            新建笔记
          </el-button>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="app-main">
      <div class="content-wrapper">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>

    <!-- 页脚 -->
    <footer class="app-footer">
      <div class="footer-content">
        <span class="footer-text">拾光 · 记录生活点滴</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
</script>

<style>
/* 字体导入 */
@import url('https://fonts.googleapis.com/css2?family=Crimson+Pro:wght@400;500;600;700&family=DM+Sans:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

/* CSS Variables */
:root {
  --bg-primary: #faf8f5;
  --bg-secondary: #f5f2ed;
  --bg-card: #ffffff;
  --text-primary: #2d2a26;
  --text-secondary: #6b6560;
  --text-muted: #9c9790;
  --accent-primary: #c4704b;
  --accent-secondary: #e8a87c;
  --accent-tertiary: #f5d5c0;
  --border-light: rgba(45, 42, 38, 0.08);
  --border-medium: rgba(45, 42, 38, 0.12);
  --shadow-soft: 0 2px 8px rgba(45, 42, 38, 0.06);
  --shadow-medium: 0 4px 16px rgba(45, 42, 38, 0.08);
  --shadow-hover: 0 8px 32px rgba(45, 42, 38, 0.12);
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 20px;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
}

body {
  font-family: 'DM Sans', 'Noto Serif SC', -apple-system, sans-serif;
  background: var(--bg-primary);
  color: var(--text-primary);
  line-height: 1.6;
  min-height: 100vh;
  -webkit-font-smoothing: antialiased;
}

/* 背景装饰 */
.app-container {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
}

.bg-decoration {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}

.bg-circle-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, var(--accent-tertiary), transparent);
  top: -100px;
  right: -100px;
  animation: float 20s ease-in-out infinite;
}

.bg-circle-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #d4e4d9, transparent);
  bottom: 20%;
  left: -50px;
  animation: float 25s ease-in-out infinite reverse;
}

.bg-circle-3 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, #f5e6d3, transparent);
  top: 40%;
  right: 20%;
  animation: float 18s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(20px, -20px) scale(1.02); }
  50% { transform: translate(-10px, 20px) scale(0.98); }
  75% { transform: translate(15px, 10px) scale(1.01); }
}

/* 头部导航 */
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(250, 248, 245, 0.85);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-light);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 40px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: baseline;
  gap: 8px;
  animation: fadeInLeft 0.6s ease-out;
}

.logo-icon {
  font-size: 28px;
  color: var(--accent-primary);
}

.logo-text {
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 26px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 2px;
}

.logo-subtitle {
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 1px;
  text-transform: uppercase;
}

.main-nav {
  display: flex;
  gap: 8px;
  animation: fadeIn 0.6s ease-out 0.1s both;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: var(--radius-md);
  text-decoration: none;
  color: var(--text-secondary);
  font-weight: 500;
  font-size: 15px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.nav-item::before {
  content: '';
  position: absolute;
  bottom: 6px;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  width: 20px;
  height: 2px;
  background: var(--accent-primary);
  border-radius: 1px;
  transition: transform 0.3s ease;
}

.nav-item:hover {
  color: var(--text-primary);
  background: var(--bg-secondary);
}

.nav-item.active {
  color: var(--accent-primary);
  background: rgba(196, 112, 75, 0.08);
}

.nav-item.active::before {
  transform: translateX(-50%) scaleX(1);
}

.nav-icon {
  font-size: 14px;
}

.header-actions {
  animation: fadeInRight 0.6s ease-out;
}

.create-btn {
  background: linear-gradient(135deg, var(--accent-primary), #a85d3d) !important;
  border: none !important;
  padding: 12px 24px !important;
  border-radius: var(--radius-md) !important;
  font-weight: 600 !important;
  font-size: 14px !important;
  color: white !important;
  box-shadow: var(--shadow-medium), 0 0 0 0 rgba(196, 112, 75, 0.3) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover), 0 4px 20px rgba(196, 112, 75, 0.25) !important;
}

.btn-icon {
  margin-right: 6px;
  font-weight: 700;
}

/* 主内容区 */
.app-main {
  position: relative;
  z-index: 1;
  padding: 40px 0;
  min-height: calc(100vh - 72px - 80px);
}

.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 40px;
}

/* 页脚 */
.app-footer {
  position: relative;
  z-index: 1;
  padding: 30px 0;
  border-top: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

.footer-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 40px;
  text-align: center;
}

.footer-text {
  font-family: 'Crimson Pro', serif;
  font-size: 14px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

/* 动画 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeInLeft {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes fadeInRight {
  from { opacity: 0; transform: translateX(20px); }
  to { opacity: 1; transform: translateX(0); }
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.4s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Element Plus 覆盖 */
.el-button--primary {
  --el-button-bg-color: var(--accent-primary);
  --el-button-border-color: var(--accent-primary);
  --el-button-hover-bg-color: #a85d3d;
  --el-button-hover-border-color: #a85d3d;
}

/* 滚动条美化 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: var(--bg-secondary);
}

::-webkit-scrollbar-thumb {
  background: var(--border-medium);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--text-muted);
}
</style>
