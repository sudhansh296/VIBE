// DEMO DATA ONLY: for UI review. In the real app all of this comes from the backend API
// (rates, balance and status are decided by the server, never by the app).
window.DEMO = {
  balance: 2850,
  hosts: [
    { id: 1, n: 'Priya', a: 24, g: 'F', c: 'Mumbai', r: 60, s: 'ONLINE', lv: 12, rt: 4.8, rc: 1240, f: '12.4k', calls: 3210, ans: 96, v: true, langs: ['Hindi', 'English'], bio: 'Music lover, chai addict ☕ Let\'s talk about movies, travel and life 💫', grad: ['#ff2a85', '#7a1fd1'] },
    { id: 2, n: 'Arjun', a: 26, g: 'M', c: 'Delhi', r: 50, s: 'ONLINE', lv: 8, rt: 4.6, rc: 640, f: '5.1k', calls: 1480, ans: 91, v: true, langs: ['Hindi', 'Punjabi'], bio: 'Cricket, gym and good conversations 🏏', grad: ['#00f2fe', '#1e3a8a'] },
    { id: 3, n: 'Ananya', a: 22, g: 'F', c: 'Bangalore', r: 80, s: 'ONLINE', lv: 15, rt: 4.9, rc: 2100, f: '21k', calls: 5120, ans: 98, vip: true, langs: ['English', 'Kannada'], bio: 'Dancer 💃 Ask me about K-dramas', grad: ['#a855f7', '#581c87'] },
    { id: 4, n: 'Rahul', a: 27, g: 'M', c: 'Pune', r: 40, s: 'BUSY', lv: 5, rt: 4.4, rc: 210, f: '1.9k', calls: 520, ans: 84, langs: ['Marathi', 'Hindi'], bio: 'Software guy, guitar at night 🎸', grad: ['#f59e0b', '#9a3412'] },
    { id: 5, n: 'Diya', a: 23, g: 'F', c: 'Chennai', r: 60, s: 'ONLINE', lv: 3, rt: 4.7, rc: 88, f: '980', calls: 140, ans: 93, isNew: true, langs: ['Tamil', 'English'], bio: 'New here 👋 Foodie and bookworm', grad: ['#10b981', '#065f46'] },
    { id: 6, n: 'Kabir', a: 25, g: 'M', c: 'Jaipur', r: 70, s: 'ONLINE', lv: 10, rt: 4.8, rc: 930, f: '8.7k', calls: 2400, ans: 95, v: true, langs: ['Hindi', 'English'], bio: 'Traveller ✈️ 18 states and counting', grad: ['#38bdf8', '#1e40af'] },
    { id: 7, n: 'Meera', a: 28, g: 'F', c: 'Kolkata', r: 100, s: 'OFFLINE', lv: 20, rt: 4.9, rc: 3400, f: '40k', calls: 9100, ans: 97, vip: true, langs: ['Bengali', 'Hindi', 'English'], bio: 'Singer 🎤 Rabindra sangeet to Bollywood', grad: ['#f43f5e', '#86198f'] },
    { id: 8, n: 'Vikram', a: 29, g: 'M', c: 'Hyderabad', r: 60, s: 'ONLINE', lv: 7, rt: 4.5, rc: 410, f: '3.3k', calls: 990, ans: 89, langs: ['Telugu', 'Hindi'], bio: 'Chef by passion 🍳 Biryani debates welcome', grad: ['#6366f1', '#4c1d95'] },
  ],
};
// audio calls cost less than video (host sets both rates; server enforces them)
window.DEMO.hosts.forEach(h => (h.ar = Math.max(20, Math.round(h.r / 2 / 10) * 10)));
// which call types each host accepts (host setting): Meera = video only, Vikram = voice only
window.DEMO.hosts.forEach(h => { h.voice = h.id !== 7; h.video = h.id !== 8; });
// paid chat: each host sets their own price per message after the free ones (server enforces)
window.DEMO.freeMessages = 5;
window.DEMO.hosts.forEach(h => (h.mp = { 3: 300, 7: 500, 2: 100 }[h.id] || 200));
window.DEMO.status = {
  ONLINE: { dot: '#3ee08f', text: '#3ee08f', pulse: true },
  BUSY: { dot: '#fb923c', text: '#fdba74' },
  OFFLINE: { dot: '#6b7280', text: '#9ca3af' },
};
window.DEMO.avatar = (h, cls = '') =>
  `<div class="${cls} flex items-center justify-center font-black text-white/90" style="background:linear-gradient(135deg,${h.grad[0]},${h.grad[1]})">${h.n[0]}</div>`;
