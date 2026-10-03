import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// The config is a function so it can read .env files through loadEnv. Vite does not
// populate process.env from them, so reading process.env.VITE_* here would silently
// give undefined.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  // Where the Spring Boot backend is listening. 8080 is a crowded port, so this can be
  // overridden with VITE_BACKEND_URL in frontend/.env.local (git-ignored).
  const backendUrl = env.VITE_BACKEND_URL || 'http://localhost:8080'

  return {
    plugins: [
      react(),
      // The PWA plugin is what makes offline field reporting possible: it generates a
      // service worker that caches the application shell, so an officer in a valley with
      // no signal can still open the app and fill in a form. The queued reports
      // themselves live in IndexedDB (see src/utils/offlineDb.ts) and are pushed to
      // /field-reports/sync when the network returns.
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
          // Cache the OpenStreetMap tiles that have already been viewed, so the map is
          // not blank when the connection drops in the field.
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
        '/api': { target: backendUrl, changeOrigin: true },
        '/ws': { target: backendUrl, changeOrigin: true, ws: true },
      },
    },

    // `vite preview` serves the built bundle from dist/. It is a plain static server and
    // uses a fraction of the memory of the dev server, which matters on a machine with
    // little headroom - and it is closer to what users actually get in production.
    // It needs the same proxy rules, since preview does not read `server.proxy`.
    preview: {
      port: 5174,
      proxy: {
        '/api': { target: backendUrl, changeOrigin: true },
        '/ws': { target: backendUrl, changeOrigin: true, ws: true },
      },
    },
  }
})
