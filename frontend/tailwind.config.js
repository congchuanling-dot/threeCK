/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        serif: ['"Noto Serif SC"', 'SimSun', 'serif'],
      },
      colors: {
        sanguo: {
          dark: '#1a0f0a',
          red: '#8b2500',
          gold: '#c9a227',
          paper: '#f5e6d3',
        },
      },
      animation: {
        'float': 'float 3s ease-in-out infinite',
      },
      keyframes: {
        float: { '0%, 100%': { transform: 'translateY(0)' }, '50%': { transform: 'translateY(-4px)' } },
      },
    },
  },
  plugins: [],
}
