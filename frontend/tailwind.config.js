/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Status palette shared by the map, the badges and the charts, so a blocked road
        // is the same red everywhere in the application.
        status: {
          open: '#16a34a',
          partial: '#eab308',
          highrisk: '#f97316',
          blocked: '#dc2626',
        },
      },
    },
  },
  plugins: [],
}
