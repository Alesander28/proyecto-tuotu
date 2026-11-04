import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Las llamadas a /api se envían al servidor Spring Boot (puerto 8080).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/pruebas/configuracion.js',
  },
});
