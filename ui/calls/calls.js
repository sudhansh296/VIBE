// Shared markup for the call-design options. Each option page styles the same three screens with its own CSS.
const ICON = {
  mic: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3Z"/><path d="M19 11a7 7 0 0 1-14 0M12 18v3"/></svg>',
  spk: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M11 5 6 9H3v6h3l5 4V5Z"/><path d="M15.5 8.5a5 5 0 0 1 0 7M18.5 5.5a9 9 0 0 1 0 13"/></svg>',
  cam: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M5 5.5h8.5a3 3 0 0 1 3 3v7a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3v-7a3 3 0 0 1 3-3z"/><path d="M17.5 10.2l3.4-2.3c.5-.3 1.1 0 1.1.6v6.9c0 .6-.6.9-1.1.6l-3.4-2.3z"/></svg>',
  flip: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M20 8h-3l-2-3H9L7 8H4a1 1 0 0 0-1 1v9a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1V9a1 1 0 0 0-1-1Z"/><path d="M9.5 13.5a2.5 2.5 0 0 1 4.3-1.8M14.5 13.5a2.5 2.5 0 0 1-4.3 1.8"/><path d="m14 10.5-.2 1.4-1.4-.2M10 16.5l.2-1.4 1.4.2"/></svg>',
  gift: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="8" width="18" height="4" rx="1"/><path d="M12 8v13M19 12v8a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1v-8"/><path d="M7.5 8a2.5 2.5 0 0 1 0-5C10 3 12 8 12 8s2-5 4.5-5a2.5 2.5 0 0 1 0 5"/></svg>',
  end: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 9c-3.2 0-6.1 1-8.4 2.6-.4.3-.6.7-.6 1.2v2.4c0 .7.6 1.2 1.3 1.1l3-.5c.6-.1 1-.6 1-1.2v-1.7c1.2-.4 2.4-.6 3.7-.6s2.5.2 3.7.6v1.7c0 .6.4 1.1 1 1.2l3 .5c.7.1 1.3-.4 1.3-1.1v-2.4c0-.5-.2-.9-.6-1.2C18.1 10 15.2 9 12 9Z"/></svg>',
  chev: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m6 9 6 6 6-6"/></svg>',
  lock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/></svg>',
};

function btn(key, label, extra = '') {
  return `<button class="cb ${extra}" data-k="${key}" onclick="this.classList.toggle('on')"><span class="ic">${ICON[key]}</span><span class="lb">${label}</span></button>`;
}
function endBtn(label = 'End') {
  return `<button class="cb endb"><span class="ic">${ICON.end}</span><span class="lb">${label}</span></button>`;
}

// three screens: voice call (connected), video calling (ringing, own camera behind), video call (connected)
function renderCalls(root) {
  root.innerHTML = `
  <figure><figcaption>Voice call</figcaption>
    <div class="ph voice">
      <div class="bg"></div>
      <div class="topline"><span class="min">${ICON.chev}</span><span class="enc">${ICON.lock} End-to-end encrypted</span><span class="sp"></span></div>
      <div class="who">
        <div class="avwrap"><div class="av">P</div></div>
        <div class="nm">Priya</div>
        <div class="st"><i class="live"></i>03:12</div>
        <div class="meta">30 coins / min</div>
      </div>
      <div class="ctrls">
        <div class="row">${btn('spk', 'Speaker')}${btn('cam', 'Video')}${btn('mic', 'Mute')}${btn('gift', 'Gift')}</div>
        <div class="endrow">${endBtn()}</div>
      </div>
    </div></figure>

  <figure><figcaption>Video calling (ringing)</figcaption>
    <div class="ph ring">
      <div class="selfcam"><div class="face"></div><div class="body"></div></div>
      <div class="shade"></div>
      <div class="topline"><span class="min">${ICON.chev}</span><span class="enc">${ICON.lock} End-to-end encrypted</span><span class="sp"></span></div>
      <div class="who small">
        <div class="avwrap"><div class="av">A</div></div>
        <div class="nm">Arjun</div>
        <div class="st">Ringing…</div>
        <div class="meta">50 coins / min · after pick-up</div>
      </div>
      <div class="ctrls">
        <div class="row">${btn('flip', 'Flip')}${btn('cam', 'Camera')}${btn('mic', 'Mute')}</div>
        <div class="endrow">${endBtn('Cancel')}</div>
      </div>
    </div></figure>

  <figure><figcaption>Video call</figcaption>
    <div class="ph incall">
      <div class="remote"><div class="face"></div><div class="body"></div></div>
      <div class="shade"></div>
      <div class="topbar"><div class="chip"><div class="av mini">A</div><div><b>Ananya</b><small>12:48</small></div></div><div class="coins"><i class="coin"></i>2,770</div></div>
      <div class="pip"><span>You</span></div>
      <div class="ctrls">
        <div class="row">${btn('mic', 'Mute')}${btn('cam', 'Camera')}${btn('flip', 'Flip')}${btn('gift', 'Gift')}${endBtn()}</div>
      </div>
    </div></figure>`;
}
