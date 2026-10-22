// Floating tab bar. Put <div id="dock" data-active="home"></div> in the page and load icons.js first.
(function () {
  const el = document.getElementById('dock'); if (!el) return;
  const a = el.dataset.active, unread = +el.dataset.unread || 0;
  const item = (key, icon, label, href, badge) => `<button class="it ${a === key ? 'on' : ''}" onclick="vibe.go('${href}')" aria-label="${label}">${IC(icon)}<span>${label}</span>${badge ? `<i class="badge">${badge}</i>` : ''}</button>`;
  el.className = 'dock';
  el.innerHTML = item('home', 'home', 'Home', '01-home.html') + item('chats', 'chat', 'Chats', '01-home.html', unread)
    + `<div class="mid"><button onclick="vibe.toast('Random video match')" aria-label="Match">${IC('bolt')}</button></div>`
    + item('wallet', 'wallet', 'Wallet', '04-wallet.html') + item('me', 'user', 'Me', '05-me.html');
})();
