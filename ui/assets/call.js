// Shared call helpers: icons, gift sheet, quick top-up, end confirm.
// DEMO only: in the real app gift catalogue/animations come from the admin panel via the API,
// and the SERVER validates prices, deducts coins and bills per minute. The app only displays results.
window.CI = {
  mic: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3Z"/><path d="M19 11a7 7 0 0 1-14 0M12 18v3"/></svg>',
  spk: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M11 5 6 9H3v6h3l5 4V5Z"/><path d="M15.5 8.5a5 5 0 0 1 0 7M18.5 5.5a9 9 0 0 1 0 13"/></svg>',
  cam: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="2.5" y="6" width="13.5" height="12" rx="3"/><path d="m16 11 5-3v8l-5-3"/></svg>',
  flip: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M20 8h-3l-2-3H9L7 8H4a1 1 0 0 0-1 1v9a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1V9a1 1 0 0 0-1-1Z"/><path d="M9.5 13.5a2.5 2.5 0 0 1 4.3-1.8M14.5 13.5a2.5 2.5 0 0 1-4.3 1.8"/></svg>',
  gift: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="3" y="8" width="18" height="4" rx="1"/><path d="M12 8v13M19 12v8a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1v-8"/><path d="M7.5 8a2.5 2.5 0 0 1 0-5C10 3 12 8 12 8s2-5 4.5-5a2.5 2.5 0 0 1 0 5"/></svg>',
  end: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 9c-3.2 0-6.1 1-8.4 2.6-.4.3-.6.7-.6 1.2v2.4c0 .7.6 1.2 1.3 1.1l3-.5c.6-.1 1-.6 1-1.2v-1.7c1.2-.4 2.4-.6 3.7-.6s2.5.2 3.7.6v1.7c0 .6.4 1.1 1 1.2l3 .5c.7.1 1.3-.4 1.3-1.1v-2.4c0-.5-.2-.9-.6-1.2C18.1 10 15.2 9 12 9Z"/></svg>',
  phone: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6.6 10.8a15.1 15.1 0 0 0 6.6 6.6l2.2-2.2c.3-.3.7-.4 1-.2 1.1.4 2.3.6 3.6.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1A17 17 0 0 1 3 4c0-.6.4-1 1-1h3.5c.6 0 1 .4 1 1 0 1.3.2 2.5.6 3.6.1.3 0 .7-.2 1l-2.3 2.2Z"/></svg>',
  video: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M5 5.5h8.5a3 3 0 0 1 3 3v7a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3v-7a3 3 0 0 1 3-3z"/><path d="M17.5 10.2l3.4-2.3c.5-.3 1.1 0 1.1.6v6.9c0 .6-.6.9-1.1.6l-3.4-2.3z"/></svg>',
  chev: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m6 9 6 6 6-6"/></svg>',
  lock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/></svg>',
  send: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>',
};
window.cbtn = (key, label, cls = '') =>
  `<button class="c-btn ${cls}" data-k="${key}"><span class="ic">${CI[key]}</span><span class="lb">${label}</span></button>`;

