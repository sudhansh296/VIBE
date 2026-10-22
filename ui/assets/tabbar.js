// Shared bottom tab bar (design 2B: center notch, floating chat ghost with heart badge).
// Usage: <div id="tabbar" data-active="home|chats|me" data-unread="3"></div><script src="assets/tabbar.js"></script>
(function () {
  const el = document.getElementById('tabbar'); if (!el) return;
  const active = el.dataset.active || 'home', unread = +(el.dataset.unread || 0);
  document.head.insertAdjacentHTML('beforeend', `<style>
    .tb2 { position: absolute; left: 0; right: 0; bottom: 0; height: 104px; z-index: 30; }
    .tb2 svg.bg { position: absolute; inset: 0; width: 100%; height: 104px; overflow: visible; }
    .tb2 .side { position: absolute; bottom: 12px; width: 96px; display: flex; flex-direction: column; align-items: center; gap: 2px; }
    .tb2 .side img { width: 50px; height: 50px; object-fit: contain; opacity: .55; }
    .tb2 .side span { font-size: 11.5px; color: #8b8b9a; }
    .tb2 .side .dot { width: 0; height: 4px; border-radius: 4px; margin-top: 3px; background: linear-gradient(90deg,#ff2a85,#00f2fe); }
    .tb2 .side.on img { opacity: 1; filter: drop-shadow(0 4px 10px rgba(0,242,254,.55)); transform: translateY(-3px); }
    .tb2 .side.on span, .tb2 .clabel.on { background: linear-gradient(90deg,#00f2fe,#ff2a85); -webkit-background-clip: text; background-clip: text; color: transparent; font-weight: 700; }
    .tb2 .side.on .dot { width: 20px; box-shadow: 0 0 8px #ff2a85; }
    .tb2 .center { position: absolute; left: 50%; bottom: 28px; width: 96px; height: 96px; margin-left: -48px; }
    .tb2 .center .fw { position: absolute; inset: 0; animation: tb-bob 3s ease-in-out infinite; }
    .tb2 .center img { width: 96px; height: 96px; object-fit: contain; animation: tb-beat 1.8s ease-in-out infinite; filter: drop-shadow(0 6px 14px rgba(255,42,133,.55)); }
    .tb2 .center .pool { position: absolute; left: 50%; bottom: -6px; width: 70px; height: 14px; margin-left: -35px; border-radius: 50%;
      background: radial-gradient(ellipse, rgba(255,42,133,.75), rgba(0,242,254,.25) 55%, transparent 72%); filter: blur(3px); }
    .tb2 .heart { position: absolute; top: 3px; right: 3px; width: 33px; height: 31px; animation: tb-hb 2.4s ease-in-out infinite; }
    .tb2 .heart b { position: absolute; inset: 0; padding-bottom: 3px; display: flex; align-items: center; justify-content: center; font-size: 12.5px; font-weight: 900; }
    .tb2 .clabel { position: absolute; left: 50%; bottom: 14px; transform: translateX(-50%); font-size: 11.5px; color: #8b8b9a; }
    @keyframes tb-bob { 0%,100% { transform: translateY(0) rotate(-2deg) } 50% { transform: translateY(-7px) rotate(2deg) } }
    @keyframes tb-beat { 0%,100% { transform: scale(1) } 12% { transform: scale(1.08,.95) } 24% { transform: scale(.97,1.04) } 36% { transform: scale(1) } }
    @keyframes tb-hb { 0%,100% { transform: scale(1) } 8% { transform: scale(1.18) } 16% { transform: scale(.96) } 24% { transform: scale(1.08) } 32% { transform: scale(1) } }
  </style>`);
  const P = 'M0,30 H138 C158,30 164,34 170,46 C180,62 194,72 210,72 C226,72 240,62 250,46 C256,34 262,30 282,30 H420';
  el.outerHTML = `<nav class="tb2">
    <svg class="bg" viewBox="0 0 420 104" preserveAspectRatio="none"><defs>
      <linearGradient id="tbf" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#16121f" stop-opacity=".96"/><stop offset="1" stop-color="#07070c"/></linearGradient>
      <linearGradient id="tbe" x1="0" x2="1"><stop offset="0" stop-color="#00f2fe" stop-opacity=".15"/><stop offset=".5" stop-color="#ff2a85"/><stop offset="1" stop-color="#00f2fe" stop-opacity=".15"/></linearGradient></defs>
      <path d="${P} V104 H0 Z" fill="url(#tbf)"/><path d="${P}" fill="none" stroke="url(#tbe)" stroke-width="1.5"/></svg>
    <a href="05-home.html" class="side ${active === 'home' ? 'on' : ''}" style="left:18px"><img src="assets/icons/home.webp" alt=""><span>Home</span><i class="dot"></i></a>
    <a href="14-chats.html" class="center"><span class="pool"></span><span class="fw"><img src="assets/icons/chat.webp" alt="Chats">
      ${unread ? `<span class="heart"><svg viewBox="0 0 30 28" width="33" height="31"><defs><linearGradient id="tbh" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ff5fa6"/><stop offset=".55" stop-color="#ff2a85"/><stop offset="1" stop-color="#b3127a"/></linearGradient></defs>
        <path d="M15 27 C6 20 1 15 1 9 C1 4.5 4.5 1 9 1 C11.6 1 13.8 2.3 15 4.3 C16.2 2.3 18.4 1 21 1 C25.5 1 29 4.5 29 9 C29 15 24 20 15 27 Z" fill="url(#tbh)" stroke="#07070c" stroke-width="1.6"/></svg><b>${unread}</b></span>` : ''}</span></a>
    <span class="clabel ${active === 'chats' ? 'on' : ''}">Chats</span>
    <a href="17-me.html" class="side ${active === 'me' ? 'on' : ''}" style="right:18px"><img src="assets/icons/profile.webp" alt=""><span>Me</span><i class="dot"></i></a>
  </nav>`;
})();
