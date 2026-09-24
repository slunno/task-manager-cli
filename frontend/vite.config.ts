import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

const backendLocal =
  process.env.HELPDESK_DEV_API_TARGET ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  server: {
    proxy: {
      '/api': backendLocal,
      '/oauth2': backendLocal,
      '/login/oauth2': backendLocal,
      '/actuator': backendLocal,
    },
  },
  test: { environment: 'jsdom', setupFiles: ['./src/test/setup.ts'] },
})
