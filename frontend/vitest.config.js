import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// The app build is configured in vite.config.js. This file only configures
// the test runner, so `npm run build` keeps using the app config untouched.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.js'],
    css: false,
    include: ['src/**/*.{test,spec}.{js,jsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],
      reportsDirectory: './coverage',
      include: ['src/**/*.{js,jsx}'],
      exclude: [
        'src/main.jsx',
        'src/test/**',
        'src/**/*.{test,spec}.{js,jsx}',
      ],
      // Floors apply to the modules that carry the logic: the API client and
      // the presentational components. The page-level containers are mostly
      // routing and data plumbing, and the landing page is a canvas-driven
      // animation, so a whole-bundle floor would only be noise.
      thresholds: {
        'src/api/*.js': {
          statements: 90,
          branches: 85,
          functions: 90,
          lines: 90,
        },
        'src/components/*.jsx': {
          statements: 60,
          branches: 65,
          functions: 55,
          lines: 60,
        },
      },
    },
  },
})
