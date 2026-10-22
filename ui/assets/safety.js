// Shared safety helpers: block list + block confirm sheet. DEMO storage (sessionStorage). In the real app the SERVER keeps the block list
// (a blocked person can't see, message or call you) and every report goes to the admin moderation queue.
window.SAFETY = {
  KEY: 'vibeBlocked',
  seed: [4, 8],   // demo: two people are already blocked
  list() { try { const v = sessionStorage.getItem(this.KEY); if (v === null) { sessionStorage.setItem(this.KEY, JSON.stringify(this.seed)); return [...this.seed]; } return JSON.parse(v); } catch (e) { return [...this.seed]; } },
  save(a) { try { sessionStorage.setItem(this.KEY, JSON.stringify(a)); } catch (e) {} },
  has(id) { return this.list().includes(id); },
  add(id) { const a = this.list(); if (!a.includes(id)) { a.push(id); this.save(a); } },
  remove(id) { this.save(this.list().filter(x => x !== id)); },
  // "Block {name}?" bottom sheet. done() runs after the user confirms.
  confirmBlock(id, name, done) {
    const phone = document.querySelector('.phone'); const old = document.getElementById('blockSheet'); if (old) old.remove();
    const w = document.createElement('div'); w.id = 'blockSheet'; w.className = 'sheet-wrap';
    w.innerHTML = `<div class="sheet glass rounded-t-3xl p-6 border-t border-cyberLine">
      <div class="w-10 h-1 rounded-full bg-white/20 mx-auto mb-4"></div>
      <div class="w-14 h-14 rounded-full mx-auto border border-dangerRed/40 bg-dangerRed/10 text-dangerRed flex items-center justify-center"><svg class="w-7 h-7" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m5.6 5.6 12.8 12.8"/></svg></div>
      <h3 class="text-lg font-semibold text-center mt-3">Block ${name}?</h3>
      <ul class="text-sm text-gray-400 mt-3 space-y-1.5 list-disc pl-6"><li>They can't message or call you</li><li>They won't see you online or in their lists</li><li>They are not told that you blocked them</li></ul>
      <p class="text-[11px] text-gray-500 mt-3">You can unblock anyone from Settings &gt; Blocked users.</p>
      <div class="grid grid-cols-2 gap-3 mt-5"><button data-x="no" class="h-12 rounded-2xl btn-ghost font-semibold">Cancel</button>
        <button data-x="yes" class="h-12 rounded-2xl font-bold text-white" style="background:linear-gradient(135deg,#ff4d5e,#b3122f)">Block</button></div></div>`;
    phone.appendChild(w); requestAnimationFrame(() => w.classList.add('open'));
    const close = () => { w.classList.remove('open'); setTimeout(() => w.remove(), 300); };
    w.onclick = e => { if (e.target === w) return close(); const b = e.target.closest('[data-x]'); if (!b) return; close(); if (b.dataset.x === 'yes') { this.add(id); vibe.toast(`${name} blocked`); if (done) done(); } };
  },
};
