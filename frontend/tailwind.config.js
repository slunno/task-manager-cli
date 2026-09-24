/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: { colors: { ink: '#10212d', ocean: '#0e6f84', mist: '#e9f1f2' } },
  },
  plugins: [],
}
