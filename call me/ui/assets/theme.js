// Shared VIBE design tokens (from C:\live ui design). Load after the Tailwind CDN script.
tailwind.config = {
  theme: {
    extend: {
      fontFamily: { sans: ['Inter', 'sans-serif'] },
      colors: {
        cyberDark: '#07070c',
        cyberCard: '#12121c',
        cyberSurface: '#1e0f17',
        cyberLine: '#262636',
        neonPink: '#ff2a85',
        neonCyan: '#00f2fe',
        cyberGold: '#f6dc9b',
        onlineGreen: '#3ee08f',
        dangerRed: '#ff4d5e',
      },
    },
  },
};

// 3D app icons made by the user (assets/icons/ic-*.webp)
window.ICON = (name, cls = 'w-6 h-6') => `<img src="assets/icons/ic-${name}.webp" class="${cls} object-contain" alt="">`;

// Small helpers shared by every page.
window.vibe = {
  toast(msg) {
    let t = document.getElementById('vibe-toast');
    if (!t) {
      t = document.createElement('div');
      t.id = 'vibe-toast';
      t.className = 'toast';
      document.querySelector('.phone').appendChild(t);
    }
    t.textContent = msg;
    t.classList.add('show');
    clearTimeout(t._h);
    t._h = setTimeout(() => t.classList.remove('show'), 1800);
  },
  go(page) {
    document.querySelector('.phone').classList.add('leaving');
    setTimeout(() => (location.href = page), 180);
  },
};
