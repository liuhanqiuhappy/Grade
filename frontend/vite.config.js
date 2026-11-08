import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发期经代理访问 /api，后端不放开 CORS
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
