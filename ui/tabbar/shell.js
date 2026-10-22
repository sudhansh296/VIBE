// Shared demo content behind each tab-bar option, so every option is judged on the same screen.
document.getElementById('content').innerHTML = `
  <header class="flex items-center justify-between px-5 pt-5 pb-3">
    <span class="text-2xl font-black grad-text">VIBE</span>
    <span class="text-xs text-gray-400 px-3 py-1.5 rounded-full bg-cyberCard border border-cyberLine">${document.title.split('·')[1] || ''}</span>
  </header>
  <div class="grid grid-cols-2 gap-3 px-4">
    ${['#ff2a85,#7a1fd1', '#00f2fe,#1e3a8a', '#a855f7,#581c87', '#f59e0b,#9a3412', '#10b981,#065f46', '#38bdf8,#1e40af']
      .map((g, i) => `<div class="h-56 rounded-2xl neon-border relative overflow-hidden">
        <div class="absolute inset-0 opacity-90" style="background:linear-gradient(135deg,${g})"></div>
        <div class="absolute inset-0 bg-gradient-to-t from-black/80 to-transparent"></div>
        <div class="absolute bottom-3 left-3 font-bold">${['Priya', 'Arjun', 'Ananya', 'Rahul', 'Diya', 'Kabir'][i]}</div></div>`).join('')}
  </div>`;

// Tab switching: elements with [data-tab] inside .tabs; adds .active and calls window.onTab(index) if defined.
const tabs = [...document.querySelectorAll('[data-tab]')];
tabs.forEach((t, i) => t.addEventListener('click', () => {
  tabs.forEach(x => x.classList.toggle('active', x === t));
  if (window.onTab) window.onTab(i, t);
}));