window.CallSheets = {
  GIFTS: {
    popular: [['🌹', 'Rose', 20], ['☕', 'Chai', 50], ['🍫', 'Choco', 99], ['🧸', 'Teddy', 199], ['🎈', 'Balloon', 30], ['🍦', 'Ice cream', 40], ['🎵', 'Song', 80], ['⭐', 'Star', 150]],
    love: [['💖', 'Heart', 25], ['💋', 'Kiss', 60], ['💐', 'Bouquet', 299], ['💌', 'Letter', 120], ['🥰', 'Hug', 90], ['💘', 'Cupid', 499], ['💍', 'Ring', 999], ['💑', 'Couple', 1499]],
    luxury: [['👑', 'Crown', 1999], ['🏎️', 'Car', 2999], ['🛥️', 'Yacht', 4999], ['✈️', 'Jet', 6999], ['🏰', 'Castle', 9999], ['💎', 'Diamond', 3999], ['🚀', 'Rocket', 7999], ['🌌', 'Galaxy', 19999]],
  },
  PACKS: [[600, 49], [1500, 99], [3500, 199]],

  /** Adds gift, top-up and end-confirm sheets to the page. ctx: { getBalance, onGift(gift,qty,total), onTopUp(coins), onEnd, summary() } */
  mount(ctx) {
    const phone = document.querySelector('.phone');
    phone.insertAdjacentHTML('beforeend', `
      <div id="giftSheet" class="sheet-wrap"><div class="sheet c-sheet rounded-t-[28px] p-5 pb-7">
        <div class="w-10 h-1 rounded-full bg-white/15 mx-auto mb-4"></div>
        <div class="flex items-center justify-between mb-4"><div class="flex gap-5" id="gTabs">
          <button class="c-tab on" data-c="popular">Popular</button><button class="c-tab" data-c="love">Love</button><button class="c-tab" data-c="luxury">Luxury</button></div>
          <span class="c-pill"><span class="coin"></span><span id="gBal"></span></span></div>
        <div id="gGrid" class="grid grid-cols-4 gap-2"></div>
        <div class="flex items-center gap-2 mt-4"><div class="flex gap-1.5" id="gQty">
          <button class="c-tile on w-11 h-10 text-xs font-semibold" data-q="1">×1</button><button class="c-tile w-11 h-10 text-xs font-semibold" data-q="5">×5</button><button class="c-tile w-11 h-10 text-xs font-semibold" data-q="10">×10</button></div>
          <button id="gSend" class="c-primary flex-1 h-11 text-sm"></button></div>
        <p class="text-[10px] text-gray-500 text-center mt-3 tracking-wide">Gift animations are uploaded from the admin panel</p>
      </div></div>
      <div id="topSheet" class="sheet-wrap"><div class="sheet c-sheet rounded-t-[28px] p-5 pb-8">
        <div class="w-10 h-1 rounded-full bg-white/15 mx-auto mb-4"></div>
        <h3 class="text-xl">Quick top-up</h3><p class="text-xs text-gray-400 mt-1">Your call keeps going while you pay</p>
        <div id="pGrid" class="grid grid-cols-3 gap-2 mt-4"></div>
        <button id="pPay" class="c-primary w-full h-12 mt-5 text-sm"></button>
        <p class="text-[10px] text-gray-500 text-center mt-2 tracking-wide">Prices from the server · Razorpay (UPI / card)</p>
      </div></div>
      <div id="endSheet" class="sheet-wrap"><div class="sheet c-sheet rounded-t-[28px] p-6 pb-8 text-center">
        <div class="w-10 h-1 rounded-full bg-white/15 mx-auto mb-5"></div>
        <h3 class="text-xl">End this call?</h3><p id="endSum" class="text-xs text-gray-400 mt-2 tracking-wider uppercase"></p>
        <div class="flex gap-3 mt-6"><button data-close class="c-ghost flex-1 h-12 text-sm">Keep talking</button>
          <button id="endYes" class="flex-1 h-12 rounded-full text-sm font-bold" style="background:#ff3b4e">End call</button></div>
      </div></div>`);

    const $ = s => document.getElementById(s);
    let cat = 'popular', pick = 0, qty = 1, pp = 1;
    const draw = () => {
      $('gBal').textContent = ctx.getBalance().toLocaleString('en-IN');
      $('gGrid').innerHTML = this.GIFTS[cat].map(([e, n, c], i) =>
        `<button data-i="${i}" class="c-tile ${i === pick ? 'on' : ''} py-2.5"><div class="text-3xl">${e}</div><div class="text-[10px] mt-1 text-gray-300 truncate px-1">${n}</div><div class="text-[10px] text-cyberGold flex items-center justify-center gap-1"><span class="coin w-3 h-3"></span>${c}</div></button>`).join('');
      const [, n, c] = this.GIFTS[cat][pick];
      $('gSend').textContent = `Send ${qty > 1 ? qty + '× ' : ''}${n} · ${(c * qty).toLocaleString('en-IN')}`;
      $('pGrid').innerHTML = this.PACKS.map(([c, p], i) => `<button data-p="${i}" class="c-tile ${i === pp ? 'on' : ''} py-3"><span class="coin w-7 h-7 mx-auto block"></span><div class="text-cyberGold font-semibold mt-1">${c.toLocaleString('en-IN')}</div><div class="text-xs text-gray-400">₹${p}</div></button>`).join('');
      $('pPay').textContent = `Pay ₹${this.PACKS[pp][1]}`;
    };
    $('gGrid').onclick = e => { const b = e.target.closest('[data-i]'); if (b) { pick = +b.dataset.i; draw(); } };
    $('gTabs').onclick = e => { const b = e.target.closest('[data-c]'); if (!b) return; cat = b.dataset.c; pick = 0; document.querySelectorAll('#gTabs .c-tab').forEach(x => x.classList.toggle('on', x === b)); draw(); };
    $('gQty').onclick = e => { const b = e.target.closest('[data-q]'); if (!b) return; qty = +b.dataset.q; document.querySelectorAll('#gQty .c-tile').forEach(x => x.classList.toggle('on', x === b)); draw(); };
    $('pGrid').onclick = e => { const b = e.target.closest('[data-p]'); if (b) { pp = +b.dataset.p; draw(); } };
    $('gSend').onclick = () => {
      const g = this.GIFTS[cat][pick], total = g[2] * qty;
      this.close();
      if (total > ctx.getBalance()) { vibe.toast('Not enough coins'); return setTimeout(() => this.open('topSheet'), 350); }
      ctx.onGift(g, qty, total); this.burst(g[0], g[1], qty);
    };
    $('pPay').onclick = () => { this.close(); ctx.onTopUp(this.PACKS[pp][0]); };
    $('endYes').onclick = () => { this.close(); ctx.onEnd(); };
    document.querySelectorAll('.sheet-wrap').forEach(w => w.addEventListener('click', e => { if (e.target === w || e.target.closest('[data-close]')) this.close(); }));
    this._draw = draw; this._ctx = ctx; draw();
  },
  open(id) { this._draw(); if (id === 'endSheet') document.getElementById('endSum').textContent = this._ctx.summary(); document.getElementById(id).classList.add('open'); },
  close() { document.querySelectorAll('.sheet-wrap.open').forEach(s => s.classList.remove('open')); },
  burst(emoji, name, qty) {
    const phone = document.querySelector('.phone');
    const b = document.createElement('div');
    b.style.cssText = 'position:absolute;left:50%;top:40%;transform:translate(-50%,-50%);z-index:70;text-align:center;pointer-events:none';
    b.innerHTML = `<div style="font-size:110px;animation:gift-pop 1.8s cubic-bezier(.2,1.4,.4,1) forwards;filter:drop-shadow(0 0 26px rgba(255,42,133,.7))">${emoji}</div>
      <div style="font-weight:300;letter-spacing:.12em;text-transform:uppercase;font-size:13px;animation:fade 1.8s forwards">${name}${qty > 1 ? ' ×' + qty : ''}</div>`;
    phone.appendChild(b); setTimeout(() => b.remove(), 1900);
  },
};
document.head.insertAdjacentHTML('beforeend', `<style>
  @keyframes gift-pop { 0% { transform: scale(.2) rotate(-20deg); opacity: 0 } 25% { transform: scale(1.12) rotate(6deg); opacity: 1 } 40% { transform: scale(1) } 80% { opacity: 1 } 100% { transform: scale(1.3) translateY(-50px); opacity: 0 } }
  @keyframes fade { 0%,10% { opacity: 0 } 25%,80% { opacity: 1 } 100% { opacity: 0 } }</style>`);
