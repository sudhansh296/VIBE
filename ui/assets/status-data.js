// DEMO DATA ONLY for the Status feature (24-hour stories). In the real app the server returns who has an active status,
// the media, the caption, who may see it (Everyone / Followers only), views and replies; statuses expire after 24 hours
// and are moderated from the admin panel.
window.STATUS = {
  items: [
    { id: 1, n: 'Priya', grad: ['#ff2a85', '#7a1fd1'], online: true, slides: [
      { bg: ['#ff9a8b', '#ff2a85', '#3b0a2e'], text: 'Golden hour in Mumbai', ago: '2h ago' },
      { bg: ['#a18cd1', '#7a1fd1', '#1a0b2e'], text: 'Chai + old songs tonight. Who is joining?', ago: '1h ago' } ] },
    { id: 3, n: 'Ananya', grad: ['#a855f7', '#581c87'], online: true, slides: [
      { bg: ['#f6d365', '#fda085', '#4a1d12'], text: 'New dance reel coming soon', ago: '3h ago' } ] },
    { id: 6, n: 'Kabir', grad: ['#38bdf8', '#1e40af'], online: true, slides: [
      { bg: ['#84fab0', '#0ea5a0', '#06242a'], text: 'Jaipur sunsets never get old', ago: '5h ago' } ] },
    { id: 2, n: 'Arjun', grad: ['#00f2fe', '#1e3a8a'], online: true, slides: [
      { bg: ['#4facfe', '#1e3a8a', '#0a1230'], text: 'Match day vibes', ago: '9h ago' } ] },
  ],
  // hosts that are online but have no status right now
  onlineOnly: [5, 8],
  seen: () => { try { return JSON.parse(sessionStorage.getItem('vibeSeen') || '[]'); } catch (e) { return []; } },
  markSeen(id) { try { const s = this.seen(); if (!s.includes(id)) { s.push(id); sessionStorage.setItem('vibeSeen', JSON.stringify(s)); } } catch (e) {} },
  mine: () => { try { return JSON.parse(sessionStorage.getItem('vibeMine') || 'null'); } catch (e) { return null; } },
  setMine(v) { try { v ? sessionStorage.setItem('vibeMine', JSON.stringify(v)) : sessionStorage.removeItem('vibeMine'); } catch (e) {} },
};
