/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        sentinel: {
          darkest: '#090D16',
          bg: '#0D111D',
          card: '#131B2E',
          cardHover: '#18223A',
          border: '#1E293B',
          borderLight: '#334155',
          cyan: '#06B6D4',
          blue: '#0284C7',
          green: '#10B981',
          red: '#EF4444',
          amber: '#F59E0B'
        }
      }
    },
  },
  plugins: [],
}
