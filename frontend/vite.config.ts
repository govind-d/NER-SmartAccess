import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// The PWA plugin is what makes offline field reporting possible: it generates a service
// worker that caches the application shell, so an officer in a valley with no signal can
// still open the app and fill in a form. The queued reports themselves live in IndexedDB
// (see src/utils/offlineDb.ts) and are pushed to /field-reports/sync when the network
// returns.
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg'],
      manifest: {
        name: 'NER SmartLogix AI',
        short_name: 'SmartLogix',
        description: 'Smart Logistics and Accessibility Intelligence for the North East',
        theme_color: '#0f172a',
        background_color: '#0f172a',
        display: 'standalone',
        start_url: '/',
        icons: [
          { src: 'icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icon-512.png', sizes: '512x512', type: 'image/png' },
        ],
      },
      workbox: {
        // Cache the OpenStreetMap tiles that have already been viewed, so the map is not
        // blank when the connection drops in the field.
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/[abc]\.tile\.openstreetmap\.org\/.*/i,
            handler: 'CacheFirst',
            options: {
              cacheName: 'osm-tiles',
              expiration: { maxEntries: 500, maxAgeSeconds: 60 * 60 * 24 * 14 },
            },
          },
        ],
      },
    }),
  ],
  // sockjs-client is an old library that still refers to Node's `global`, which does
  // not exist in a browser. Without this alias the whole app dies at import time with
  // "global is not defined" - and neither tsc nor the production build catches it,
  // because it is a runtime reference inside a dependency.
  define: {
    global: 'globalThis',
  },
  // The same substitution has to be applied while Vite pre-bundles dependencies with
  // esbuild, otherwise sockjs-client keeps its own `global` reference inside the cached
  // chunk in node_modules/.vite and the error survives the fix above.
  optimizeDeps: {
    esbuildOptions: {
      define: { global: 'globalThis' },
    },
  },
  server: {
    port: 5173,
    proxy: {
      // Talk to Spring Boot in development without any CORS configuration at all.
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/ws': { target: 'http://localhost:8080', changeOrigin: true, ws: true },
    },
  },
})
