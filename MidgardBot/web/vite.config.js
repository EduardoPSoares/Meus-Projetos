import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    {
      name: 'remove-crossorigin',
      transformIndexHtml(html) {
        return html.replace(/\s+crossorigin/g, '')
      }
    }
  ],
  server: {
    proxy: {
      '/api': 'http://localhost:7070'
    }
  },
  build: {
    outDir: '../src/main/resources/webapp',
    emptyOutDir: true
  }
})
