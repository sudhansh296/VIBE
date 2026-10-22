// Icon set for the call-me edition: clean rounded outline glyphs, 2px stroke, drawn in code (no image files).
// Usage: IC('home', 'w-6 h-6')  ->  <svg> markup with currentColor.
window.IC = (name, cls = 'ico') => {
  const I = {
    home: '<path d="M3.5 10.8 12 3.6l8.5 7.2"/><path d="M5.5 9.6V19a1.5 1.5 0 0 0 1.5 1.5h3.2v-5.2h3.6v5.2H17a1.5 1.5 0 0 0 1.5-1.5V9.6"/>',
    bolt: '<path d="M13 2.8 5 13.2h6.2L10.4 21.2 19 10.4h-6.3Z"/>',
    chat: '<path d="M5.5 4.5h13A2.5 2.5 0 0 1 21 7v8.2a2.5 2.5 0 0 1-2.5 2.5H11l-4.6 3.3v-3.3H5.5A2.5 2.5 0 0 1 3 15.2V7a2.5 2.5 0 0 1 2.5-2.5Z"/><path d="M8 9.3h8M8 12.6h5"/>',
    wallet: '<rect x="3" y="5.5" width="18" height="14.5" rx="3.2"/><path d="M3 10h18"/><circle cx="16.6" cy="14.8" r="1.1" fill="currentColor"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4.2 20.5c.6-3.6 3.7-5.5 7.8-5.5s7.2 1.9 7.8 5.5"/>',
    video: '<rect x="2.5" y="6" width="13" height="12" rx="3.2" fill="currentColor"/><path fill="currentColor" d="m15.5 10.3 4.9-2.7a.7.7 0 0 1 1.1.6v7.6a.7.7 0 0 1-1.1.6l-4.9-2.7Z"/>',
    heart: '<path d="M12 20.2S3.8 15.2 3.8 9.3A4.6 4.6 0 0 1 12 6.8a4.6 4.6 0 0 1 8.2 2.5c0 5.9-8.2 10.9-8.2 10.9Z"/>',
    bell: '<path d="M6.2 16.2V11a5.8 5.8 0 0 1 11.6 0v5.2l1.7 1.8H4.5Z"/><path d="M10 20.6h4"/>',
    search: '<circle cx="10.6" cy="10.6" r="6.4"/><path d="m20 20-4.9-4.9"/>',
    filter: '<path d="M4 7h9M18 7h2M4 17h2M11 17h9"/><circle cx="15.5" cy="7" r="2.3"/><circle cx="8.5" cy="17" r="2.3"/>',
    star: '<path d="m12 3.2 2.7 5.5 6 .9-4.4 4.2 1 6-5.3-2.8-5.3 2.8 1-6L3.3 9.6l6-.9Z"/>',
    lock: '<rect x="4.8" y="10.8" width="14.4" height="10" rx="3"/><path d="M8 10.8V8.2a4 4 0 0 1 8 0v2.6"/>',
    shield: '<path d="M12 3 5 5.8v6c0 4.4 3 7.5 7 9.2 4-1.7 7-4.8 7-9.2v-6Z"/><path d="m9 12 2.3 2.3L15.5 10"/>',
    check: '<path d="m5 12.6 4.4 4.4L19 7.6"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    close: '<path d="m6 6 12 12M18 6 6 18"/>',
    back: '<path d="m14.5 5.5-6.5 6.5 6.5 6.5"/>',
    chev: '<path d="m9.5 5.5 6.5 6.5-6.5 6.5"/>',
    end: '<path d="M2.8 14.4C6.6 10.6 17.4 10.6 21.2 14.4a1.5 1.5 0 0 1 0 2.1l-1.2 1.2a1.4 1.4 0 0 1-1.7.2l-2.2-1.3a1.4 1.4 0 0 1-.7-1.2v-1.4a9.3 9.3 0 0 0-4.8 0v1.4a1.4 1.4 0 0 1-.7 1.2l-2.2 1.3a1.4 1.4 0 0 1-1.7-.2L2.8 16.5a1.5 1.5 0 0 1 0-2.1Z"/>',
    mic: '<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5.5 11.2a6.5 6.5 0 0 0 13 0M12 17.7v3.3"/>',
    micoff: '<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5.5 11.2a6.5 6.5 0 0 0 13 0M12 17.7v3.3M4 4l16 16"/>',
    flip: '<path d="M4 9a8.2 8.2 0 0 1 14.3-2.3M20 4.8v3.4h-3.4"/><path d="M20 15a8.2 8.2 0 0 1-14.3 2.3M4 19.2v-3.4h3.4"/>',
    gift: '<rect x="3.5" y="8" width="17" height="4" rx="1.2"/><path d="M5 12v7a1.5 1.5 0 0 0 1.5 1.5h11A1.5 1.5 0 0 0 19 19v-7M12 8v12.5"/><path d="M12 8C10.6 4.4 6.6 4.4 7 6.6c.3 1.6 3 1.4 5 1.4Zm0 0c1.4-3.6 5.4-3.6 5-1.4-.3 1.6-3 1.4-5 1.4Z"/>',
    send: '<path d="M21 3.5 3.5 10.2l6.6 3 3 6.6Z"/><path d="m10.1 13.2 4.6-4.6"/>',
    pin: '<path d="M12 21s-6.8-5.6-6.8-11a6.8 6.8 0 0 1 13.6 0c0 5.4-6.8 11-6.8 11Z"/><circle cx="12" cy="10" r="2.5"/>',
    crown: '<path d="m3.5 8 4.3 4 4.2-6.5 4.2 6.5 4.3-4-1.5 10.5h-14Z"/>',
    gear: '<path d="M4 7h9M18 7h2M4 17h2M11 17h9"/><circle cx="15.5" cy="7" r="2.3"/><circle cx="8.5" cy="17" r="2.3"/>',
    cam: '<path d="M8.6 4.5h6.8l1.4 2H20a1.8 1.8 0 0 1 1.8 1.8v9.9A1.8 1.8 0 0 1 20 20H4a1.8 1.8 0 0 1-1.8-1.8V8.3A1.8 1.8 0 0 1 4 6.5h3.2Z"/><circle cx="12" cy="13" r="3.6"/>',
    clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5.3l3.4 2"/>',
  };
  return `<svg class="${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${I[name] || I.star}</svg>`;
};
