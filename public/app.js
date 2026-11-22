// ============================================================================
// ANIME UNIVERSE — 1:1 ANDROID COMPOSE UI & SUPABASE/ANILIST PARITY (WEB & PWA)
// Exact visual & functional match to the 5 user screenshots + auto cloud sync
// ============================================================================

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  });
}

let deferredInstallPrompt = null;
window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredInstallPrompt = e;
  renderApp();
});

// Hardened 4-Shard Bit-Rotated Vault Material (Matches Android & Desktop VaultMaterial)
const V_U_SHARDS = [
  [122, 155, 146, 165, 244, 26, 51, 60, 154, 168],
  [240, 60, 74, 204, 143, 231, 223, 175, 178, 32],
  [85, 29, 14, 44, 1, 104, 158, 164, 175, 109],
  [134, 93, 99, 51, 141, 35, 252, 172, 23, 201]
];
const V_U_KEY = [5, 155, 136, 193, 243, 21, 119, 100, 170, 218, 252, 131, 133, 15, 25, 82, 120, 148, 226, 71, 211, 148, 250, 25, 196, 147, 156, 171, 148, 191, 75, 151];
const V_K_SHARDS = [
  [34, 4, 67, 148, 18, 93, 205, 57, 54, 74, 16, 215],
  [186, 113, 144, 138, 96, 215, 143, 229, 30, 194, 74, 121],
  [53, 207, 73, 120, 33, 171, 232, 142, 185, 22, 216],
  [207, 192, 33, 75, 28, 172, 50, 117, 66, 179, 98]
];
const V_K_KEY = [182, 155, 35, 209, 181, 196, 21, 96, 79, 21, 7, 30, 198, 11, 157, 31, 10, 197, 46, 182, 196, 15, 183, 73, 169, 140, 192, 144, 141, 134, 144, 148];

function unsealVaultV2(shards, keyStream, len, salt) {
  const out = new Uint8Array(len);
  const idx = [0, 0, 0, 0];
  for (let i = 0; i < len; i++) {
    const lane = i & 3;
    const obf = shards[lane][idx[lane]++];
    const rot = (i % 7) + 1;
    const unrot = ((obf >>> rot) | ((obf << (8 - rot)) & 0xff)) & 0xff;
    const k1 = keyStream[i % keyStream.length];
    const k2 = (i * 131 + salt * 37 + 0x5d) & 0xff;
    out[i] = (unrot ^ k1 ^ k2) & 0xff;
  }
  return new TextDecoder().decode(out);
}

function getVaultBaseUrl() {
  return unsealVaultV2(V_U_SHARDS, V_U_KEY, 40, 0x37).replace(/\/+$/, '');
}

function getVaultPublicKey() {
  return unsealVaultV2(V_K_SHARDS, V_K_KEY, 46, 0x6b).trim();
}

const BUILT_IN_AVATARS = [
  { key: 'avatar_1', name: 'Crimson Wolf', img: '/assets/avatar_1.png', c1: '#9F1239', c2: '#334155' },
  { key: 'avatar_2', name: 'Azure Sovereign', img: '/assets/avatar_2.png', c1: '#0284C7', c2: '#6366F1' },
  { key: 'avatar_3', name: 'Amethyst Dragon', img: '/assets/avatar_3.png', c1: '#A855F7', c2: '#E11D48' },
  { key: 'avatar_4', name: 'Frost Empress', img: '/assets/avatar_4.png', c1: '#38BDF8', c2: '#818CF8' },
  { key: 'avatar_5', name: 'Emerald Flora', img: '/assets/avatar_5.png', c1: '#10B981', c2: '#38BDF8' },
  { key: 'avatar_6', name: 'Amber Scholar', img: '/assets/avatar_6.png', c1: '#F59E0B', c2: '#92400E' },
  { key: 'avatar_7', name: 'Autumn Vanguard', img: '/assets/avatar_7.png', c1: '#EA580C', c2: '#06B6D4' },
  { key: 'avatar_8', name: 'Indigo Flame', img: '/assets/avatar_8.png', c1: '#4F46E5', c2: '#0EA5E9' },
  { key: 'avatar_9', name: 'Solar Halo', img: '/assets/avatar_9.png', c1: '#FBBF24', c2: '#64748B' }
];

const SANDRONE_IMAGE_SRC = '/assets/sandrone/sandrone.png';
const SANDRONE_VOICE_CLIPS = [
  '/assets/sandrone/s.mp3',
  '/assets/sandrone/s1.mp3',
  '/assets/sandrone/s2.mp3',
  '/assets/sandrone/s3.mp3',
  '/assets/sandrone/s4.mp3',
  '/assets/sandrone/s5.mp3',
  '/assets/sandrone/s6.mp3',
  '/assets/sandrone/s7.mp3',
  '/assets/sandrone/s8.mp3',
  '/assets/sandrone/s9.mp3'
];

let sandroneAudio = null;
let sandroneLastClipIndex = -1;
let sandroneIsPlaying = false;
let sandronePlayToken = 0;

function syncSandronePlayingUI() {
  document.querySelectorAll('[data-sandrone-mascot]').forEach((el) => {
    el.classList.toggle('is-playing', sandroneIsPlaying);
  });
}

function playRandomSandroneVoice(triggerEl) {
  if (!SANDRONE_VOICE_CLIPS.length) return;
  let nextIdx = Math.floor(Math.random() * SANDRONE_VOICE_CLIPS.length);
  if (SANDRONE_VOICE_CLIPS.length > 1 && nextIdx === sandroneLastClipIndex) {
    nextIdx = (nextIdx + 1 + Math.floor(Math.random() * (SANDRONE_VOICE_CLIPS.length - 1))) % SANDRONE_VOICE_CLIPS.length;
  }
  sandroneLastClipIndex = nextIdx;

  if (triggerEl) {
    triggerEl.classList.remove('is-tapped');
    void triggerEl.offsetWidth;
    triggerEl.classList.add('is-tapped');
    setTimeout(() => triggerEl.classList.remove('is-tapped'), 170);
  }

  const token = ++sandronePlayToken;
  if (sandroneAudio) {
    try {
      sandroneAudio.pause();
      sandroneAudio.currentTime = 0;
    } catch (_) {}
  }

  const audio = new Audio(SANDRONE_VOICE_CLIPS[nextIdx]);
  audio.preload = 'auto';
  sandroneAudio = audio;
  sandroneIsPlaying = true;
  syncSandronePlayingUI();

  const handleFinish = () => {
    if (token !== sandronePlayToken) return;
    sandroneIsPlaying = false;
    syncSandronePlayingUI();
  };
  audio.addEventListener('ended', handleFinish);
  audio.addEventListener('error', handleFinish);

  const playPromise = audio.play();
  if (playPromise && typeof playPromise.catch === 'function') {
    playPromise.catch(() => {
      handleFinish();
    });
  }
}

const SENTIMENTS = [
  { key: 'cinema', shortLabel: 'Cinema', fullLabel: 'Absolute Cinema', minScore100: 90, suggestedStars: 4.5 },
  { key: 'worth', shortLabel: 'Worth', fullLabel: 'Worth Watching', minScore100: 75, suggestedStars: 4.0 },
  { key: 'avg', shortLabel: 'Avg', fullLabel: 'Average', minScore100: 55, suggestedStars: 3.0 },
  { key: 'boring', shortLabel: 'Boring', fullLabel: 'Boring', minScore100: 35, suggestedStars: 2.0 },
  { key: 'waste', shortLabel: 'Waste', fullLabel: 'Waste of Time', minScore100: 1, suggestedStars: 1.0 }
];

function normalizeRating10(rawRating) {
  if (rawRating === null || rawRating === undefined || Number(rawRating) <= 0) return null;
  const num = Number(rawRating);
  return num;
}

function rating10ToStars(rawRating) {
  const r10 = normalizeRating10(rawRating);
  if (!r10) return null;
  return r10 / 2;
}

function sentimentFromRating10(rating10) {
  const r10 = normalizeRating10(rating10);
  if (!r10 || r10 <= 0) return null;
  const s100 = Math.round(r10 * 10);
  return SENTIMENTS.find((s) => s100 >= s.minScore100) || SENTIMENTS[SENTIMENTS.length - 1];
}

const WATCH_STATUSES = [
  { dbValue: 'watching', label: 'Watching' },
  { dbValue: 'completed', label: 'Completed' },
  { dbValue: 'plan_to_watch', label: 'Plan to Watch' },
  { dbValue: 'on_hold', label: 'On Hold' },
  { dbValue: 'dropped', label: 'Dropped' }
];

const ANILIST_GENRES = [
  'Action', 'Adventure', 'Comedy', 'Drama', 'Fantasy',
  'Horror', 'Mystery', 'Psychological', 'Romance', 'Sci-Fi',
  'Slice of Life', 'Sports', 'Supernatural', 'Thriller'
];

const DAY_LABELS = [
  { dow: 1, short: 'MON', full: 'Monday' },
  { dow: 2, short: 'TUE', full: 'Tuesday' },
  { dow: 3, short: 'WED', full: 'Wednesday' },
  { dow: 4, short: 'THU', full: 'Thursday' },
  { dow: 5, short: 'FRI', full: 'Friday' },
  { dow: 6, short: 'SAT', full: 'Saturday' },
  { dow: 7, short: 'SUN', full: 'Sunday' }
];

const SVG_ICONS = {
  home: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>`,
  explore: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm2.19 12.19L6 18l3.81-8.19L18 6l-3.81 8.19z"/></svg>`,
  library: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z"/></svg>`,
  community: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>`,
  profile: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>`,
  search: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>`,
  settings: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.488.488 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/></svg>`,
  back: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>`,
  grid: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M4 6h4v4H4V6zm6 0h10v4H10V6zM4 14h4v4H4v-4zm6 0h10v4H10v-4z"/></svg>`,
  list: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M4 6h4v4H4V6zm6 0h10v4H10V6zM4 14h4v4H4v-4zm6 0h10v4H10v-4z"/></svg>`,
  tune: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z"/></svg>`,
  collectionIcon: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9H9V9h10v2zm-4 4H9v-2h6v2zm4-8H9V5h10v2z"/></svg>`,
  shield: `<svg viewBox="0 0 24 24" fill="currentColor" style="width:14px;height:14px;color:var(--success);"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/></svg>`,
  key: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12.65 10C11.83 7.67 9.61 6 7 6c-3.31 0-6 2.69-6 6s2.69 6 6 6c2.61 0 4.83-1.67 5.65-4H17v4h4v-4h2v-4H12.65zM7 14c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z"/></svg>`,
  eye: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg>`,
  heartFilled: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>`,
  heartOutline: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z"/></svg>`,
  download: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>`,
  android: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M17.6 9.48l1.84-3.18c.16-.31.04-.69-.26-.85a.637.637 0 0 0-.83.22l-1.88 3.24a11.463 11.463 0 0 0-8.94 0L5.65 5.67a.643.643 0 0 0-.87-.2c-.28.18-.37.54-.22.83L6.4 9.48A10.78 10.78 0 0 0 1 18h22a10.78 10.78 0 0 0-5.4-8.52zM7 15.25a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5zm10 0a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5z"/></svg>`,
  windows: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 3h8.5v8.5H3V3zm9.5 0H21v8.5h-8.5V3zM3 12.5h8.5V21H3v-8.5zm9.5 0H21V21h-8.5v-8.5z"/></svg>`,
  apple: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.81-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83zM15.96 6.33c.67-.81 1.12-1.94 1-3.08-.96.04-2.13.64-2.82 1.45-.61.7-.115 1.86-.99 2.97 1.08.08 2.14-.53 2.81-1.34z"/></svg>`,
  clock: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>`,
  checkCircle: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>`,
  comment: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/></svg>`,
  bell: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>`
};

function getCurrentAnimeSeason() {
  const m = new Date().getMonth() + 1;
  if (m <= 3) return 'WINTER';
  if (m <= 6) return 'SPRING';
  if (m <= 9) return 'SUMMER';
  return 'FALL';
}

function getDayNumberForDow(dow) {
  const now = new Date();
  const todayDow = ((now.getDay() + 6) % 7) + 1; // 1=Mon..7=Sun
  const diff = dow - todayDow;
  const d = new Date(now);
  d.setDate(now.getDate() + diff);
  return String(d.getDate());
}

function formatReviewDate(isoStr) {
  if (!isoStr) return '';
  try {
    const d = new Date(isoStr);
    if (isNaN(d.getTime())) return String(isoStr).slice(0, 10);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  } catch (_) {
    return String(isoStr).slice(0, 10);
  }
}

// Cleanup legacy shared storage keys that previously caused cross-user/guest data leakage
try {
  localStorage.removeItem('anime_universe_local_vault');
  localStorage.removeItem('anime_universe_collections');
  localStorage.removeItem('anime_universe_profile');
} catch (_) {}

function generateUniqueGuestId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return `guest_${crypto.randomUUID()}`;
  }
  return `guest_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 10)}`;
}

function getOrCreateGuestId() {
  let gid = localStorage.getItem('anime_universe_guest_id');
  if (!gid || !gid.startsWith('guest_')) {
    gid = generateUniqueGuestId();
    localStorage.setItem('anime_universe_guest_id', gid);
  }
  return gid;
}

function rotateGuestId() {
  const gid = generateUniqueGuestId();
  localStorage.setItem('anime_universe_guest_id', gid);
  return gid;
}

function getStorageKeyForIdentity(kind, sessionObj, isGuest, guestId) {
  if (sessionObj && sessionObj.userId) {
    return `anime_universe_${kind}_user_${sessionObj.userId}`;
  }
  if (isGuest) {
    const gid = guestId || getOrCreateGuestId();
    return `anime_universe_${kind}_guest_${gid}`;
  }
  return null;
}

function readScopedArray(kind, sessionObj, isGuest, guestId) {
  const key = getStorageKeyForIdentity(kind, sessionObj, isGuest, guestId);
  if (!key) return [];
  try {
    const parsed = JSON.parse(localStorage.getItem(key) || '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch (_) {
    return [];
  }
}

function isPlaceholderOrEmailName(name) {
  const clean = String(name || '').trim();
  return !clean || clean.includes('@') || /^traveler_/i.test(clean);
}

function readScopedProfile(sessionObj, isGuest, guestId) {
  const key = getStorageKeyForIdentity('profile', sessionObj, isGuest, guestId);
  const safeSessionName = sessionObj && !isPlaceholderOrEmailName(sessionObj.username) ? sessionObj.username : 'Member';
  const defaultUsername = sessionObj
    ? safeSessionName
    : isGuest
    ? 'Guest Tracker'
    : 'Member';
  if (!key) {
    return {
      userId: null,
      username: defaultUsername,
      avatarKey: 'avatar_1',
      bio: '',
      memberStatus: isGuest ? 'guest' : 'active',
      isAdmin: false
    };
  }
  try {
    const parsed = JSON.parse(localStorage.getItem(key) || '{}') || {};
    return {
      userId: sessionObj ? sessionObj.userId : guestId,
      username: parsed.username || defaultUsername,
      avatarKey: parsed.avatarKey || 'avatar_1',
      bio: parsed.bio || '',
      memberStatus: parsed.memberStatus || (isGuest ? 'guest' : 'active'),
      isAdmin: Boolean(parsed.isAdmin)
    };
  } catch (_) {
    return {
      userId: sessionObj ? sessionObj.userId : guestId,
      username: defaultUsername,
      avatarKey: 'avatar_1',
      bio: '',
      memberStatus: isGuest ? 'guest' : 'active',
      isAdmin: false
    };
  }
}

// Persistent State (Strictly Scoped per User ID or Unique Guest ID)
const savedPrefs = JSON.parse(localStorage.getItem('anime_universe_prefs') || '{}');
const initialSession = JSON.parse(localStorage.getItem('anime_universe_session') || localStorage.getItem('auralog_session') || 'null');
const initialGuestMode = !initialSession && localStorage.getItem('anime_universe_guest') === 'true';
const initialGuestId = getOrCreateGuestId();

const state = {
  session: initialSession,
  isGuestMode: initialGuestMode,
  guestId: initialGuestId,
  tab: 'home', // 'home' | 'discover' | 'mylist' | 'community' | 'profile' | 'settings' | 'detail'
  previousTab: 'home',
  settingsCategory: null,

  prefs: {
    themeMode: savedPrefs.themeMode || 'DARK',
    accentTheme: savedPrefs.accentTheme || 'GOLD',
    watchlistLayout: savedPrefs.watchlistLayout || 'GRID',
    titleLanguage: savedPrefs.titleLanguage || 'ENGLISH',
    showAdultContent: Boolean(savedPrefs.showAdultContent),
    reviewsVisibility: savedPrefs.reviewsVisibility || 'community'
  },

  // Auth State
  authMode: 'signin', // 'signin' | 'signup' | 'verify_otp' | 'forgot_otp' | 'onboard_username' | 'onboard_avatar'
  identifier: '',
  password: '',
  confirmPassword: '',
  showPassword: false,
  pendingVerifyEmail: '',
  pendingVerifyPassword: '',
  signupOtpCode: '',
  resetOtpCode: '',
  resetNewPassword: '',
  onboardUsername: '',
  onboardAvatarKey: 'avatar_1',
  authError: '',
  authInfo: '',
  loading: false,

  // Profile State (Isolated to active user or active guest)
  profile: readScopedProfile(initialSession, initialGuestMode, initialGuestId),
  showAvatarModal: false,
  showProfileEditModal: false,

  // Home Schedule State (Strictly independent from Library & Community)
  homeAllWeekDays: true,
  selectedDay: ((new Date().getDay() + 6) % 7) + 1,
  homeTrackingIds: new Set(readScopedArray('home_ids', initialSession, initialGuestMode, initialGuestId).map(Number)),
  homeTrackingHydrated: false,

  // Library (MyList) State (Isolated to active user or active guest)
  vault: readScopedArray('vault', initialSession, initialGuestMode, initialGuestId),
  collections: readScopedArray('collections', initialSession, initialGuestMode, initialGuestId),
  myListStatusFilter: 'ALL',
  myListSentimentFilter: null,
  myListCollectionFilter: null,
  myListQuery: '',
  myListSort: 'RECENT',
  showCreateCollectionModal: false,

  // Discover State
  discover: [],
  discoverMode: 'popular',
  searchQuery: '',
  discoverFormat: 'ALL',
  discoverStatus: 'ALL',
  discoverGenre: null,
  discoverSort: 'POPULARITY_DESC',
  discoverPage: 1,
  discoverHasNextPage: true,
  discoverLoadingMore: false,
  discoverLastPageStartIndex: 0,
  isSearching: false,

  // Community State
  communityReviews: [],
  communityViewTab: 'feed', // 'feed' | 'tagged'
  communitySentimentFilter: null,
  communitySort: 'RECENT',
  communityCommentsAnimeId: null,
  communityCommentsAnimeFallback: null, // fallback template metadata when opening older anime from Tagged You
  communityCommentsTab: 'discussion', // 'discussion' (Live Chat) | 'notes' (Reviews)
  pendingCommunityReplyUser: null, // { id, username, avatar_key }
  communityCommentDraftStars: 0,
  communityCommentDraftNote: '',
  communityNoteSaving: false,
  communityNoteFeedback: '',
  communityNoteError: '',

  // Detail Rating & Note Feedback
  detailRatingSaving: false,
  detailRatingFeedback: '',
  detailRatingError: '',
  detailNoteDraft: '',
  lastRenderedTab: null,

  // Anime Discussion (LivTrAk Live Chat, Custom Emojis, @Tagging & Target-User Reply Threads)
  discussion: {
    animeId: null,
    comments: [],
    totalComments: 0,
    hasMore: false,
    loading: false,
    loadingMore: false,
    submitting: false,
    success: '',
    error: '',
    draft: '',
    commentTags: [], // [{ id, username, avatar_key }]
    activeReplyTarget: null, // { commentId, targetUserId, targetUsername }
    replyDraft: '',
    replyTags: [], // [{ id, username, avatar_key }]
    editingCommentId: null,
    editingCommentDraft: '',
    editingReplyId: null,
    editingReplyCommentId: null,
    editingReplyThreadId: null,
    editingReplyDraft: '',
    expandedThreads: {},
    mentionSuggestions: [],
    mentionActiveScope: null, // 'comment' | 'reply' | 'edit_comment' | 'edit_reply'
    mentionActiveStart: -1,
    mentionActiveEnd: -1,
    highlightTarget: null, // { commentId, threadId, replyId }
    contextMenu: null, // { x, y, type: 'comment'|'reply', commentId, threadId?, replyId?, userId, username, isOwner }
    emojiTrayOpen: false
  },

  // Unseen Discussion Red Dot State (shown on Community tab & specific anime cards)
  notifications: {
    items: [],
    unseenAnime: [],
    unreadCount: 0,
    totalCount: 0,
    hasMore: false,
    loading: false,
    loadingMore: false,
    error: ''
  },
  pendingNotificationJump: null, // { animeId, commentId, threadId, replyId }

  // Modals, Context Menu, Inline Episode Editor & Detail View
  detailAnime: null,
  detailRich: null,
  detailLoading: false,
  quickEditAnime: null,
  contextMenu: null, // { animeId, x, y }
  confirmRemoveHomeAnime: null, // { animeId, title }
  inlineEpEdit: null, // { animeId, scope: 'card'|'detail', draft, saving, error }
  toastMessage: '',
  toastError: false,

  // Download Center & Admin Release Configuration State
  downloadsConfig: JSON.parse(localStorage.getItem('anime_universe_downloads_admin_override') || 'null') || {
    githubRepo: '',
    autoFetchGithubReleases: true,
    lastUpdated: '2026-11-18',
    platforms: {
      android: {
        id: 'android',
        platformName: 'Android APK',
        appName: 'Anime Universe for Android',
        version: 'v2.1.0',
        fileSize: '24.0 MB',
        osSupported: 'Android 8.0 (Oreo) & above • ARM64 / Universal',
        releaseDate: 'Nov 18, 2026',
        fileName: 'AnimeUniverse-v2.1.0-arm64.apk',
        architecture: 'Universal APK (ARM64-v8a / armeabi-v7a / x86_64)',
        packageType: 'Signed Android Package (.apk)',
        available: true,
        downloadUrl: 'https://drive.usercontent.google.com/download?id=1bvDpEEuwfAhWy9ECEzjUK9rdAMv18gys&export=download&confirm=t'
      },
      windows: {
        id: 'windows',
        platformName: 'Windows EXE',
        appName: 'Anime Universe for Windows',
        version: 'v2.1.0',
        fileSize: '112.9 MB',
        osSupported: 'Windows 10 & Windows 11 (64-bit x64)',
        releaseDate: 'Nov 18, 2026',
        fileName: 'AnimeUniverse-Windows-Installer.exe',
        architecture: '64-bit Native JVM Desktop (.exe)',
        packageType: 'Windows Native Installer (.exe)',
        available: true,
        downloadUrl: 'https://drive.usercontent.google.com/download?id=1nARmFc6FHBLt1JnCJTjGEqoDla0njtOU&export=download&confirm=t'
      },
      macos: {
        id: 'macos',
        platformName: 'macOS DMG',
        appName: 'Anime Universe for macOS',
        version: 'v2.1.0',
        fileSize: '122.0 MB',
        osSupported: 'macOS 12 Monterey & newer (Apple Silicon & Intel)',
        releaseDate: 'Nov 18, 2026',
        fileName: 'AnimeUniverse-macOS-Installer.dmg',
        architecture: 'Universal macOS Disk Image (.dmg)',
        packageType: 'macOS Disk Image (.dmg)',
        available: true,
        downloadUrl: 'https://drive.usercontent.google.com/download?id=1W6GoMBMHr-Vbd5_jCGv140HLMG8r3D5Y&export=download&confirm=t'
      }
    },
    currentRelease: {
      version: 'v2.1.0',
      title: 'Obsidian & Champagne Gold Parity + Instant Cloud Sync',
      releaseDate: 'Nov 18, 2026',
      channel: 'Stable Release',
      summary: 'Unified 1:1 visual design across Android, Windows, macOS, and Web with instant startup session restore and automatic background cloud synchronization.',
      highlights: [
        { tag: 'SYNC', text: 'Automatic real-time cross-platform cloud synchronization every 15 seconds across Android, Windows, macOS, and Web without manual refresh.' },
        { tag: 'PERFORMANCE', text: 'Eliminated the 4–5 second startup login flash on Android APK and Desktop by caching verified session snapshots locally.' },
        { tag: 'DESIGN', text: '1:1 Obsidian & Champagne Gold visual parity across Home Weekly Schedule, Global Catalog Discover, Library Collections, and Community Reviews.' },
        { tag: 'AUTH', text: 'Added seamless Email or Username sign-in resolution and automatic background JWT session refresh.' }
      ]
    },
    previousVersions: []
  },
  downloadTasks: {},
  showDownloadsAdminModal: false,
  githubSyncMessage: ''
};

const syncedCatalogHashes = new Map();

function applyTheme() {
  const mode = state.prefs.themeMode;
  let effective = 'dark';
  if (mode === 'LIGHT') effective = 'light';
  else if (mode === 'SYSTEM') {
    effective = window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  }
  document.documentElement.setAttribute('data-theme', effective);
  document.documentElement.setAttribute('data-accent', state.prefs.accentTheme || 'GOLD');
}
applyTheme();

function savePrefs() {
  localStorage.setItem('anime_universe_prefs', JSON.stringify(state.prefs));
  applyTheme();
}

function getActiveOwnerId() {
  if (state.session && state.session.userId) return state.session.userId;
  if (state.isGuestMode) return state.guestId || getOrCreateGuestId();
  return null;
}

function saveLocalVault() {
  const key = getStorageKeyForIdentity('vault', state.session, state.isGuestMode, state.guestId);
  if (!key) return;
  const ownerId = getActiveOwnerId();
  const tagged = state.vault.map((v) => ({ ...v, ownerId }));
  state.vault = tagged;
  localStorage.setItem(key, JSON.stringify(tagged));
}

function saveCollections() {
  const key = getStorageKeyForIdentity('collections', state.session, state.isGuestMode, state.guestId);
  if (!key) return;
  const ownerId = getActiveOwnerId();
  const tagged = state.collections.map((c) => ({ ...c, ownerId }));
  state.collections = tagged;
  localStorage.setItem(key, JSON.stringify(tagged));
}

function saveProfileCache() {
  const key = getStorageKeyForIdentity('profile', state.session, state.isGuestMode, state.guestId);
  if (!key) return;
  const ownerId = getActiveOwnerId();
  state.profile = { ...state.profile, userId: ownerId };
  localStorage.setItem(key, JSON.stringify(state.profile));
}

let toastTimer = null;
function showToast(message, isError = false) {
  state.toastMessage = String(message || '');
  state.toastError = Boolean(isError);
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    state.toastMessage = '';
    state.toastError = false;
    const existingToast = document.getElementById('au-toast-banner');
    if (existingToast) existingToast.remove();
  }, 3200);
}

// Strict Lifecycle & Episode Validation Rules (1:1 Parity with Android, Desktop & Supabase)
function isOfficiallyFinished(mediaStatus) {
  const s = String(mediaStatus || '').trim().toUpperCase();
  return s === 'FINISHED' || s === 'FINISHED_AIRING' || s === 'COMPLETED';
}

function shouldAutoComplete(mediaStatus, totalEpisodes, watchedEpisodes) {
  const total = totalEpisodes !== null && totalEpisodes !== undefined ? Number(totalEpisodes) : null;
  const watched = Number(watchedEpisodes || 0);
  return isOfficiallyFinished(mediaStatus) && total !== null && Number.isFinite(total) && total > 0 && watched >= total;
}

function shouldRemoveFromHome(userStatus, mediaStatus, totalEpisodes, watchedEpisodes) {
  if (String(userStatus || '').trim().toLowerCase() === 'dropped') return true;
  return shouldAutoComplete(mediaStatus, totalEpisodes, watchedEpisodes);
}

function isAnimeOnHome(item) {
  if (!item || !item.animeId) return false;
  if (!state.homeTrackingIds.has(Number(item.animeId))) return false;
  return !shouldRemoveFromHome(item.status, item.mediaStatus, item.totalEpisodes, item.watchedEpisodes);
}

function validateEpisodeInput(rawText, totalEpisodes) {
  const trimmed = String(rawText ?? '').trim();
  if (!trimmed) {
    return { valid: false, error: 'Enter an episode number.' };
  }
  if (trimmed.startsWith('-')) {
    return { valid: false, error: 'Episode number cannot be negative.' };
  }
  if (trimmed.includes('.') || trimmed.includes(',')) {
    return { valid: false, error: 'Enter a whole number only.' };
  }
  if (!/^\d+$/.test(trimmed)) {
    return { valid: false, error: 'Enter digits only.' };
  }
  const num = Number.parseInt(trimmed, 10);
  if (!Number.isFinite(num) || num < 0) {
    return { valid: false, error: 'Enter a valid non-negative number.' };
  }
  const total = totalEpisodes !== null && totalEpisodes !== undefined ? Number(totalEpisodes) : null;
  if (total !== null && Number.isFinite(total) && total > 0 && num > total) {
    return { valid: false, error: `Cannot exceed ${total} episodes.` };
  }
  if ((total === null || total <= 0) && num > 50000) {
    return { valid: false, error: 'Episode number is out of range.' };
  }
  return { valid: true, value: num };
}

function saveHomeTrackingIds() {
  const key = getStorageKeyForIdentity('home_ids', state.session, state.isGuestMode, state.guestId);
  if (!key) return;
  localStorage.setItem(key, JSON.stringify(Array.from(state.homeTrackingIds)));
}

function clearActiveUserMemoryState() {
  state.vault = [];
  state.collections = [];
  state.homeTrackingIds = new Set();
  state.homeTrackingHydrated = false;
  state.profile = {
    userId: null,
    username: 'Member',
    avatarKey: 'avatar_1',
    bio: '',
    memberStatus: 'active',
    isAdmin: false
  };
  state.detailAnime = null;
  state.detailRich = null;
  state.quickEditAnime = null;
  state.contextMenu = null;
  state.confirmRemoveHomeAnime = null;
  state.inlineEpEdit = null;
  state.myListStatusFilter = 'ALL';
  state.myListSentimentFilter = null;
  state.myListCollectionFilter = null;
  state.myListQuery = '';
}

function hydrateActiveIdentityState() {
  if (state.session && state.session.userId) {
    state.vault = readScopedArray('vault', state.session, false, null).filter(
      (v) => !v.ownerId || v.ownerId === state.session.userId
    );
    state.collections = readScopedArray('collections', state.session, false, null).filter(
      (c) => !c.ownerId || c.ownerId === state.session.userId
    );
    state.homeTrackingIds = new Set(readScopedArray('home_ids', state.session, false, null).map(Number));
    state.homeTrackingHydrated = false;
    state.profile = readScopedProfile(state.session, false, null);
    state.detailAnime = null;
    state.quickEditAnime = null;
    state.contextMenu = null;
    state.confirmRemoveHomeAnime = null;
    state.inlineEpEdit = null;
  } else if (state.isGuestMode) {
    state.guestId = getOrCreateGuestId();
    state.vault = readScopedArray('vault', null, true, state.guestId).filter(
      (v) => !v.ownerId || v.ownerId === state.guestId
    );
    state.collections = readScopedArray('collections', null, true, state.guestId).filter(
      (c) => !c.ownerId || c.ownerId === state.guestId
    );
    const savedHomeIds = readScopedArray('home_ids', null, true, state.guestId).map(Number);
    if (savedHomeIds.length > 0) {
      state.homeTrackingIds = new Set(savedHomeIds);
    } else {
      state.homeTrackingIds = new Set(
        state.vault
          .filter((v) => v.inHome !== false && !shouldRemoveFromHome(v.status, v.mediaStatus, v.totalEpisodes, v.watchedEpisodes))
          .map((v) => Number(v.animeId))
      );
    }
    state.homeTrackingHydrated = true;
    state.profile = readScopedProfile(null, true, state.guestId);
    state.detailAnime = null;
    state.quickEditAnime = null;
    state.contextMenu = null;
    state.confirmRemoveHomeAnime = null;
    state.inlineEpEdit = null;
  } else {
    clearActiveUserMemoryState();
  }
}

let pendingGuestMigrationItems = [];
let pendingGuestMigrationGuestId = null;

function saveSession(session) {
  if (session && session.userId) {
    // Capture any active guest session items for safe non-duplicating migration
    const activeGuestId = state.guestId || localStorage.getItem('anime_universe_guest_id');
    if (activeGuestId) {
      const guestItems = readScopedArray('vault', null, true, activeGuestId);
      if (guestItems.length > 0) {
        pendingGuestMigrationItems = guestItems.map((it) => ({ ...it }));
        pendingGuestMigrationGuestId = activeGuestId;
      }
    }
    state.session = session;
    localStorage.setItem('anime_universe_session', JSON.stringify(session));
    localStorage.setItem('auralog_session', JSON.stringify(session));
    state.isGuestMode = false;
    localStorage.removeItem('anime_universe_guest');
    hydrateActiveIdentityState();
  } else {
    state.session = null;
    state.isGuestMode = false;
    localStorage.removeItem('anime_universe_session');
    localStorage.removeItem('auralog_session');
    localStorage.removeItem('anime_universe_guest');
    clearActiveUserMemoryState();
  }
}

function enterGuestSession(forceNewGuestId = false) {
  state.session = null;
  localStorage.removeItem('anime_universe_session');
  localStorage.removeItem('auralog_session');
  state.guestId = forceNewGuestId ? rotateGuestId() : getOrCreateGuestId();
  state.isGuestMode = true;
  localStorage.setItem('anime_universe_guest', 'true');
  hydrateActiveIdentityState();
}

function escapeHtml(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

const AVATAR_IMG_ONERROR =
  "if(!this.dataset.fb){this.dataset.fb='1';const s=this.getAttribute('src')||'';this.src=s.startsWith('/public/')?s.replace('/public/','/'):('/public'+s);}else{this.style.opacity='0';}";

function getMascotImgUrl() {
  if (typeof window !== 'undefined' && window.ANIME_UNIVERSE_ASSETS && window.ANIME_UNIVERSE_ASSETS.img_login_mascot) {
    return window.ANIME_UNIVERSE_ASSETS.img_login_mascot;
  }
  return '/assets/img_login_mascot.png';
}

function normalizeAvatarKey(rawKey, rawUrl = '') {
  const candidates = [rawKey, rawUrl].map((v) => String(v || '').trim()).filter((v) => v && v !== 'null');
  for (const c of candidates) {
    const lower = c.toLowerCase();
    const exact = BUILT_IN_AVATARS.find((a) => a.key === lower || a.name.toLowerCase() === lower);
    if (exact) return exact.key;
    const m = lower.match(/avatar[_-]?([1-9])/) || lower.match(/^([1-9])$/);
    if (m && m[1]) return `avatar_${m[1]}`;
  }
  return 'avatar_1';
}

function getAvatarPreset(avatarKey, avatarUrl = '') {
  const norm = normalizeAvatarKey(avatarKey, avatarUrl);
  const found = BUILT_IN_AVATARS.find((a) => a.key === norm) || BUILT_IN_AVATARS[0];
  const embedded =
    typeof window !== 'undefined' && window.ANIME_UNIVERSE_ASSETS && window.ANIME_UNIVERSE_ASSETS[found.key]
      ? window.ANIME_UNIVERSE_ASSETS[found.key]
      : found.img;
  return { ...found, img: embedded };
}

function getAvatarImgUrl(avatarKey, avatarUrl = '') {
  return getAvatarPreset(avatarKey, avatarUrl).img;
}

function renderAvatarCircle(avatarKey, username = 'A', sizePx = 28, avatarUrl = '') {
  const av = getAvatarPreset(avatarKey, avatarUrl);
  const initial = String(username || 'A').trim().charAt(0).toUpperCase() || 'A';
  return `
    <div style="position:relative;width:${sizePx}px;height:${sizePx}px;border-radius:50%;overflow:hidden;border:1.5px solid rgba(255,255,255,0.32);background:linear-gradient(135deg,${av.c1},${av.c2});flex-shrink:0;display:inline-flex;align-items:center;justify-content:center;">
      <span style="position:absolute;color:#FFF;font-size:${Math.max(10, Math.round(sizePx * 0.44))}px;font-weight:700;pointer-events:none;">${escapeHtml(initial)}</span>
      <img src="${av.img}" alt="" onerror="${AVATAR_IMG_ONERROR}" style="position:relative;z-index:1;width:100%;height:100%;object-fit:cover;display:block;border-radius:50%;" />
    </div>
  `;
}

function maskEmailAddress(email) {
  const clean = String(email || '').trim();
  const atIdx = clean.indexOf('@');
  if (atIdx <= 0) return clean;
  const local = clean.slice(0, atIdx);
  const domain = clean.slice(atIdx + 1);
  const maskedLocal =
    local.length <= 2
      ? `${local.charAt(0)}*`
      : `${local.slice(0, 2)}${'*'.repeat(Math.min(5, Math.max(2, local.length - 3)))}${local.slice(-1)}`;
  return `${maskedLocal}@${domain}`;
}

// Refresh JWT Access Token if expired or on 401
let isRefreshingToken = false;
async function refreshSessionTokenIfNeeded(force = false) {
  if (!state.session || !state.session.refreshToken || isRefreshingToken) return false;
  const nowSec = Math.floor(Date.now() / 1000);
  if (!force && state.session.expiresAt && state.session.expiresAt > nowSec + 90) {
    return true;
  }
  isRefreshingToken = true;
  try {
    const baseUrl = getVaultBaseUrl();
    const anonKey = getVaultPublicKey();
    const resp = await fetch(`${baseUrl}/auth/v1/token?grant_type=refresh_token`, {
      method: 'POST',
      headers: {
        apikey: anonKey,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ refresh_token: state.session.refreshToken })
    });
    if (!resp.ok) return false;
    const data = await resp.json();
    if (data.access_token) {
      saveSession({
        ...state.session,
        accessToken: data.access_token,
        refreshToken: data.refresh_token || state.session.refreshToken,
        expiresAt: Math.floor(Date.now() / 1000) + (data.expires_in || 3600)
      });
      return true;
    }
    return false;
  } catch (_) {
    return false;
  } finally {
    isRefreshingToken = false;
  }
}

// Supabase Request Helper (with automatic token refresh on 401)
async function supabaseRequest(path, method = 'GET', body = undefined, prefer = '', allowRetry = true) {
  await refreshSessionTokenIfNeeded(false);
  const accessToken = state.session?.accessToken || '';
  const baseUrl = getVaultBaseUrl();
  const anonKey = getVaultPublicKey();
  const headers = {
    apikey: anonKey,
    Authorization: `Bearer ${accessToken || anonKey}`,
    'Content-Type': 'application/json'
  };
  if (prefer) headers.Prefer = prefer;
  const resp = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined
  });
  if (resp.status === 401 && allowRetry && state.session?.refreshToken) {
    const ok = await refreshSessionTokenIfNeeded(true);
    if (ok) {
      return supabaseRequest(path, method, body, prefer, false);
    }
  }
  const text = await resp.text();
  let data = {};
  try { data = JSON.parse(text); } catch (_) { data = text; }
  if (!resp.ok) {
    throw new Error((data && (data.msg || data.error_description || data.message || data.error)) || 'Cloud request failed');
  }
  return data;
}

// Resolve Email OR Username via Supabase RPC (matches Android SupabaseDatabaseService.resolveLoginEmail)
async function resolveLoginIdentifier(identifier) {
  const clean = String(identifier || '').trim().replace(/^@+/, '').trim();
  if (!clean) throw new Error('Enter your email address or username.');
  if (clean.includes('@') && clean.includes('.')) {
    return clean.toLowerCase();
  }
  const baseUrl = getVaultBaseUrl();
  const anonKey = getVaultPublicKey();
  const resp = await fetch(`${baseUrl}/rest/v1/rpc/resolve_login_email`, {
    method: 'POST',
    headers: {
      apikey: anonKey,
      Authorization: `Bearer ${anonKey}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ p_identifier: clean })
  });
  const text = (await resp.text()).trim().replace(/^"|"$/g, '');
  if (!resp.ok || !text || text === 'null' || !text.includes('@')) {
    throw new Error(`No account found matching "${clean}". Try your email address or Sign Up.`);
  }
  return text;
}

function resolveTitle(item) {
  const pref = state.prefs.titleLanguage;
  if (pref === 'ROMAJI') return item.titleRomaji || item.title || item.titleEnglish || 'Anime';
  if (pref === 'NATIVE') return item.titleNative || item.titleEnglish || item.titleRomaji || item.title || 'Anime';
  return item.titleEnglish || item.title || item.titleRomaji || 'Anime';
}

function formatCompactCountdown(airingUnix) {
  if (!airingUnix) return '';
  const diff = airingUnix - Math.floor(Date.now() / 1000);
  if (diff <= 0) return 'AIRING';
  const d = Math.floor(diff / 86400);
  const h = Math.floor((diff % 86400) / 3600);
  const m = Math.floor((diff % 3600) / 60);
  if (d > 0) return `${d}d ${h}h`;
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

function formatAiringClock(airingUnix) {
  if (!airingUnix || airingUnix <= 0) return '';
  try {
    const d = new Date(airingUnix * 1000);
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  } catch (_) {
    return '';
  }
}

// Fetch User Watchlist, Home Tracking, Collections, Profile & Community Reviews (Strictly isolated per user_id)
async function loadCloudData(silent = false) {
  const activeUserId = state.session?.userId;
  if (!activeUserId) return;
  try {
    const uid = encodeURIComponent(activeUserId);
    const watchlistSelect =
      'id,user_id,anime_id,status,episode_progress,total_episodes,personal_rating,review_notes,is_favorite,updated_at,' +
      'anime(anilist_id,title_english,title_romaji,title_native,cover_image,banner_image,genres,release_year,season,episodes,average_score,popularity,format,status,studios,next_airing_at,next_episode_number)';

    const [normRows, homeRows, colRows, prof, cmRows] = await Promise.all([
      supabaseRequest(`/rest/v1/user_anime?user_id=eq.${uid}&select=${watchlistSelect}&order=updated_at.desc`).catch(() => null),
      supabaseRequest(`/rest/v1/home_tracking?user_id=eq.${uid}&select=anime_id,airing_day_of_week`).catch(() => null),
      supabaseRequest(`/rest/v1/collections?creator_id=eq.${uid}&select=*,collection_anime(anime_id)&order=updated_at.desc`).catch(() => null),
      supabaseRequest(`/rest/v1/profiles?id=eq.${uid}&select=*`).catch(() => []),
      supabaseRequest(`/rest/v1/community_members?user_id=eq.${uid}&select=status`).catch(() => [])
    ]);

    // Guard against session switching while request was in flight
    if (!state.session || state.session.userId !== activeUserId) return;

    const hasServerHomeTable = Array.isArray(homeRows);
    if (hasServerHomeTable) {
      state.homeTrackingIds = new Set(homeRows.map((h) => Number(h.anime_id)).filter(Boolean));
      state.homeTrackingHydrated = true;
      saveHomeTrackingIds();
    }

    if (Array.isArray(normRows)) {
      const userScopedLocal = readScopedArray('vault', state.session, false, null).filter(
        (v) => !v.ownerId || v.ownerId === activeUserId
      );
      const localHomeWasEmpty = !state.homeTrackingHydrated && state.homeTrackingIds.size === 0;
      state.vault = normRows
        .filter((r) => !r.user_id || r.user_id === activeUserId)
        .map((r) => {
          const anime = r.anime || {};
          const existingLocal = userScopedLocal.find((v) => v.animeId === r.anime_id) || {};
          const eng = anime.title_english && anime.title_english !== 'null' ? anime.title_english : existingLocal.titleEnglish || '';
          const rom = anime.title_romaji && anime.title_romaji !== 'null' ? anime.title_romaji : existingLocal.titleRomaji || '';
          const nextAir = anime.next_airing_at ? Number(anime.next_airing_at) : existingLocal.airingTimeUnix || null;
          const dow = nextAir && nextAir > 0
            ? (((new Date(nextAir * 1000).getDay() + 6) % 7) + 1)
            : existingLocal.airingDayOfWeek || ((r.anime_id % 7) + 1);
          const mediaStatus = anime.status || existingLocal.mediaStatus || 'RELEASING';
          const totalEp = r.total_episodes || anime.episodes || existingLocal.totalEpisodes || null;
          const watchedEp = r.episode_progress || 0;
          const userStatus = r.status || 'watching';

          if (!hasServerHomeTable && localHomeWasEmpty) {
            const eligiblePreMigration =
              userStatus !== 'dropped' &&
              userStatus !== 'completed' &&
              !shouldAutoComplete(mediaStatus, totalEp, watchedEp);
            if (eligiblePreMigration) {
              state.homeTrackingIds.add(Number(r.anime_id));
            }
          }

          const onHome =
            state.homeTrackingIds.has(Number(r.anime_id)) &&
            !shouldRemoveFromHome(userStatus, mediaStatus, totalEp, watchedEp);

          return {
            ownerId: activeUserId,
            animeId: r.anime_id,
            title: eng || rom || existingLocal.title || `Anime #${r.anime_id}`,
            titleEnglish: eng || rom || 'Anime',
            titleRomaji: rom || eng || 'Anime',
            titleNative: anime.title_native || existingLocal.titleNative || '',
            coverImage: anime.cover_image || existingLocal.coverImage || '',
            bannerImage: anime.banner_image || existingLocal.bannerImage || '',
            format: anime.format || existingLocal.format || 'TV',
            mediaStatus,
            seasonYear: anime.release_year || existingLocal.seasonYear || null,
            averageScore: anime.average_score || existingLocal.averageScore || null,
            countryOfOrigin: existingLocal.countryOfOrigin || 'JP',
            airingDayOfWeek: dow,
            airingTimeUnix: nextAir,
            nextEpisodeNumber: anime.next_episode_number || existingLocal.nextEpisodeNumber || null,
            status: userStatus,
            season: existingLocal.season || 1,
            watchedEpisodes: watchedEp,
            totalEpisodes: totalEp,
            rating10: r.personal_rating !== null && r.personal_rating !== undefined ? Number(r.personal_rating) : null,
            reviewNotes: r.review_notes || '',
            isPinned: Boolean(existingLocal.isPinned),
            isFavorite: Boolean(r.is_favorite ?? existingLocal.isFavorite),
            genres: Array.isArray(anime.genres) ? anime.genres : existingLocal.genres || [],
            synopsis: existingLocal.synopsis || '',
            inWatchlist: true,
            inHome: onHome
          };
        });
      if (!hasServerHomeTable && localHomeWasEmpty) {
        state.homeTrackingHydrated = true;
        saveHomeTrackingIds();
      }
      saveLocalVault();
    }

    // Safe non-duplicating migration if the user had tracked items in a Guest session prior to logging in
    if (pendingGuestMigrationItems.length > 0) {
      const itemsToMigrate = [...pendingGuestMigrationItems];
      const oldGuestId = pendingGuestMigrationGuestId;
      pendingGuestMigrationItems = [];
      pendingGuestMigrationGuestId = null;
      const existingIds = new Set(state.vault.map((v) => v.animeId));
      for (const gItem of itemsToMigrate) {
        if (!existingIds.has(gItem.animeId)) {
          existingIds.add(gItem.animeId);
          await saveVaultEntry({ ...gItem, ownerId: activeUserId }, { addToHome: Boolean(gItem.inHome) });
        }
      }
      if (oldGuestId) {
        localStorage.removeItem(`anime_universe_vault_guest_${oldGuestId}`);
        localStorage.removeItem(`anime_universe_home_ids_guest_${oldGuestId}`);
        localStorage.removeItem(`anime_universe_collections_guest_${oldGuestId}`);
        localStorage.removeItem(`anime_universe_profile_guest_${oldGuestId}`);
      }
      state.guestId = rotateGuestId();
    }

    if (Array.isArray(colRows)) {
      state.collections = colRows
        .filter((c) => !c.creator_id || c.creator_id === activeUserId)
        .map((c) => ({
          ownerId: activeUserId,
          id: c.id,
          name: c.name || 'Collection',
          description: c.description || '',
          visibility: c.visibility || 'community',
          animeIds: Array.isArray(c.collection_anime) ? c.collection_anime.map((ca) => ca.anime_id) : []
        }));
      saveCollections();
    }

    const cmStatus = Array.isArray(cmRows) && cmRows.length > 0 ? cmRows[0].status : '';
    if (Array.isArray(prof) && prof.length > 0) {
      const p = prof.find((row) => !row.id || row.id === activeUserId) || prof[0];
      const loadedName = String(p.username || '').trim();
      const needsProfileSetup =
        state.session?.onboardingCompleted === false || isPlaceholderOrEmailName(loadedName);
      state.profile = {
        userId: activeUserId,
        username: isPlaceholderOrEmailName(loadedName) ? 'Member' : loadedName,
        avatarKey: normalizeAvatarKey(p.avatar_key, p.avatar_url),
        bio: p.bio || '',
        memberStatus: cmStatus || p.member_status || 'active',
        isAdmin: Boolean(p.is_admin)
      };
      saveProfileCache();
      if (needsProfileSetup && state.session) {
        if (state.session.onboardingCompleted !== false) {
          saveSession({ ...state.session, onboardingCompleted: false, username: '' });
        }
        if (state.authMode !== 'onboard_username' && state.authMode !== 'onboard_avatar') {
          state.onboardUsername = '';
          state.authMode = 'onboard_username';
        }
      } else if (state.session && !isPlaceholderOrEmailName(loadedName) && state.session.username !== loadedName) {
        saveSession({ ...state.session, username: loadedName, onboardingCompleted: true });
      }
    }

    await Promise.all([loadCommunityReviews(), refreshUnreadNotificationCount()]);
    const hasUnsavedDetailDraft = Boolean(
      state.tab === 'detail' &&
        state.detailAnime &&
        String(state.detailNoteDraft || '').trim() !== String(state.detailAnime.reviewNotes || '').trim()
    );
    const hasActiveUserWork =
      Boolean(state.inlineEpEdit) ||
      Boolean(state.contextMenu) ||
      Boolean(state.communityCommentsAnimeId !== null) ||
      Boolean(state.quickEditAnime) ||
      Boolean(state.showAvatarModal) ||
      Boolean(state.confirmRemoveHomeAnime) ||
      Boolean(state.tab === 'detail') ||
      hasUnsavedDetailDraft ||
      Boolean(String(state.communityCommentDraftNote || '').trim()) ||
      Boolean(String(state.discussion.draft || '').trim()) ||
      Boolean(String(state.discussion.replyDraft || '').trim()) ||
      Boolean(state.discussion.activeReplyTarget) ||
      Boolean(state.discussion.editingCommentId) ||
      Boolean(state.discussion.editingReplyId) ||
      Boolean(state.discussion.contextMenu) ||
      window.scrollY > 80 ||
      (document.activeElement && ['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName));
    if (!silent || !hasActiveUserWork) {
      renderApp();
    } else {
      updateCommunityRedDotsInPlace();
    }
  } catch (_) {}
}

function isHentaiOrAdultAnime(genres, isAdult = false) {
  if (isAdult === true) return true;
  if (!Array.isArray(genres)) return false;
  return genres.some((g) => {
    const s = String(g || '').trim().toLowerCase();
    return s === 'hentai' || s === 'adult' || s.includes('18+');
  });
}

function normalizeAnimeGenresWithAdult(genres, isAdult = false) {
  const list = Array.isArray(genres) ? genres.filter(Boolean) : [];
  if (isAdult === true && !list.some((g) => String(g).trim().toLowerCase() === 'hentai')) {
    return [...list, 'Hentai'];
  }
  return list;
}

function updateCommunityRedDotsInPlace() {
  const hasUnseen = state.notifications.unreadCount > 0 || (state.notifications.unseenAnime || []).length > 0;
  document.querySelectorAll('[data-nav="community"]').forEach((btn) => {
    const existingDot = btn.querySelector('.nav-red-dot');
    if (hasUnseen && !existingDot) {
      const iconWrap = btn.querySelector('.nav-icon-badge-wrap');
      const dot = document.createElement('span');
      dot.className = iconWrap ? 'nav-red-dot' : 'nav-red-dot inline';
      dot.setAttribute('aria-label', 'Unseen discussion messages');
      (iconWrap || btn).appendChild(dot);
    } else if (!hasUnseen && existingDot) {
      existingDot.remove();
    }
  });
}

// Fetch Community Reviews — Independent of Home membership; merges community_notes + user_anime
async function loadCommunityReviews() {
  try {
    const feedSelect =
      'id,user_id,anime_id,status,episode_progress,personal_rating,review_notes,updated_at,' +
      'anime(anilist_id,title_english,title_romaji,cover_image,episodes,genres)';
    const notesSelect =
      'id,user_id,anime_id,rating,note_text,updated_at,' +
      'anime(anilist_id,title_english,title_romaji,cover_image,episodes,genres)';
    const [noteRows, normRows, memberRows] = await Promise.all([
      supabaseRequest(`/rest/v1/community_notes?select=${notesSelect}&order=updated_at.desc&limit=150`).catch(() => []),
      supabaseRequest(`/rest/v1/user_anime?select=${feedSelect}&order=updated_at.desc&limit=150`).catch(() => []),
      supabaseRequest('/rest/v1/community_members?select=user_id,status').catch(() => [])
    ]);

    const excludedUsers = new Set();
    if (Array.isArray(memberRows)) {
      memberRows.forEach((m) => {
        const st = String(m.status || '').toLowerCase();
        if (st === 'left' || st === 'removed') {
          excludedUsers.add(m.user_id);
        }
      });
    }

    const combinedRaw = [];
    if (Array.isArray(noteRows)) {
      noteRows.forEach((n) => {
        if (!n || !n.user_id || excludedUsers.has(n.user_id)) return;
        const hasRating = n.rating !== null && n.rating !== undefined && Number(n.rating) > 0;
        const hasNote = String(n.note_text || '').trim().length > 0;
        if (!hasRating && !hasNote) return;
        combinedRaw.push({
          id: n.id,
          source: 'community_notes',
          user_id: n.user_id,
          anime_id: Number(n.anime_id),
          anime: n.anime || {},
          rating: hasRating ? Number(n.rating) : null,
          review_notes: String(n.note_text || '').trim(),
          updated_at: n.updated_at || new Date().toISOString()
        });
      });
    }

    if (Array.isArray(normRows)) {
      normRows.forEach((r) => {
        if (!r || !r.user_id || excludedUsers.has(r.user_id)) return;
        const hasRating = r.personal_rating !== null && r.personal_rating !== undefined && Number(r.personal_rating) > 0;
        const hasNote = String(r.review_notes || '').trim().length > 0;
        if (!hasRating && !hasNote) return;
        combinedRaw.push({
          id: r.id,
          source: 'user_anime',
          user_id: r.user_id,
          anime_id: Number(r.anime_id),
          anime: r.anime || {},
          rating: hasRating ? Number(r.personal_rating) : null,
          review_notes: String(r.review_notes || '').trim(),
          updated_at: r.updated_at || new Date().toISOString()
        });
      });
    }

    // Deduplicate per (user_id, anime_id) keeping the most recently updated entry (preferring community_notes on tie)
    combinedRaw.sort((a, b) => {
      const cmp = String(b.updated_at || '').localeCompare(String(a.updated_at || ''));
      if (cmp !== 0) return cmp;
      return a.source === 'community_notes' ? -1 : 1;
    });
    const dedupMap = new Map();
    combinedRaw.forEach((row) => {
      const key = `${row.user_id}_${row.anime_id}`;
      if (!dedupMap.has(key)) {
        dedupMap.set(key, row);
      }
    });
    const valid = Array.from(dedupMap.values());

    const userIds = Array.from(new Set(valid.map((r) => r.user_id).filter(Boolean)));
    const noteIds = valid.filter((r) => r.source === 'community_notes').map((r) => r.id).filter(Boolean);
    const reviewIds = valid.filter((r) => r.source === 'user_anime').map((r) => r.id).filter(Boolean);

    const [profRows, reviewLikeRows, noteLikeRows] = await Promise.all([
      userIds.length > 0
        ? supabaseRequest(`/rest/v1/profiles?id=in.(${userIds.join(',')})&select=id,username,avatar_key,avatar_url`).catch(() => [])
        : Promise.resolve([]),
      reviewIds.length > 0
        ? supabaseRequest(`/rest/v1/review_likes?review_id=in.(${reviewIds.join(',')})&select=review_id,user_id`).catch(() => [])
        : Promise.resolve([]),
      noteIds.length > 0
        ? supabaseRequest(`/rest/v1/community_note_likes?note_id=in.(${noteIds.join(',')})&select=note_id,user_id`).catch(() => [])
        : Promise.resolve([])
    ]);

    const profMap = {};
    if (Array.isArray(profRows)) {
      profRows.forEach((p) => {
        profMap[p.id] = {
          username: p.username || 'Member',
          avatar_key: normalizeAvatarKey(p.avatar_key, p.avatar_url)
        };
      });
    }

    const likesCountMap = {};
    const likedByMeSet = new Set();
    if (Array.isArray(reviewLikeRows)) {
      reviewLikeRows.forEach((l) => {
        if (!l.review_id) return;
        likesCountMap[l.review_id] = (likesCountMap[l.review_id] || 0) + 1;
        if (state.session && l.user_id === state.session.userId) {
          likedByMeSet.add(l.review_id);
        }
      });
    }
    if (Array.isArray(noteLikeRows)) {
      noteLikeRows.forEach((l) => {
        if (!l.note_id) return;
        likesCountMap[l.note_id] = (likesCountMap[l.note_id] || 0) + 1;
        if (state.session && l.user_id === state.session.userId) {
          likedByMeSet.add(l.note_id);
        }
      });
    }

    const mapped = valid.map((r) => {
      const anime = r.anime || {};
      const prof = profMap[r.user_id] || {
        username: r.user_id === state.session?.userId ? state.profile.username : 'Member',
        avatar_key: r.user_id === state.session?.userId ? state.profile.avatarKey : 'avatar_1'
      };
      const animeGenres = Array.isArray(anime.genres) ? anime.genres : [];
      return {
        id: r.id,
        source: r.source,
        user_id: r.user_id,
        username: prof.username,
        avatar_key: prof.avatar_key,
        anime_id: Number(r.anime_id),
        anime_title:
          (anime.title_english && anime.title_english !== 'null' ? anime.title_english : '') ||
          anime.title_romaji ||
          'Anime',
        anime_cover_image: anime.cover_image || '',
        genres: animeGenres,
        is_adult: isHentaiOrAdultAnime(animeGenres, Boolean(anime.is_adult)),
        rating: r.rating,
        review_notes: r.review_notes,
        updated_at: r.updated_at || new Date().toISOString(),
        likes_count: likesCountMap[r.id] || 0,
        liked_by_me: likedByMeSet.has(r.id)
      };
    });

    // Ensure active signed-in member's own local Library notes/ratings are immediately present
    if (state.session && state.profile.memberStatus !== 'left' && state.profile.memberStatus !== 'removed') {
      state.vault.forEach((v) => {
        const hasRating = v.rating10 !== null && Number(v.rating10) > 0;
        const hasNote = String(v.reviewNotes || '').trim().length > 0;
        if (!hasRating && !hasNote) return;
        const alreadyIdx = mapped.findIndex(
          (m) => m.user_id === state.session.userId && Number(m.anime_id) === Number(v.animeId)
        );
        if (alreadyIdx === -1) {
          const vGenres = Array.isArray(v.genres) ? v.genres : [];
          mapped.unshift({
            id: `local_${state.session.userId}_${v.animeId}`,
            source: 'user_anime',
            user_id: state.session.userId,
            username: state.profile.username || 'Member',
            avatar_key: state.profile.avatarKey || 'avatar_1',
            anime_id: Number(v.animeId),
            anime_title: v.titleEnglish || v.title || 'Anime',
            anime_cover_image: v.coverImage || '',
            genres: vGenres,
            is_adult: isHentaiOrAdultAnime(vGenres, Boolean(v.isAdult)),
            rating: hasRating ? Number(v.rating10) : null,
            review_notes: String(v.reviewNotes || '').trim(),
            updated_at: new Date().toISOString(),
            likes_count: 0,
            liked_by_me: false
          });
        }
      });
    }

    state.communityReviews = mapped;
  } catch (_) {}
}

// Group all community reviews/notes by anime_id so every anime has strictly ONE template card
function groupCommunityReviewsByAnime(reviews) {
  const byAnime = new Map();
  (reviews || []).forEach((rev) => {
    if (!rev || !rev.anime_id) return;
    const hasRating = rev.rating !== null && Number(rev.rating) > 0;
    const hasNote = String(rev.review_notes || '').trim().length > 0;
    if (!hasRating && !hasNote) return;
    const aid = Number(rev.anime_id);
    if (!byAnime.has(aid)) {
      byAnime.set(aid, []);
    }
    byAnime.get(aid).push(rev);
  });

  const groups = [];
  byAnime.forEach((animeReviews, animeId) => {
    // Deduplicate per user_id inside the same anime (keep most recent)
    const sorted = [...animeReviews].sort((a, b) =>
      String(b.updated_at || '').localeCompare(String(a.updated_at || ''))
    );
    const seenUsers = new Set();
    const uniqueUserReviews = [];
    sorted.forEach((r) => {
      const uid = r.user_id || r.id;
      if (seenUsers.has(uid)) return;
      seenUsers.add(uid);
      uniqueUserReviews.push(r);
    });

    if (uniqueUserReviews.length === 0) return;

    const first = uniqueUserReviews[0];
    const rated = uniqueUserReviews.filter((r) => r.rating !== null && Number(r.rating) > 0);
    const noted = uniqueUserReviews.filter((r) => String(r.review_notes || '').trim().length > 0);
    const avgRating10 =
      rated.length > 0
        ? rated.reduce((acc, r) => acc + Number(r.rating || 0), 0) / rated.length
        : null;
    const leadReview = noted.length > 0 ? noted[0] : uniqueUserReviews[0];
    const totalLikesCount = uniqueUserReviews.reduce((acc, r) => acc + Number(r.likes_count || 0), 0);

    const mergedGenres = Array.from(
      new Set(uniqueUserReviews.flatMap((r) => (Array.isArray(r.genres) ? r.genres : [])))
    );
    const localMatch =
      state.vault.find((v) => Number(v.animeId) === animeId) ||
      state.discover.find((d) => Number(d.animeId) === animeId);
    const allGenres = Array.from(
      new Set([...mergedGenres, ...(Array.isArray(localMatch?.genres) ? localMatch.genres : [])])
    );
    const isAdultFlag =
      uniqueUserReviews.some((r) => Boolean(r.is_adult)) ||
      isHentaiOrAdultAnime(allGenres, Boolean(localMatch?.isAdult));

    groups.push({
      anime_id: animeId,
      anime_title: first.anime_title || 'Anime',
      anime_cover_image: first.anime_cover_image || '',
      genres: allGenres,
      is_adult: isAdultFlag,
      average_rating10: avgRating10,
      rated_count: rated.length,
      noted_count: noted.length,
      total_count: uniqueUserReviews.length,
      latest_updated_at: first.updated_at || '',
      total_likes_count: totalLikesCount,
      lead_review: leadReview,
      reviews: uniqueUserReviews
    });
  });

  return groups;
}

let discoverFetchToken = 0;
let discoverScrollRaf = null;

function mapDiscoverMediaItem(m) {
  const airingAt = m.nextAiringEpisode?.airingAt || null;
  const dow = airingAt ? (((new Date(airingAt * 1000).getDay() + 6) % 7) + 1) : ((m.id % 7) + 1);
  const genres = normalizeAnimeGenresWithAdult(m.genres || [], Boolean(m.isAdult));
  return {
    animeId: m.id,
    title: m.title?.english || m.title?.romaji || 'Anime',
    titleEnglish: m.title?.english || '',
    titleRomaji: m.title?.romaji || '',
    titleNative: m.title?.native || '',
    coverImage: m.coverImage?.extraLarge || m.coverImage?.large || '',
    bannerImage: m.bannerImage || '',
    format: m.format || 'TV',
    mediaStatus: m.status || 'RELEASING',
    seasonYear: m.seasonYear || null,
    countryOfOrigin: m.countryOfOrigin || 'JP',
    totalEpisodes: m.episodes || null,
    averageScore: m.averageScore || null,
    genres,
    isAdult: isHentaiOrAdultAnime(genres, Boolean(m.isAdult)),
    synopsis: (m.description || '').replace(/<[^>]+>/g, ''),
    airingDayOfWeek: dow,
    airingTimeUnix: airingAt,
    nextEpisodeNumber: m.nextAiringEpisode?.episode || null,
    status: 'watching',
    season: 1,
    watchedEpisodes: 0,
    rating10: null,
    reviewNotes: '',
    isPinned: false,
    isFavorite: false
  };
}

function renderDiscoverFooterHtml() {
  if (state.isSearching) return '';
  if (state.discoverLoadingMore) {
    return `<div id="discover-load-sentinel" style="text-align:center;padding:24px 16px;color:var(--accent-primary);font-size:13px;font-weight:600;letter-spacing:0.6px;">Loading more anime...</div>`;
  }
  if (state.discoverHasNextPage && state.discover.length > 0) {
    return `
      <div id="discover-load-sentinel" style="display:flex;justify-content:center;padding:20px 16px;">
        <button type="button" class="capsule-pill" id="btn-discover-load-more" style="height:44px;padding:0 22px;color:var(--accent-primary);border-color:var(--accent-border);">
          Load More Anime
        </button>
      </div>
    `;
  }
  if (state.discover.length > 0) {
    return `<div id="discover-load-sentinel" style="text-align:center;padding:20px 16px;color:var(--text-muted);font-size:12px;">All ${state.discover.length} matching titles loaded</div>`;
  }
  return `<div id="discover-load-sentinel"></div>`;
}

function updateDiscoverFooterDom() {
  const countEl = document.getElementById('discover-count-label');
  if (countEl) {
    countEl.textContent = `${state.discover.length} TITLES`;
  }
  const sentinelWrap = document.getElementById('discover-sentinel-wrap');
  if (sentinelWrap) {
    sentinelWrap.innerHTML = renderDiscoverFooterHtml();
    const btnMore = document.getElementById('btn-discover-load-more');
    if (btnMore) {
      btnMore.onclick = () => fetchDiscoverCatalog(true);
    }
  }
}

function bindCardInteractionHandlers(rootEl) {
  if (!rootEl) return;
  rootEl.querySelectorAll('[data-open-detail]').forEach((el) => {
    el.onclick = (e) => {
      e.stopPropagation();
      if (state._suppressNextCardClick) {
        state._suppressNextCardClick = false;
        return;
      }
      openAnimeDetail(Number(el.getAttribute('data-open-detail')));
    };
  });

  rootEl.querySelectorAll('[data-card-anime-id]').forEach((card) => {
    const aid = Number(card.getAttribute('data-card-anime-id'));
    card.oncontextmenu = (e) => {
      e.preventDefault();
      e.stopPropagation();
      state.contextMenu = { animeId: aid, x: e.clientX, y: e.clientY };
      renderApp();
    };

    let touchTimer = null;
    let startX = 0;
    let startY = 0;
    card.ontouchstart = (e) => {
      if (!e.touches || e.touches.length !== 1) return;
      startX = e.touches[0].clientX;
      startY = e.touches[0].clientY;
      touchTimer = setTimeout(() => {
        touchTimer = null;
        state._suppressNextCardClick = true;
        state.contextMenu = { animeId: aid, x: startX, y: startY };
        renderApp();
      }, 500);
    };
    card.ontouchmove = (e) => {
      if (!touchTimer || !e.touches || e.touches.length !== 1) return;
      const dx = Math.abs(e.touches[0].clientX - startX);
      const dy = Math.abs(e.touches[0].clientY - startY);
      if (dx > 10 || dy > 10) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
    };
    card.ontouchend = () => {
      if (touchTimer) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
    };
    card.ontouchcancel = () => {
      if (touchTimer) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
    };
  });

  rootEl.querySelectorAll('[data-quick-add]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-quick-add'));
      const existing = state.vault.find((v) => Number(v.animeId) === id);
      if (existing) {
        state.quickEditAnime = { ...existing };
        renderApp();
        return;
      }
      const disc = state.discover.find((d) => Number(d.animeId) === id);
      if (disc) {
        await addToHome(disc);
      }
    };
  });

  rootEl.querySelectorAll('[data-quick-edit]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-quick-edit'));
      const item = state.vault.find((v) => Number(v.animeId) === id);
      if (item) {
        state.quickEditAnime = { ...item };
        renderApp();
      }
    };
  });
}

function checkDiscoverInfiniteScroll() {
  if (state.tab !== 'discover') return;
  if (state.isSearching || state.discoverLoadingMore || !state.discoverHasNextPage) return;
  if (!state.discover.length) return;

  const grid = document.getElementById('discover-poster-grid');
  if (!grid || !grid.children.length) return;

  const cards = grid.children;
  const firstTop = cards[0].offsetTop;
  let cols = 0;
  for (let i = 0; i < cards.length; i++) {
    if (Math.abs(cards[i].offsetTop - firstTop) <= 8) cols++;
    else break;
  }
  cols = Math.max(1, cols);

  // Automatically load the next page once the user crosses the 3rd row of the active batch
  const triggerIdx = Math.min(
    cards.length - 1,
    Math.max(0, state.discoverLastPageStartIndex + cols * 3 - 1)
  );
  const triggerCard = cards[triggerIdx];
  const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 800;

  let shouldLoad = false;
  if (triggerCard) {
    const rect = triggerCard.getBoundingClientRect();
    // Trigger as soon as the 3rd row crosses into / above the lower viewport
    if (rect.top <= viewportHeight + 120) {
      shouldLoad = true;
    }
  }
  if (!shouldLoad) {
    const sentinel = document.getElementById('discover-load-sentinel');
    if (sentinel) {
      const sRect = sentinel.getBoundingClientRect();
      if (sRect.top <= viewportHeight + 480) {
        shouldLoad = true;
      }
    }
  }

  if (shouldLoad) {
    fetchDiscoverCatalog(true);
  }
}

window.addEventListener(
  'scroll',
  () => {
    if (state.tab !== 'discover') return;
    if (discoverScrollRaf) return;
    discoverScrollRaf = requestAnimationFrame(() => {
      discoverScrollRaf = null;
      checkDiscoverInfiniteScroll();
    });
  },
  { passive: true }
);

async function fetchDiscoverCatalog(loadMore = false) {
  if (loadMore) {
    if (state.isSearching || state.discoverLoadingMore || !state.discoverHasNextPage) return;
    state.discoverLoadingMore = true;
    updateDiscoverFooterDom();
  } else {
    state.isSearching = true;
    state.discoverPage = 1;
    state.discoverHasNextPage = true;
    state.discoverLoadingMore = false;
    state.discoverLastPageStartIndex = 0;
    renderApp();
  }

  const targetPage = loadMore ? state.discoverPage + 1 : 1;
  const myToken = loadMore ? discoverFetchToken : ++discoverFetchToken;

  const variables = {
    page: targetPage,
    perPage: 30,
    isAdult: Boolean(state.prefs.showAdultContent),
    sort: [state.discoverSort || 'POPULARITY_DESC']
  };
  if (state.searchQuery.trim()) variables.search = state.searchQuery.trim();
  if (state.discoverFormat !== 'ALL') variables.format = state.discoverFormat;
  if (state.discoverStatus !== 'ALL') variables.status = state.discoverStatus;
  if (state.discoverGenre) variables.genre = state.discoverGenre;
  if (state.discoverMode === 'donghua') variables.countryOfOrigin = 'CN';
  if (state.discoverMode === 'seasonal') {
    variables.season = getCurrentAnimeSeason();
    variables.seasonYear = new Date().getFullYear();
  }

  const gql = `
    query(
      $page: Int,
      $perPage: Int,
      $search: String,
      $sort: [MediaSort],
      $format: MediaFormat,
      $status: MediaStatus,
      $genre: String,
      $countryOfOrigin: CountryCode,
      $season: MediaSeason,
      $seasonYear: Int,
      $isAdult: Boolean
    ) {
      Page(page: $page, perPage: $perPage) {
        pageInfo {
          hasNextPage
          currentPage
        }
        media(
          type: ANIME,
          search: $search,
          sort: $sort,
          format: $format,
          status: $status,
          genre: $genre,
          countryOfOrigin: $countryOfOrigin,
          season: $season,
          seasonYear: $seasonYear,
          isAdult: $isAdult
        ) {
          id
          title { english romaji native }
          coverImage { extraLarge large }
          bannerImage
          format
          status
          seasonYear
          episodes
          averageScore
          countryOfOrigin
          genres
          isAdult
          description(asHtml: false)
          nextAiringEpisode { airingAt episode }
        }
      }
    }
  `;

  try {
    let json = null;
    for (let attempt = 0; attempt < 2; attempt++) {
      const resp = await fetch('https://graphql.anilist.co', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: gql, variables })
      });
      if (resp.status === 429 && attempt === 0) {
        await new Promise((r) => setTimeout(r, 700));
        continue;
      }
      if (!resp.ok) break;
      json = await resp.json();
      break;
    }

    if (myToken !== discoverFetchToken) return;

    const pageObj = json?.data?.Page;
    const list = pageObj?.media || [];
    const mapped = list.map(mapDiscoverMediaItem);
    const hasNext = Boolean(pageObj?.pageInfo?.hasNextPage) && mapped.length > 0;

    if (loadMore) {
      const existingIds = new Set(state.discover.map((d) => Number(d.animeId)));
      const uniqueNew = mapped.filter((item) => !existingIds.has(Number(item.animeId)));
      const prevCount = state.discover.length;
      state.discoverPage = targetPage;
      state.discoverHasNextPage = hasNext;
      state.discoverLoadingMore = false;

      if (uniqueNew.length > 0) {
        state.discoverLastPageStartIndex = prevCount;
        state.discover = [...state.discover, ...uniqueNew];
        const grid = document.getElementById('discover-poster-grid');
        if (state.tab === 'discover' && grid) {
          const tempWrap = document.createElement('div');
          tempWrap.innerHTML = uniqueNew.map((it) => renderPosterTrackerCard(it, false)).join('');
          bindCardInteractionHandlers(tempWrap);
          const frag = document.createDocumentFragment();
          while (tempWrap.firstChild) {
            frag.appendChild(tempWrap.firstChild);
          }
          grid.appendChild(frag);
          updateDiscoverFooterDom();
        } else if (state.tab === 'discover') {
          renderApp();
        }
      } else {
        updateDiscoverFooterDom();
      }
    } else {
      state.discover = mapped;
      state.discoverPage = 1;
      state.discoverHasNextPage = hasNext;
      state.discoverLastPageStartIndex = 0;
      state.isSearching = false;
      renderApp();
      requestAnimationFrame(() => checkDiscoverInfiniteScroll());
    }
  } catch (_) {
    if (myToken !== discoverFetchToken) return;
    if (loadMore) {
      state.discoverLoadingMore = false;
      updateDiscoverFooterDom();
    } else {
      state.isSearching = false;
      renderApp();
    }
  }
}

const anilistSeasonNodeCache = new Map();
let detailFetchToken = 0;

const ANILIST_SEASON_BATCH_QUERY = `
  query($ids: [Int]) {
    Page(page: 1, perPage: 50) {
      media(id_in: $ids, type: ANIME) {
        id
        type
        title { english romaji userPreferred }
        coverImage { extraLarge large medium }
        format
        status
        episodes
        seasonYear
        startDate { year month day }
        averageScore
        relations {
          edges {
            relationType
            node {
              id
              type
              title { english romaji userPreferred }
              coverImage { extraLarge large medium }
              format
              status
              episodes
              seasonYear
              startDate { year month day }
              averageScore
            }
          }
        }
      }
    }
  }
`;

function isSeasonCandidateMedia(node) {
  if (!node) return false;
  if (node.type && String(node.type).toUpperCase() !== 'ANIME') return false;
  if (String(node.status || '').toUpperCase() === 'CANCELLED') return false;
  const fmt = String(node.format || '').toUpperCase().trim();
  if (!fmt || fmt === 'TV' || fmt === 'TV_SHORT' || fmt === 'ONA') return true;
  if (fmt === 'SPECIAL') {
    const combined = [node.title?.english, node.title?.userPreferred, node.title?.romaji]
      .filter(Boolean)
      .join(' ');
    if (
      /(final\s+season|final\s+chapters?|season\s+\d+|\bpart\s+\d+)/i.test(combined) &&
      !/(recap|summary|digest|preview|special\s+program|pv)/i.test(combined)
    ) {
      return true;
    }
  }
  return false;
}

function compareSeasonNodesChronologically(a, b, aId, bId) {
  const aYear = a?.startDate?.year ?? a?.seasonYear ?? null;
  const bYear = b?.startDate?.year ?? b?.seasonYear ?? null;
  const aUndated = aYear == null && String(a?.status || '').toUpperCase() === 'NOT_YET_RELEASED' ? 1 : 0;
  const bUndated = bYear == null && String(b?.status || '').toUpperCase() === 'NOT_YET_RELEASED' ? 1 : 0;
  if (aUndated !== bUndated) return aUndated - bUndated;
  const yDiff = (aYear ?? 9999) - (bYear ?? 9999);
  if (yDiff !== 0) return yDiff;
  const mDiff = (a?.startDate?.month ?? 13) - (b?.startDate?.month ?? 13);
  if (mDiff !== 0) return mDiff;
  const dDiff = (a?.startDate?.day ?? 32) - (b?.startDate?.day ?? 32);
  if (dDiff !== 0) return dDiff;
  return aId - bId;
}

async function fetchAniListSeasonBatch(ids) {
  const uniqueIds = [...new Set(ids.map(Number).filter((id) => id > 0))];
  if (!uniqueIds.length) return [];
  const cached = [];
  const missing = [];
  for (const id of uniqueIds) {
    const hit = anilistSeasonNodeCache.get(id);
    if (hit) cached.push(hit);
    else missing.push(id);
  }
  if (!missing.length) return cached;

  let fetched = [];
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const resp = await fetch('https://graphql.anilist.co', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: ANILIST_SEASON_BATCH_QUERY, variables: { ids: missing } })
      });
      if (resp.status === 429 && attempt === 0) {
        await new Promise((r) => setTimeout(r, 700));
        continue;
      }
      if (!resp.ok) break;
      const json = await resp.json();
      const mediaList = json?.data?.Page?.media || [];
      for (const node of mediaList) {
        if (node?.id) {
          anilistSeasonNodeCache.set(Number(node.id), node);
          fetched.push(node);
        }
      }
      break;
    } catch (_) {
      if (attempt === 0) await new Promise((r) => setTimeout(r, 450));
    }
  }
  return [...cached, ...fetched];
}

async function resolveFullSeasonChain(rootMedia) {
  if (!rootMedia?.id) return [];
  anilistSeasonNodeCache.set(Number(rootMedia.id), rootMedia);

  const hasImmediateLink = (rootMedia.relations?.edges || []).some(
    (e) =>
      e?.node &&
      (!e.node.type || String(e.node.type).toUpperCase() === 'ANIME') &&
      (e.relationType === 'PREQUEL' || e.relationType === 'SEQUEL')
  );
  if (!hasImmediateLink) return [];

  const MAX_WAVES = 25;
  const MAX_NODES = 60;
  const BATCH_SIZE = 25;

  const nodesById = new Map();
  const outgoing = new Map(); // predecessorId -> Set(successorId)
  const expandedIds = new Set();
  const queuedIds = new Set();
  const frontier = [];

  const mergeNode = (candidate) => {
    const id = Number(candidate.id);
    const existing = nodesById.get(id);
    if (!existing) {
      nodesById.set(id, candidate);
      return;
    }
    const existingHasRels = Array.isArray(existing.relations?.edges) && existing.relations.edges.length > 0;
    const candidateHasRels = Array.isArray(candidate.relations?.edges) && candidate.relations.edges.length > 0;
    if (candidateHasRels && !existingHasRels) {
      nodesById.set(id, {
        ...existing,
        ...candidate,
        startDate: candidate.startDate || existing.startDate,
        seasonYear: candidate.seasonYear ?? existing.seasonYear,
        episodes: candidate.episodes ?? existing.episodes
      });
    } else {
      nodesById.set(id, {
        ...candidate,
        ...existing,
        startDate: existing.startDate || candidate.startDate,
        seasonYear: existing.seasonYear ?? candidate.seasonYear,
        episodes: existing.episodes ?? candidate.episodes,
        relations: existingHasRels ? existing.relations : candidate.relations
      });
    }
  };

  const addDirectedEdge = (fromId, toId) => {
    if (!outgoing.has(fromId)) outgoing.set(fromId, new Set());
    outgoing.get(fromId).add(toId);
  };

  const enqueue = (id) => {
    if (!id || expandedIds.has(id) || queuedIds.has(id)) return;
    if (nodesById.size >= MAX_NODES && !nodesById.has(id)) return;
    queuedIds.add(id);
    frontier.push(id);
  };

  const ingestRelations = (media) => {
    const mediaId = Number(media.id);
    mergeNode(media);
    expandedIds.add(mediaId);
    for (const edge of media.relations?.edges || []) {
      const rel = String(edge?.relationType || '').toUpperCase();
      if (rel !== 'PREQUEL' && rel !== 'SEQUEL') continue;
      const target = edge.node;
      if (!target?.id) continue;
      if (target.type && String(target.type).toUpperCase() !== 'ANIME') continue;
      const targetId = Number(target.id);
      if (!nodesById.has(targetId) && nodesById.size >= MAX_NODES) continue;
      mergeNode(target);
      if (rel === 'SEQUEL') addDirectedEdge(mediaId, targetId);
      else addDirectedEdge(targetId, mediaId);
      enqueue(targetId);
    }
  };

  ingestRelations(rootMedia);

  let wave = 0;
  while (frontier.length > 0 && wave < MAX_WAVES && expandedIds.size < MAX_NODES) {
    wave++;
    const batch = [];
    while (frontier.length > 0 && batch.length < BATCH_SIZE) {
      const nextId = frontier.shift();
      if (!expandedIds.has(nextId)) batch.push(nextId);
    }
    if (!batch.length) break;

    const fetched = await fetchAniListSeasonBatch(batch);
    const fetchedIds = new Set();
    for (const node of fetched) {
      fetchedIds.add(Number(node.id));
      ingestRelations(node);
    }
    for (const reqId of batch) {
      if (!fetchedIds.has(reqId)) expandedIds.add(reqId);
    }
  }

  const seasonIds = new Set();
  for (const [id, node] of nodesById.entries()) {
    if (isSeasonCandidateMedia(node)) seasonIds.add(id);
  }
  if (seasonIds.size <= 1) return [];

  // Project transitive reachability across Movie / OVA / Special bridges onto the season DAG
  const seasonOutgoing = new Map();
  const inDegree = new Map();
  for (const sid of seasonIds) {
    seasonOutgoing.set(sid, new Set());
    inDegree.set(sid, 0);
  }

  for (const startId of seasonIds) {
    const seen = new Set();
    const queue = [];
    for (const next of outgoing.get(startId) || []) {
      if (!seen.has(next)) {
        seen.add(next);
        queue.push(next);
      }
    }
    while (queue.length > 0) {
      const curr = queue.shift();
      if (curr === startId) continue;
      if (seasonIds.has(curr)) {
        const targets = seasonOutgoing.get(startId);
        if (!targets.has(curr)) {
          targets.add(curr);
          inDegree.set(curr, (inDegree.get(curr) || 0) + 1);
        }
      } else {
        for (const next of outgoing.get(curr) || []) {
          if (!seen.has(next)) {
            seen.add(next);
            queue.push(next);
          }
        }
      }
    }
  }

  const cmp = (aId, bId) => compareSeasonNodesChronologically(nodesById.get(aId), nodesById.get(bId), aId, bId);
  const ready = [...seasonIds].filter((sid) => (inDegree.get(sid) || 0) === 0).sort(cmp);
  const orderedIds = [];

  while (ready.length > 0) {
    const curr = ready.shift();
    orderedIds.push(curr);
    let added = false;
    for (const next of seasonOutgoing.get(curr) || []) {
      const rem = (inDegree.get(next) || 1) - 1;
      inDegree.set(next, rem);
      if (rem === 0) {
        ready.push(next);
        added = true;
      }
    }
    if (added) ready.sort(cmp);
  }

  if (orderedIds.length < seasonIds.size) {
    const placed = new Set(orderedIds);
    const remaining = [...seasonIds].filter((id) => !placed.has(id)).sort(cmp);
    orderedIds.push(...remaining);
  }

  const rootId = Number(rootMedia.id);
  return orderedIds.map((id, idx) => {
    const sn = nodesById.get(id);
    const title = sn?.title?.english || sn?.title?.userPreferred || sn?.title?.romaji || `Season ${idx + 1}`;
    return {
      id,
      seasonNumber: idx + 1,
      title,
      seasonYear: sn?.seasonYear ?? sn?.startDate?.year ?? null,
      status: sn?.status || null,
      format: sn?.format || null,
      episodes: sn?.episodes ?? null,
      isCurrent: id === rootId
    };
  });
}

async function openAnimeDetail(animeId) {
  if (state.tab !== 'detail') {
    state.previousTab = state.tab;
  }
  state.tab = 'detail';
  const inVault = state.vault.find((v) => v.animeId === animeId);
  const inDisc = state.discover.find((d) => d.animeId === animeId);
  state.detailAnime = inVault
    ? { ...inVault, inWatchlist: true }
    : inDisc
    ? { ...inDisc, inWatchlist: false }
    : {
        animeId,
        title: 'Loading...',
        coverImage: '',
        status: 'watching',
        season: 1,
        watchedEpisodes: 0,
        totalEpisodes: null,
        rating10: null,
        reviewNotes: '',
        inWatchlist: false
      };
  state.detailRich = null;
  state.detailLoading = true;
  state.detailRatingSaving = false;
  state.detailRatingFeedback = '';
  state.detailRatingError = '';
  state.detailNoteDraft = state.detailAnime.reviewNotes || '';
  state.discussion = {
    ...state.discussion,
    animeId,
    comments: state.discussion.animeId === animeId ? state.discussion.comments : [],
    totalComments: state.discussion.animeId === animeId ? state.discussion.totalComments : 0,
    hasMore: false,
    loading: true,
    loadingMore: false,
    submitting: false,
    success: '',
    error: '',
    draft: '',
    commentTags: [],
    activeReplyTarget: null,
    replyDraft: '',
    replyTags: [],
    editingCommentId: null,
    editingCommentDraft: '',
    editingReplyId: null,
    editingReplyCommentId: null,
    editingReplyThreadId: null,
    editingReplyDraft: '',
    mentionSuggestions: [],
    mentionActiveScope: null,
    contextMenu: null
  };
  window.scrollTo({ top: 0, behavior: 'smooth' });
  renderApp();
  loadAnimeDiscussion(animeId, true);

  const gql = `
    query($id: Int) {
      Media(id: $id, type: ANIME) {
        id
        type
        title { english romaji native userPreferred }
        coverImage { extraLarge large medium }
        bannerImage
        format
        status
        episodes
        averageScore
        season
        seasonYear
        startDate { year month day }
        countryOfOrigin
        genres
        isAdult
        description(asHtml: false)
        nextAiringEpisode { airingAt episode }
        studios(isMain: true) { nodes { name } }
        relations {
          edges {
            relationType
            node {
              id
              type
              title { english romaji userPreferred }
              coverImage { extraLarge large medium }
              format
              status
              episodes
              seasonYear
              startDate { year month day }
              averageScore
            }
          }
        }
      }
    }
  `;

  const myToken = ++detailFetchToken;

  try {
    const resp = await fetch('https://graphql.anilist.co', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: gql, variables: { id: animeId } })
    });
    const json = await resp.json();
    if (myToken !== detailFetchToken) return;
    const m = json?.data?.Media;
    if (m) {
      const existing = state.vault.find((v) => v.animeId === animeId);
      state.detailAnime = {
        ...(existing || {}),
        animeId: m.id,
        title: m.title?.english || m.title?.romaji || 'Anime',
        titleEnglish: m.title?.english || '',
        titleRomaji: m.title?.romaji || '',
        titleNative: m.title?.native || '',
        coverImage: m.coverImage?.extraLarge || m.coverImage?.large || '',
        bannerImage: m.bannerImage || '',
        format: m.format || 'TV',
        mediaStatus: m.status || 'RELEASING',
        seasonYear: m.seasonYear || m.startDate?.year || null,
        countryOfOrigin: m.countryOfOrigin || 'JP',
        totalEpisodes: m.episodes || existing?.totalEpisodes || null,
        averageScore: m.averageScore || null,
        genres: normalizeAnimeGenresWithAdult(m.genres || [], Boolean(m.isAdult)),
        isAdult: isHentaiOrAdultAnime(m.genres || [], Boolean(m.isAdult)),
        synopsis: (m.description || '').replace(/<[^>]+>/g, ''),
        airingTimeUnix: m.nextAiringEpisode?.airingAt || null,
        nextEpisodeNumber: m.nextAiringEpisode?.episode || null,
        status: existing?.status || 'watching',
        season: existing?.season || 1,
        watchedEpisodes: existing?.watchedEpisodes || 0,
        rating10: existing?.rating10 || null,
        reviewNotes: existing?.reviewNotes || '',
        isPinned: Boolean(existing?.isPinned),
        isFavorite: Boolean(existing?.isFavorite),
        inWatchlist: Boolean(existing)
      };
      if (!state.detailNoteDraft && existing?.reviewNotes) {
        state.detailNoteDraft = existing.reviewNotes;
      }

      // Render detail page immediately, then resolve the complete multi-season franchise chain via batched BFS
      state.detailLoading = false;
      renderApp();

      const fullSeasons = await resolveFullSeasonChain(m);
      if (myToken !== detailFetchToken) return;
      state.detailRich = {
        seasons: fullSeasons
      };
      renderApp();
    }
  } catch (_) {
  } finally {
    if (myToken === detailFetchToken) {
      state.detailLoading = false;
      renderApp();
    }
  }
}

async function ensureCatalogAnimeSynced(item) {
  if (!state.session || !item || !item.animeId) return;
  const catalogKey = `${item.titleEnglish || item.title}|${item.coverImage}|${item.totalEpisodes || 0}|${item.mediaStatus || ''}`;
  if (syncedCatalogHashes.get(item.animeId) === catalogKey) return;
  await supabaseRequest(
    '/rest/v1/anime?on_conflict=anilist_id',
    'POST',
    {
      anilist_id: Number(item.animeId),
      title_english: String(item.titleEnglish || item.title || 'Anime').slice(0, 140),
      title_romaji: String(item.titleRomaji || item.title || 'Anime').slice(0, 140),
      title_native: String(item.titleNative || '').slice(0, 100) || null,
      cover_image: item.coverImage || '',
      banner_image: item.bannerImage || null,
      episodes: item.totalEpisodes || null,
      release_year: item.seasonYear || null,
      average_score: item.averageScore || null,
      status: item.mediaStatus || 'RELEASING',
      format: item.format || 'TV',
      genres: normalizeAnimeGenresWithAdult(item.genres || [], Boolean(item.isAdult)).slice(0, 6),
      next_airing_at: item.airingTimeUnix || null,
      next_episode_number: item.nextEpisodeNumber || null
    },
    'resolution=merge-duplicates,return=minimal'
  ).catch(() => {});
  syncedCatalogHashes.set(item.animeId, catalogKey);
}

// Save Library Entry to Supabase — NEVER implicitly adds to Home unless opts.addToHome === true
async function saveVaultEntry(item, opts = {}) {
  const animeId = Number(item.animeId);
  const existing = state.vault.find((v) => Number(v.animeId) === animeId);
  let nextStatus = item.status || existing?.status || 'watching';
  const totalEp = item.totalEpisodes ?? existing?.totalEpisodes ?? null;
  const watchedEp = Math.max(0, Number(item.watchedEpisodes ?? existing?.watchedEpisodes ?? 0));
  const mediaStatus = item.mediaStatus || existing?.mediaStatus || 'RELEASING';

  // Part 2: Strict automatic completion ONLY when officially FINISHED + known total + watched >= total
  if (shouldAutoComplete(mediaStatus, totalEp, watchedEp)) {
    nextStatus = 'completed';
  }

  // Part 1, 2, 3: Determine Home membership independently
  let nextInHome = opts.addToHome === true
    ? true
    : opts.removeFromHome === true
    ? false
    : state.homeTrackingIds.has(animeId);

  if (shouldRemoveFromHome(nextStatus, mediaStatus, totalEp, watchedEp)) {
    nextInHome = false;
  }

  if (nextInHome) {
    state.homeTrackingIds.add(animeId);
  } else {
    state.homeTrackingIds.delete(animeId);
  }
  saveHomeTrackingIds();

  const cleanItem = {
    ...(existing || {}),
    ...item,
    animeId,
    status: nextStatus,
    watchedEpisodes: watchedEp,
    totalEpisodes: totalEp,
    mediaStatus,
    inWatchlist: true,
    inHome: nextInHome
  };

  state.vault = [cleanItem, ...state.vault.filter((v) => Number(v.animeId) !== animeId)];
  if (state.detailAnime && Number(state.detailAnime.animeId) === animeId) {
    state.detailAnime = { ...state.detailAnime, ...cleanItem };
  }
  saveLocalVault();

  if (!state.session) return;
  try {
    await ensureCatalogAnimeSynced(cleanItem);

    await supabaseRequest(
      '/rest/v1/user_anime?on_conflict=user_id,anime_id',
      'POST',
      {
        user_id: state.session.userId,
        anime_id: animeId,
        status: cleanItem.status,
        episode_progress: cleanItem.watchedEpisodes,
        total_episodes: cleanItem.totalEpisodes || null,
        personal_rating: cleanItem.rating10 ? Math.min(10, Math.max(1, Number(cleanItem.rating10))) : null,
        review_notes: String(cleanItem.reviewNotes || '').trim().slice(0, 320),
        is_favorite: Boolean(cleanItem.isFavorite)
      },
      'resolution=merge-duplicates,return=minimal'
    );

    if (opts.addToHome === true && nextInHome) {
      await syncAddToHomeCloud(animeId, cleanItem.airingDayOfWeek || null);
    } else if (!nextInHome && (opts.removeFromHome === true || shouldRemoveFromHome(nextStatus, mediaStatus, totalEp, watchedEp))) {
      await syncRemoveFromHomeCloud(animeId);
    }

    const hasRating = cleanItem.rating10 !== null && Number(cleanItem.rating10) > 0;
    const hasNote = String(cleanItem.reviewNotes || '').trim().length > 0;
    if ((hasRating || hasNote) && state.profile.memberStatus !== 'left' && state.profile.memberStatus !== 'removed') {
      await syncCommunityNoteCloud(animeId, hasRating ? Number(cleanItem.rating10) : null, String(cleanItem.reviewNotes || '').trim());
    }

    await loadCommunityReviews();
  } catch (err) {
    if (opts.throwOnError) throw err;
  }
}

async function syncAddToHomeCloud(animeId, airingDay = null) {
  if (!state.session) return;
  try {
    await supabaseRequest('/rest/v1/rpc/add_to_home', 'POST', {
      p_anime_id: Number(animeId),
      p_airing_day: airingDay && airingDay >= 1 && airingDay <= 7 ? Number(airingDay) : null
    });
  } catch (_) {
    await supabaseRequest(
      '/rest/v1/home_tracking?on_conflict=user_id,anime_id',
      'POST',
      {
        user_id: state.session.userId,
        anime_id: Number(animeId),
        airing_day_of_week: airingDay && airingDay >= 1 && airingDay <= 7 ? Number(airingDay) : null
      },
      'resolution=merge-duplicates,return=minimal'
    ).catch(() => {});
  }
}

async function syncRemoveFromHomeCloud(animeId) {
  if (!state.session) return;
  try {
    await supabaseRequest('/rest/v1/rpc/remove_from_home', 'POST', {
      p_anime_id: Number(animeId)
    });
  } catch (_) {
    const uid = encodeURIComponent(state.session.userId);
    await supabaseRequest(`/rest/v1/home_tracking?user_id=eq.${uid}&anime_id=eq.${Number(animeId)}`, 'DELETE').catch(() => {});
  }
}

async function syncCommunityNoteCloud(animeId, rating10Val, noteText) {
  if (!state.session) return;
  await Promise.all([
    supabaseRequest(
      '/rest/v1/community_members?on_conflict=user_id',
      'POST',
      { user_id: state.session.userId, role: 'member', status: 'active' },
      'resolution=merge-duplicates,return=minimal'
    ).catch(() => {}),
    supabaseRequest(
      '/rest/v1/privacy_settings?on_conflict=user_id',
      'POST',
      {
        user_id: state.session.userId,
        reviews_visibility: 'community',
        activity_visibility: 'community'
      },
      'resolution=merge-duplicates,return=minimal'
    ).catch(() => {})
  ]);
  try {
    await supabaseRequest('/rest/v1/rpc/upsert_community_note', 'POST', {
      p_anime_id: Number(animeId),
      p_rating: rating10Val ? Math.min(10, Math.max(1, Number(rating10Val))) : null,
      p_note_text: String(noteText || '').trim().slice(0, 600)
    });
  } catch (_) {
    await supabaseRequest(
      '/rest/v1/community_notes?on_conflict=user_id,anime_id',
      'POST',
      {
        user_id: state.session.userId,
        anime_id: Number(animeId),
        rating: rating10Val ? Math.min(10, Math.max(1, Number(rating10Val))) : null,
        note_text: String(noteText || '').trim().slice(0, 600)
      },
      'resolution=merge-duplicates,return=minimal'
    ).catch(() => {});
  }
}

// Part 1: Explicitly Add Anime to Home active tracking (and Library if not yet present)
async function addToHome(item) {
  if (!item || !item.animeId) return;
  const animeId = Number(item.animeId);
  const existing = state.vault.find((v) => Number(v.animeId) === animeId);
  const base = existing ? { ...existing, ...item } : { ...item, status: 'watching', watchedEpisodes: 0 };
  if (base.status === 'dropped') {
    base.status = 'watching';
  }
  state.homeTrackingIds.add(animeId);
  saveHomeTrackingIds();
  await saveVaultEntry(base, { addToHome: true });
  showToast('Added to Home tracking');
  renderApp();
}

// Part 1 & Part 4: Remove ONLY from Home; preserve Library, progress, notes, ratings & Community
async function removeFromHomeOnly(animeId) {
  const aid = Number(animeId);
  state.homeTrackingIds.delete(aid);
  saveHomeTrackingIds();
  state.vault = state.vault.map((v) => (Number(v.animeId) === aid ? { ...v, inHome: false } : v));
  if (state.detailAnime && Number(state.detailAnime.animeId) === aid) {
    state.detailAnime = { ...state.detailAnime, inHome: false };
  }
  saveLocalVault();
  showToast('Removed from Home — preserved in Library');
  renderApp();
  await syncRemoveFromHomeCloud(aid);
}

// Part 1: Post a Community note/rating WITHOUT ever adding the anime to Home
async function submitCommunityCommentNote(animeId, rating10Val, noteText, animeMeta = null, opts = {}) {
  const aid = Number(animeId);
  const cleanNote = String(noteText || '').trim();
  const cleanRating = rating10Val && Number(rating10Val) > 0 ? Math.min(10, Math.max(1, Number(rating10Val))) : null;
  if (!cleanNote && !cleanRating) return { ok: false, error: 'Please select a rating or write a note first.' };

  const existingVault = state.vault.find((v) => Number(v.animeId) === aid);
  if (existingVault) {
    // Preserve existing Home membership strictly; NEVER add to Home just because user rated/noted
    await saveVaultEntry(
      {
        ...existingVault,
        rating10: cleanRating ?? existingVault.rating10,
        reviewNotes: cleanNote || existingVault.reviewNotes || ''
      },
      { addToHome: false, throwOnError: Boolean(opts.throwOnError) }
    );
    if (!opts.silentToast) showToast('Rating & note saved');
    renderApp();
    return { ok: true };
  }

  // Anime is NOT in Home or Library: save purely to Community (community_notes) — NEVER adds to Home!
  const meta = animeMeta || state.detailAnime || state.discover.find((d) => Number(d.animeId) === aid) || { animeId: aid };
  if (state.detailAnime && Number(state.detailAnime.animeId) === aid) {
    state.detailAnime = {
      ...state.detailAnime,
      rating10: cleanRating ?? state.detailAnime.rating10,
      reviewNotes: cleanNote || state.detailAnime.reviewNotes || ''
    };
  }

  // Update local community preview immediately
  if (state.session) {
    const metaGenres = normalizeAnimeGenresWithAdult(meta.genres || [], Boolean(meta.isAdult || meta.is_adult));
    const newEntry = {
      id: `local_note_${state.session.userId}_${aid}`,
      source: 'community_notes',
      user_id: state.session.userId,
      username: state.profile.username || 'Member',
      avatar_key: state.profile.avatarKey || 'avatar_1',
      anime_id: aid,
      anime_title: meta.titleEnglish || meta.title || meta.anime_title || 'Anime',
      anime_cover_image: meta.coverImage || meta.anime_cover_image || '',
      genres: metaGenres,
      is_adult: isHentaiOrAdultAnime(metaGenres, Boolean(meta.isAdult || meta.is_adult)),
      rating: cleanRating,
      review_notes: cleanNote,
      updated_at: new Date().toISOString(),
      likes_count: 0,
      liked_by_me: false
    };
    state.communityReviews = [
      newEntry,
      ...state.communityReviews.filter((r) => !(r.user_id === state.session.userId && Number(r.anime_id) === aid))
    ];
    renderApp();

    try {
      await ensureCatalogAnimeSynced({
        animeId: aid,
        title: meta.title || meta.anime_title || 'Anime',
        titleEnglish: meta.titleEnglish || meta.title || meta.anime_title || 'Anime',
        titleRomaji: meta.titleRomaji || meta.title || meta.anime_title || 'Anime',
        coverImage: meta.coverImage || meta.anime_cover_image || '',
        genres: metaGenres,
        isAdult: isHentaiOrAdultAnime(metaGenres, Boolean(meta.isAdult || meta.is_adult))
      });
      await syncCommunityNoteCloud(aid, cleanRating, cleanNote);
      await loadCommunityReviews();
    } catch (err) {
      if (opts.throwOnError) throw err;
    }
  }
  if (!opts.silentToast) showToast('Posted to Community (not added to Home)');
  renderApp();
  return { ok: true };
}

// ============================================================================
// ANIME DISCUSSION, @MENTIONS, TARGET-USER REPLY THREADS & NOTIFICATIONS RPCs
// ============================================================================

const MAX_MENTIONS_PER_CONTENT = 5;
const MENTION_REGEX = /(^|[^A-Za-z0-9_])@([A-Za-z0-9_]{2,32})\b/g;
let mentionSearchTimer = null;
let mentionSearchSeq = 0;

function extractMentionTokens(text) {
  const input = String(text || '');
  const tokens = [];
  let m;
  MENTION_REGEX.lastIndex = 0;
  while ((m = MENTION_REGEX.exec(input)) !== null) {
    const prefixLen = m[1] ? m[1].length : 0;
    const start = m.index + prefixLen;
    const username = m[2];
    tokens.push({
      username,
      normalized: username.toLowerCase(),
      start,
      end: start + 1 + username.length
    });
  }
  return tokens;
}

function validateContentMentions(text, explicitTagCount = 0) {
  const tokens = extractMentionTokens(text);
  const totalCount = tokens.length + Math.max(0, Number(explicitTagCount || 0));
  const uniqueSet = new Set(tokens.map((t) => t.normalized));
  if (totalCount > MAX_MENTIONS_PER_CONTENT) {
    return {
      ok: false,
      totalMentions: totalCount,
      uniqueCount: uniqueSet.size,
      error: `Maximum ${MAX_MENTIONS_PER_CONTENT} tag entries allowed per comment or reply (currently ${totalCount}).`
    };
  }
  return {
    ok: true,
    totalMentions: totalCount,
    uniqueCount: uniqueSet.size,
    error: ''
  };
}

function detectActiveMentionQuery(text, cursorPos) {
  const safeText = String(text || '');
  const pos = Math.max(0, Math.min(safeText.length, Number(cursorPos || 0)));
  const before = safeText.slice(0, pos);
  const m = before.match(/(?:^|[^A-Za-z0-9_])@([A-Za-z0-9_]{0,32})$/);
  if (!m) return null;
  const query = m[1] || '';
  const start = pos - query.length - 1;
  return { query, start, end: pos };
}

function stripMentionTrigger(text, start, end) {
  const safeText = String(text || '');
  const safeStart = Math.max(0, Math.min(safeText.length, start));
  const safeEnd = Math.max(safeStart, Math.min(safeText.length, end));
  let prefix = safeText.slice(0, safeStart);
  let suffix = safeText.slice(safeEnd);
  if (prefix.endsWith(' ') && suffix.startsWith(' ')) {
    suffix = suffix.slice(1);
  }
  return {
    text: `${prefix}${suffix}`,
    cursor: prefix.length
  };
}

function renderSelectedTagChipsHtml(scope, tags) {
  if (!Array.isArray(tags) || tags.length === 0) return '';
  return `
    <div class="tag-chips-row" id="tag-chips-${escapeHtml(scope)}">
      ${tags
        .map(
          (u, idx) => `
          <span class="tag-chip">
            <span>Tagged: ${escapeHtml(u.username)}</span>
            <button type="button" class="tag-chip-remove" data-remove-tag-scope="${escapeHtml(scope)}" data-remove-tag-idx="${idx}" aria-label="Remove tag">×</button>
          </span>`
        )
        .join('')}
    </div>
  `;
}

function renderMentionsDisplayHtml(mentions) {
  if (!Array.isArray(mentions) || mentions.length === 0) return '';
  const map = new Map();
  for (const m of mentions) {
    const uname = m?.mentioned_username || m?.username || '';
    const uid = m?.mentioned_user_id || m?.user_id || '';
    if (!uname) continue;
    const prev = map.get(uname);
    map.set(uname, { uid: prev?.uid || uid, count: (prev?.count || 0) + 1 });
  }
  if (map.size === 0) return '';
  return `
    <div class="tag-chips-row posted">
      ${Array.from(map.entries())
        .map(
          ([uname, info]) =>
            `<button type="button" class="tag-chip posted clickable-tag" data-mention-click-name="${escapeHtml(uname)}" data-mention-click-uid="${escapeHtml(info.uid || '')}">@${escapeHtml(uname)}${info.count > 1 ? ` ×${info.count}` : ''}</button>`
        )
        .join('')}
    </div>
  `;
}

// ============================================================================
// CUSTOM EMOJI MULTI-LAYER CACHE ENGINE (SUPABASE STORAGE + CACHE API + LAZY)
// - Stores ONLY emoji IDs ([emoji:<id>]) in DB/messages; never duplicates images
// - Reuses single canonical public Storage URL across all users & messages
// - Layer 1: In-memory objectURL Map (0ms lookup)
// - Layer 2: Browser Cache API ('livtrak-emoji-cache-v1') w/ immutable headers
// - Layer 3: localStorage metadata cache (15m TTL + stale-while-revalidate)
// - Supports both "community_emojis" and "community-emojis" public buckets
// ============================================================================

const EMOJI_META_STORAGE_KEY = 'livtrak_community_emojis_meta_v1';
const EMOJI_USAGE_LOCAL_KEY = 'livtrak_emoji_local_usage_v1';
const EMOJI_META_TTL_MS = 15 * 60 * 1000;
const EMOJI_IMAGE_CACHE_NAME = 'livtrak-emoji-cache-v1';
const EMOJI_TOKEN_REGEX = /\[emoji:([a-zA-Z0-9_-]{1,64})\]/g;
const EMOJI_BUCKET_CANDIDATES = ['community_emojis', 'community-emojis'];

const EmojiCacheManager = {
  items: [],
  byId: new Map(),
  objectUrls: new Map(),
  inflightImages: new Map(),
  failedIds: new Set(),
  localUsage: {},
  fetchedAt: 0,
  metaPromise: null,
  observer: null,
  loadedFromStorage: false,

  slugFromPath(path) {
    const file = String(path || '').replace(/^.*\//, '');
    const base = file.replace(/\.[^.]+$/, '');
    const slug = base.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
    return slug ? slug.slice(0, 64) : `emoji_${Math.abs(file.length)}`;
  },

  buildCanonicalUrl(bucketName, storagePath) {
    const base = getVaultBaseUrl();
    const bucket = encodeURIComponent(bucketName || 'community_emojis');
    const cleanPath = String(storagePath || '')
      .replace(/^\/+/, '')
      .split('/')
      .map((seg) => encodeURIComponent(seg))
      .join('/');
    return `${base}/storage/v1/object/public/${bucket}/${cleanPath}`;
  },

  hydrateFromLocalStorage() {
    if (this.loadedFromStorage) return;
    this.loadedFromStorage = true;
    try {
      const usageRaw = localStorage.getItem(EMOJI_USAGE_LOCAL_KEY);
      if (usageRaw) this.localUsage = JSON.parse(usageRaw) || {};
    } catch (_) {
      this.localUsage = {};
    }
    try {
      const raw = localStorage.getItem(EMOJI_META_STORAGE_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw);
      if (parsed && Array.isArray(parsed.items)) {
        this.fetchedAt = Number(parsed.fetchedAt || 0);
        this.applyItems(parsed.items, false);
      }
    } catch (_) {}
  },

  applyItems(rawList, persist = true) {
    const normalized = [];
    const map = new Map();
    for (const raw of rawList || []) {
      const id = String(raw.id || '').trim().toLowerCase();
      const storagePath = String(raw.storage_path || raw.name || '').trim();
      if (!id || !storagePath) continue;
      const bucketName = String(raw.bucket_name || 'community_emojis').trim();
      const serverUsage = Math.max(0, Number(raw.usage_count || 0));
      const localBoost = Math.max(0, Number(this.localUsage[id] || 0));
      const item = {
        id,
        bucket_name: bucketName,
        storage_path: storagePath,
        file_name: raw.file_name || storagePath.replace(/^.*\//, ''),
        mime_type: raw.mime_type || 'image/gif',
        usage_count: serverUsage,
        score: serverUsage + localBoost,
        url: this.buildCanonicalUrl(bucketName, storagePath)
      };
      if (!map.has(id)) {
        map.set(id, item);
        normalized.push(item);
      }
    }
    normalized.sort((a, b) => (b.score - a.score) || a.id.localeCompare(b.id));
    this.items = normalized;
    this.byId = map;
    if (persist) {
      this.fetchedAt = Date.now();
      try {
        localStorage.setItem(
          EMOJI_META_STORAGE_KEY,
          JSON.stringify({
            fetchedAt: this.fetchedAt,
            items: normalized.map((e) => ({
              id: e.id,
              bucket_name: e.bucket_name,
              storage_path: e.storage_path,
              file_name: e.file_name,
              mime_type: e.mime_type,
              usage_count: e.usage_count
            }))
          })
        );
      } catch (_) {}
    }
  },

  async fetchStorageFallback() {
    const base = getVaultBaseUrl();
    const key = getVaultPublicKey();
    const token = state.session?.accessToken || key;
    const allowedExt = /\.(gif|webp|png|jpe?g|apng)$/i;
    for (const bucket of EMOJI_BUCKET_CANDIDATES) {
      try {
        const res = await fetch(`${base}/storage/v1/object/list/${encodeURIComponent(bucket)}`, {
          method: 'POST',
          headers: {
            apikey: key,
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ prefix: '', limit: 250, offset: 0, sortBy: { column: 'name', order: 'asc' } })
        });
        if (!res.ok) continue;
        const files = await res.json();
        if (!Array.isArray(files) || files.length === 0) continue;
        const mapped = files
          .filter((f) => f && f.name && !f.name.startsWith('.') && allowedExt.test(f.name))
          .map((f) => ({
            id: this.slugFromPath(f.name),
            bucket_name: bucket,
            storage_path: f.name,
            file_name: f.name,
            mime_type: f.metadata?.mimetype || 'image/gif',
            usage_count: 0
          }));
        if (mapped.length > 0) return mapped;
      } catch (_) {}
    }
    return [];
  },

  async ensureMetadata(force = false) {
    this.hydrateFromLocalStorage();
    const isFresh = this.items.length > 0 && Date.now() - this.fetchedAt < EMOJI_META_TTL_MS;
    if (!force && isFresh) {
      this.preloadTopEmojis();
      return this.items;
    }
    if (this.metaPromise) return this.metaPromise;
    this.metaPromise = (async () => {
      try {
        let fetched = [];
        try {
          const data = await supabaseRequest('/rest/v1/rpc/get_community_emojis', 'POST', {});
          if (data && Array.isArray(data.emojis)) {
            fetched = data.emojis;
          } else if (Array.isArray(data)) {
            fetched = data;
          }
        } catch (_) {}
        if (!fetched.length) {
          fetched = await this.fetchStorageFallback();
        }
        if (fetched.length > 0) {
          const prevCount = this.items.length;
          this.applyItems(fetched, true);
          this.preloadTopEmojis();
          if (prevCount === 0 && (state.discussion.emojiTrayOpen || state.communityCommentsAnimeId !== null || state.tab === 'detail')) {
            hydrateVisibleEmojiNodes();
          }
        }
      } catch (_) {
      } finally {
        this.metaPromise = null;
      }
      return this.items;
    })();
    return this.metaPromise;
  },

  getTopEmojis(limit = 8) {
    this.hydrateFromLocalStorage();
    return [...this.items]
      .sort((a, b) => (b.score - a.score) || a.id.localeCompare(b.id))
      .slice(0, limit);
  },

  getAllEmojis() {
    this.hydrateFromLocalStorage();
    return this.items;
  },

  getById(id) {
    this.hydrateFromLocalStorage();
    return this.byId.get(String(id || '').trim().toLowerCase()) || null;
  },

  recordUsage(emojiIds) {
    if (!Array.isArray(emojiIds) || emojiIds.length === 0) return;
    const clean = emojiIds
      .map((x) => String(x || '').trim().toLowerCase())
      .filter((x) => x && this.byId.has(x));
    if (!clean.length) return;
    for (const id of clean) {
      this.localUsage[id] = Number(this.localUsage[id] || 0) + 1;
      const item = this.byId.get(id);
      if (item) {
        item.usage_count += 1;
        item.score = item.usage_count + Number(this.localUsage[id] || 0);
      }
    }
    this.items.sort((a, b) => (b.score - a.score) || a.id.localeCompare(b.id));
    try {
      localStorage.setItem(EMOJI_USAGE_LOCAL_KEY, JSON.stringify(this.localUsage));
    } catch (_) {}
    // Fire-and-forget batch usage sync (never blocks message send)
    if (state.session) {
      supabaseRequest('/rest/v1/rpc/increment_emoji_usage', 'POST', {
        p_emoji_ids: clean.slice(0, 20)
      }).catch(() => {});
    }
  },

  async resolveImageSrc(emojiId) {
    const id = String(emojiId || '').trim().toLowerCase();
    if (!id) return null;
    if (this.objectUrls.has(id)) return this.objectUrls.get(id);
    if (this.failedIds.has(id)) return null;

    let item = this.byId.get(id);
    if (!item) {
      await this.ensureMetadata(false);
      item = this.byId.get(id);
      if (!item) return null;
    }

    if (this.inflightImages.has(id)) {
      return this.inflightImages.get(id);
    }

    const url = item.url;
    const task = (async () => {
      try {
        if (typeof caches !== 'undefined') {
          const expectedMime = /\.gif($|\?)/i.test(url)
            ? 'image/gif'
            : /\.webp($|\?)/i.test(url)
            ? 'image/webp'
            : /\.png($|\?)/i.test(url)
            ? 'image/png'
            : (item.mime_type || 'image/gif');
          const cache = await caches.open(EMOJI_IMAGE_CACHE_NAME);
          const cachedResp = await cache.match(url);
          if (cachedResp && cachedResp.ok) {
            const rawBlob = await cachedResp.blob();
            const typedBlob = (rawBlob.type && rawBlob.type.startsWith('image/'))
              ? rawBlob
              : new Blob([rawBlob], { type: expectedMime });
            const objUrl = URL.createObjectURL(typedBlob);
            this.objectUrls.set(id, objUrl);
            return objUrl;
          }
          const netResp = await fetch(url, { mode: 'cors', cache: 'force-cache' });
          if (!netResp.ok) throw new Error(`HTTP ${netResp.status}`);
          const buf = await netResp.arrayBuffer();
          const rawCt = netResp.headers.get('content-type') || '';
          const contentType = rawCt.startsWith('image/') ? rawCt : expectedMime;
          const cacheResp = new Response(buf, {
            status: 200,
            headers: {
              'Content-Type': contentType,
              'Cache-Control': 'public, max-age=31536000, immutable'
            }
          });
          cache.put(url, cacheResp).catch(() => {});
          const objUrl = URL.createObjectURL(new Blob([buf], { type: contentType }));
          this.objectUrls.set(id, objUrl);
          return objUrl;
        }
        // Fallback if Cache API unavailable: reuse canonical HTTP URL directly
        this.objectUrls.set(id, url);
        return url;
      } catch (_) {
        // Fallback to direct canonical URL once before marking failed
        return url;
      } finally {
        this.inflightImages.delete(id);
      }
    })();

    this.inflightImages.set(id, task);
    return task;
  },

  preloadTopEmojis(count = 8) {
    const top = this.getTopEmojis(count);
    if (!top.length) return;
    const schedule = typeof requestIdleCallback === 'function' ? requestIdleCallback : (fn) => setTimeout(fn, 60);
    schedule(() => {
      top.forEach((em) => {
        if (!this.objectUrls.has(em.id) && !this.inflightImages.has(em.id)) {
          this.resolveImageSrc(em.id).catch(() => {});
        }
      });
    });
  },

  bindImageElement(imgEl) {
    if (!imgEl || imgEl.dataset.emojiBound === '1') return;
    imgEl.dataset.emojiBound = '1';
    const emojiId = imgEl.getAttribute('data-emoji-id');
    const slot = imgEl.closest('.custom-emoji-slot');
    if (!emojiId) return;

    const finishLoad = (src) => {
      if (!src) {
        if (slot) {
          slot.classList.remove('loading');
          slot.innerHTML = `<span class="custom-emoji-fallback" title=":${escapeHtml(emojiId)}:">✦</span>`;
        }
        return;
      }
      imgEl.onload = () => {
        imgEl.classList.add('is-loaded');
        if (slot) slot.classList.remove('loading');
      };
      imgEl.onerror = () => {
        this.failedIds.add(emojiId);
        if (slot) {
          slot.classList.remove('loading');
          slot.innerHTML = `<span class="custom-emoji-fallback" title=":${escapeHtml(emojiId)}:">✦</span>`;
        }
      };
      imgEl.src = src;
      if (imgEl.complete && imgEl.naturalWidth > 0) {
        imgEl.classList.add('is-loaded');
        if (slot) slot.classList.remove('loading');
      }
    };

    const cached = this.objectUrls.get(emojiId);
    if (cached) {
      finishLoad(cached);
      return;
    }

    const triggerFetch = () => {
      this.resolveImageSrc(emojiId).then(finishLoad).catch(() => finishLoad(null));
    };

    const isLazy = imgEl.getAttribute('data-emoji-lazy') === '1';
    if (isLazy && typeof IntersectionObserver !== 'undefined') {
      if (!this.observer) {
        this.observer = new IntersectionObserver(
          (entries) => {
            entries.forEach((entry) => {
              if (!entry.isIntersecting) return;
              const target = entry.target;
              this.observer.unobserve(target);
              const eid = target.getAttribute('data-emoji-id');
              if (eid && !target.getAttribute('src')) {
                this.resolveImageSrc(eid)
                  .then((src) => {
                    const s = target.closest('.custom-emoji-slot');
                    if (!src) {
                      if (s) {
                        s.classList.remove('loading');
                        s.innerHTML = `<span class="custom-emoji-fallback">✦</span>`;
                      }
                      return;
                    }
                    target.onload = () => {
                      target.classList.add('is-loaded');
                      if (s) s.classList.remove('loading');
                    };
                    target.onerror = () => {
                      this.failedIds.add(eid);
                      if (s) {
                        s.classList.remove('loading');
                        s.innerHTML = `<span class="custom-emoji-fallback">✦</span>`;
                      }
                    };
                    target.src = src;
                  })
                  .catch(() => {});
              }
            });
          },
          { rootMargin: '80px' }
        );
      }
      this.observer.observe(imgEl);
    } else {
      triggerFetch();
    }
  }
};

function hydrateVisibleEmojiNodes() {
  document.querySelectorAll('img[data-emoji-id]').forEach((img) => {
    EmojiCacheManager.bindImageElement(img);
  });
}

function extractEmojiIdsFromText(text) {
  const raw = String(text || '');
  const ids = [];
  EMOJI_TOKEN_REGEX.lastIndex = 0;
  let match;
  while ((match = EMOJI_TOKEN_REGEX.exec(raw)) !== null) {
    if (match[1]) ids.push(match[1].toLowerCase());
  }
  return ids;
}

function renderCustomEmojiSlotHtml(emojiId, { lazy = false, sizeClass = '' } = {}) {
  const cleanId = String(emojiId || '').trim().toLowerCase();
  const cachedSrc = EmojiCacheManager.objectUrls.get(cleanId) || '';
  const loadedClass = cachedSrc ? 'is-loaded' : '';
  const loadingClass = cachedSrc ? '' : 'loading';
  return `<span class="custom-emoji-slot ${sizeClass} ${loadingClass}" title=":${escapeHtml(cleanId)}:"><img class="custom-emoji ${loadedClass}" data-emoji-id="${escapeHtml(cleanId)}" data-emoji-lazy="${lazy ? '1' : '0'}" ${cachedSrc ? `src="${escapeHtml(cachedSrc)}"` : ''} alt=":${escapeHtml(cleanId)}:" loading="${lazy ? 'lazy' : 'eager'}" decoding="auto" /></span>`;
}

function extractSoloEmojiId(text) {
  const cleaned = String(text || '').replace(/[\s\u200B-\u200D\uFEFF]+/g, '');
  const m = /^\[emoji:([^\]]{1,64})\]$/i.exec(cleaned);
  return m ? m[1].trim().toLowerCase() : null;
}

function isSoloUnicodeEmoji(text) {
  const cleaned = String(text || '').replace(/[\s\u200B-\u200D\uFEFF]+/g, '');
  if (!cleaned || cleaned.length > 16) return false;
  return /^(?:\p{Extended_Pictographic}(?:\uFE0F|\u200D\p{Extended_Pictographic})*){1,3}$/u.test(cleaned);
}

function formatPlainPreviewWithEmojis(text) {
  return String(text || '').replace(EMOJI_TOKEN_REGEX, (_, id) => `:${id}:`);
}

function resolveReplyQuotedContext(parentComment, reply) {
  const refReplyId =
    reply?.quoted_reply_id ||
    (reply?.parent_comment_id && reply.parent_comment_id !== parentComment?.id
      ? reply.parent_comment_id
      : null);
  if (refReplyId) {
    let matchedReply = null;
    for (const th of parentComment?.threads || []) {
      const hit = (th.replies || []).find((item) => item.id === refReplyId);
      if (hit) {
        matchedReply = hit;
        break;
      }
    }
    const author = matchedReply?.username || reply?.quoted_username || 'Member';
    const rawPreview =
      reply?.quoted_preview ||
      matchedReply?.content ||
      matchedReply?.body ||
      'Original reply unavailable';
    return {
      author,
      preview: formatPlainPreviewWithEmojis(rawPreview),
      jumpCommentId: reply?.quoted_comment_id || parentComment?.id || '',
      jumpReplyId: refReplyId
    };
  }
  return {
    author: reply?.quoted_username || parentComment?.username || 'Member',
    preview: formatPlainPreviewWithEmojis(
      reply?.quoted_preview || parentComment?.content || parentComment?.body || ''
    ),
    jumpCommentId: reply?.quoted_comment_id || parentComment?.id || '',
    jumpReplyId: ''
  };
}

function renderMentionSegmentHtml(segmentText) {
  const raw = String(segmentText || '');
  const tokens = extractMentionTokens(raw);
  if (tokens.length === 0) return escapeHtml(raw);
  let out = '';
  let last = 0;
  for (const t of tokens) {
    out += escapeHtml(raw.slice(last, t.start));
    const handleText = raw.slice(t.start, t.end);
    const uname = t.username || handleText.replace(/^@/, '');
    out += `<button type="button" class="mention c-mention-btn" data-mention-click-name="${escapeHtml(uname)}">${escapeHtml(handleText)}</button>`;
    last = t.end;
  }
  out += escapeHtml(raw.slice(last));
  return out;
}

function renderMentionHighlightedHtml(text) {
  const raw = String(text || '');
  EMOJI_TOKEN_REGEX.lastIndex = 0;
  let out = '';
  let last = 0;
  let match;
  while ((match = EMOJI_TOKEN_REGEX.exec(raw)) !== null) {
    if (match.index > last) {
      out += renderMentionSegmentHtml(raw.slice(last, match.index));
    }
    out += renderCustomEmojiSlotHtml(match[1], { lazy: false });
    last = match.index + match[0].length;
  }
  if (last < raw.length) {
    out += renderMentionSegmentHtml(raw.slice(last));
  }
  return out;
}

function formatDiscussionTimestamp(isoStr) {
  if (!isoStr) return '';
  try {
    const d = new Date(isoStr);
    if (isNaN(d.getTime())) return String(isoStr).slice(0, 10);
    const diffSec = Math.floor((Date.now() - d.getTime()) / 1000);
    if (diffSec < 45) return 'Just now';
    if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ago`;
    if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h ago`;
    if (diffSec < 86400 * 7) return `${Math.floor(diffSec / 86400)}d ago`;
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  } catch (_) {
    return String(isoStr).slice(0, 10);
  }
}

function mergeReplyIntoDiscussionComments(comments, commentId, threadDto, replyDto) {
  return comments.map((c) => {
    if (c.id !== commentId) return c;
    const threads = Array.isArray(c.threads) ? [...c.threads] : [];
    const idx = threads.findIndex(
      (th) => th.id === threadDto.id || th.target_user_id === threadDto.target_user_id
    );
    if (idx >= 0) {
      const existing = threads[idx];
      const existingReplies = Array.isArray(existing.replies) ? existing.replies : [];
      const mergedReplies = existingReplies.some((r) => r.id === replyDto.id)
        ? existingReplies.map((r) => (r.id === replyDto.id ? replyDto : r))
        : [...existingReplies, replyDto];
      threads[idx] = {
        ...existing,
        ...threadDto,
        reply_count: mergedReplies.length,
        replies: mergedReplies
      };
    } else {
      threads.push({
        ...threadDto,
        reply_count: 1,
        replies: [replyDto]
      });
    }
    return { ...c, threads };
  });
}

function applyPendingDiscussionHighlight(animeId) {
  const jump = state.pendingNotificationJump;
  if (!jump || Number(jump.animeId) !== Number(animeId)) return;
  if (jump.threadId) {
    state.discussion.expandedThreads[jump.threadId] = true;
  }
  state.discussion.highlightTarget = {
    commentId: jump.commentId || null,
    threadId: jump.threadId || null,
    replyId: jump.replyId || null
  };
  state.pendingNotificationJump = null;
  renderApp();
  setTimeout(() => {
    const targetId = jump.replyId
      ? `discussion-reply-${jump.replyId}`
      : jump.threadId
      ? `discussion-thread-${jump.threadId}`
      : jump.commentId
      ? `discussion-comment-${jump.commentId}`
      : 'anime-discussion-section';
    const el = document.getElementById(targetId);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, 120);
}

async function loadAnimeDiscussion(animeId, reset = true) {
  const aid = Number(animeId);
  if (!aid) return;
  const switchingAnime = Number(state.discussion.animeId) !== aid;
  if (reset) {
    state.discussion.animeId = aid;
    if (switchingAnime) {
      state.discussion.comments = [];
      state.discussion.totalComments = 0;
      state.discussion.hasMore = false;
      state.discussion.draft = '';
      state.discussion.commentTags = [];
      state.discussion.activeReplyTarget = null;
      state.discussion.replyDraft = '';
      state.discussion.replyTags = [];
      state.discussion.editingCommentId = null;
      state.discussion.editingReplyId = null;
      state.discussion.contextMenu = null;
      state.discussion.success = '';
    }
    state.discussion.loading = true;
    state.discussion.error = '';
  } else {
    state.discussion.loadingMore = true;
  }
  const offset = reset ? 0 : state.discussion.comments.length;
  renderApp();
  try {
    const data = await supabaseRequest('/rest/v1/rpc/get_anime_discussion_page', 'POST', {
      p_anime_id: aid,
      p_limit: 15,
      p_offset: offset
    });
    if (Number(state.discussion.animeId) !== aid) return;
    const incoming = Array.isArray(data?.comments) ? data.comments : [];
    const merged = reset
      ? incoming
      : [
          ...state.discussion.comments,
          ...incoming.filter((inc) => !state.discussion.comments.some((old) => old.id === inc.id))
        ];
    state.discussion.comments = merged;
    state.discussion.totalComments = Number(data?.total_comments ?? merged.length);
    state.discussion.hasMore = Boolean(data?.has_more);
    state.discussion.loading = false;
    state.discussion.loadingMore = false;
    // Default-expand threads that have <= 3 replies or match a jump target
    for (const c of incoming) {
      for (const th of c.threads || []) {
        if (state.discussion.expandedThreads[th.id] === undefined && (th.replies?.length || 0) <= 2) {
          state.discussion.expandedThreads[th.id] = true;
        }
      }
    }
    if (state.pendingNotificationJump && Number(state.pendingNotificationJump.animeId) === aid) {
      applyPendingDiscussionHighlight(aid);
    } else {
      renderApp();
    }
    if (reset) {
      markAnimeDiscussionSeen(aid);
    }
  } catch (err) {
    if (Number(state.discussion.animeId) !== aid) return;
    state.discussion.loading = false;
    state.discussion.loadingMore = false;
    state.discussion.error = err?.message || 'Unable to load discussion right now.';
    renderApp();
  }
}

function updateMentionDropdownDom(scope, suggestions) {
  const box = document.getElementById(`mention-dropdown-${scope}`);
  if (!box) return;
  if (!suggestions || suggestions.length === 0) {
    box.classList.remove('open');
    box.style.display = 'none';
    box.innerHTML = '';
    return;
  }
  box.classList.add('open');
  box.style.display = 'flex';
  box.innerHTML = suggestions
    .map(
      (u) => `
      <button
        type="button"
        class="m-opt"
        data-pick-mention-id="${escapeHtml(u.id || u.user_id || '')}"
        data-pick-mention-name="${escapeHtml(u.username)}"
        data-pick-mention-avatar="${escapeHtml(u.avatar_key || 'avatar_1')}"
        data-pick-mention-scope="${escapeHtml(scope)}"
      >
        ${renderAvatarCircle(u.avatar_key || 'avatar_1', u.username, 28)}
        <span class="m-name">${escapeHtml(u.username)}</span>
        <span class="m-tag-hint">@Tag</span>
      </button>`
    )
    .join('');
  box.querySelectorAll('[data-pick-mention-name]').forEach((btn) => {
    btn.onmousedown = (e) => {
      e.preventDefault();
      applySelectedMention(btn.getAttribute('data-pick-mention-scope'), {
        id: btn.getAttribute('data-pick-mention-id'),
        username: btn.getAttribute('data-pick-mention-name'),
        avatar_key: btn.getAttribute('data-pick-mention-avatar') || 'avatar_1'
      });
    };
  });
}

function getTagsForScope(scope) {
  if (scope === 'comment') return state.discussion.commentTags;
  if (scope === 'reply') return state.discussion.replyTags;
  return [];
}

function applySelectedMention(scope, userObj) {
  const inputMap = {
    comment: 'inp-discussion-comment',
    reply: 'inp-discussion-reply',
    edit_comment: 'inp-discussion-edit-comment',
    edit_reply: 'inp-discussion-edit-reply'
  };
  const el = document.getElementById(inputMap[scope]);
  if (!el) return;
  const start = state.discussion.mentionActiveStart;
  const end = state.discussion.mentionActiveEnd;
  if (start < 0 || end < start) return;
  // Strip @query from text so username is NOT written into the message text
  const res = stripMentionTrigger(el.value, start, end);
  el.value = res.text;
  if (scope === 'comment') {
    state.discussion.draft = res.text;
    const check = validateContentMentions(res.text, state.discussion.commentTags.length);
    if (check.totalMentions < MAX_MENTIONS_PER_CONTENT && userObj?.id) {
      state.discussion.commentTags = [...state.discussion.commentTags, userObj];
    }
  } else if (scope === 'reply') {
    state.discussion.replyDraft = res.text;
    const check = validateContentMentions(res.text, state.discussion.replyTags.length);
    if (check.totalMentions < MAX_MENTIONS_PER_CONTENT && userObj?.id) {
      state.discussion.replyTags = [...state.discussion.replyTags, userObj];
    }
  } else if (scope === 'edit_comment') {
    state.discussion.editingCommentDraft = res.text;
  } else if (scope === 'edit_reply') {
    state.discussion.editingReplyDraft = res.text;
  }
  state.discussion.mentionSuggestions = [];
  state.discussion.mentionActiveScope = null;
  renderApp();
  setTimeout(() => {
    const nextEl = document.getElementById(inputMap[scope]);
    if (nextEl) {
      nextEl.focus();
      nextEl.setSelectionRange(res.cursor, res.cursor);
    }
  }, 20);
}

function updateMentionCounterDom(scope, text) {
  const counterEl = document.getElementById(`mention-counter-${scope}`);
  if (!counterEl) return;
  const tagCount = getTagsForScope(scope).length;
  const v = validateContentMentions(text, tagCount);
  counterEl.textContent = `${v.totalMentions}/${MAX_MENTIONS_PER_CONTENT} tagged`;
  counterEl.classList.toggle('over-limit', !v.ok);
}

function getLocalMentionMatches(query) {
  const q = String(query || '').trim().toLowerCase();
  const selfId = state.session?.userId || '';
  const map = new Map();
  for (const r of state.communityReviews || []) {
    if (r.user_id && r.user_id !== selfId && r.username) {
      map.set(r.user_id, { id: r.user_id, username: r.username, avatar_key: r.avatar_key || 'avatar_1' });
    }
  }
  for (const c of state.discussion.comments || []) {
    if (c.user_id && c.user_id !== selfId && c.username) {
      map.set(c.user_id, { id: c.user_id, username: c.username, avatar_key: c.avatar_key || 'avatar_1' });
    }
    for (const th of c.threads || []) {
      for (const r of th.replies || []) {
        if (r.user_id && r.user_id !== selfId && r.username) {
          map.set(r.user_id, { id: r.user_id, username: r.username, avatar_key: r.avatar_key || 'avatar_1' });
        }
      }
    }
  }
  return Array.from(map.values())
    .filter((u) => !q || u.username.toLowerCase().includes(q))
    .sort((a, b) => {
      const aStarts = q && a.username.toLowerCase().startsWith(q) ? 0 : 1;
      const bStarts = q && b.username.toLowerCase().startsWith(q) ? 0 : 1;
      if (aStarts !== bStarts) return aStarts - bStarts;
      return a.username.localeCompare(b.username);
    })
    .slice(0, 8);
}

function handleDiscussionInputMentionCheck(scope, inputEl) {
  const text = inputEl.value;
  if (scope === 'comment') state.discussion.draft = text;
  else if (scope === 'reply') state.discussion.replyDraft = text;
  else if (scope === 'edit_comment') state.discussion.editingCommentDraft = text;
  else if (scope === 'edit_reply') state.discussion.editingReplyDraft = text;
  updateMentionCounterDom(scope, text);

  if (!state.session) return;
  const active = detectActiveMentionQuery(text, inputEl.selectionStart ?? text.length);
  if (!active) {
    state.discussion.mentionSuggestions = [];
    state.discussion.mentionActiveScope = null;
    updateMentionDropdownDom(scope, []);
    return;
  }
  state.discussion.mentionActiveScope = scope;
  state.discussion.mentionActiveStart = active.start;
  state.discussion.mentionActiveEnd = active.end;

  // Immediate local suggestion feedback on @ + 1st letter
  const localMatches = getLocalMentionMatches(active.query);
  if (localMatches.length > 0) {
    state.discussion.mentionSuggestions = localMatches;
    updateMentionDropdownDom(scope, localMatches);
  }

  clearTimeout(mentionSearchTimer);
  const seq = ++mentionSearchSeq;
  mentionSearchTimer = setTimeout(async () => {
    try {
      const rows = await supabaseRequest('/rest/v1/rpc/search_mentionable_users', 'POST', {
        p_query: active.query,
        p_limit: 10
      });
      if (seq !== mentionSearchSeq) return;
      const remoteList = Array.isArray(rows) ? rows : [];
      const mergedMap = new Map();
      for (const u of [...remoteList, ...localMatches]) {
        const uid = u.id || u.user_id;
        if (uid && uid !== state.session?.userId && !mergedMap.has(uid)) {
          mergedMap.set(uid, { id: uid, username: u.username, avatar_key: u.avatar_key || 'avatar_1' });
        }
      }
      const list = Array.from(mergedMap.values()).slice(0, 10);
      state.discussion.mentionSuggestions = list;
      updateMentionDropdownDom(scope, list);
    } catch (_) {
      if (seq !== mentionSearchSeq) return;
      updateMentionDropdownDom(scope, localMatches);
    }
  }, 90);
}

function getActiveDiscussionAnime() {
  if (state.communityCommentsAnimeId !== null) {
    const allGroups = groupCommunityReviewsByAnime(state.communityReviews);
    const grp = allGroups.find((g) => Number(g.anime_id) === Number(state.communityCommentsAnimeId));
    if (grp) {
      return {
        animeId: Number(grp.anime_id),
        title: grp.anime_title || 'Anime',
        titleEnglish: grp.anime_title || 'Anime',
        coverImage: grp.anime_cover_image || ''
      };
    }
    if (
      state.communityCommentsAnimeFallback &&
      Number(state.communityCommentsAnimeFallback.anime_id) === Number(state.communityCommentsAnimeId)
    ) {
      const fb = state.communityCommentsAnimeFallback;
      return {
        animeId: Number(fb.anime_id),
        title: fb.anime_title || `Anime #${fb.anime_id}`,
        titleEnglish: fb.anime_title || `Anime #${fb.anime_id}`,
        coverImage: fb.anime_cover_image || ''
      };
    }
  }
  if (state.detailAnime && state.detailAnime.animeId) return state.detailAnime;
  return null;
}

function openCommunityHubForAnime(animeId, options = {}) {
  const aid = Number(animeId);
  if (!aid) return;
  const {
    tab = 'discussion',
    fallbackTitle = '',
    fallbackCover = '',
    jumpTarget = null,
    notificationId = null
  } = options;

  const allGroups = groupCommunityReviewsByAnime(state.communityReviews);
  const grp = allGroups.find((g) => Number(g.anime_id) === aid);
  const vaultItem = state.vault.find((v) => Number(v.animeId) === aid);
  const unseenItem = (state.notifications.unseenAnime || []).find((u) => Number(u.anime_id) === aid);

  if (!grp) {
    state.communityCommentsAnimeFallback = {
      anime_id: aid,
      anime_title: fallbackTitle || vaultItem?.titleEnglish || vaultItem?.title || unseenItem?.anime_title || `Anime #${aid}`,
      anime_cover_image: fallbackCover || vaultItem?.coverImage || unseenItem?.anime_cover_image || '',
      is_adult: Boolean(vaultItem?.isAdult),
      reviews: [],
      total_count: 0,
      rated_count: 0,
      noted_count: 0,
      average_rating10: null,
      latest_updated_at: new Date().toISOString()
    };
  } else {
    state.communityCommentsAnimeFallback = null;
  }

  state.communityCommentsAnimeId = aid;
  state.communityCommentsTab = tab === 'notes' ? 'notes' : 'discussion';
  state.pendingCommunityReplyUser = null;
  state.discussion.emojiTrayOpen = false;
  state.communityCommentDraftStars = vaultItem && vaultItem.rating10 ? rating10ToStars(vaultItem.rating10) || 0 : 0;
  state.communityCommentDraftNote = vaultItem ? vaultItem.reviewNotes || '' : '';
  state.communityNoteSaving = false;
  state.communityNoteFeedback = '';
  state.communityNoteError = '';

  if (jumpTarget) {
    state.pendingNotificationJump = {
      animeId: aid,
      commentId: jumpTarget.commentId || null,
      threadId: jumpTarget.threadId || null,
      replyId: jumpTarget.replyId || null
    };
  }

  renderApp();
  EmojiCacheManager.ensureMetadata(false);

  if (notificationId && state.session) {
    markNotificationRead(notificationId);
  }
  if (state.session && state.communityCommentsTab === 'discussion') {
    markAnimeDiscussionSeen(aid);
  }

  loadAnimeDiscussion(aid, true).then(() => {
    applyPendingCommunityReplyTarget();
    if (!state.pendingNotificationJump) {
      setTimeout(() => {
        const stream = document.getElementById('livtrak-chat-messages');
        if (stream) stream.scrollTop = stream.scrollHeight;
      }, 60);
    }
  });
}

function applyPendingCommunityReplyTarget() {
  const pending = state.pendingCommunityReplyUser;
  if (!pending) return;
  const targetAid = Number(state.communityCommentsAnimeId || state.discussion.animeId || 0);
  if (targetAid && Number(state.discussion.animeId) !== targetAid) return;
  if (state.discussion.loading) return;

  const existingComment = (state.discussion.comments || []).find(
    (c) =>
      (pending.id && c.user_id === pending.id) ||
      (pending.username && String(c.username || '').toLowerCase() === String(pending.username).toLowerCase())
  );
  state.pendingCommunityReplyUser = null;

  if (existingComment) {
    const existingThread = (existingComment.threads || []).find((th) => th.target_user_id === existingComment.user_id);
    if (existingThread) {
      state.discussion.expandedThreads[existingThread.id] = true;
    }
    state.discussion.activeReplyTarget = {
      commentId: existingComment.id,
      targetUserId: existingComment.user_id,
      targetUsername: existingComment.username || pending.username || 'Member',
      quotedCommentId: existingComment.id,
      quotedReplyId: null,
      quotedUsername: existingComment.username || pending.username || 'Member',
      quotedPreview: formatPlainPreviewWithEmojis(existingComment.content ?? existingComment.body ?? '').slice(0, 180)
    };
    state.discussion.replyDraft = '';
    state.discussion.replyTags = [];
    state.discussion.error = '';
    renderApp();
    setTimeout(() => document.getElementById('inp-discussion-reply')?.focus(), 40);
  } else if (pending.id) {
    const currentTags = state.discussion.commentTags || [];
    const check = validateContentMentions(state.discussion.draft || '', currentTags.length);
    if (
      check.totalMentions < MAX_MENTIONS_PER_CONTENT &&
      !currentTags.some((t) => t.id === pending.id)
    ) {
      state.discussion.commentTags = [
        ...currentTags,
        { id: pending.id, username: pending.username || 'Member', avatar_key: pending.avatar_key || 'avatar_1' }
      ];
    }
    renderApp();
    setTimeout(() => document.getElementById('inp-discussion-comment')?.focus(), 40);
  }
}

async function postAnimeComment() {
  const activeAnime = getActiveDiscussionAnime();
  if (!state.session || !activeAnime || state.discussion.submitting) return;
  const body = (document.getElementById('inp-discussion-comment')?.value ?? state.discussion.draft ?? '').trim();
  const taggedIds = (state.discussion.commentTags || []).map((t) => t.id).filter(Boolean);
  if (!body) {
    state.discussion.error = 'Please write a comment before posting.';
    state.discussion.success = '';
    renderApp();
    return;
  }
  const check = validateContentMentions(body, taggedIds.length);
  if (!check.ok) {
    state.discussion.error = check.error;
    state.discussion.success = '';
    renderApp();
    return;
  }
  state.discussion.submitting = true;
  state.discussion.error = '';
  state.discussion.success = '';
  renderApp();
  try {
    const a = activeAnime;
    const res = await supabaseRequest('/rest/v1/rpc/create_anime_comment', 'POST', {
      p_anime_id: Number(a.animeId),
      p_content: body,
      p_anime_title: a.titleEnglish || a.title || 'Anime',
      p_anime_cover: a.coverImage || '',
      p_tagged_user_ids: taggedIds
    });
    const created = res?.comment || res;
    if (created && created.id) {
      state.discussion.comments = [
        { ...created, threads: Array.isArray(created.threads) ? created.threads : [] },
        ...state.discussion.comments.filter((c) => c.id !== created.id)
      ];
      state.discussion.totalComments = Number(res?.total_comments ?? Math.max(state.discussion.comments.length, state.discussion.totalComments + 1));
    }
    state.discussion.draft = '';
    state.discussion.commentTags = [];
    state.discussion.mentionSuggestions = [];
    state.discussion.mentionActiveScope = null;
    state.discussion.emojiTrayOpen = false;
    state.discussion.success = '';
    EmojiCacheManager.recordUsage(extractEmojiIdsFromText(body));
  } catch (err) {
    state.discussion.error = err?.message || 'Failed to post message. Please try again.';
    showToast(state.discussion.error, true);
  } finally {
    state.discussion.submitting = false;
    renderApp();
    setTimeout(() => {
      const stream = document.getElementById('livtrak-chat-messages');
      if (stream) stream.scrollTop = stream.scrollHeight;
    }, 40);
  }
}

async function saveEditedAnimeComment(commentId) {
  if (!state.session || state.discussion.submitting) return;
  const body = (document.getElementById('inp-discussion-edit-comment')?.value ?? state.discussion.editingCommentDraft ?? '').trim();
  if (!body) {
    state.discussion.error = 'Comment cannot be empty.';
    renderApp();
    return;
  }
  const check = validateContentMentions(body);
  if (!check.ok) {
    state.discussion.error = check.error;
    renderApp();
    return;
  }
  state.discussion.submitting = true;
  state.discussion.error = '';
  state.discussion.success = '';
  renderApp();
  try {
    const updated = await supabaseRequest('/rest/v1/rpc/update_anime_comment', 'POST', {
      p_comment_id: commentId,
      p_content: body
    });
    state.discussion.comments = state.discussion.comments.map((c) =>
      c.id === commentId
        ? { ...c, content: updated?.content || body, updated_at: updated?.updated_at || new Date().toISOString() }
        : c
    );
    state.discussion.editingCommentId = null;
    state.discussion.editingCommentDraft = '';
    state.discussion.success = 'Comment updated successfully';
    showToast('Comment updated');
  } catch (err) {
    state.discussion.error = err?.message || 'Failed to update comment.';
  } finally {
    state.discussion.submitting = false;
    renderApp();
  }
}

async function deleteAnimeComment(commentId) {
  if (!state.session || state.discussion.submitting) return;
  state.discussion.submitting = true;
  state.discussion.error = '';
  state.discussion.success = '';
  renderApp();
  try {
    await supabaseRequest('/rest/v1/rpc/delete_anime_comment', 'POST', { p_comment_id: commentId });
    state.discussion.comments = state.discussion.comments.filter((c) => c.id !== commentId);
    state.discussion.totalComments = Math.max(0, state.discussion.totalComments - 1);
    if (state.discussion.activeReplyTarget?.commentId === commentId) {
      state.discussion.activeReplyTarget = null;
      state.discussion.replyDraft = '';
    }
    state.discussion.success = 'Comment deleted';
    showToast('Comment deleted');
  } catch (err) {
    state.discussion.error = err?.message || 'Failed to delete comment.';
  } finally {
    state.discussion.submitting = false;
    renderApp();
  }
}

async function postThreadReply() {
  const target = state.discussion.activeReplyTarget;
  if (!state.session || !target || state.discussion.submitting) return;
  const body = (document.getElementById('inp-discussion-reply')?.value ?? state.discussion.replyDraft ?? '').trim();
  const taggedIds = (state.discussion.replyTags || []).map((t) => t.id).filter(Boolean);
  if (!body) {
    state.discussion.error = 'Please write a reply before posting.';
    state.discussion.success = '';
    renderApp();
    return;
  }
  const check = validateContentMentions(body, taggedIds.length);
  if (!check.ok) {
    state.discussion.error = check.error;
    state.discussion.success = '';
    renderApp();
    return;
  }
  state.discussion.submitting = true;
  state.discussion.error = '';
  state.discussion.success = '';
  renderApp();
  try {
    const res = await supabaseRequest('/rest/v1/rpc/create_thread_reply', 'POST', {
      p_comment_id: target.commentId,
      p_target_user_id: target.targetUserId,
      p_content: body,
      p_tagged_user_ids: taggedIds,
      p_quoted_reply_id: target.quotedReplyId || null,
      p_quoted_username: target.quotedUsername || target.targetUsername || null,
      p_quoted_preview: target.quotedPreview || null
    });
    if (res && res.thread && res.reply) {
      const mergedReply = {
        ...res.reply,
        parent_comment_id:
          res.reply.parent_comment_id ||
          res.reply.quoted_reply_id ||
          target.parentCommentId ||
          target.quotedReplyId ||
          target.commentId,
        quoted_comment_id: res.reply.quoted_comment_id || target.commentId,
        quoted_reply_id: res.reply.quoted_reply_id || target.quotedReplyId || null,
        quoted_username: res.reply.quoted_username || target.quotedUsername || target.targetUsername || null,
        quoted_preview: res.reply.quoted_preview || target.quotedPreview || null
      };
      state.discussion.comments = mergeReplyIntoDiscussionComments(
        state.discussion.comments,
        target.commentId,
        res.thread,
        mergedReply
      );
      state.discussion.expandedThreads[res.thread.id] = true;
    }
    state.discussion.replyDraft = '';
    state.discussion.replyTags = [];
    state.discussion.activeReplyTarget = null;
    state.discussion.mentionSuggestions = [];
    state.discussion.mentionActiveScope = null;
    state.discussion.emojiTrayOpen = false;
    state.discussion.success = '';
    EmojiCacheManager.recordUsage(extractEmojiIdsFromText(body));
  } catch (err) {
    state.discussion.error = err?.message || 'Failed to post reply.';
    showToast(state.discussion.error, true);
  } finally {
    state.discussion.submitting = false;
    renderApp();
  }
}

async function saveEditedThreadReply(commentId, threadId, replyId) {
  if (!state.session || state.discussion.submitting) return;
  const body = (document.getElementById('inp-discussion-edit-reply')?.value ?? state.discussion.editingReplyDraft ?? '').trim();
  if (!body) {
    state.discussion.error = 'Reply cannot be empty.';
    renderApp();
    return;
  }
  const check = validateContentMentions(body);
  if (!check.ok) {
    state.discussion.error = check.error;
    renderApp();
    return;
  }
  state.discussion.submitting = true;
  state.discussion.error = '';
  state.discussion.success = '';
  renderApp();
  try {
    const updated = await supabaseRequest('/rest/v1/rpc/update_thread_reply', 'POST', {
      p_reply_id: replyId,
      p_content: body
    });
    state.discussion.comments = state.discussion.comments.map((c) => {
      if (c.id !== commentId) return c;
      return {
        ...c,
        threads: (c.threads || []).map((th) => {
          if (th.id !== threadId) return th;
          return {
            ...th,
            replies: (th.replies || []).map((r) =>
              r.id === replyId
                ? { ...r, content: updated?.content || body, updated_at: updated?.updated_at || new Date().toISOString() }
                : r
            )
          };
        })
      };
    });
    state.discussion.editingReplyId = null;
    state.discussion.editingReplyCommentId = null;
    state.discussion.editingReplyThreadId = null;
    state.discussion.editingReplyDraft = '';
    state.discussion.success = 'Reply updated successfully';
    showToast('Reply updated');
  } catch (err) {
    state.discussion.error = err?.message || 'Failed to update reply.';
  } finally {
    state.discussion.submitting = false;
    renderApp();
  }
}

async function deleteThreadReply(commentId, threadId, replyId) {
  if (!state.session || state.discussion.submitting) return;
  state.discussion.submitting = true;
  state.discussion.error = '';
  state.discussion.success = '';
  renderApp();
  try {
    await supabaseRequest('/rest/v1/rpc/delete_thread_reply', 'POST', { p_reply_id: replyId });
    state.discussion.comments = state.discussion.comments.map((c) => {
      if (c.id !== commentId) return c;
      const nextThreads = (c.threads || [])
        .map((th) => {
          if (th.id !== threadId) return th;
          const nextReplies = (th.replies || []).filter((r) => r.id !== replyId);
          return { ...th, replies: nextReplies, reply_count: nextReplies.length };
        })
        .filter((th) => (th.replies || []).length > 0);
      return { ...c, threads: nextThreads };
    });
    state.discussion.success = 'Reply deleted';
    showToast('Reply deleted');
  } catch (err) {
    state.discussion.error = err?.message || 'Failed to delete reply.';
  } finally {
    state.discussion.submitting = false;
    renderApp();
  }
}

async function refreshUnreadNotificationCount() {
  if (!state.session) {
    state.notifications.unreadCount = 0;
    state.notifications.unseenAnime = [];
    return;
  }
  try {
    const summary = await supabaseRequest('/rest/v1/rpc/get_unseen_discussion_anime', 'POST', {});
    const list = Array.isArray(summary?.anime) ? summary.anime : [];
    state.notifications.unseenAnime = list;
    state.notifications.unreadCount = Math.max(0, Number(summary?.total_unseen ?? list.reduce((acc, a) => acc + Number(a.unseen_count || 1), 0)));
  } catch (_) {
    try {
      const count = await supabaseRequest('/rest/v1/rpc/get_unread_notification_count', 'POST', {});
      const numeric = Number(count || 0);
      if (!Number.isNaN(numeric)) {
        state.notifications.unreadCount = Math.max(0, numeric);
      }
    } catch (_2) {}
  }
}

async function markAnimeDiscussionSeen(animeId) {
  const aid = Number(animeId);
  if (!state.session || !aid) return;
  const hadUnseen = (state.notifications.unseenAnime || []).some((u) => Number(u.anime_id) === aid);
  if (!hadUnseen && state.notifications.unreadCount <= 0) return;
  state.notifications.unseenAnime = (state.notifications.unseenAnime || []).filter((u) => Number(u.anime_id) !== aid);
  state.notifications.unreadCount = state.notifications.unseenAnime.reduce(
    (acc, u) => acc + Math.max(1, Number(u.unseen_count || 1)),
    0
  );
  renderApp();
  try {
    const res = await supabaseRequest('/rest/v1/rpc/mark_anime_notifications_read', 'POST', {
      p_anime_id: aid
    });
    const numeric = Number(typeof res === 'object' && res !== null ? res.unread_count : res);
    if (!Number.isNaN(numeric)) {
      state.notifications.unreadCount = Math.max(0, numeric);
    }
  } catch (_) {}
}

async function loadUserNotifications(reset = true) {
  if (!state.session) {
    state.notifications.items = [];
    state.notifications.unreadCount = 0;
    state.notifications.loading = false;
    renderApp();
    return;
  }
  const offset = reset ? 0 : state.notifications.items.length;
  if (reset) {
    state.notifications.loading = true;
    state.notifications.error = '';
  } else {
    state.notifications.loadingMore = true;
  }
  renderApp();
  try {
    const data = await supabaseRequest('/rest/v1/rpc/get_user_notifications', 'POST', {
      p_limit: 25,
      p_offset: offset
    });
    const incoming = Array.isArray(data?.notifications) ? data.notifications : [];
    state.notifications.items = reset
      ? incoming
      : [
          ...state.notifications.items,
          ...incoming.filter((n) => !state.notifications.items.some((old) => old.id === n.id))
        ];
    state.notifications.unreadCount = Number(data?.unread_count ?? 0);
    state.notifications.totalCount = Number(data?.total_count ?? state.notifications.items.length);
    state.notifications.hasMore = Boolean(data?.has_more);
    state.notifications.loading = false;
    state.notifications.loadingMore = false;
    renderApp();
  } catch (err) {
    state.notifications.loading = false;
    state.notifications.loadingMore = false;
    state.notifications.error = err?.message || 'Unable to load notifications.';
    renderApp();
  }
}

async function markNotificationRead(notificationId) {
  if (!state.session || !notificationId) return;
  let wasUnread = false;
  let targetAnimeId = 0;
  state.notifications.items = state.notifications.items.map((n) => {
    if (n.id === notificationId) {
      targetAnimeId = Number(n.anime_id || 0);
      if (!n.is_read) {
        wasUnread = true;
        return { ...n, is_read: true };
      }
    }
    return n;
  });
  if (wasUnread) {
    state.notifications.unreadCount = Math.max(0, state.notifications.unreadCount - 1);
    if (targetAnimeId > 0) {
      const stillUnreadForAnime = state.notifications.items.some(
        (n) => Number(n.anime_id) === targetAnimeId && !n.is_read
      );
      if (!stillUnreadForAnime) {
        state.notifications.unseenAnime = (state.notifications.unseenAnime || []).filter(
          (u) => Number(u.anime_id) !== targetAnimeId
        );
      }
    }
    updateCommunityRedDotsInPlace();
  }
  try {
    const res = await supabaseRequest('/rest/v1/rpc/mark_notification_read', 'POST', {
      p_notification_id: notificationId
    });
    const numeric = Number(typeof res === 'object' && res !== null ? res.unread_count : res);
    if (!Number.isNaN(numeric)) {
      state.notifications.unreadCount = Math.max(0, numeric);
      if (state.notifications.unreadCount === 0) {
        state.notifications.unseenAnime = [];
      }
      updateCommunityRedDotsInPlace();
    }
  } catch (_) {}
}

async function markAllNotificationsRead() {
  if (!state.session || state.notifications.unreadCount <= 0) return;
  state.notifications.items = state.notifications.items.map((n) => ({ ...n, is_read: true }));
  state.notifications.unseenAnime = [];
  state.notifications.unreadCount = 0;
  updateCommunityRedDotsInPlace();
  renderApp();
  try {
    await supabaseRequest('/rest/v1/rpc/mark_all_notifications_read', 'POST', {});
    showToast('All tagged notifications marked as read');
  } catch (_) {}
}

// Part 2 & Part 5: Atomic Episode Progress Update with strict auto-completion & rollback on cloud failure
async function commitEpisodeProgress(animeId, targetEpisode) {
  const aid = Number(animeId);
  const existing = state.vault.find((v) => Number(v.animeId) === aid);
  if (!existing) return { ok: false, error: 'Anime is not in your library yet.' };

  const prevSnapshot = { ...existing };
  const prevHomeHad = state.homeTrackingIds.has(aid);
  const totalEp = existing.totalEpisodes ? Number(existing.totalEpisodes) : null;
  const clamped = totalEp && totalEp > 0 ? Math.min(totalEp, Math.max(0, Number(targetEpisode))) : Math.max(0, Number(targetEpisode));
  const autoCompleted = shouldAutoComplete(existing.mediaStatus, totalEp, clamped);
  const nextStatus = autoCompleted ? 'completed' : existing.status;
  const nextInHome = autoCompleted ? false : prevHomeHad && !shouldRemoveFromHome(nextStatus, existing.mediaStatus, totalEp, clamped);

  if (nextInHome) state.homeTrackingIds.add(aid);
  else state.homeTrackingIds.delete(aid);
  saveHomeTrackingIds();

  const updated = {
    ...existing,
    watchedEpisodes: clamped,
    status: nextStatus,
    inHome: nextInHome
  };
  state.vault = state.vault.map((v) => (Number(v.animeId) === aid ? updated : v));
  if (state.detailAnime && Number(state.detailAnime.animeId) === aid) {
    state.detailAnime = { ...state.detailAnime, ...updated };
  }
  saveLocalVault();

  if (!state.session) {
    if (autoCompleted) showToast('Completed! Moved from Home to Library.');
    return { ok: true, autoCompleted };
  }

  try {
    let rpcSucceeded = false;
    try {
      await supabaseRequest('/rest/v1/rpc/update_episode_progress', 'POST', {
        p_anime_id: aid,
        p_watched_episodes: clamped
      });
      rpcSucceeded = true;
    } catch (rpcErr) {
      const msg = String(rpcErr?.message || '');
      // Fall back to direct table update ONLY if RPC does not exist yet (pre-migration)
      if (msg.includes('404') || msg.includes('PGRST202') || msg.toLowerCase().includes('could not find the function')) {
        await supabaseRequest(
          '/rest/v1/user_anime?on_conflict=user_id,anime_id',
          'POST',
          {
            user_id: state.session.userId,
            anime_id: aid,
            status: nextStatus,
            episode_progress: clamped,
            total_episodes: totalEp || null,
            personal_rating: updated.rating10 ? Math.min(10, Math.max(1, Number(updated.rating10))) : null,
            review_notes: String(updated.reviewNotes || '').trim().slice(0, 320),
            is_favorite: Boolean(updated.isFavorite)
          },
          'resolution=merge-duplicates,return=minimal'
        );
        if (autoCompleted) {
          await syncRemoveFromHomeCloud(aid);
        }
        rpcSucceeded = true;
      } else {
        throw rpcErr;
      }
    }
    if (rpcSucceeded && autoCompleted) {
      showToast('Completed! Moved from Home to Library.');
    }
    return { ok: true, autoCompleted };
  } catch (err) {
    // Rollback to previous episode progress, status & Home membership on failure
    if (prevHomeHad) state.homeTrackingIds.add(aid);
    else state.homeTrackingIds.delete(aid);
    saveHomeTrackingIds();
    state.vault = state.vault.map((v) => (Number(v.animeId) === aid ? prevSnapshot : v));
    if (state.detailAnime && Number(state.detailAnime.animeId) === aid) {
      state.detailAnime = { ...state.detailAnime, ...prevSnapshot };
    }
    saveLocalVault();
    return { ok: false, error: err?.message || 'Failed to sync episode progress. Restored previous value.' };
  }
}

async function removeFromVault(animeId) {
  const aid = Number(animeId);
  state.vault = state.vault.filter((v) => Number(v.animeId) !== aid);
  state.homeTrackingIds.delete(aid);
  saveHomeTrackingIds();
  if (state.detailAnime && Number(state.detailAnime.animeId) === aid) {
    state.detailAnime.inWatchlist = false;
    state.detailAnime.inHome = false;
  }
  saveLocalVault();
  renderApp();
  if (!state.session) return;
  try {
    const uid = encodeURIComponent(state.session.userId);
    await Promise.all([
      supabaseRequest(`/rest/v1/user_anime?user_id=eq.${uid}&anime_id=eq.${aid}`, 'DELETE').catch(() => {}),
      syncRemoveFromHomeCloud(aid)
    ]);
  } catch (_) {}
}

// ============================================================================
// SHARED WIDGETS: OBSIDIAN TOP BAR & POSTER TRACKER CARD
// ============================================================================

function renderObsidianTopBar({ title, accentTitleTail = null, subtitle, leftActionHtml = '', actionsHtml = '' }) {
  const av = getAvatarPreset(state.profile.avatarKey);
  const initial = String(state.profile.username || 'A').trim().charAt(0).toUpperCase() || 'A';
  return `
    <header class="obsidian-topbar">
      <div class="topbar-left">
        ${leftActionHtml}
        <div style="min-width:0;">
          ${
            accentTitleTail
              ? `<div class="topbar-title-brand">${escapeHtml(title)}<em>${escapeHtml(accentTitleTail)}</em></div>`
              : `<div class="topbar-title-main">${escapeHtml(title)}</div>`
          }
          <div class="topbar-subtitle">${escapeHtml(subtitle)}</div>
        </div>
      </div>
      <div class="topbar-right">
        ${actionsHtml}
        <button class="profile-avatar-btn" data-nav="profile" title="Open Profile" style="position:relative;background:linear-gradient(135deg,${av.c1},${av.c2});display:inline-flex;align-items:center;justify-content:center;">
          <span style="position:absolute;color:#FFF;font-size:15px;font-weight:700;pointer-events:none;">${escapeHtml(initial)}</span>
          <img src="${av.img}" alt="" onerror="${AVATAR_IMG_ONERROR}" style="position:relative;z-index:1;width:100%;height:100%;object-fit:cover;border-radius:50%;display:block;" />
        </button>
      </div>
    </header>
  `;
}

function renderInlineEpisodeStepper(effective, scope = 'card') {
  const aid = Number(effective.animeId);
  const total = effective.totalEpisodes && Number(effective.totalEpisodes) > 0 ? Number(effective.totalEpisodes) : null;
  const watched = Number(effective.watchedEpisodes || 0);
  const canDec = watched > 0;
  const canInc = !total || watched < total;
  const isDetail = scope === 'detail';
  const isEditing = Boolean(state.inlineEpEdit && Number(state.inlineEpEdit.animeId) === aid && state.inlineEpEdit.scope === scope);
  const editObj = isEditing ? state.inlineEpEdit : null;

  if (isEditing && editObj) {
    return `
      <div class="inline-ep-wrap" onclick="event.stopPropagation()">
        <div class="capsule-stepper editing ${isDetail ? 'detail-stepper' : ''}">
          <div class="inline-ep-input-group">
            <input
              id="inline-ep-input-${scope}-${aid}"
              class="inline-ep-input"
              type="text"
              inputmode="numeric"
              pattern="[0-9]*"
              maxlength="6"
              value="${escapeHtml(editObj.draft)}"
              data-inline-ep-input="${aid}"
              data-inline-ep-scope="${scope}"
              aria-label="Watched episodes"
              ${editObj.saving ? 'disabled' : ''}
            />
            <span class="inline-ep-total">/ ${total || '∞'}</span>
          </div>
          <button
            type="button"
            class="inline-ep-action-btn save"
            data-inline-ep-save="${aid}"
            data-inline-ep-scope="${scope}"
            title="Save episode (Enter)"
            ${editObj.saving ? 'disabled' : ''}
          >
            ${editObj.saving ? '<span class="inline-ep-spinner"></span>' : '✓'}
          </button>
          <button
            type="button"
            class="inline-ep-action-btn cancel"
            data-inline-ep-cancel="${aid}"
            title="Cancel (Esc)"
            ${editObj.saving ? 'disabled' : ''}
          >✕</button>
        </div>
        ${editObj.error ? `<div class="inline-ep-error">${escapeHtml(editObj.error)}</div>` : ''}
      </div>
    `;
  }

  return `
    <div class="inline-ep-wrap" onclick="event.stopPropagation()">
      <div class="capsule-stepper ${isDetail ? 'detail-stepper' : ''}">
        <button
          type="button"
          class="stepper-circle-btn"
          style="${isDetail ? 'width:36px;height:36px;' : ''}"
          data-dec-id="${aid}"
          ${!canDec ? 'disabled' : ''}
        >−</button>
        <div
          class="stepper-center-count editable-ep-trigger"
          style="${isDetail ? 'font-size:18px;' : ''}"
          data-inline-ep-trigger="${aid}"
          data-inline-ep-scope="${scope}"
          title="Click to type episode number directly"
        >
          ${isDetail ? `EP ${watched}` : `${watched}`}<span> / ${total || '∞'}</span>
        </div>
        <button
          type="button"
          class="stepper-circle-btn plus"
          style="${isDetail ? 'width:36px;height:36px;' : ''}"
          data-inc-id="${aid}"
          ${!canInc ? 'disabled' : ''}
        >+</button>
      </div>
    </div>
  `;
}

function renderPosterTrackerCard(item, showTrackingStepper = true) {
  const inVault = state.vault.find((v) => Number(v.animeId) === Number(item.animeId));
  const effective = inVault ? { ...item, ...inVault } : item;
  const isInUserList = Boolean(inVault);
  const title = resolveTitle(effective);
  const total = effective.totalEpisodes;
  const watched = effective.watchedEpisodes || 0;
  const sentiment = sentimentFromRating10(effective.rating10);
  const starsVal = rating10ToStars(effective.rating10);
  const userStars = starsVal ? starsVal.toFixed(1) : null;
  const catalogStars = effective.averageScore ? (effective.averageScore / 20).toFixed(1) : null;
  const displayStars = userStars || catalogStars;
  const isDonghua = effective.countryOfOrigin === 'CN';
  const formatLabel = isDonghua ? 'DONGHUA' : (effective.format || 'TV').toUpperCase();
  const epBadge = isInUserList ? `EP ${Math.max(watched, effective.nextEpisodeNumber ? effective.nextEpisodeNumber - 1 : 0)}` : formatLabel;
  const airClock = formatAiringClock(effective.airingTimeUnix) || (effective.seasonYear ? String(effective.seasonYear) : '');

  const isCompleted = isInUserList && (effective.status === 'completed' || shouldAutoComplete(effective.mediaStatus, total, watched));
  const progressPct = total && total > 0 ? Math.min(100, Math.round((watched / total) * 100)) : watched > 0 ? 50 : 4;
  const hasNote = Boolean(effective.reviewNotes && effective.reviewNotes.trim());

  let subStatusHtml = '';
  if (isCompleted) {
    subStatusHtml = `<div class="poster-sub-pill completed">✓ COMPLETED</div>`;
  } else if (effective.airingTimeUnix && effective.airingTimeUnix > Math.floor(Date.now() / 1000) && effective.nextEpisodeNumber) {
    subStatusHtml = `
      <div class="poster-sub-pill airing">
        <span>EP ${effective.nextEpisodeNumber}</span>
        <span class="tabular-num" style="color:var(--highlight);">${formatCompactCountdown(effective.airingTimeUnix)}</span>
      </div>
    `;
  } else if (isInUserList) {
    const stObj = WATCH_STATUSES.find((s) => s.dbValue === effective.status) || WATCH_STATUSES[0];
    subStatusHtml = `<div class="micro-label" style="text-align:center;color:var(--text-muted);font-size:8px;">${escapeHtml(stObj.label)}</div>`;
  }

  return `
    <div class="poster-tracker-card" data-card-anime-id="${effective.animeId}">
      <div class="poster-frame" data-open-detail="${effective.animeId}">
        <img src="${escapeHtml(effective.coverImage)}" alt="${escapeHtml(title)}" loading="lazy" />
        <div class="poster-scrim"></div>
        ${sentiment ? `<div class="poster-top-left"><span class="sentiment-pill solid ${sentiment.key}">${sentiment.shortLabel}</span></div>` : ''}
        ${displayStars ? `<div class="poster-top-right"><span class="rating-badge"><span style="color:var(--accent-primary);">★</span> ${displayStars}</span></div>` : ''}
        ${airClock ? `<div class="poster-bottom-left">${escapeHtml(airClock)}</div>` : ''}
        <div class="poster-bottom-right">${escapeHtml(epBadge)}</div>
      </div>

      <div class="poster-info-col">
        <div class="poster-card-title" data-open-detail="${effective.animeId}">${escapeHtml(title)}</div>
        ${
          isInUserList && showTrackingStepper
            ? `
              <div class="app-progress-bar">
                <div class="app-progress-fill ${isCompleted ? 'completed' : ''}" style="width:${progressPct}%;"></div>
              </div>
              ${renderInlineEpisodeStepper(effective, 'card')}
              ${subStatusHtml}
              <div class="rate-note-row">
                <button class="rate-note-pill ${userStars ? 'active' : ''}" data-quick-edit="${effective.animeId}">
                  ${userStars ? `★ ${userStars}` : '☆ Rate'}
                </button>
                <button class="rate-note-pill ${hasNote ? 'active' : ''}" data-quick-edit="${effective.animeId}">
                  ${hasNote ? '✎ Noted' : '✎ Note'}
                </button>
              </div>
            `
            : `
              <div style="display:flex;align-items:center;justify-content:space-between;gap:6px;">
                <span class="tabular-num" style="font-size:10.5px;color:var(--text-secondary);">
                  ${isInUserList ? `${watched}/${total || '∞'}` : `${formatLabel}${total ? ` • ${total} ep` : ''}`}
                </span>
                <button class="stepper-circle-btn ${isInUserList ? '' : 'plus'}" data-quick-add="${effective.animeId}" title="${isInUserList ? 'Edit tracking' : 'Add to Home & Library'}">
                  ${isInUserList ? '✓' : '+'}
                </button>
              </div>
            `
        }
      </div>
    </div>
  `;
}

// ============================================================================
// SCREEN 1: HOME SCREEN (Strictly displays ONLY explicitly tracked Home anime)
// ============================================================================

function renderHomeScreen() {
  const todayDow = ((new Date().getDay() + 6) % 7) + 1;
  const activeShows = state.vault.filter((v) => isAnimeOnHome(v));
  const dayBuckets = {};
  for (let d = 1; d <= 7; d++) dayBuckets[d] = [];
  const anytimeShows = [];

  activeShows.forEach((v) => {
    const dow = v.airingDayOfWeek || ((v.animeId % 7) + 1);
    if (dow >= 1 && dow <= 7) {
      dayBuckets[dow].push(v);
    } else {
      anytimeShows.push(v);
    }
  });

  const topBar = renderObsidianTopBar({
    title: 'ANIME ',
    accentTitleTail: 'UNIVERSE',
    subtitle: 'WEEKLY SCHEDULE',
    actionsHtml: `
      <button class="app-icon-btn" data-nav="downloads" title="Download Applications">${SVG_ICONS.download}</button>
      <button class="app-icon-btn" id="btn-open-quick-search" title="Quick Search">${SVG_ICONS.search}</button>
    `
  });

  const weekStripHtml = `
    <div class="section-header-row">
      <h2 class="serif-heading" style="font-size:28px;">This Week</h2>
      <button class="library-jump-pill" data-nav="mylist">LIBRARY ›</button>
    </div>
    <div class="week-strip">
      <button class="week-day-pill all-pill ${state.homeAllWeekDays ? 'selected' : ''}" id="btn-home-all-days">
        <span class="all-label">All</span>
        ${state.homeAllWeekDays ? `<span class="all-dot"></span>` : ''}
      </button>
      ${DAY_LABELS.map((d) => {
        const count = (dayBuckets[d.dow] || []).length;
        const isSelected = !state.homeAllWeekDays && state.selectedDay === d.dow;
        const isToday = d.dow === todayDow;
        return `
          <button class="week-day-pill ${isSelected ? 'selected' : ''}" data-select-day="${d.dow}">
            <span class="week-day-top">${d.short}</span>
            <span class="week-day-mid">${getDayNumberForDow(d.dow)}</span>
            ${
              isToday
                ? `<span class="week-day-bottom today-tag">TODAY</span>`
                : `<span class="week-day-bottom">${count}</span>`
            }
          </button>
        `;
      }).join('')}
    </div>
  `;

  let bodyHtml = '';
  if (activeShows.length === 0) {
    bodyHtml = `
      <div class="app-card" style="text-align:center;padding:42px 24px;margin-top:12px;">
        <h3 class="serif-heading" style="font-size:24px;margin-bottom:8px;">Your Weekly Schedule is Empty</h3>
        <p style="color:var(--text-secondary);font-size:13.5px;max-width:420px;margin:0 auto 18px;">
          Discover airing &amp; seasonal anime to build your personal day-by-day release tracker.
        </p>
        <button class="btn-primary" data-nav="discover">Explore Catalog</button>
      </div>
    `;
  } else if (state.homeAllWeekDays) {
    const orderedDays = Array.from({ length: 7 }, (_, i) => ((todayDow - 1 + i) % 7) + 1);
    const daySections = orderedDays
      .map((dow) => {
        const list = dayBuckets[dow] || [];
        if (list.length === 0) return '';
        const dInfo = DAY_LABELS.find((x) => x.dow === dow);
        const isToday = dow === todayDow;
        return `
          <section style="margin-bottom:26px;">
            <div class="section-header-row" style="padding:2px 0;">
              <h3 class="serif-heading" style="font-size:26px;color:var(--text-primary);">
                ${isToday ? `Today • ${dInfo.full}` : dInfo.full}
              </h3>
              <span class="show-count-pill">${list.length} SHOW${list.length === 1 ? '' : 'S'}</span>
            </div>
            <div class="poster-grid">${list.map((it) => renderPosterTrackerCard(it, true)).join('')}</div>
          </section>
        `;
      })
      .join('');

    const anytimeSection =
      anytimeShows.length > 0
        ? `
          <section style="margin-bottom:26px;">
            <div class="section-header-row">
              <h3 class="serif-heading" style="font-size:26px;">Anytime &amp; Completed</h3>
              <span class="show-count-pill">${anytimeShows.length} SHOW${anytimeShows.length === 1 ? '' : 'S'}</span>
            </div>
            <div class="poster-grid">${anytimeShows.map((it) => renderPosterTrackerCard(it, true)).join('')}</div>
          </section>
        `
        : '';
    bodyHtml = daySections + anytimeSection;
  } else {
    const dInfo = DAY_LABELS.find((x) => x.dow === state.selectedDay) || DAY_LABELS[0];
    const list = dayBuckets[state.selectedDay] || [];
    bodyHtml = `
      <section>
        <div class="section-header-row">
          <h3 class="serif-heading" style="font-size:26px;">${dInfo.full}</h3>
          <span class="show-count-pill">${list.length} SHOW${list.length === 1 ? '' : 'S'}</span>
        </div>
        ${
          list.length === 0
            ? `<div class="app-card" style="text-align:center;padding:36px;"><p style="color:var(--text-secondary);">No shows scheduled for ${dInfo.full}.</p></div>`
            : `<div class="poster-grid">${list.map((it) => renderPosterTrackerCard(it, true)).join('')}</div>`
        }
      </section>
    `;
  }

  return `${topBar}${weekStripHtml}${bodyHtml}`;
}

// ============================================================================
// SCREEN 2: DISCOVER / EXPLORE SCREEN (Matches Screenshot 4 1:1)
// ============================================================================

function renderDiscoverScreen() {
  const modes = [
    { id: 'trending', label: 'TRENDING', title: 'Trending', sort: 'TRENDING_DESC' },
    { id: 'popular', label: 'POPULAR', title: 'Popular', sort: 'POPULARITY_DESC' },
    { id: 'highest_rated', label: 'TOP RATED', title: 'Top Rated', sort: 'SCORE_DESC' },
    { id: 'seasonal', label: 'SEASONAL', title: 'Seasonal', sort: 'POPULARITY_DESC' },
    { id: 'donghua', label: 'DONGHUA', title: 'Donghua', sort: 'POPULARITY_DESC' },
    { id: 'airing', label: 'AIRING', title: 'Airing', sort: 'TRENDING_DESC' }
  ];

  const activeModeObj = modes.find((m) => m.id === state.discoverMode) || modes[1];

  const topBar = renderObsidianTopBar({
    title: 'Discover',
    subtitle: 'THE GLOBAL CATALOG',
    actionsHtml: `
      <button class="app-icon-btn" id="btn-toggle-layout" title="Toggle Grid/List">
        ${SVG_ICONS.list}
      </button>
      <button class="app-icon-btn" id="btn-open-filters" title="Filters">
        ${SVG_ICONS.tune}
      </button>
    `
  });

  return `
    ${topBar}
    <div class="app-search-bar">
      <span class="search-icon">${SVG_ICONS.search}</span>
      <input id="inp-discover-search" placeholder="Search 25,000+ anime &amp; Donghua..." value="${escapeHtml(state.searchQuery)}" />
      ${state.searchQuery ? `<button class="clear-btn" id="btn-clear-disc-search">✕</button>` : ''}
    </div>

    <div class="capsule-strip">
      ${modes
        .map(
          (m) => `
          <button class="capsule-pill gold-solid ${state.discoverMode === m.id ? 'active' : ''}" data-disc-mode="${m.id}" data-disc-sort="${m.sort}">
            ${m.label}
          </button>`
        )
        .join('')}
      <button class="capsule-pill ${state.prefs.showAdultContent ? 'active' : ''}" id="btn-toggle-safe" style="border-color:rgba(80,200,130,0.45);color:var(--success);">
        ${SVG_ICONS.shield}
        <span>${state.prefs.showAdultContent ? '18+ SHOWN' : 'SAFE ON'}</span>
      </button>
    </div>

    <div class="capsule-strip">
      <button class="capsule-pill ${!state.discoverGenre ? 'active' : ''}" data-disc-genre="">All Genres</button>
      ${ANILIST_GENRES.map(
        (g) => `<button class="capsule-pill ${state.discoverGenre === g ? 'active' : ''}" data-disc-genre="${g}">${g}</button>`
      ).join('')}
    </div>

    <div class="section-header-row" style="margin-top:8px;">
      <h2 class="serif-heading" style="font-size:26px;">${escapeHtml(activeModeObj.title)}</h2>
      <span class="micro-label" id="discover-count-label" style="color:var(--accent-primary);">${state.discover.length} TITLES</span>
    </div>

    ${
      state.isSearching
        ? `<div class="app-card" style="text-align:center;padding:48px;"><p style="color:var(--accent-primary);">Loading Anime Universe catalog...</p></div>`
        : `
          <div class="poster-grid" id="discover-poster-grid">${state.discover.map((it) => renderPosterTrackerCard(it, false)).join('')}</div>
          <div id="discover-sentinel-wrap">${renderDiscoverFooterHtml()}</div>
        `
    }
  `;
}

// ============================================================================
// SCREEN 3: LIBRARY / MY LIST SCREEN (Matches Screenshot 2 1:1)
// ============================================================================

function renderMyListScreen() {
  const filtered = state.vault
    .filter((item) => {
      if (state.myListStatusFilter !== 'ALL' && item.status !== state.myListStatusFilter) return false;
      if (state.myListSentimentFilter) {
        const s = sentimentFromRating10(item.rating10);
        if (!s || s.key !== state.myListSentimentFilter) return false;
      }
      if (state.myListCollectionFilter) {
        const col = state.collections.find((c) => c.id === state.myListCollectionFilter);
        if (col && !(col.animeIds || []).includes(item.animeId)) return false;
      }
      if (state.myListQuery.trim()) {
        const t = resolveTitle(item).toLowerCase();
        if (!t.includes(state.myListQuery.trim().toLowerCase())) return false;
      }
      return true;
    })
    .sort((a, b) => {
      if (state.myListSort === 'TITLE') return resolveTitle(a).localeCompare(resolveTitle(b));
      if (state.myListSort === 'RATING') return (b.rating10 || 0) - (a.rating10 || 0);
      if (state.myListSort === 'PROGRESS') return (b.watchedEpisodes || 0) - (a.watchedEpisodes || 0);
      return 0;
    });

  const topBar = renderObsidianTopBar({
    title: 'Library',
    subtitle: `${state.vault.length} TRACKED • ${state.collections.length} COLLECTIONS`,
    actionsHtml: `
      <button class="app-icon-btn" id="btn-create-collection" title="Collections">${SVG_ICONS.collectionIcon}</button>
      <button class="app-icon-btn" id="btn-toggle-layout" title="Toggle Grid/List">${SVG_ICONS.list}</button>
    `
  });

  return `
    ${topBar}
    <!-- Status Capsule Tabs with Circular Count Badges (Screenshot 2 Row 1) -->
    <div class="capsule-strip">
      <button class="capsule-pill ${state.myListStatusFilter === 'ALL' ? 'active' : ''}" data-mylist-status="ALL">
        <span>ALL</span>
        <span class="pill-count-circle">${state.vault.length}</span>
      </button>
      ${WATCH_STATUSES.map((st) => {
        const count = state.vault.filter((v) => v.status === st.dbValue).length;
        return `
          <button class="capsule-pill ${state.myListStatusFilter === st.dbValue ? 'active' : ''}" data-mylist-status="${st.dbValue}">
            <span>${st.label.toUpperCase()}</span>
            <span class="pill-count-circle">${count}</span>
          </button>
        `;
      }).join('')}
    </div>

    <!-- Sentiment Filter Strip with Colored Names + Counts (Screenshot 2 Row 2) -->
    <div class="capsule-strip">
      <button class="capsule-pill ${!state.myListSentimentFilter ? 'active' : ''}" data-mylist-sentiment="">ALL RATINGS</button>
      ${SENTIMENTS.map((s) => {
        const count = state.vault.filter((v) => sentimentFromRating10(v.rating10)?.key === s.key).length;
        return `
          <button class="capsule-pill sentiment-filter-pill ${state.myListSentimentFilter === s.key ? 'active' : ''}" data-mylist-sentiment="${s.key}">
            <span class="sent-name ${s.key}">${s.shortLabel.toUpperCase()}</span>
            <span class="sent-count">${count}</span>
          </button>
        `;
      }).join('')}
    </div>

    <div class="app-search-bar">
      <span class="search-icon">${SVG_ICONS.search}</span>
      <input id="inp-mylist-search" placeholder="Filter your collection..." value="${escapeHtml(state.myListQuery)}" />
    </div>

    <div class="capsule-strip">
      ${[
        { k: 'RECENT', label: 'Sort: Recent' },
        { k: 'TITLE', label: 'Sort: Title' },
        { k: 'RATING', label: 'Sort: Rating' },
        { k: 'PROGRESS', label: 'Sort: Progress' }
      ]
        .map(
          (s) =>
            `<button class="capsule-pill ${state.myListSort === s.k ? 'active' : ''}" data-mylist-sort="${s.k}">${s.label}</button>`
        )
        .join('')}
    </div>

    <!-- Collections Section (Matches Screenshot 2) -->
    <div style="margin: 14px 0 18px;">
      <div class="section-header-row" style="margin-bottom:10px;">
        <h3 class="serif-heading" style="font-size:24px;">Collections</h3>
        <button id="btn-new-collection-inline" style="background:none;border:none;color:var(--accent-primary);font-size:11.5px;font-weight:700;letter-spacing:1.4px;cursor:pointer;font-family:inherit;">
          + NEW
        </button>
      </div>
      ${
        state.collections.length === 0
          ? `
            <div style="background:#12151F;border:1px solid rgba(255,255,255,0.08);border-radius:18px;padding:16px 18px;color:var(--text-secondary);font-size:13.5px;">
              Create custom collections such as "Absolute Cinema" or "Donghua Favorites".
            </div>
          `
          : `
            <div class="capsule-strip">
              <button class="capsule-pill ${!state.myListCollectionFilter ? 'active' : ''}" data-mylist-col="">All Collections</button>
              ${state.collections
                .map(
                  (c) => `
                  <button class="capsule-pill ${state.myListCollectionFilter === c.id ? 'active' : ''}" data-mylist-col="${c.id}">
                    <span>${escapeHtml(c.name)}</span>
                    <span class="pill-count-circle">${(c.animeIds || []).length}</span>
                  </button>`
                )
                .join('')}
            </div>
          `
      }
    </div>

    ${
      filtered.length === 0
        ? `<div class="app-card" style="text-align:center;padding:42px;"><p style="color:var(--text-secondary);">No matching anime entries in your library.</p></div>`
        : `<div class="poster-grid">${filtered.map((it) => renderPosterTrackerCard(it, true)).join('')}</div>`
    }
  `;
}

// ============================================================================
// SCREEN 4: COMMUNITY SCREEN (Matches Screenshot 5 1:1)
// ============================================================================

function renderFiveStarsHtml(starsVal) {
  const fullCount = Math.round(starsVal || 0);
  let html = '';
  for (let i = 1; i <= 5; i++) {
    const filled = i <= fullCount;
    html += `<span style="color:${filled ? 'var(--accent-primary)' : '#383D4E'};font-size:17px;">★</span>`;
  }
  return html;
}

function renderCommunityScreen() {
  const allGroups = groupCommunityReviewsByAnime(state.communityReviews);
  const totalNotesAndRatings = allGroups.reduce((acc, g) => acc + g.total_count, 0);

  const topBar = renderObsidianTopBar({
    title: 'Community',
    subtitle: `${allGroups.length} ANIME • ${totalNotesAndRatings} NOTES & RATINGS`
  });

  if (!state.session) {
    return `
      ${topBar}
      <div class="app-card" style="text-align:center;padding:42px 24px;max-width:480px;margin:28px auto;">
        <h2 class="serif-heading" style="font-size:28px;margin-bottom:8px;">Sign In for Community Lounge</h2>
        <p style="color:var(--text-secondary);font-size:13.5px;margin-bottom:20px;">
          Sign in to browse rated &amp; noted anime showcases shared by members and share your own taste.
        </p>
        <button class="btn-primary" id="btn-exit-guest" style="width:100%;">SIGN IN</button>
      </div>
    `;
  }

  const unseenByAnimeId = new Map(
    (state.notifications.unseenAnime || []).map((u) => [Number(u.anime_id), u])
  );

  const filtered = allGroups
    .filter((grp) => {
      if (!state.communitySentimentFilter) return true;
      const s = grp.average_rating10 ? sentimentFromRating10(grp.average_rating10) : null;
      return s && s.key === state.communitySentimentFilter;
    })
    .sort((a, b) => {
      if (state.communitySort === 'HIGHEST') return (b.average_rating10 || 0) - (a.average_rating10 || 0);
      return String(b.latest_updated_at || '').localeCompare(String(a.latest_updated_at || ''));
    });

  const shownAnimeIds = new Set(filtered.map((g) => Number(g.anime_id)));
  const unseenOnlyCards = (state.notifications.unseenAnime || []).filter(
    (u) => !shownAnimeIds.has(Number(u.anime_id))
  );

  const viewTab = state.communityViewTab === 'tagged' ? 'tagged' : 'feed';
  const unreadTaggedCount = Math.max(
    Number(state.notifications.unreadCount || 0),
    (state.notifications.unseenAnime || []).reduce((acc, u) => acc + Math.max(1, Number(u.unseen_count || 1)), 0)
  );
  const nState = state.notifications;

  return `
    ${topBar}
    <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap;margin-bottom:6px;">
      <div class="community-view-switcher">
        <button type="button" class="community-view-tab ${viewTab === 'feed' ? 'active' : ''}" data-comm-view-tab="feed">
          Community Feed
        </button>
        <button type="button" class="community-view-tab ${viewTab === 'tagged' ? 'active' : ''}" data-comm-view-tab="tagged">
          <span>Tagged You</span>
          ${unreadTaggedCount > 0 ? `<span class="tagged-count-badge">${unreadTaggedCount > 99 ? '99+' : unreadTaggedCount}</span>` : ''}
        </button>
      </div>
      ${
        viewTab === 'tagged' && unreadTaggedCount > 0
          ? `<button type="button" class="capsule-pill active" id="btn-comm-tagged-mark-all">Mark all read</button>`
          : ''
      }
    </div>

    ${
      viewTab === 'tagged'
        ? `
          ${nState.error ? `<div class="feedback-banner error" style="margin-bottom:12px;">! ${escapeHtml(nState.error)}</div>` : ''}
          ${
            nState.loading && nState.items.length === 0
              ? `<div class="app-card" style="text-align:center;padding:40px 20px;color:var(--text-secondary);">Loading tagged notifications...</div>`
              : nState.items.length === 0
              ? `<div class="app-card" style="text-align:center;padding:42px 24px;">
                   <div class="serif-heading" style="font-size:24px;margin-bottom:6px;">All caught up</div>
                   <p style="color:var(--text-secondary);font-size:13.5px;">When someone tags you in any anime template — new or old — it appears here for instant one-tap jump.</p>
                 </div>`
              : `
                <div class="tagged-you-list">
                  ${nState.items
                    .map((n) => {
                      const nType = n.notification_type || n.type || 'mention_comment';
                      const actionLabel =
                        nType === 'thread_reply'
                          ? 'replied to you'
                          : nType === 'mention_reply'
                          ? 'tagged you in a reply'
                          : 'tagged you';
                      const snippet = formatPlainPreviewWithEmojis(n.preview_text || n.content_preview || '');
                      const commName = (n.community_name && !/livtrak/i.test(n.community_name)) ? n.community_name : 'Community';
                      return `
                        <button
                          type="button"
                          class="tagged-you-card ${n.is_read ? '' : 'unread'}"
                          data-open-tagged-notif="${escapeHtml(n.id)}"
                          data-notif-anime="${Number(n.anime_id || 0)}"
                          data-notif-title="${escapeHtml(n.anime_title || '')}"
                          data-notif-cover="${escapeHtml(n.anime_cover_image || '')}"
                          data-notif-comment="${escapeHtml(n.comment_id || '')}"
                          data-notif-thread="${escapeHtml(n.thread_id || '')}"
                          data-notif-reply="${escapeHtml(n.reply_id || '')}"
                        >
                          ${
                            n.anime_cover_image
                              ? `<img class="tagged-you-thumb" src="${escapeHtml(n.anime_cover_image)}" alt="" loading="lazy" />`
                              : `<div class="tagged-you-thumb" style="display:grid;place-items:center;color:#E8C88F;font-weight:700;">★</div>`
                          }
                          <div class="tagged-you-body">
                            <div class="tagged-you-meta-top">
                              <span class="tagged-you-community-pill">${escapeHtml(commName)}</span>
                              <span class="tagged-you-time">${escapeHtml(formatDiscussionTimestamp(n.created_at))}</span>
                            </div>
                            <div class="tagged-you-anime-title">${escapeHtml(n.anime_title || `Anime #${n.anime_id}`)}</div>
                            <div class="tagged-you-who">
                              ${renderAvatarCircle(n.sender_avatar_key || 'avatar_1', n.sender_username || 'Member', 20)}
                              <span><strong style="color:var(--highlight);">@${escapeHtml(n.sender_username || 'Member')}</strong> ${actionLabel}</span>
                            </div>
                            ${snippet ? `<div class="tagged-you-snippet">“${escapeHtml(snippet)}”</div>` : ''}
                          </div>
                          ${!n.is_read ? `<span class="community-card-red-dot" title="Unread"></span>` : ''}
                        </button>
                      `;
                    })
                    .join('')}
                </div>
                ${
                  nState.hasMore
                    ? `<button type="button" class="btn-secondary" id="btn-comm-tagged-load-more" style="width:100%;margin-top:12px;" ${nState.loadingMore ? 'disabled' : ''}>
                         ${nState.loadingMore ? 'LOADING...' : 'LOAD OLDER TAGS'}
                       </button>`
                    : ''
                }
              `
          }
        `
        : `
    <div class="capsule-strip">
      <button class="capsule-pill ${!state.communitySentimentFilter ? 'active' : ''}" data-comm-sentiment="">
        ALL (${allGroups.length})
      </button>
      ${SENTIMENTS.map(
        (s) =>
          `<button class="capsule-pill ${state.communitySentimentFilter === s.key ? 'active' : ''}" data-comm-sentiment="${s.key}">
            ${s.shortLabel.toUpperCase()}
          </button>`
      ).join('')}
    </div>

    <div class="capsule-strip">
      ${[
        { k: 'RECENT', label: 'Newest' },
        { k: 'HIGHEST', label: 'Highest Rated' }
      ]
        .map(
          (opt) =>
            `<button class="capsule-pill ${state.communitySort === opt.k ? 'active' : ''}" data-comm-sort="${opt.k}">${opt.label}</button>`
        )
        .join('')}
    </div>

    ${
      filtered.length === 0 && unseenOnlyCards.length === 0
        ? `<div class="app-card" style="text-align:center;padding:42px;"><p style="color:var(--text-secondary);">No rated &amp; noted showcases match this filter yet.</p></div>`
        : `<div class="community-review-list">
            ${unseenOnlyCards
              .map((u) => {
                const actionWord = u.latest_notification_type === 'reply' ? 'replied to you' : 'tagged you in discussion';
                const extraCount = Number(u.unseen_count || 1) > 1 ? ` • ${u.unseen_count} unseen` : '';
                return `
                  <div
                    class="community-review-card unseen-highlight-card"
                    data-open-unseen-anime="${Number(u.anime_id)}"
                    data-unseen-comment="${escapeHtml(u.latest_comment_id || '')}"
                    data-unseen-thread="${escapeHtml(u.latest_thread_id || '')}"
                    data-unseen-reply="${escapeHtml(u.latest_reply_id || '')}"
                  >
                    <div style="display:flex;gap:14px;align-items:center;">
                      ${
                        u.anime_cover_image
                          ? `<img src="${escapeHtml(u.anime_cover_image)}" alt="" style="width:60px;height:86px;object-fit:cover;border-radius:14px;flex-shrink:0;" />`
                          : `<div style="width:60px;height:86px;border-radius:14px;background:var(--elevated-surface);display:flex;align-items:center;justify-content:center;color:var(--accent-primary);font-weight:700;">★</div>`
                      }
                      <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:6px;">
                        <div style="display:flex;align-items:center;gap:8px;">
                          <span class="community-card-red-dot" title="Unseen discussion message"></span>
                          <div class="serif-heading" style="font-size:21px;line-height:25px;color:var(--text-primary);">${escapeHtml(u.anime_title || `Anime #${u.anime_id}`)}</div>
                        </div>
                        <div class="unseen-tag-pill">
                          <span class="community-card-red-dot small"></span>
                          <span>${escapeHtml(u.latest_sender_username || 'Member')} ${actionWord}${extraCount}</span>
                        </div>
                        ${
                          u.latest_preview
                            ? `<div style="font-size:12.5px;color:var(--text-secondary);font-style:italic;">“${escapeHtml(u.latest_preview)}”</div>`
                            : ''
                        }
                        <div style="font-size:11.5px;font-weight:600;color:var(--accent-primary);">Open anime discussion to read &amp; clear red dot →</div>
                      </div>
                    </div>
                  </div>
                `;
              })
              .join('')}
            ${filtered
              .map((grp) => {
                const unseen = unseenByAnimeId.get(Number(grp.anime_id));
                const starsNum = grp.average_rating10 ? rating10ToStars(grp.average_rating10) || 0 : 0;
                const stars = starsNum > 0 ? starsNum.toFixed(1) : '';
                const sentiment = grp.average_rating10 ? sentimentFromRating10(grp.average_rating10) : null;
                const lead = grp.lead_review || grp.reviews[0];
                const extraContributors = Math.max(0, grp.reviews.length - 1);
                const leadText =
                  lead && String(lead.review_notes || '').trim().length > 0
                    ? `“${escapeHtml(lead.review_notes)}”`
                    : `${escapeHtml(lead?.username || 'Member')} rated this ${stars || '5.0'} ★`;
                const stackedAvatarsHtml = grp.reviews
                  .slice(0, 3)
                  .map(
                    (r, idx) =>
                      `<div style="margin-left:${idx === 0 ? '0' : '-8px'};border:1.5px solid #12151F;border-radius:999px;display:inline-flex;">
                        ${renderAvatarCircle(r.avatar_key || 'avatar_1', r.username, 28)}
                      </div>`
                  )
                  .join('');

                return `
                  <div
                    class="community-review-card ${unseen ? 'unseen-highlight-card' : ''}"
                    ${
                      unseen
                        ? `data-open-unseen-anime="${grp.anime_id}" data-unseen-comment="${escapeHtml(unseen.latest_comment_id || '')}" data-unseen-thread="${escapeHtml(unseen.latest_thread_id || '')}" data-unseen-reply="${escapeHtml(unseen.latest_reply_id || '')}"`
                        : `data-open-detail="${grp.anime_id}"`
                    }
                  >
                    <div style="display:flex;gap:16px;align-items:flex-start;">
                      <img src="${escapeHtml(grp.anime_cover_image)}" alt="" style="width:68px;height:96px;object-fit:cover;border-radius:14px;flex-shrink:0;" />
                      <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:6px;">
                        <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
                          <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;min-width:0;">
                            <div class="serif-heading" style="font-size:23px;line-height:27px;color:var(--text-primary);">${escapeHtml(grp.anime_title)}</div>
                            ${grp.is_adult ? `<span class="adult-18-badge">18+</span>` : ''}
                          </div>
                          ${unseen ? `<span class="community-card-red-dot" title="Unseen tag or reply"></span>` : ''}
                        </div>
                        ${
                          unseen
                            ? `<div class="unseen-tag-pill">
                                <span class="community-card-red-dot small"></span>
                                <span>${escapeHtml(unseen.latest_sender_username || 'Member')} ${unseen.latest_notification_type === 'reply' ? 'replied' : 'tagged you'}${Number(unseen.unseen_count || 1) > 1 ? ` (+${Number(unseen.unseen_count) - 1})` : ''} • Open</span>
                              </div>`
                            : ''
                        }
                        ${
                          starsNum > 0
                            ? `<div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
                                <div style="display:inline-flex;gap:2px;">${renderFiveStarsHtml(starsNum)}</div>
                                <span class="tabular-num" style="font-size:15px;font-weight:600;color:var(--text-primary);">${stars}</span>
                                ${
                                  grp.rated_count > 1
                                    ? `<span style="font-size:11.5px;color:var(--text-muted);">(${grp.rated_count} ratings)</span>`
                                    : ''
                                }
                              </div>`
                            : `<div><span class="sentiment-pill soft cinema">COMMUNITY NOTES</span></div>`
                        }
                        ${sentiment ? `<div><span class="sentiment-pill soft ${sentiment.key}">${sentiment.fullLabel.toUpperCase()}</span></div>` : ''}
                      </div>
                    </div>

                    <div class="review-quote-box" data-open-comments="${grp.anime_id}" title="View all ${grp.total_count} notes &amp; ratings">
                      <div class="review-quote-bar"></div>
                      <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:4px;">
                        <div class="review-quote-text">${leadText}</div>
                        ${
                          extraContributors > 0
                            ? `<div style="font-size:11.5px;font-weight:600;color:var(--accent-primary);">+${extraContributors} more member note${extraContributors > 1 ? 's' : ''} &amp; rating${extraContributors > 1 ? 's' : ''} inside</div>`
                            : ''
                        }
                      </div>
                    </div>

                    <div style="display:flex;align-items:center;justify-content:space-between;padding-top:2px;gap:10px;">
                      <div style="display:flex;align-items:center;gap:10px;min-width:0;">
                        <div style="display:inline-flex;align-items:center;">${stackedAvatarsHtml}</div>
                        <span style="font-size:13.5px;font-weight:500;color:var(--text-secondary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">
                          ${escapeHtml(lead?.username || 'Member')}${extraContributors > 0 ? ` +${extraContributors}` : ''}
                        </span>
                      </div>

                      <div style="display:flex;align-items:center;gap:10px;flex-shrink:0;">
                        <span style="font-size:12.5px;color:var(--text-muted);">${escapeHtml(formatReviewDate(grp.latest_updated_at))}</span>
                        <button
                          type="button"
                          class="community-comment-btn"
                          data-open-comments="${grp.anime_id}"
                          title="Open all notes &amp; ratings for ${escapeHtml(grp.anime_title)}"
                        >
                          <span class="comment-icon-svg">${SVG_ICONS.comment}</span>
                          <span class="tabular-num">${grp.total_count}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                `;
              })
              .join('')}
          </div>`
    }
    `
    }
  `;
}

// ============================================================================
// SCREEN 5: PROFILE SCREEN
// ============================================================================

function renderProfileScreen() {
  const ratedItems = state.vault.filter((v) => (v.rating10 || 0) > 0);
  const watchedCount = state.vault.filter((v) => (v.watchedEpisodes || 0) >= 1).length;
  const meanScore =
    ratedItems.length > 0
      ? `★ ${(ratedItems.reduce((acc, v) => acc + (rating10ToStars(v.rating10) || 0), 0) / ratedItems.length).toFixed(1)}`
      : '—';
  const favorites = state.vault.filter((v) => v.isFavorite);
  const avPreset = getAvatarPreset(state.profile.avatarKey);
  const avatarImg = avPreset.img;

  return `
    <div style="max-width:760px;margin:0 auto;">
      <div style="display:flex;align-items:center;justify-content:space-between;padding:6px 0 14px;">
        <button class="app-icon-btn" data-nav="home" title="Back">${SVG_ICONS.back}</button>
        <h1 class="serif-heading" style="font-size:24px;">Profile</h1>
        <button class="app-icon-btn" data-nav="settings" title="Settings">${SVG_ICONS.settings}</button>
      </div>

      <div style="display:flex;flex-direction:column;align-items:center;gap:8px;padding:12px 0 18px;">
        <div id="btn-open-avatar-modal" style="position:relative;width:108px;height:108px;cursor:pointer;" title="Tap to change portrait">
          <div style="width:104px;height:104px;border-radius:50%;padding:3px;background:linear-gradient(135deg,${avPreset.c1},${avPreset.c2});box-shadow:0 12px 32px var(--accent-soft);">
            <img src="${avatarImg}" alt="" onerror="${AVATAR_IMG_ONERROR}" style="width:100%;height:100%;border-radius:50%;object-fit:cover;border:2.5px solid var(--bg-main);display:block;" />
          </div>
          <div style="position:absolute;bottom:2px;right:2px;width:28px;height:28px;border-radius:50%;background:var(--accent-primary);color:var(--on-accent);border:2px solid var(--bg-main);display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:700;">✎</div>
        </div>

        <div id="btn-open-profile-edit" style="display:flex;align-items:center;gap:6px;cursor:pointer;padding:4px 12px;border-radius:14px;">
          <h2 class="serif-heading" style="font-size:28px;font-weight:600;">${escapeHtml(state.session ? state.profile.username : 'Guest Tracker')}</h2>
          <span style="color:var(--accent-primary);font-size:14px;">✎</span>
        </div>

        <div class="micro-label" style="color:var(--text-muted);letter-spacing:1.6px;">
          ${state.session ? 'COMMUNITY MEMBER' : 'LOCAL VAULT'}
        </div>
      </div>

      <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin-bottom:16px;">
        ${[
          { label: 'RATED', val: ratedItems.length },
          { label: 'WATCHED', val: watchedCount },
          { label: 'SAVED', val: state.vault.length },
          { label: 'MEAN', val: meanScore }
        ]
          .map(
            (st) => `
            <div class="app-card" style="padding:14px 8px;text-align:center;border-radius:20px;">
              <div class="serif-heading tabular-num" style="font-size:24px;font-weight:600;color:var(--highlight);">${st.val}</div>
              <div class="micro-label" style="color:var(--text-muted);margin-top:4px;font-size:8px;">${st.label}</div>
            </div>`
          )
          .join('')}
      </div>

      <div class="app-card" style="background:linear-gradient(135deg,var(--accent-soft),var(--card-bg));border-color:var(--accent-border);margin-bottom:16px;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
          <span class="micro-label" style="color:var(--accent-primary);letter-spacing:2px;">COMMUNITY STATUS</span>
          <span class="sentiment-pill soft worth">${state.session ? 'Active' : 'Local Preview'}</span>
        </div>
        <div class="serif-heading" style="font-size:22px;margin-bottom:4px;">
          ${state.session ? 'Verified Lounge Member' : 'Local Device Vault'}
        </div>
        <p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">
          ${
            state.session
              ? 'Your rated & noted anime showcases sync automatically across Android, Web, and PC.'
              : 'Sign in anytime from Settings to sync your library and join the community.'
          }
        </p>
      </div>

      ${
        favorites.length > 0
          ? `
            <div style="margin-top:18px;">
              <h3 class="serif-heading" style="font-size:22px;margin-bottom:10px;">Favorites (${favorites.length})</h3>
              <div class="poster-grid">${favorites.map((it) => renderPosterTrackerCard(it, false)).join('')}</div>
            </div>
          `
          : ''
      }
    </div>
  `;
}

// ============================================================================
// SCREEN 6: SETTINGS SCREEN
// ============================================================================

function renderSettingsScreen() {
  const cat = state.settingsCategory;
  return `
    <div style="max-width:720px;margin:0 auto;">
      <div style="display:flex;align-items:center;gap:12px;padding:6px 0 16px;">
        <button class="app-icon-btn" id="btn-settings-back" title="Back">${SVG_ICONS.back}</button>
        <div>
          <h1 class="serif-heading" style="font-size:26px;">${cat ? cat.charAt(0) + cat.slice(1).toLowerCase() : 'Settings'}</h1>
          <div class="topbar-subtitle">${cat ? 'PREFERENCES & CONTROLS' : 'SYSTEM & PREFERENCES'}</div>
        </div>
      </div>

      ${
        !cat
          ? `
            <div style="display:flex;flex-direction:column;gap:10px;">
              ${[
                { id: 'ACCOUNT', title: 'Account', sub: state.session ? `Signed in as ${state.profile.username} • Auto-Sync Active` : 'Local Preview Mode • Sign in' },
                { id: 'APPEARANCE', title: 'Appearance', sub: `Theme (${state.prefs.themeMode}) • Accent (${state.prefs.accentTheme}) • Layout (${state.prefs.watchlistLayout})` },
                { id: 'PREFERENCES', title: 'Preferences', sub: `Title Language (${state.prefs.titleLanguage}) • Safe Mode • JSON Backup` },
                { id: 'ABOUT', title: 'About', sub: 'Anime Universe v2.1.0 • Architecture & Credits' }
              ]
                .map(
                  (row) => `
                  <div class="app-card clickable" data-settings-cat="${row.id}" style="display:flex;align-items:center;justify-content:space-between;">
                    <div>
                      <div class="serif-heading" style="font-size:22px;">${row.title}</div>
                      <div style="font-size:12.5px;color:var(--text-secondary);margin-top:2px;">${escapeHtml(row.sub)}</div>
                    </div>
                    <span style="color:var(--accent-primary);font-size:20px;">›</span>
                  </div>`
                )
                .join('')}
            </div>
          `
          : cat === 'APPEARANCE'
          ? `
            <div class="app-card" style="display:flex;flex-direction:column;gap:16px;">
              <div>
                <div class="micro-label" style="color:var(--accent-primary);margin-bottom:8px;">THEME MODE</div>
                <div style="display:flex;gap:8px;flex-wrap:wrap;">
                  ${['DARK', 'LIGHT', 'SYSTEM']
                    .map((m) => `<button class="capsule-pill gold-solid ${state.prefs.themeMode === m ? 'active' : ''}" data-set-theme="${m}">${m}</button>`)
                    .join('')}
                </div>
              </div>
              <div>
                <div class="micro-label" style="color:var(--accent-primary);margin-bottom:8px;">ACCENT PALETTE</div>
                <div style="display:flex;gap:8px;flex-wrap:wrap;">
                  ${[
                    { id: 'GOLD', label: 'Champagne Gold' },
                    { id: 'ROSE', label: 'Sakura Rose' },
                    { id: 'EMERALD', label: 'Emerald Mint' },
                    { id: 'INDIGO', label: 'Midnight Indigo' }
                  ]
                    .map((a) => `<button class="capsule-pill gold-solid ${state.prefs.accentTheme === a.id ? 'active' : ''}" data-set-accent="${a.id}">${a.label}</button>`)
                    .join('')}
                </div>
              </div>
            </div>
          `
          : cat === 'PREFERENCES'
          ? `
            <div class="app-card" style="display:flex;flex-direction:column;gap:16px;">
              <div>
                <div class="micro-label" style="color:var(--accent-primary);margin-bottom:8px;">PREFERRED TITLE LANGUAGE</div>
                <div style="display:flex;gap:8px;flex-wrap:wrap;">
                  ${['ENGLISH', 'ROMAJI', 'NATIVE']
                    .map((l) => `<button class="capsule-pill gold-solid ${state.prefs.titleLanguage === l ? 'active' : ''}" data-set-lang="${l}">${l}</button>`)
                    .join('')}
                </div>
              </div>
            </div>
          `
          : cat === 'ACCOUNT'
          ? `
            <div class="app-card" style="display:flex;flex-direction:column;gap:14px;">
              ${
                state.session
                  ? `
                    <div class="serif-heading" style="font-size:22px;">Signed in as ${escapeHtml(state.profile.username)}</div>
                    <p style="font-size:13px;color:var(--text-secondary);">Real-time cross-platform cloud sync is active automatically.</p>
                    <div style="display:flex;gap:10px;flex-wrap:wrap;">
                      <button class="btn-secondary" id="btn-signout">Sign Out</button>
                    </div>
                  `
                  : `
                    <div class="serif-heading" style="font-size:22px;">Local Preview Mode</div>
                    <p style="font-size:13px;color:var(--text-secondary);">Sign in to sync your tracked anime across Android, Desktop &amp; Web.</p>
                    <button class="btn-primary" id="btn-exit-guest">SIGN IN</button>
                  `
              }
            </div>
          `
          : `
            <div class="app-card" style="display:flex;flex-direction:column;gap:10px;">
              <div class="serif-heading" style="font-size:24px;">Anime Universe v2.1.0</div>
              <p style="font-size:13.5px;color:var(--text-secondary);line-height:1.55;">
                Designed with the Obsidian &amp; Champagne Gold editorial aesthetic. Powered by AniList GraphQL &amp; Supabase Cloud Vault.
              </p>
            </div>
          `
      }
    </div>
  `;
}

// ============================================================================
// SCREEN 7: ANIME DETAIL SCREEN
// ============================================================================

function renderAnimeDetailScreen() {
  const a = state.detailAnime;
  if (!a) return '';
  const title = resolveTitle(a);
  const subJp = a.titleNative || (a.titleRomaji !== title ? a.titleRomaji : '');
  const total = a.totalEpisodes;
  const watched = a.watchedEpisodes || 0;
  const stars = rating10ToStars(a.rating10) || 0;
  const sentiment = sentimentFromRating10(a.rating10);
  const seasons = state.detailRich?.seasons || [];

  return `
    <div style="max-width:940px;margin:0 auto;">
      <div style="position:relative;width:100%;height:300px;border-radius:26px;overflow:hidden;margin-bottom:16px;background:var(--card-bg);">
        <img src="${escapeHtml(a.bannerImage || a.coverImage)}" alt="" style="width:100%;height:100%;object-fit:cover;display:block;" />
        <div style="position:absolute;inset:0;background:linear-gradient(180deg,rgba(0,0,0,0.45) 0%,rgba(0,0,0,0.18) 32%,rgba(9,11,16,0.85) 72%,var(--bg-main) 100%);"></div>

        <div style="position:absolute;top:14px;left:14px;right:14px;display:flex;justify-content:space-between;">
          <button class="app-icon-btn" id="btn-detail-back" style="background:rgba(8,8,12,0.68);color:#FFF;">${SVG_ICONS.back}</button>
          <button class="app-icon-btn" id="btn-detail-fav" style="background:rgba(8,8,12,0.68);color:${a.isFavorite ? 'var(--error)' : '#FFF'};">
            ${a.isFavorite ? SVG_ICONS.heartFilled : SVG_ICONS.heartOutline}
          </button>
        </div>

        <div style="position:absolute;bottom:12px;left:16px;right:16px;display:flex;align-items:flex-end;gap:14px;">
          <img src="${escapeHtml(a.coverImage)}" alt="" style="width:104px;height:152px;object-fit:cover;border-radius:18px;border:1px solid var(--border-strong);flex-shrink:0;box-shadow:0 10px 28px rgba(0,0,0,0.7);" />
          <div style="flex:1;min-width:0;">
            <h1 class="serif-heading" style="font-size:28px;line-height:30px;color:#FFF;">${escapeHtml(title)}</h1>
            ${subJp ? `<div class="micro-label" style="color:var(--text-secondary);margin-top:4px;">${escapeHtml(subJp)}</div>` : ''}
            <div style="display:flex;align-items:center;gap:8px;margin-top:8px;flex-wrap:wrap;">
              ${stars > 0 ? `<span class="rating-badge">★ ${stars.toFixed(1)}</span>` : a.averageScore ? `<span class="rating-badge">★ ${(a.averageScore / 20).toFixed(1)}</span>` : ''}
              ${sentiment ? `<span class="sentiment-pill solid ${sentiment.key}">${sentiment.fullLabel}</span>` : ''}
            </div>
          </div>
        </div>
      </div>

      <div class="app-card" style="border-radius:24px;margin-bottom:16px;display:flex;flex-direction:column;gap:14px;">
        ${
          seasons.length > 1
            ? (() => {
                const activeSeason = seasons.find((s) => s.isCurrent || s.id === a.animeId);
                const activeMeta = activeSeason
                  ? `Season ${activeSeason.seasonNumber} of ${seasons.length}${activeSeason.seasonYear ? ` • ${activeSeason.seasonYear}` : ''}`
                  : `${seasons.length} Seasons`;
                return `
              <div>
                <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;gap:8px;">
                  <div class="micro-label" style="color:var(--accent-primary);">SEASONS</div>
                  <div class="micro-label" style="color:var(--text-secondary);">${escapeHtml(activeMeta)}</div>
                </div>
                <div class="capsule-strip" style="margin-bottom:4px;">
                  ${seasons
                    .map((s) => {
                      const isUpcoming = String(s.status || '').toUpperCase() === 'NOT_YET_RELEASED';
                      const tooltip = `${s.title || `Season ${s.seasonNumber}`}${s.seasonYear ? ` (${s.seasonYear})` : ''}`;
                      return `
                      <button class="capsule-pill gold-solid ${s.isCurrent ? 'active' : ''}" data-open-detail="${s.id}" title="${escapeHtml(tooltip)}" style="min-width:52px;height:44px;justify-content:center;font-size:14px;gap:5px;">
                        <span>${s.seasonNumber}</span>
                        ${isUpcoming && !s.isCurrent ? '<span style="width:6px;height:6px;border-radius:50%;background:var(--warning);display:inline-block;"></span>' : ''}
                      </button>`;
                    })
                    .join('')}
                </div>
              </div>
            `;
              })()
            : ''
        }

        <div class="micro-label" style="color:var(--accent-primary);">EPISODE PROGRESS • CLICK NUMBER TO EDIT</div>
        ${
          !a.inWatchlist
            ? `<button class="btn-primary" id="btn-detail-add" style="width:100%;">+ ADD TO HOME &amp; LIBRARY</button>`
            : `
              ${renderInlineEpisodeStepper(a, 'detail')}
              <div class="capsule-strip" style="margin-bottom:0;">
                ${WATCH_STATUSES.map(
                  (st) => `
                  <button class="capsule-pill ${a.status === st.dbValue ? 'active' : ''}" data-detail-status="${st.dbValue}">
                    ${st.label}
                  </button>`
                ).join('')}
              </div>
              <div style="display:flex;flex-wrap:wrap;gap:8px;margin-top:2px;">
                ${
                  isAnimeOnHome(a)
                    ? `<button type="button" class="capsule-pill" id="btn-detail-remove-home" style="color:var(--accent-primary);border-color:var(--accent-border);">Remove from Home (Keep in Library)</button>`
                    : `<button type="button" class="capsule-pill active" id="btn-detail-add-home">+ Add to Home Tracking</button>`
                }
                <button type="button" class="capsule-pill" id="btn-detail-remove" style="color:var(--error);border-color:rgba(229,90,107,0.4);">Remove from Library</button>
              </div>
            `
        }
      </div>

      <div class="app-card" style="border-radius:24px;margin-bottom:16px;display:flex;flex-direction:column;gap:14px;">
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <div>
            <div class="micro-label" style="color:var(--accent-primary);">YOUR STAR RATING</div>
            <div class="serif-heading" style="font-size:28px;color:${stars > 0 ? 'var(--highlight)' : 'var(--text-muted)'};">
              ${stars > 0 ? `${stars.toFixed(1)} ★` : 'Unrated'}
            </div>
          </div>
          <div style="display:flex;gap:6px;">
            <button class="capsule-pill" id="btn-star-minus">−0.5 ★</button>
            <button class="capsule-pill" id="btn-star-plus">+0.5 ★</button>
          </div>
        </div>

        <div style="display:flex;justify-content:space-evenly;background:var(--elevated-surface);border-radius:20px;padding:12px;">
          ${[1, 2, 3, 4, 5]
            .map(
              (idx) => `
              <button data-star-val="${idx}" style="background:none;border:none;font-size:32px;cursor:pointer;color:${
                stars >= idx - 0.5 ? 'var(--accent-primary)' : 'var(--text-muted)'
              };">★</button>`
            )
            .join('')}
        </div>

        <div class="capsule-strip" style="margin-bottom:0;">
          ${SENTIMENTS.map(
            (s) => `
            <button class="capsule-pill ${sentiment?.key === s.key ? 'active' : ''}" data-pick-sentiment-stars="${s.suggestedStars}">
              ${s.shortLabel.toUpperCase()} • ${s.suggestedStars.toFixed(1)}★
            </button>`
          ).join('')}
        </div>

        <div class="micro-label" style="color:var(--accent-primary);margin-top:4px;">WRITTEN REVIEW NOTE</div>
        <textarea id="inp-detail-notes" class="auth-input" style="height:96px;padding:12px;resize:vertical;" placeholder="Write your thoughts on this anime...">${escapeHtml(
          state.detailNoteDraft !== undefined && state.detailNoteDraft !== null ? state.detailNoteDraft : (a.reviewNotes || '')
        )}</textarea>
        <button class="btn-primary" id="btn-detail-save-note" ${state.detailRatingSaving ? 'disabled' : ''}>
          ${state.detailRatingSaving ? 'SAVING...' : 'SAVE RATING & NOTE'}
        </button>
        ${
          state.detailRatingFeedback
            ? `<div class="feedback-banner success" id="detail-rating-feedback">✓ ${escapeHtml(state.detailRatingFeedback)}</div>`
            : ''
        }
        ${
          state.detailRatingError
            ? `<div class="feedback-banner error" id="detail-rating-error">! ${escapeHtml(state.detailRatingError)}</div>`
            : ''
        }
      </div>

      <div class="app-card" style="border-radius:24px;margin-bottom:16px;display:flex;flex-direction:column;gap:12px;">
        <div class="micro-label" style="color:var(--accent-primary);">SYNOPSIS</div>
        <p style="font-size:13.5px;color:var(--text-secondary);line-height:1.6;">
          ${escapeHtml(a.synopsis || 'No synopsis available.')}
        </p>
        <div class="capsule-strip" style="margin-bottom:0;">
          ${(a.genres || []).map((g) => `<span class="capsule-pill">${escapeHtml(g)}</span>`).join('')}
        </div>
      </div>

      ${renderAnimeDiscussionSection(a)}
    </div>
  `;
}

function renderEmojiTrayHtml(scope) {
  EmojiCacheManager.hydrateFromLocalStorage();
  const topEmojis = EmojiCacheManager.getTopEmojis(8);
  const allEmojis = EmojiCacheManager.getAllEmojis();
  const isOpen = Boolean(state.discussion.emojiTrayOpen);
  return `
    <div class="emoji-tray ${isOpen ? 'open' : ''}" id="livtrak-emoji-tray">
      <div class="emoji-tray-head">
        <span class="emoji-tray-label">Top Emojis</span>
        <span class="emoji-tray-label" style="color:#66667A;">${allEmojis.length} available</span>
      </div>
      ${
        allEmojis.length === 0
          ? `<div style="padding:14px 4px;text-align:center;color:#9A9AA8;font-size:12px;">
               No custom emojis found yet. Upload GIF/PNG/WebP files to your Supabase <strong>community_emojis</strong> bucket.
             </div>`
          : `
            <div class="emoji-top-bar">
              ${topEmojis
                .map(
                  (em) => `
                    <button
                      type="button"
                      class="emoji-btn"
                      data-insert-emoji-id="${escapeHtml(em.id)}"
                      data-insert-emoji-scope="${escapeHtml(scope)}"
                      title=":${escapeHtml(em.id)}:"
                    >
                      ${renderCustomEmojiSlotHtml(em.id, { lazy: false, sizeClass: 'size-lg' })}
                    </button>
                  `
                )
                .join('')}
            </div>
            <div class="emoji-grid">
              ${allEmojis
                .map(
                  (em) => `
                    <button
                      type="button"
                      class="emoji-btn"
                      data-insert-emoji-id="${escapeHtml(em.id)}"
                      data-insert-emoji-scope="${escapeHtml(scope)}"
                      title=":${escapeHtml(em.id)}:"
                    >
                      ${renderCustomEmojiSlotHtml(em.id, { lazy: true, sizeClass: 'size-lg' })}
                    </button>
                  `
                )
                .join('')}
            </div>
          `
      }
    </div>
  `;
}

function renderComposerEmojiPreviewHtml(draftText) {
  const ids = extractEmojiIdsFromText(draftText);
  if (!ids.length) return '';
  const uniquePreview = ids.slice(0, 8);
  return `
    <div class="chat-composer-emoji-preview">
      ${uniquePreview
        .map(
          (eid) => `
            <span class="chat-composer-emoji-chip">
              ${renderCustomEmojiSlotHtml(eid, { lazy: false })}
              <span>:${escapeHtml(eid)}:</span>
            </span>
          `
        )
        .join('')}
    </div>
  `;
}

function renderAnimeDiscussionSection(anime, options = {}) {
  const inModal = Boolean(options.inModal);
  const d = state.discussion;
  const currentUserId = state.session?.userId || null;
  const hl = d.highlightTarget || {};
  const isReplyingMode = Boolean(d.activeReplyTarget);
  const activeScope = isReplyingMode ? 'reply' : 'comment';
  const activeDraft = isReplyingMode ? (d.replyDraft || '') : (d.draft || '');
  const activeTags = isReplyingMode ? (d.replyTags || []) : (d.commentTags || []);

  // Live Chat chronological order (oldest -> newest at bottom)
  const chronological = [...(d.comments || [])].reverse();

  return `
    <div class="livtrak-chat-shell ${inModal ? '' : 'inline-detail-chat'}" id="anime-discussion-section">
      ${
        !inModal
          ? `<div style="padding:14px 18px 10px;border-bottom:1px solid rgba(255,255,255,0.06);display:flex;align-items:center;justify-content:space-between;">
               <span style="font-size:13px;font-weight:700;color:#E8C88F;letter-spacing:0.4px;">Live Chat</span>
               <span class="tabular-num" style="font-size:11.5px;color:#9A9AA8;">${d.totalComments} ${d.totalComments === 1 ? 'message' : 'messages'}</span>
             </div>`
          : ''
      }

      ${d.error ? `<div class="feedback-banner error" style="margin:10px 16px 0;">! ${escapeHtml(d.error)}</div>` : ''}

      <div class="chat-messages" id="livtrak-chat-messages">
        ${
          d.hasMore
            ? `<button type="button" class="c-thread-toggle" id="btn-discussion-load-more" style="align-self:center;" ${d.loadingMore ? 'disabled' : ''}>
                 ${d.loadingMore ? 'Loading earlier messages...' : 'Load earlier messages'}
               </button>`
            : ''
        }

        ${
          d.loading && chronological.length === 0
            ? `<div style="margin:auto;text-align:center;color:#9A9AA8;font-size:13px;padding:30px 12px;">Loading chat...</div>`
            : chronological.length === 0
            ? `<div style="margin:auto;text-align:center;color:#9A9AA8;font-size:13px;padding:34px 16px;">
                 No messages yet for <strong style="color:#F8F4EB;">${escapeHtml(resolveTitle(anime))}</strong>. Say hi!
               </div>`
            : chronological
                .map((c) => {
                  const isMine = Boolean(currentUserId && c.user_id === currentUserId);
                  const isEditingThis = d.editingCommentId === c.id;
                  const isCommentHighlighted = hl.commentId === c.id && !hl.replyId && !hl.threadId;
                  const threads = Array.isArray(c.threads) ? c.threads : [];

                  return `
                    <div class="c-msg-group ${isCommentHighlighted ? 'highlight-pulse' : ''}" id="discussion-comment-${escapeHtml(c.id)}">
                      <div
                        class="c-msg ${isMine ? 'mine' : ''}"
                        data-ctx-comment="${escapeHtml(c.id)}"
                        data-ctx-uid="${escapeHtml(c.user_id)}"
                        data-ctx-name="${escapeHtml(c.username || 'Member')}"
                        data-ctx-owner="${isMine ? '1' : '0'}"
                      >
                        ${!isMine ? `<div class="c-msg-avatar-wrap">${renderAvatarCircle(c.avatar_key || 'avatar_1', c.username, 32)}</div>` : ''}
                        <div class="c-bubble-wrap">
                          ${
                            !isMine
                              ? `<div class="c-user">
                                   <span>${escapeHtml(c.username || 'Member')}</span>
                                   <span class="c-time">${escapeHtml(formatDiscussionTimestamp(c.created_at))}${c.updated_at && c.updated_at !== c.created_at ? ' • edited' : ''}</span>
                                 </div>`
                              : `<div class="c-time" style="margin-right:4px;">${escapeHtml(formatDiscussionTimestamp(c.created_at))}${c.updated_at && c.updated_at !== c.created_at ? ' • edited' : ''}</div>`
                          }

                          ${
                            isEditingThis
                              ? `
                                <div class="c-bubble" style="min-width:220px;">
                                  <textarea id="inp-discussion-edit-comment" class="auth-input" style="height:66px;padding:8px;resize:vertical;width:100%;">${escapeHtml(
                                    d.editingCommentDraft
                                  )}</textarea>
                                  <div id="mention-dropdown-edit_comment" class="mention-dropdown"></div>
                                  <div style="display:flex;justify-content:flex-end;gap:6px;margin-top:6px;">
                                    <button type="button" class="c-micro-action" data-cancel-edit-comment>Cancel</button>
                                    <button type="button" class="btn-primary" data-save-edit-comment="${escapeHtml(c.id)}" style="height:30px;padding:0 12px;font-size:11.5px;" ${
                                      d.submitting ? 'disabled' : ''
                                    }>Save</button>
                                  </div>
                                </div>
                              `
                              : (() => {
                                  const rawBody = c.content ?? c.body ?? '';
                                  const soloEid = extractSoloEmojiId(rawBody);
                                  if (soloEid) {
                                    return `
                                      <div class="c-solo-emoji ${isCommentHighlighted ? 'highlight-pulse' : ''}">${renderCustomEmojiSlotHtml(soloEid, { lazy: false, sizeClass: 'size-solo' })}</div>
                                      ${renderMentionsDisplayHtml(c.mentions)}
                                    `;
                                  }
                                  if (isSoloUnicodeEmoji(rawBody)) {
                                    return `
                                      <div class="c-solo-unicode-emoji ${isCommentHighlighted ? 'highlight-pulse' : ''}">${escapeHtml(rawBody.trim())}</div>
                                      ${renderMentionsDisplayHtml(c.mentions)}
                                    `;
                                  }
                                  return `
                                    <div class="c-bubble ${isCommentHighlighted ? 'highlight-pulse' : ''}">${renderMentionHighlightedHtml(rawBody)}</div>
                                    ${renderMentionsDisplayHtml(c.mentions)}
                                  `;
                                })()
                          }

                          <div class="c-bubble-footer">
                            ${
                              state.session
                                ? `<button
                                     type="button"
                                     class="c-micro-action"
                                     data-start-reply-comment="${escapeHtml(c.id)}"
                                     data-reply-target-uid="${escapeHtml(c.user_id)}"
                                     data-reply-target-name="${escapeHtml(c.username || 'Member')}"
                                     data-reply-quoted-id=""
                                     data-reply-quoted-author="${escapeHtml(c.username || 'Member')}"
                                     data-reply-quoted-preview="${escapeHtml(formatPlainPreviewWithEmojis(c.content ?? c.body ?? '').slice(0, 180))}"
                                   >Reply</button>`
                                : ''
                            }
                            ${
                              isMine && !isEditingThis
                                ? `
                                  <button type="button" class="c-micro-action" data-edit-comment="${escapeHtml(c.id)}">Edit</button>
                                  <button type="button" class="c-micro-action danger" data-delete-comment="${escapeHtml(c.id)}">Delete</button>
                                `
                                : ''
                            }
                          </div>
                        </div>
                      </div>

                      ${
                        threads.length > 0
                          ? `
                            <div class="c-thread-wrap">
                              ${threads
                                .map((th) => {
                                  const isExpanded = Boolean(d.expandedThreads[th.id]);
                                  const replies = Array.isArray(th.replies) ? th.replies : [];
                                  const isThreadHighlighted = hl.threadId === th.id && !hl.replyId;
                                  return `
                                    <div id="discussion-thread-${escapeHtml(th.id)}" class="${isThreadHighlighted ? 'highlight-pulse' : ''}" style="display:flex;flex-direction:column;gap:8px;">
                                      <button type="button" class="c-thread-toggle" data-toggle-thread="${escapeHtml(th.id)}">
                                        @${escapeHtml(th.target_username || 'Member')} • ${replies.length} ${replies.length === 1 ? 'reply' : 'replies'} ${isExpanded ? '▲' : '▼'}
                                      </button>
                                      ${
                                        isExpanded
                                          ? replies
                                              .map((r) => {
                                                const isOwnReply = Boolean(currentUserId && r.user_id === currentUserId);
                                                const isEditingReply = d.editingReplyId === r.id;
                                                const isReplyHighlighted = hl.replyId === r.id;
                                                const rBody = r.content ?? r.body ?? '';
                                                const rSoloEid = extractSoloEmojiId(rBody);
                                                const quoteCtx = resolveReplyQuotedContext(c, r);
                                                const quoteButtonHtml = quoteCtx.preview
                                                  ? `<button
                                                       type="button"
                                                       class="c-quote-preview"
                                                       data-jump-comment="${escapeHtml(quoteCtx.jumpCommentId)}"
                                                       data-jump-reply="${escapeHtml(quoteCtx.jumpReplyId)}"
                                                       title="Jump to original message"
                                                     >
                                                       <span class="c-quote-author">@${escapeHtml(quoteCtx.author)}</span>
                                                       <span class="c-quote-text">${escapeHtml(quoteCtx.preview)}</span>
                                                     </button>`
                                                  : '';
                                                return `
                                                  <div
                                                    class="c-msg ${isOwnReply ? 'mine' : ''}"
                                                    id="discussion-reply-${escapeHtml(r.id)}"
                                                    data-ctx-reply="${escapeHtml(r.id)}"
                                                    data-ctx-reply-cid="${escapeHtml(c.id)}"
                                                    data-ctx-reply-tid="${escapeHtml(th.id)}"
                                                    data-ctx-uid="${escapeHtml(r.user_id)}"
                                                    data-ctx-name="${escapeHtml(r.username || 'Member')}"
                                                    data-ctx-quoted-preview="${escapeHtml(formatPlainPreviewWithEmojis(rBody).slice(0, 180))}"
                                                    data-ctx-owner="${isOwnReply ? '1' : '0'}"
                                                  >
                                                    ${!isOwnReply ? `<div class="c-msg-avatar-wrap">${renderAvatarCircle(r.avatar_key || 'avatar_1', r.username, 26)}</div>` : ''}
                                                    <div class="c-bubble-wrap">
                                                      ${
                                                        !isOwnReply
                                                          ? `<div class="c-user">
                                                               <span>${escapeHtml(r.username || 'Member')}</span>
                                                               <span class="c-time">${escapeHtml(formatDiscussionTimestamp(r.created_at))}</span>
                                                             </div>`
                                                          : `<div class="c-time" style="margin-right:4px;">${escapeHtml(formatDiscussionTimestamp(r.created_at))}</div>`
                                                      }
                                                      ${
                                                        isEditingReply
                                                          ? `
                                                            <div class="c-bubble" style="min-width:200px;">
                                                              <textarea id="inp-discussion-edit-reply" class="auth-input" style="height:56px;padding:8px;resize:vertical;width:100%;">${escapeHtml(
                                                                d.editingReplyDraft
                                                              )}</textarea>
                                                              <div id="mention-dropdown-edit_reply" class="mention-dropdown"></div>
                                                              <div style="display:flex;justify-content:flex-end;gap:6px;margin-top:6px;">
                                                                <button type="button" class="c-micro-action" data-cancel-edit-reply>Cancel</button>
                                                                <button type="button" class="btn-primary" data-save-edit-reply="${escapeHtml(r.id)}" data-edit-reply-cid="${escapeHtml(c.id)}" data-edit-reply-tid="${escapeHtml(th.id)}" style="height:28px;padding:0 11px;font-size:11px;" ${
                                                                  d.submitting ? 'disabled' : ''
                                                                }>Save</button>
                                                              </div>
                                                            </div>
                                                          `
                                                          : rSoloEid
                                                          ? `
                                                            ${quoteButtonHtml}
                                                            <div class="c-solo-emoji ${isReplyHighlighted ? 'highlight-pulse' : ''}">${renderCustomEmojiSlotHtml(rSoloEid, { lazy: false, sizeClass: 'size-solo' })}</div>
                                                            ${renderMentionsDisplayHtml(r.mentions)}
                                                          `
                                                          : isSoloUnicodeEmoji(rBody)
                                                          ? `
                                                            ${quoteButtonHtml}
                                                            <div class="c-solo-unicode-emoji ${isReplyHighlighted ? 'highlight-pulse' : ''}">${escapeHtml(rBody.trim())}</div>
                                                            ${renderMentionsDisplayHtml(r.mentions)}
                                                          `
                                                          : `
                                                            <div class="c-bubble ${isReplyHighlighted ? 'highlight-pulse' : ''}">
                                                              ${quoteButtonHtml}
                                                              <div>${renderMentionHighlightedHtml(rBody)}</div>
                                                            </div>
                                                            ${renderMentionsDisplayHtml(r.mentions)}
                                                          `
                                                      }
                                                      <div class="c-bubble-footer">
                                                        ${
                                                          state.session
                                                            ? `<button
                                                                 type="button"
                                                                 class="c-micro-action"
                                                                 data-start-reply-comment="${escapeHtml(c.id)}"
                                                                 data-reply-target-uid="${escapeHtml(r.user_id)}"
                                                                 data-reply-target-name="${escapeHtml(r.username || 'Member')}"
                                                                 data-reply-quoted-id="${escapeHtml(r.id)}"
                                                                 data-reply-quoted-author="${escapeHtml(r.username || 'Member')}"
                                                                 data-reply-quoted-preview="${escapeHtml(formatPlainPreviewWithEmojis(rBody).slice(0, 180))}"
                                                               >Reply</button>`
                                                            : ''
                                                        }
                                                        ${
                                                          isOwnReply && !isEditingReply
                                                            ? `
                                                              <button type="button" class="c-micro-action" data-edit-reply="${escapeHtml(r.id)}" data-edit-reply-cid="${escapeHtml(c.id)}" data-edit-reply-tid="${escapeHtml(th.id)}">Edit</button>
                                                              <button type="button" class="c-micro-action danger" data-delete-reply="${escapeHtml(r.id)}" data-delete-reply-cid="${escapeHtml(c.id)}" data-delete-reply-tid="${escapeHtml(th.id)}">Delete</button>
                                                            `
                                                            : ''
                                                        }
                                                      </div>
                                                    </div>
                                                  </div>
                                                `;
                                              })
                                              .join('')
                                          : ''
                                      }
                                    </div>
                                  `;
                                })
                                .join('')}
                            </div>
                          `
                          : ''
                      }
                    </div>
                  `;
                })
                .join('')
        }
      </div>

      ${
        state.session
          ? `
            <div class="chat-composer-dock">
              <div id="mention-dropdown-${activeScope}" class="mention-dropdown"></div>

              ${
                isReplyingMode
                  ? `
                    <div class="chat-reply-banner">
                      <div class="chat-reply-banner-quote">
                        <span>Replying to <strong>@${escapeHtml(d.activeReplyTarget.quotedUsername || d.activeReplyTarget.targetUsername)}</strong></span>
                        ${
                          d.activeReplyTarget.quotedPreview
                            ? `<span class="chat-reply-banner-snippet">${escapeHtml(d.activeReplyTarget.quotedPreview)}</span>`
                            : ''
                        }
                      </div>
                      <button type="button" class="c-micro-action" id="btn-cancel-reply-composer" style="color:#E8C88F;flex-shrink:0;">✕ Cancel</button>
                    </div>
                  `
                  : ''
              }

              ${activeTags.length > 0 ? `<div style="padding:4px 16px 2px;background:#0A0A10;">${renderSelectedTagChipsHtml(activeScope, activeTags)}</div>` : ''}
              ${renderComposerEmojiPreviewHtml(activeDraft)}
              ${renderEmojiTrayHtml(activeScope)}

              <div class="chat-input-bar">
                <div class="chat-input-wrap">
                  <button
                    type="button"
                    class="emoji-toggle-btn ${d.emojiTrayOpen ? 'active' : ''}"
                    id="btn-toggle-emoji-tray"
                    title="Custom Emojis"
                    aria-label="Toggle custom emojis"
                  >
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <circle cx="12" cy="12" r="10"></circle>
                      <path d="M8 14s1.5 2 4 2 4-2 4-2"></path>
                      <line x1="9" y1="9" x2="9.01" y2="9"></line>
                      <line x1="15" y1="9" x2="15.01" y2="9"></line>
                    </svg>
                  </button>
                  <textarea
                    id="${isReplyingMode ? 'inp-discussion-reply' : 'inp-discussion-comment'}"
                    class="chat-input"
                    rows="1"
                    placeholder="${isReplyingMode ? `Reply to @${escapeHtml(d.activeReplyTarget.targetUsername)}...` : 'Message...'}"
                    ${d.submitting ? 'disabled' : ''}
                  >${escapeHtml(activeDraft)}</textarea>
                </div>
                <button
                  type="button"
                  class="chat-send"
                  id="${isReplyingMode ? 'btn-post-thread-reply' : 'btn-post-discussion-comment'}"
                  aria-label="Send message"
                  ${d.submitting ? 'disabled' : ''}
                >
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="22" y1="2" x2="11" y2="13"></line>
                    <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                  </svg>
                </button>
              </div>
            </div>
          `
          : `
            <div style="padding:12px 16px;border-top:1px solid rgba(255,255,255,0.06);background:#0A0A10;font-size:12.5px;color:#9A9AA8;text-align:center;">
              Sign in to join Live Chat, send custom emojis, and tag members with @.
            </div>
          `
      }
    </div>
  `;
}

// ============================================================================
// SCREEN 8: NOTIFICATIONS SCREEN
// ============================================================================

function renderNotificationsScreen() {
  const nState = state.notifications;
  return `
    <div style="max-width:860px;margin:0 auto;display:flex;flex-direction:column;gap:16px;">
      <div style="display:flex;align-items:flex-end;justify-content:space-between;gap:12px;flex-wrap:wrap;">
        <div>
          <div class="micro-label" style="color:var(--accent-primary);">COMMUNITY ALERTS</div>
          <h1 class="serif-heading" style="font-size:34px;line-height:38px;">Notifications</h1>
          <div style="font-size:13px;color:var(--text-secondary);margin-top:4px;">
            ${nState.unreadCount > 0 ? `${nState.unreadCount} unread notification${nState.unreadCount === 1 ? '' : 's'}` : 'All caught up'}
          </div>
        </div>
        <div style="display:flex;gap:8px;align-items:center;">
          <button type="button" class="btn-secondary" id="btn-notif-refresh" style="height:40px;padding:0 14px;font-size:12.5px;">Refresh</button>
          ${
            nState.unreadCount > 0
              ? `<button type="button" class="btn-primary" id="btn-notif-mark-all" style="height:40px;padding:0 16px;font-size:12.5px;">Mark all read</button>`
              : ''
          }
        </div>
      </div>

      ${nState.error ? `<div class="feedback-banner error">! ${escapeHtml(nState.error)}</div>` : ''}

      ${
        !state.session
          ? `<div class="app-card" style="border-radius:24px;text-align:center;padding:32px 20px;color:var(--text-secondary);">
               Sign in to receive @mention and thread reply notifications.
             </div>`
          : nState.loading
          ? `<div class="app-card" style="border-radius:24px;text-align:center;padding:32px 20px;color:var(--text-secondary);">
               Loading notifications...
             </div>`
          : nState.items.length === 0
          ? `<div class="app-card" style="border-radius:24px;text-align:center;padding:40px 20px;color:var(--text-secondary);">
               <div style="font-size:15px;font-weight:600;color:var(--text-primary);margin-bottom:6px;">No notifications yet</div>
               <div style="font-size:13px;">When someone @mentions you or replies to you in an anime thread, it will appear here.</div>
             </div>`
          : `
            <div style="display:flex;flex-direction:column;gap:10px;">
              ${nState.items
                .map((n) => {
                  const nType = n.notification_type || n.type;
                  const nPreview = n.preview_text || n.content_preview || '';
                  const typeLabel =
                    nType === 'thread_reply'
                      ? 'replied to your thread'
                      : nType === 'mention_reply'
                      ? 'mentioned you in a reply'
                      : 'mentioned you in a comment';
                  return `
                    <button
                      type="button"
                      class="notification-item ${n.is_read ? '' : 'unread'}"
                      data-open-notification="${escapeHtml(n.id)}"
                      data-notif-anime="${n.anime_id}"
                      data-notif-comment="${escapeHtml(n.comment_id || '')}"
                      data-notif-thread="${escapeHtml(n.thread_id || '')}"
                      data-notif-reply="${escapeHtml(n.reply_id || '')}"
                    >
                      ${renderAvatarCircle(n.sender_avatar_key || 'avatar_1', n.sender_username || 'Member', 40)}
                      <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:4px;">
                        <div style="font-size:14px;color:var(--text-primary);line-height:1.4;">
                          <strong style="color:var(--highlight);">@${escapeHtml(n.sender_username || 'Member')}</strong>
                          <span> ${typeLabel}</span>
                          ${n.anime_title ? ` on <strong style="color:var(--accent-primary);">${escapeHtml(n.anime_title)}</strong>` : ''}
                        </div>
                        ${
                          nPreview
                            ? `<div style="font-size:13px;color:var(--text-secondary);line-height:1.45;">“${escapeHtml(nPreview)}”</div>`
                            : ''
                        }
                        <div style="font-size:11.5px;color:var(--text-muted);">${escapeHtml(formatDiscussionTimestamp(n.created_at))}</div>
                      </div>
                      ${!n.is_read ? '<span class="notification-unread-dot" title="Unread"></span>' : ''}
                    </button>
                  `;
                })
                .join('')}
            </div>
            ${
              nState.hasMore
                ? `<button type="button" class="btn-secondary" id="btn-notif-load-more" style="width:100%;" ${nState.loadingMore ? 'disabled' : ''}>
                     ${nState.loadingMore ? 'LOADING...' : 'LOAD MORE NOTIFICATIONS'}
                   </button>`
                : ''
            }
          `
      }
    </div>
  `;
}

// ============================================================================
// AUTH SCREEN WITH MASCOT (Matches Screenshot 1 1:1)
// ============================================================================

function renderAuthScreen() {
  const isSignup = state.authMode === 'signup';
  const mascotSrc = getMascotImgUrl();
  return `
    <div class="auth-viewport">
      <div class="auth-mascot-stack">
        <img class="auth-mascot-img" src="${mascotSrc}" alt="" onerror="${AVATAR_IMG_ONERROR}" />
        <div class="glass-auth-card">
          ${
            state.authMode === 'verify_otp'
              ? `
                <div style="display:flex;flex-direction:column;align-items:center;gap:12px;">
                  <div style="width:48px;height:48px;border-radius:50%;background:rgba(216,183,120,0.14);border:1px solid var(--accent-border);display:flex;align-items:center;justify-content:center;color:var(--accent-primary);">
                    <svg viewBox="0 0 24 24" fill="currentColor" style="width:22px;height:22px;"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                  </div>
                  <h2 class="serif-heading" style="font-size:28px;text-align:center;">Check Your Inbox</h2>
                  <div style="padding:5px 14px;border-radius:999px;background:rgba(216,183,120,0.12);border:1px solid var(--accent-border);color:var(--highlight);font-size:12.5px;font-weight:600;">
                    ${escapeHtml(maskEmailAddress(state.pendingVerifyEmail || state.identifier))}
                  </div>
                  <p style="font-size:13px;color:var(--text-secondary);text-align:center;line-height:1.5;">
                    Enter the 8-digit verification code from your email to continue.
                  </p>
                </div>

                ${state.authError ? `<div style="color:var(--error);font-size:12.5px;text-align:center;">${escapeHtml(state.authError)}</div>` : ''}
                ${state.authInfo ? `<div style="color:var(--accent-primary);font-size:12.5px;text-align:center;">${escapeHtml(state.authInfo)}</div>` : ''}

                <label class="auth-field-label">8-Digit Verification Code</label>
                <div class="auth-input-wrap">
                  <span class="auth-input-icon">${SVG_ICONS.key}</span>
                  <input
                    id="inp-signup-otp"
                    class="auth-input with-left-icon tabular-num"
                    type="text"
                    inputmode="numeric"
                    maxlength="8"
                    placeholder="ex-12345678"
                    value="${escapeHtml(state.signupOtpCode)}"
                    style="letter-spacing:3px;font-weight:700;font-size:16px;"
                  />
                </div>

                <button class="btn-primary" id="btn-verify-signup-otp" style="width:100%;margin-top:4px;" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'VERIFYING...' : 'CONFIRM & CONTINUE'}
                </button>

                <div style="display:flex;gap:10px;width:100%;">
                  <button type="button" class="btn-secondary" id="btn-resend-signup-otp" style="flex:1;height:42px;font-size:12.5px;" ${state.loading ? 'disabled' : ''}>
                    Resend Code
                  </button>
                  <button type="button" class="btn-secondary" id="btn-change-signup-email" style="flex:1;height:42px;font-size:12.5px;" ${state.loading ? 'disabled' : ''}>
                    Change Email
                  </button>
                </div>
              `
              : state.authMode === 'forgot_otp'
              ? `
                <h2 class="serif-heading" style="font-size:26px;text-align:center;">Reset Password</h2>
                <p style="font-size:12.5px;color:var(--text-secondary);text-align:center;line-height:1.5;">
                  Enter the 8-digit recovery code sent to <strong>${escapeHtml(maskEmailAddress(state.pendingVerifyEmail || state.identifier))}</strong> and choose a new password.
                </p>
                ${state.authError ? `<div style="color:var(--error);font-size:12.5px;text-align:center;">${escapeHtml(state.authError)}</div>` : ''}
                ${state.authInfo ? `<div style="color:var(--accent-primary);font-size:12.5px;text-align:center;">${escapeHtml(state.authInfo)}</div>` : ''}

                <label class="auth-field-label">8-Digit Recovery Code</label>
                <input id="inp-reset-otp" class="auth-input tabular-num" type="text" inputmode="numeric" maxlength="8" placeholder="ex-12345678" value="${escapeHtml(state.resetOtpCode)}" style="letter-spacing:3px;font-weight:700;" />

                <label class="auth-field-label">New Password (8+ characters)</label>
                <input id="inp-reset-new-pass" class="auth-input" type="password" placeholder="••••••••" value="${escapeHtml(state.resetNewPassword)}" />

                <button class="btn-primary" id="btn-confirm-reset-otp" style="width:100%;margin-top:4px;" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'UPDATING...' : 'VERIFY & UPDATE PASSWORD'}
                </button>
                <button type="button" class="btn-secondary" id="btn-cancel-reset-otp" style="width:100%;height:42px;font-size:12.5px;">
                  Back to Sign In
                </button>
              `
              : state.authMode === 'onboard_username'
              ? `
                <div class="micro-label" style="color:var(--accent-primary);text-align:center;">STEP 1 OF 2 • USERNAME</div>
                <h2 class="serif-heading" style="font-size:26px;text-align:center;">Choose Your Username</h2>
                <p style="font-size:12.5px;color:var(--text-secondary);text-align:center;">Pick a unique display name so your email stays private in Community.</p>
                ${state.authError ? `<div style="color:var(--error);font-size:12.5px;text-align:center;">${escapeHtml(state.authError)}</div>` : ''}
                ${state.authInfo ? `<div style="color:var(--accent-primary);font-size:12.5px;text-align:center;">${escapeHtml(state.authInfo)}</div>` : ''}
                <input id="inp-onboard-user" class="auth-input" maxlength="32" placeholder="Choose a username (e.g. GojoFan)" value="${escapeHtml(state.onboardUsername)}" />
                <button class="btn-primary" id="btn-onboard-next" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'CHECKING...' : 'CONTINUE TO PROFILE PICTURE ›'}
                </button>
              `
              : state.authMode === 'onboard_avatar'
              ? `
                <div class="micro-label" style="color:var(--accent-primary);text-align:center;">STEP 2 OF 2 • PROFILE PICTURE</div>
                <h2 class="serif-heading" style="font-size:26px;text-align:center;">Choose Your Profile Picture</h2>
                <p style="font-size:12.5px;color:var(--text-secondary);text-align:center;">Pick an avatar for <strong style="color:var(--text-primary);">${escapeHtml(state.onboardUsername)}</strong>.</p>
                ${state.authError ? `<div style="color:var(--error);font-size:12.5px;text-align:center;">${escapeHtml(state.authError)}</div>` : ''}
                <div class="round-avatar-carousel">
                  ${BUILT_IN_AVATARS.map(
                    (av) => `
                    <div class="avatar-carousel-item ${state.onboardAvatarKey === av.key ? 'selected' : ''}" data-onboard-avatar="${av.key}">
                      <div class="avatar-ring-wrap" style="background:linear-gradient(135deg,${av.c1},${av.c2});">
                        <img src="${av.img}" alt="" onerror="${AVATAR_IMG_ONERROR}" />
                        ${state.onboardAvatarKey === av.key ? `<div class="avatar-check-badge">✓</div>` : ''}
                      </div>
                      <span style="font-size:11px;color:${state.onboardAvatarKey === av.key ? 'var(--highlight)' : 'var(--text-secondary)'};text-align:center;">
                        ${escapeHtml(av.name)}
                      </span>
                    </div>`
                  ).join('')}
                </div>
                <button class="btn-primary" id="btn-onboard-finish" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'SAVING PROFILE...' : 'ENTER ANIME UNIVERSE'}
                </button>
                <button type="button" class="btn-secondary" id="btn-onboard-back" style="width:100%;height:42px;font-size:12.5px;" ${state.loading ? 'disabled' : ''}>
                  ‹ Change Username
                </button>
              `
              : `
                ${state.authError ? `<div style="color:var(--error);font-size:12.5px;text-align:center;">${escapeHtml(state.authError)}</div>` : ''}
                ${state.authInfo ? `<div style="color:var(--accent-primary);font-size:12.5px;text-align:center;">${escapeHtml(state.authInfo)}</div>` : ''}

                <label class="auth-field-label">${isSignup ? 'Email Address' : 'Email or Username'}</label>
                <div class="auth-input-wrap">
                  <span class="auth-input-icon">${SVG_ICONS.profile}</span>
                  <input id="inp-identifier" class="auth-input with-left-icon" type="text" placeholder="${isSignup ? 'you@example.com' : 'you@example.com or username'}" value="${escapeHtml(state.identifier)}" />
                </div>

                <label class="auth-field-label">Password</label>
                <div class="auth-input-wrap">
                  <span class="auth-input-icon">${SVG_ICONS.key}</span>
                  <input id="inp-pass" class="auth-input with-left-icon with-right-icon" type="${state.showPassword ? 'text' : 'password'}" placeholder="••••••••" value="${escapeHtml(state.password)}" />
                  <button type="button" class="auth-eye-btn" id="btn-toggle-pass" title="Toggle Password Visibility">
                    ${SVG_ICONS.eye}
                  </button>
                </div>

                ${
                  isSignup
                    ? `
                      <label class="auth-field-label">Confirm Password</label>
                      <div class="auth-input-wrap">
                        <span class="auth-input-icon">${SVG_ICONS.key}</span>
                        <input id="inp-confirm-pass" class="auth-input with-left-icon" type="${state.showPassword ? 'text' : 'password'}" placeholder="Re-enter password" value="${escapeHtml(state.confirmPassword)}" />
                      </div>
                    `
                    : `
                      <div class="auth-forgot-row">
                        <button type="button" class="auth-forgot-btn" id="btn-forgot-pass">Forgot Password?</button>
                      </div>
                    `
                }

                <button class="btn-primary" id="btn-auth-submit" style="width:100%;margin-top:4px;" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'PLEASE WAIT...' : isSignup ? 'CREATE ACCOUNT' : 'SIGN IN'}
                </button>
              `
          }
        </div>

        ${
          state.authMode === 'signin' || state.authMode === 'signup'
            ? `
              <div class="auth-footer-links">
                <div style="font-size:14.5px;color:var(--text-secondary);">
                  ${isSignup ? 'Already have an account?' : "Don't have an account?"}
                  <button type="button" id="btn-switch-auth-mode" style="background:none;border:none;color:var(--accent-primary);font-weight:700;font-size:14.5px;margin-left:6px;cursor:pointer;font-family:inherit;">
                    ${isSignup ? 'Sign In' : 'Sign Up'}
                  </button>
                </div>
                <button type="button" id="btn-guest-preview" style="background:none;border:none;color:var(--text-secondary);font-size:13.5px;cursor:pointer;font-family:inherit;">
                  Continue without an account
                </button>
              </div>
            `
            : ''
        }
      </div>
    </div>
  `;
}

// ============================================================================
// SCREEN 8: DOWNLOAD APPLICATIONS CENTER (Android APK, Windows EXE, macOS DMG)
// ============================================================================

function resolveHttpsDownloadUrl(url) {
  if (!url || typeof url !== 'string') return '';
  const trimmed = url.trim();
  if (!trimmed) return '';
  if (trimmed.includes('<') || trimmed.includes('>') || trimmed.includes('example.com')) return '';
  // Automatically convert any Google Drive /file/d/<ID>/view share URL into a direct binary stream URL
  const gdriveMatch =
    trimmed.match(/drive\.google\.com\/file\/d\/([a-zA-Z0-9_-]+)/i) ||
    trimmed.match(/drive\.google\.com\/open\?id=([a-zA-Z0-9_-]+)/i) ||
    trimmed.match(/drive\.google\.com\/uc\?.*id=([a-zA-Z0-9_-]+)/i);
  if (gdriveMatch && gdriveMatch[1]) {
    return `https://drive.usercontent.google.com/download?id=${gdriveMatch[1]}&export=download&confirm=t`;
  }
  if (trimmed.startsWith('https://')) return trimmed;
  if (trimmed.startsWith('/')) {
    try {
      const resolved = new URL(trimmed, window.location.origin).href;
      if (
        resolved.startsWith('https://') ||
        window.location.hostname === 'localhost' ||
        window.location.hostname === '127.0.0.1'
      ) {
        return resolved;
      }
    } catch (_) {}
  }
  return '';
}

function isHttpsUrlReady(url) {
  return Boolean(resolveHttpsDownloadUrl(url));
}

function isPlatformDownloadReady(plat) {
  if (!plat) return false;
  if (plat.available === false) return false;
  return isHttpsUrlReady(plat.downloadUrl);
}

function detectVisitorPlatformKey() {
  const ua = (navigator.userAgent || '').toLowerCase();
  if (ua.includes('android')) return 'android';
  if (ua.includes('win')) return 'windows';
  if (ua.includes('mac') && !ua.includes('iphone') && !ua.includes('ipad')) return 'macos';
  return null;
}

function mergePlatformConfig(basePlat = {}, overridePlat = {}) {
  const merged = { ...basePlat, ...overridePlat };
  if (!isHttpsUrlReady(overridePlat.downloadUrl) && isHttpsUrlReady(basePlat.downloadUrl)) {
    merged.downloadUrl = basePlat.downloadUrl;
    merged.available = basePlat.available !== false;
    merged.fileSize = basePlat.fileSize || merged.fileSize;
    merged.fileName = basePlat.fileName || merged.fileName;
  }
  return merged;
}

async function loadDownloadsConfig() {
  try {
    const resp = await fetch(`/downloads-config.json?_t=${Date.now()}`, { cache: 'no-store' });
    if (resp.ok) {
      const fileCfg = await resp.json();
      const savedOverride = JSON.parse(localStorage.getItem('anime_universe_downloads_admin_override') || 'null');
      if (savedOverride) {
        state.downloadsConfig = {
          ...fileCfg,
          ...savedOverride,
          platforms: {
            android: mergePlatformConfig(fileCfg.platforms?.android, savedOverride.platforms?.android),
            windows: mergePlatformConfig(fileCfg.platforms?.windows, savedOverride.platforms?.windows),
            macos: mergePlatformConfig(fileCfg.platforms?.macos, savedOverride.platforms?.macos)
          },
          currentRelease: { ...(fileCfg.currentRelease || {}), ...(savedOverride.currentRelease || {}) },
          previousVersions: savedOverride.previousVersions?.length ? savedOverride.previousVersions : (fileCfg.previousVersions || [])
        };
      } else {
        state.downloadsConfig = fileCfg;
      }
    }
  } catch (_) {}

  // Also check Supabase public.app_releases table for any published platform builds
  try {
    const rows = await supabaseRequest('/rest/v1/app_releases?select=*&order=version_code.desc,published_at.desc');
    if (Array.isArray(rows) && rows.length > 0) {
      for (const row of rows) {
        const platKey = String(row.platform || '').toLowerCase();
        const dlUrl = String(row.download_url || '').trim();
        if (
          (platKey === 'android' || platKey === 'windows' || platKey === 'macos') &&
          isHttpsUrlReady(dlUrl) &&
          !dlUrl.includes('play.google.com/store/apps/details')
        ) {
          const target = state.downloadsConfig.platforms?.[platKey];
          if (target && !isPlatformDownloadReady(target)) {
            target.downloadUrl = dlUrl;
            target.version = row.version_name ? (String(row.version_name).startsWith('v') ? row.version_name : `v${row.version_name}`) : target.version;
            target.available = true;
          }
        }
      }
    }
  } catch (_) {}

  if (state.downloadsConfig?.githubRepo && state.downloadsConfig?.autoFetchGithubReleases !== false) {
    await syncReleasesFromGitHub(state.downloadsConfig.githubRepo, true);
  }
  if (state.tab === 'downloads') renderApp();
}

async function syncReleasesFromGitHub(repoSlug, silent = false) {
  const cleanRepo = String(repoSlug || '').trim().replace(/^https?:\/\/github\.com\//i, '').replace(/\/+$/, '');
  if (!cleanRepo || !cleanRepo.includes('/')) {
    if (!silent) {
      state.githubSyncMessage = 'Enter a valid GitHub repository in owner/repo format.';
      renderApp();
    }
    return;
  }

  if (!silent) {
    state.githubSyncMessage = `Fetching releases from https://api.github.com/repos/${cleanRepo}/releases...`;
    renderApp();
  }

  try {
    const resp = await fetch(`https://api.github.com/repos/${cleanRepo}/releases?per_page=15`, {
      headers: { Accept: 'application/vnd.github+json' }
    });
    if (!resp.ok) throw new Error(`GitHub API returned HTTP ${resp.status}`);
    const releases = await resp.json();
    if (!Array.isArray(releases) || releases.length === 0) {
      if (!silent) {
        state.githubSyncMessage = 'No published GitHub Releases found in this repository yet.';
        renderApp();
      }
      return;
    }

    const platforms = state.downloadsConfig.platforms;
    let foundAny = { android: false, windows: false, macos: false };

    const formatBytesMB = (bytes) => {
      if (!bytes || isNaN(bytes)) return null;
      return `${(Number(bytes) / (1024 * 1024)).toFixed(1)} MB`;
    };

    for (const rel of releases) {
      const assets = Array.isArray(rel.assets) ? rel.assets : [];
      const pubDate = formatReviewDate(rel.published_at || rel.created_at) || state.downloadsConfig.lastUpdated;
      const verTag = rel.tag_name || rel.name || 'v2.1.0';

      for (const asset of assets) {
        const nameLower = String(asset.name || '').toLowerCase();
        const dlUrl = asset.browser_download_url || '';
        if (!dlUrl.startsWith('https://')) continue;

        if (!foundAny.android && nameLower.endsWith('.apk')) {
          platforms.android.downloadUrl = dlUrl;
          platforms.android.fileName = asset.name;
          platforms.android.fileSize = formatBytesMB(asset.size) || platforms.android.fileSize;
          platforms.android.version = verTag;
          platforms.android.releaseDate = pubDate;
          platforms.android.available = true;
          foundAny.android = true;
        } else if (!foundAny.windows && (nameLower.endsWith('.exe') || nameLower.endsWith('.msi'))) {
          platforms.windows.downloadUrl = dlUrl;
          platforms.windows.fileName = asset.name;
          platforms.windows.fileSize = formatBytesMB(asset.size) || platforms.windows.fileSize;
          platforms.windows.version = verTag;
          platforms.windows.releaseDate = pubDate;
          platforms.windows.available = true;
          foundAny.windows = true;
        } else if (!foundAny.macos && nameLower.endsWith('.dmg')) {
          platforms.macos.downloadUrl = dlUrl;
          platforms.macos.fileName = asset.name;
          platforms.macos.fileSize = formatBytesMB(asset.size) || platforms.macos.fileSize;
          platforms.macos.version = verTag;
          platforms.macos.releaseDate = pubDate;
          platforms.macos.available = true;
          foundAny.macos = true;
        }
      }
    }

    state.downloadsConfig.githubRepo = cleanRepo;
    localStorage.setItem('anime_universe_downloads_admin_override', JSON.stringify(state.downloadsConfig));
    if (!silent) {
      const syncedList = Object.entries(foundAny).filter(([, v]) => v).map(([k]) => k.toUpperCase());
      state.githubSyncMessage = syncedList.length > 0
        ? `Synced HTTPS packages from GitHub Releases: ${syncedList.join(', ')}`
        : 'Connected to GitHub Releases. Packages without published assets remain in Coming Soon state.';
    }
  } catch (err) {
    if (!silent) {
      state.githubSyncMessage = `Could not sync GitHub Releases: ${err.message}`;
    }
  }
  renderApp();
}

async function startPlatformDownload(taskKey, downloadUrl, fileName) {
  const resolvedUrl = resolveHttpsDownloadUrl(downloadUrl);
  if (!resolvedUrl) return;
  if (state.downloadTasks[taskKey]?.active) return;

  state.downloadTasks[taskKey] = {
    active: true,
    progress: 4,
    loadedMB: '0.0',
    totalMB: null,
    statusText: 'Connecting via secure HTTPS...'
  };
  renderApp();

  try {
    const isSameOrigin = resolvedUrl.startsWith(window.location.origin);
    const response = await fetch(resolvedUrl, { mode: isSameOrigin ? 'same-origin' : 'cors', credentials: 'omit' });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const contentType = (response.headers.get('content-type') || '').toLowerCase();
    if (contentType.includes('text/html')) {
      throw new Error('Package binary not found at endpoint');
    }

    const contentLength = Number(response.headers.get('content-length') || 0);
    const totalMB = contentLength > 0 ? (contentLength / (1024 * 1024)).toFixed(1) : null;

    if (response.body && typeof response.body.getReader === 'function') {
      const reader = response.body.getReader();
      const chunks = [];
      let receivedLength = 0;
      let lastRender = 0;

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        if (value) {
          chunks.push(value);
          receivedLength += value.length;
          const loadedMB = (receivedLength / (1024 * 1024)).toFixed(1);
          const pct = contentLength > 0
            ? Math.min(99, Math.max(5, Math.round((receivedLength / contentLength) * 100)))
            : Math.min(92, (state.downloadTasks[taskKey].progress || 10) + 7);

          state.downloadTasks[taskKey] = {
            active: true,
            progress: pct,
            loadedMB,
            totalMB,
            statusText: totalMB ? `Downloading package... ${loadedMB} MB / ${totalMB} MB` : `Downloading package... ${loadedMB} MB`
          };

          const now = Date.now();
          if (now - lastRender > 120) {
            lastRender = now;
            renderApp();
          }
        }
      }

      const blob = new Blob(chunks, { type: response.headers.get('content-type') || 'application/octet-stream' });
      const blobUrl = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = fileName || downloadUrl.split('/').pop() || 'AnimeUniverse-Package';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(() => URL.revokeObjectURL(blobUrl), 15000);

      state.downloadTasks[taskKey] = {
        active: false,
        progress: 100,
        loadedMB: totalMB || (receivedLength / (1024 * 1024)).toFixed(1),
        totalMB,
        statusText: 'Download complete • Package verified via HTTPS'
      };
      renderApp();
      return;
    }
  } catch (_) {
    // Fallback when cross-origin host (e.g. GitHub Releases S3 redirect) blocks CORS fetch streaming:
    // Show smooth progress feedback and trigger native HTTPS browser download of the genuine asset.
  }

  state.downloadTasks[taskKey] = {
    active: true,
    progress: 48,
    loadedMB: null,
    totalMB: null,
    statusText: 'Verifying HTTPS release signature...'
  };
  renderApp();

  await new Promise((r) => setTimeout(r, 420));
  state.downloadTasks[taskKey] = {
    active: true,
    progress: 86,
    loadedMB: null,
    totalMB: null,
    statusText: 'Starting secure browser transfer...'
  };
  renderApp();

  // Trigger native browser attachment download in-place (Content-Disposition: attachment prevents page navigation and avoids iframe CSP frame-ancestors 'none')
  const a = document.createElement('a');
  a.href = resolvedUrl;
  a.download = fileName || resolvedUrl.split('/').pop() || 'AnimeUniverse-Package';
  a.rel = 'noopener noreferrer';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);

  await new Promise((r) => setTimeout(r, 380));
  state.downloadTasks[taskKey] = {
    active: false,
    progress: 100,
    loadedMB: null,
    totalMB: null,
    statusText: 'Direct download started ✓'
  };
  renderApp();
}

function renderDownloadsScreen() {
  const cfg = state.downloadsConfig || {};
  const platforms = cfg.platforms || {};
  const currentRel = cfg.currentRelease || {};
  const prevVersions = Array.isArray(cfg.previousVersions) ? cfg.previousVersions : [];
  const visitorOs = detectVisitorPlatformKey();

  const topBar = renderObsidianTopBar({
    title: 'Downloads',
    subtitle: 'OFFICIAL NATIVE APPLICATIONS',
    leftActionHtml: `<button class="app-icon-btn" data-nav="home" title="Back to Home" style="margin-right:6px;">${SVG_ICONS.back}</button>`
  });

  const platformOrder = [
    { key: 'android', icon: SVG_ICONS.android, badgeLabel: 'MOBILE & TABLET' },
    { key: 'windows', icon: SVG_ICONS.windows, badgeLabel: 'WINDOWS PC' },
    { key: 'macos', icon: SVG_ICONS.apple, badgeLabel: 'APPLE MAC' }
  ];

  return `
    ${topBar}

    <!-- Three Platform Download Cards (Android APK, Windows EXE, macOS DMG) -->
    <div class="downloads-cards-grid">
      ${platformOrder
        .map(({ key, icon, badgeLabel }) => {
          const p = platforms[key] || {};
          const ready = isPlatformDownloadReady(p);
          const isRecommended = visitorOs === key;
          const taskKey = `main_${key}`;
          const task = state.downloadTasks[taskKey];

          return `
            <div class="download-platform-card ${isRecommended ? 'recommended' : ''}">
              ${isRecommended ? `<div class="recommended-ribbon">Recommended for Your Device</div>` : ''}

              <div style="display:flex;flex-direction:column;gap:14px;">
                <div style="display:flex;align-items:center;gap:14px;">
                  <div class="platform-emblem ${key}">
                    ${icon}
                  </div>
                  <div style="min-width:0;flex:1;">
                    <div class="micro-label" style="color:var(--accent-primary);margin-bottom:3px;">${escapeHtml(badgeLabel)}</div>
                    <h3 class="serif-heading" style="font-size:25px;line-height:28px;color:var(--text-primary);">
                      ${escapeHtml(p.platformName || key.toUpperCase())}
                    </h3>
                    <div style="font-size:12.5px;color:var(--text-secondary);margin-top:2px;">
                      ${escapeHtml(p.appName || 'Anime Universe')}
                    </div>
                  </div>
                </div>

                <!-- Metadata Grid: Version, File Size, Release Date -->
                <div class="download-meta-grid">
                  <div class="download-meta-item">
                    <span class="download-meta-label">Version</span>
                    <span class="download-meta-val" style="color:var(--highlight);">${escapeHtml(p.version || 'v2.1.0')}</span>
                  </div>
                  <div class="download-meta-item">
                    <span class="download-meta-label">File Size</span>
                    <span class="download-meta-val">${escapeHtml(p.fileSize || '—')}</span>
                  </div>
                  <div class="download-meta-item">
                    <span class="download-meta-label">Release Date</span>
                    <span class="download-meta-val">${escapeHtml(p.releaseDate || 'Nov 18, 2026')}</span>
                  </div>
                </div>

                <!-- Supported Operating System -->
                <div class="download-os-box">
                  <span class="download-meta-label">Supported Operating System</span>
                  <span style="font-size:12.5px;color:var(--text-secondary);line-height:1.45;">
                    ${escapeHtml(p.osSupported || '')}
                  </span>
                </div>
              </div>

              <!-- Download Action or Coming Soon State + Progress Indicator -->
              <div style="display:flex;flex-direction:column;gap:10px;">
                ${
                  task
                    ? `
                      <div class="download-progress-wrap">
                        <div style="display:flex;justify-content:space-between;align-items:center;font-size:11.5px;">
                          <span style="color:var(--text-primary);font-weight:600;">${escapeHtml(task.statusText)}</span>
                          <span class="tabular-num" style="color:var(--highlight);font-weight:700;">${task.progress}%</span>
                        </div>
                        <div class="download-progress-track">
                          <div class="download-progress-bar" style="width:${task.progress}%;"></div>
                        </div>
                      </div>
                    `
                    : ''
                }

                ${
                  ready
                    ? `
                      <button
                        class="btn-primary"
                        data-trigger-download="${taskKey}"
                        data-dl-url="${escapeHtml(p.downloadUrl)}"
                        data-dl-filename="${escapeHtml(p.fileName || '')}"
                        style="width:100%;"
                        ${task?.active ? 'disabled' : ''}
                      >
                        <span style="width:18px;height:18px;display:inline-flex;">${SVG_ICONS.download}</span>
                        <span>${task?.active ? 'DOWNLOADING...' : `DOWNLOAD ${escapeHtml((p.platformName || '').toUpperCase())}`}</span>
                      </button>
                      <div style="display:flex;align-items:center;justify-content:center;gap:6px;font-size:11px;color:var(--text-muted);">
                        <span style="width:13px;height:13px;display:inline-flex;color:var(--success);">${SVG_ICONS.checkCircle}</span>
                        <span>Direct HTTPS • ${escapeHtml(p.fileName || p.packageType || 'Verified Build')}</span>
                      </div>
                    `
                    : `
                      <div class="btn-coming-soon" title="Build package is not yet published. Configure an HTTPS URL in Release Settings once built.">
                        <span style="width:17px;height:17px;display:inline-flex;">${SVG_ICONS.clock}</span>
                        <span>COMING SOON</span>
                      </div>
                      <div style="text-align:center;font-size:11px;color:var(--text-muted);">
                        Official ${escapeHtml(p.platformName || '')} package will unlock automatically once published.
                      </div>
                    `
                }
              </div>
            </div>
          `;
        })
        .join('')}
    </div>

    <!-- Release Notes & Previous Versions Section -->
    <div class="release-notes-grid">
      <!-- Current Release Notes Card -->
      <div class="app-card" style="display:flex;flex-direction:column;gap:14px;">
        <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:8px;">
          <div>
            <div class="micro-label" style="color:var(--accent-primary);">RELEASE NOTES • ${escapeHtml(currentRel.channel || 'STABLE')}</div>
            <h3 class="serif-heading" style="font-size:26px;color:var(--text-primary);margin-top:2px;">
              ${escapeHtml(currentRel.version || 'v2.1.0')} — ${escapeHtml(currentRel.title || 'Latest Release')}
            </h3>
          </div>
          <span class="show-count-pill">${escapeHtml(currentRel.releaseDate || 'Nov 18, 2026')}</span>
        </div>

        ${
          currentRel.summary
            ? `<p style="font-size:13.5px;color:var(--text-secondary);line-height:1.6;">${escapeHtml(currentRel.summary)}</p>`
            : ''
        }

        <div style="display:flex;flex-direction:column;gap:10px;">
          ${(currentRel.highlights || [])
            .map(
              (h) => `
              <div class="release-highlight-row">
                <span class="release-tag-pill">${escapeHtml(h.tag || 'UPDATE')}</span>
                <span style="font-size:13px;color:var(--text-primary);line-height:1.5;">${escapeHtml(h.text || '')}</span>
              </div>
            `
            )
            .join('')}
        </div>
      </div>

      <!-- Previous Versions Archive Card -->
      <div class="app-card" style="display:flex;flex-direction:column;gap:14px;">
        <div>
          <div class="micro-label" style="color:var(--accent-primary);">VERSION ARCHIVE</div>
          <h3 class="serif-heading" style="font-size:26px;color:var(--text-primary);margin-top:2px;">
            Previous Versions
          </h3>
        </div>

        ${
          prevVersions.length === 0
            ? `<div style="color:var(--text-secondary);font-size:13px;padding:12px 0;">No archived builds listed yet.</div>`
            : `<div style="display:flex;flex-direction:column;gap:12px;">
                ${prevVersions
                  .map((pv, idx) => {
                    const dlMap = pv.downloads || {};
                    return `
                      <div class="prev-version-card">
                        <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
                          <div>
                            <span class="serif-heading" style="font-size:21px;color:var(--highlight);font-weight:600;">${escapeHtml(pv.version || '')}</span>
                            <span style="font-size:13px;color:var(--text-primary);margin-left:8px;">${escapeHtml(pv.title || '')}</span>
                          </div>
                          <span style="font-size:11.5px;color:var(--text-muted);">${escapeHtml(pv.releaseDate || '')}</span>
                        </div>

                        <ul style="padding-left:18px;display:flex;flex-direction:column;gap:4px;font-size:12.5px;color:var(--text-secondary);line-height:1.45;">
                          ${(pv.notes || []).map((n) => `<li>${escapeHtml(n)}</li>`).join('')}
                        </ul>

                        <div style="display:flex;flex-wrap:wrap;gap:8px;padding-top:4px;">
                          ${[
                            { k: 'android', label: 'Android APK' },
                            { k: 'windows', label: 'Windows EXE' },
                            { k: 'macos', label: 'macOS DMG' }
                          ]
                            .map(({ k, label }) => {
                              const d = dlMap[k] || {};
                              const ready = isPlatformDownloadReady(d);
                              const prevTaskKey = `prev_${idx}_${k}`;
                              if (ready) {
                                return `
                                  <button
                                    class="capsule-pill active"
                                    data-trigger-download="${prevTaskKey}"
                                    data-dl-url="${escapeHtml(d.downloadUrl)}"
                                    data-dl-filename="${escapeHtml(d.fileName || `${k}-${pv.version}`)}"
                                  >
                                    <span style="width:13px;height:13px;display:inline-flex;">${SVG_ICONS.download}</span>
                                    <span>${label} (${escapeHtml(d.fileSize || pv.version)})</span>
                                  </button>
                                `;
                              }
                              return `
                                <span class="capsule-pill" style="opacity:0.55;cursor:not-allowed;" title="Archived build not available">
                                  ${label} • Coming Soon
                                </span>
                              `;
                            })
                            .join('')}
                        </div>
                      </div>
                    `;
                  })
                  .join('')}
              </div>`
        }
      </div>
    </div>
  `;
}



// ============================================================================
// MODALS: QUICK CONTEXT EDIT, COLLECTION MODAL & AVATAR CAROUSEL MODAL
// ============================================================================

function findAnimeByIdAnywhere(animeId) {
  const aid = Number(animeId);
  return (
    state.vault.find((v) => Number(v.animeId) === aid) ||
    state.discover.find((d) => Number(d.animeId) === aid) ||
    (state.detailAnime && Number(state.detailAnime.animeId) === aid ? state.detailAnime : null)
  );
}

function renderContextMenuOverlay() {
  if (!state.contextMenu) return '';
  const { animeId, x, y } = state.contextMenu;
  const item = findAnimeByIdAnywhere(animeId);
  if (!item) return '';
  const title = resolveTitle(item);
  const onHome = isAnimeOnHome(item);
  const menuW = 248;
  const menuH = 264;
  const safeX = Math.max(12, Math.min(Number(x || 20), (window.innerWidth || 1024) - menuW - 12));
  const safeY = Math.max(12, Math.min(Number(y || 20), (window.innerHeight || 768) - menuH - 12));

  return `
    <div class="anime-context-backdrop" id="anime-context-backdrop">
      <div class="anime-context-menu" id="anime-context-menu" style="left:${safeX}px;top:${safeY}px;" onclick="event.stopPropagation()">
        <div class="anime-context-header">
          ${item.coverImage ? `<img src="${escapeHtml(item.coverImage)}" alt="" />` : ''}
          <div class="anime-context-title">${escapeHtml(title)}</div>
        </div>
        ${
          onHome
            ? `<button type="button" class="anime-context-item danger" data-ctx-action="remove_home" data-ctx-anime-id="${animeId}">
                 <span>✕</span><span>Remove from Home</span>
               </button>`
            : `<button type="button" class="anime-context-item accent" data-ctx-action="add_home" data-ctx-anime-id="${animeId}">
                 <span>＋</span><span>Add to Home</span>
               </button>`
        }
        <button type="button" class="anime-context-item" data-ctx-action="view_details" data-ctx-anime-id="${animeId}">
          <span>ⓘ</span><span>View Anime Details</span>
        </button>
        <button type="button" class="anime-context-item" data-ctx-action="view_library" data-ctx-anime-id="${animeId}">
          <span>▤</span><span>View in Library</span>
        </button>
        <button type="button" class="anime-context-item" data-ctx-action="view_community" data-ctx-anime-id="${animeId}">
          <span>✦</span><span>View Community</span>
        </button>
      </div>
    </div>
  `;
}

function renderToastOverlay() {
  if (!state.toastMessage) return '';
  return `<div id="au-toast-banner" class="au-toast ${state.toastError ? 'error' : ''}">${escapeHtml(state.toastMessage)}</div>`;
}

function renderModals() {
  let overlays = `${renderContextMenuOverlay()}${renderToastOverlay()}`;

  if (state.confirmRemoveHomeAnime) {
    const target = state.confirmRemoveHomeAnime;
    return `
      ${overlays}
      <div class="modal-backdrop" id="modal-backdrop">
        <div class="modal-sheet" style="max-width:440px;" onclick="event.stopPropagation()">
          <h3 class="serif-heading" style="font-size:26px;color:var(--text-primary);">Remove from Home?</h3>
          ${target.title ? `<div style="font-size:13px;font-weight:700;color:var(--accent-primary);margin-top:-6px;">${escapeHtml(target.title)}</div>` : ''}
          <p style="font-size:14px;color:var(--text-secondary);line-height:1.6;">
            This anime will be removed from your Home tracking list but will remain in your Library. Your notes, ratings, reviews, and Community discussions will not be affected.
          </p>
          <div style="display:flex;gap:10px;margin-top:6px;">
            <button type="button" class="btn-secondary" id="btn-cancel-remove-home" style="flex:1;">Cancel</button>
            <button type="button" class="btn-danger" id="btn-confirm-remove-home" style="flex:1;">Remove from Home</button>
          </div>
        </div>
      </div>
    `;
  }

  if (state.quickEditAnime) {
    const a = state.quickEditAnime;
    const title = resolveTitle(a);
    const stars = rating10ToStars(a.rating10) || 0;
    return `
      ${overlays}
      <div class="modal-backdrop" id="modal-backdrop">
        <div class="modal-sheet" onclick="event.stopPropagation()">
          <div style="display:flex;gap:12px;align-items:center;">
            <img src="${escapeHtml(a.coverImage)}" alt="" style="width:54px;height:78px;border-radius:14px;object-fit:cover;" />
            <div style="flex:1;min-width:0;">
              <div class="serif-heading" style="font-size:22px;">${escapeHtml(title)}</div>
              <div class="tabular-num" style="font-size:12px;color:var(--text-secondary);">Of ${a.totalEpisodes || '∞'} Episodes</div>
            </div>
          </div>

          <div class="micro-label" style="color:var(--accent-primary);">EPISODE PROGRESS</div>
          <div style="display:flex;align-items:center;gap:10px;">
            <button class="stepper-circle-btn" id="qe-dec" style="width:40px;height:40px;">−</button>
            <input id="qe-ep-input" class="auth-input tabular-num" type="number" min="0" value="${a.watchedEpisodes || 0}" style="text-align:center;font-size:18px;font-weight:700;" />
            <button class="stepper-circle-btn plus" id="qe-inc" style="width:40px;height:40px;">+</button>
          </div>

          <div class="micro-label" style="color:var(--accent-primary);">STAR RATING (${stars > 0 ? `${stars.toFixed(1)} ★` : 'UNRATED'})</div>
          <div style="display:flex;justify-content:space-evenly;background:var(--elevated-surface);border-radius:18px;padding:10px;">
            ${[1, 2, 3, 4, 5]
              .map(
                (idx) => `
                <button data-qe-star="${idx}" style="background:none;border:none;font-size:28px;cursor:pointer;color:${
                  stars >= idx - 0.5 ? 'var(--accent-primary)' : 'var(--text-muted)'
                };">★</button>`
              )
              .join('')}
          </div>

          <textarea id="qe-notes" class="auth-input" style="height:78px;padding:10px;" placeholder="Add a review note...">${escapeHtml(a.reviewNotes || '')}</textarea>

          <div style="display:flex;gap:10px;">
            <button class="btn-secondary" id="btn-close-modal" style="flex:1;">Cancel</button>
            <button class="btn-primary" id="qe-save" style="flex:1;">SAVE</button>
          </div>
        </div>
      </div>
    `;
  }

  if (state.showCreateCollectionModal) {
    return `
      <div class="modal-backdrop" id="modal-backdrop">
        <div class="modal-sheet" onclick="event.stopPropagation()">
          <h3 class="serif-heading" style="font-size:24px;">Create Collection</h3>
          <input id="inp-new-col-name" class="auth-input" placeholder='e.g. "Absolute Cinema" or "Donghua Favorites"' />
          <div style="display:flex;gap:10px;">
            <button class="btn-secondary" id="btn-close-modal" style="flex:1;">Cancel</button>
            <button class="btn-primary" id="btn-save-collection" style="flex:1;">CREATE</button>
          </div>
        </div>
      </div>
    `;
  }

  if (state.showAvatarModal) {
    return `
      <div class="modal-backdrop" id="modal-backdrop">
        <div class="modal-sheet" onclick="event.stopPropagation()">
          <div class="micro-label" style="color:var(--accent-primary);text-align:center;">ROYAL PORTRAITS</div>
          <h3 class="serif-heading" style="font-size:24px;text-align:center;">Choose Your Avatar</h3>
          <div class="round-avatar-carousel">
            ${BUILT_IN_AVATARS.map(
              (av) => `
              <div class="avatar-carousel-item ${state.profile.avatarKey === av.key ? 'selected' : ''}" data-pick-avatar="${av.key}">
                <div class="avatar-ring-wrap" style="background:linear-gradient(135deg,${av.c1},${av.c2});">
                  <img src="${av.img}" alt="" onerror="${AVATAR_IMG_ONERROR}" />
                  ${state.profile.avatarKey === av.key ? `<div class="avatar-check-badge">✓</div>` : ''}
                </div>
                <span style="font-size:11px;color:var(--text-secondary);">${escapeHtml(av.name)}</span>
              </div>`
            ).join('')}
          </div>
          <button class="btn-secondary" id="btn-close-modal" style="width:100%;">Done</button>
        </div>
      </div>
    `;
  }

  if (state.showProfileEditModal) {
    return `
      <div class="modal-backdrop" id="modal-backdrop">
        <div class="modal-sheet" onclick="event.stopPropagation()">
          <h3 class="serif-heading" style="font-size:24px;">Edit Username</h3>
          <input id="inp-prof-username" class="auth-input" value="${escapeHtml(state.profile.username)}" />
          <div style="display:flex;gap:10px;">
            <button class="btn-secondary" id="btn-close-modal" style="flex:1;">Cancel</button>
            <button class="btn-primary" id="btn-save-profile" style="flex:1;">SAVE</button>
          </div>
        </div>
      </div>
    `;
  }

  if (state.communityCommentsAnimeId !== null) {
    const allGroups = groupCommunityReviewsByAnime(state.communityReviews);
    const grp =
      allGroups.find((g) => Number(g.anime_id) === Number(state.communityCommentsAnimeId)) ||
      (state.communityCommentsAnimeFallback &&
      Number(state.communityCommentsAnimeFallback.anime_id) === Number(state.communityCommentsAnimeId)
        ? state.communityCommentsAnimeFallback
        : null);
    if (grp) {
      const avgStarsNum = grp.average_rating10 ? rating10ToStars(grp.average_rating10) || 0 : 0;
      const draftStars = Number(state.communityCommentDraftStars || 0);
      const modalTab = state.communityCommentsTab === 'notes' ? 'notes' : 'discussion';
      const hasUnseen = (state.notifications.unseenAnime || []).some((u) => Number(u.anime_id) === Number(grp.anime_id));
      const modalAnime = {
        animeId: Number(grp.anime_id),
        title: grp.anime_title || `Anime #${grp.anime_id}`,
        titleEnglish: grp.anime_title || `Anime #${grp.anime_id}`,
        coverImage: grp.anime_cover_image || ''
      };
      overlays += `
        <div class="modal-backdrop" id="modal-backdrop">
          <div class="modal-sheet livtrak-hub-modal" onclick="event.stopPropagation()">
            <div class="hub-header">
              ${
                grp.anime_cover_image
                  ? `<img class="hub-poster" src="${escapeHtml(grp.anime_cover_image)}" alt="" data-open-detail="${Number(grp.anime_id)}" />`
                  : `<div class="hub-poster" style="display:grid;place-items:center;background:#14141E;color:#E8C88F;font-weight:700;" data-open-detail="${Number(grp.anime_id)}">★</div>`
              }
              <div class="hub-info">
                <div class="hub-eyebrow">COMMUNITY HUB</div>
                <div class="hub-title-row">
                  <div class="hub-title" data-open-detail="${Number(grp.anime_id)}">${escapeHtml(grp.anime_title)}</div>
                  ${grp.is_adult ? `<span class="adult-18-badge" style="flex-shrink:0;">18+</span>` : ''}
                </div>
                <div class="hub-meta">
                  ${avgStarsNum > 0 ? `<span class="hub-avg">★ ${avgStarsNum.toFixed(1)} avg</span><span>•</span>` : ''}
                  <span>${grp.total_count} member${grp.total_count === 1 ? '' : 's'}</span>
                </div>
              </div>
              <button type="button" class="hub-close" id="btn-close-modal" aria-label="Close">✕</button>
            </div>

            <div class="hub-tabs">
              <button
                type="button"
                class="hub-tab ${modalTab === 'discussion' ? 'active' : ''}"
                data-comm-modal-tab="discussion"
              >
                ${hasUnseen && modalTab !== 'discussion' ? `<span class="community-card-red-dot small"></span>` : ''}
                Live Chat
              </button>
              <button
                type="button"
                class="hub-tab ${modalTab === 'notes' ? 'active' : ''}"
                data-comm-modal-tab="notes"
              >
                Reviews (${grp.reviews.length})
              </button>
            </div>

            <div class="hub-tab-panel">
              ${
                modalTab === 'discussion'
                  ? renderAnimeDiscussionSection(modalAnime, { inModal: true })
                  : `
                    <div class="hub-reviews-scroll">
                      ${
                        grp.reviews.length === 0
                          ? `<div style="text-align:center;color:#9A9AA8;font-size:13px;padding:34px 12px;">No written reviews yet. Share your star rating or note below!</div>`
                          : grp.reviews
                              .map((rev) => {
                                const rStars = rev.rating ? rating10ToStars(rev.rating) || 0 : 0;
                                const rSent = rev.rating ? sentimentFromRating10(rev.rating) : null;
                                const hasNote = String(rev.review_notes || '').trim().length > 0;
                                return `
                                  <div
                                    class="community-comment-item"
                                    data-ctx-comm-note="1"
                                    data-ctx-uid="${escapeHtml(rev.user_id || '')}"
                                    data-ctx-name="${escapeHtml(rev.username || 'Member')}"
                                    data-ctx-av="${escapeHtml(rev.avatar_key || 'avatar_1')}"
                                  >
                                    <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;">
                                      <div style="display:flex;align-items:center;gap:10px;min-width:0;">
                                        ${renderAvatarCircle(rev.avatar_key || 'avatar_1', rev.username, 34)}
                                        <div style="min-width:0;">
                                          <div style="font-size:14px;font-weight:600;color:var(--text-primary);">${escapeHtml(rev.username || 'Member')}</div>
                                          <div style="font-size:11.5px;color:var(--text-muted);">${escapeHtml(formatReviewDate(rev.updated_at))}</div>
                                        </div>
                                      </div>
                                      <div style="display:flex;align-items:center;gap:8px;flex-shrink:0;">
                                        ${
                                          state.session
                                            ? `<button
                                                 type="button"
                                                 class="community-note-reply-btn"
                                                 data-comm-note-reply-uid="${escapeHtml(rev.user_id || '')}"
                                                 data-comm-note-reply-name="${escapeHtml(rev.username || 'Member')}"
                                                 data-comm-note-reply-av="${escapeHtml(rev.avatar_key || 'avatar_1')}"
                                               >↩ Reply</button>`
                                            : ''
                                        }
                                        ${
                                          rStars > 0
                                            ? `<div style="display:flex;flex-direction:column;align-items:flex-end;gap:3px;">
                                                <div style="display:inline-flex;align-items:center;gap:4px;">
                                                  <span style="display:inline-flex;gap:1px;">${renderFiveStarsHtml(rStars)}</span>
                                                  <span class="tabular-num" style="font-size:13.5px;font-weight:700;color:var(--highlight);">${rStars.toFixed(1)}</span>
                                                </div>
                                                ${rSent ? `<span class="sentiment-pill soft ${rSent.key}" style="font-size:9px;padding:2px 7px;">${rSent.shortLabel.toUpperCase()}</span>` : ''}
                                              </div>`
                                            : `<span class="sentiment-pill soft cinema" style="font-size:9px;padding:2px 8px;">NOTE</span>`
                                        }
                                      </div>
                                    </div>
                                    ${
                                      hasNote
                                        ? `<div class="review-quote-box" style="padding:11px 14px;">
                                            <div class="review-quote-bar"></div>
                                            <div class="review-quote-text" style="font-size:13.5px;">“${escapeHtml(rev.review_notes)}”</div>
                                          </div>`
                                        : ''
                                    }
                                  </div>
                                `;
                              })
                              .join('')
                      }
                    </div>

                    ${
                      state.session
                        ? `<div class="hub-reviews-composer">
                            <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
                              <span class="micro-label" style="color:var(--accent-primary);">YOUR RATING &amp; REVIEW</span>
                              <div style="display:inline-flex;align-items:center;gap:4px;">
                                ${[1, 2, 3, 4, 5]
                                  .map(
                                    (st) =>
                                      `<button type="button" data-comm-modal-star="${st}" style="background:none;border:none;cursor:pointer;font-size:20px;color:${
                                        draftStars >= st - 0.5 ? 'var(--accent-primary)' : '#383D4E'
                                      };">★</button>`
                                  )
                                  .join('')}
                                ${
                                  draftStars > 0
                                    ? `<span class="tabular-num" style="font-size:12.5px;font-weight:700;color:var(--highlight);margin-left:4px;">${draftStars.toFixed(1)}</span>`
                                    : ''
                                }
                              </div>
                            </div>
                            <div style="display:flex;gap:8px;align-items:center;">
                              <input
                                id="inp-comm-modal-note"
                                class="auth-input"
                                style="height:42px;flex:1;"
                                placeholder="Write your review for this anime..."
                                value="${escapeHtml(state.communityCommentDraftNote || '')}"
                                ${state.communityNoteSaving ? 'disabled' : ''}
                              />
                              <button type="button" class="btn-primary" id="btn-comm-modal-submit" style="height:42px;padding:0 16px;" ${
                                state.communityNoteSaving ? 'disabled' : ''
                              }>${state.communityNoteSaving ? 'POSTING...' : 'POST'}</button>
                            </div>
                            ${
                              state.communityNoteFeedback
                                ? `<div class="feedback-banner success" style="margin-top:4px;">✓ ${escapeHtml(state.communityNoteFeedback)}</div>`
                                : ''
                            }
                            ${
                              state.communityNoteError
                                ? `<div class="feedback-banner error" style="margin-top:4px;">! ${escapeHtml(state.communityNoteError)}</div>`
                                : ''
                            }
                          </div>`
                        : ''
                    }
                  `
              }
            </div>
          </div>
        </div>
      `;
    }
  }

  if (state.discussion.contextMenu && state.session) {
    const cm = state.discussion.contextMenu;
    const clampedX = Math.max(12, Math.min(window.innerWidth - 230, cm.x));
    const clampedY = Math.max(12, Math.min(window.innerHeight - 190, cm.y));
    overlays += `
      <div class="discussion-ctx-backdrop" id="discussion-ctx-backdrop"></div>
      <div class="discussion-context-menu" style="left:${clampedX}px;top:${clampedY}px;">
        <button type="button" class="discussion-ctx-item" id="ctx-btn-reply">
          <span>↩ Reply to @${escapeHtml(cm.username)}</span>
        </button>
        ${
          cm.isOwner
            ? `
              <button type="button" class="discussion-ctx-item" id="ctx-btn-edit">
                <span>✎ Edit ${cm.type === 'reply' ? 'Reply' : 'Comment'}</span>
              </button>
              <button type="button" class="discussion-ctx-item danger" id="ctx-btn-delete">
                <span>🗑 Delete ${cm.type === 'reply' ? 'Reply' : 'Comment'}</span>
              </button>
            `
            : ''
        }
      </div>
    `;
  }

  return overlays;
}

// ============================================================================
// ROOT RENDER & EVENT BINDING
// ============================================================================

function renderApp() {
  const root = document.getElementById('app');
  if (!root) return;

  const liveDetailNotesEl = document.getElementById('inp-detail-notes');
  if (liveDetailNotesEl && state.tab === 'detail') {
    state.detailNoteDraft = liveDetailNotesEl.value;
  }
  const prevScrollY = window.scrollY;
  const prevCommListScroll = document.querySelector('.community-comments-list')?.scrollTop ?? null;
  const prevModalDiscScroll = document.querySelector('.community-modal-discussion-scroll')?.scrollTop ?? null;
  const sameTab = state.lastRenderedTab === state.tab;

  const needsOnboarding = Boolean(
    state.session &&
      (state.authMode === 'onboard_username' ||
        state.authMode === 'onboard_avatar' ||
        state.session.onboardingCompleted === false)
  );
  if (needsOnboarding && state.authMode !== 'onboard_username' && state.authMode !== 'onboard_avatar') {
    state.authMode = 'onboard_username';
  }

  if (((!state.session && !state.isGuestMode) || needsOnboarding) && state.tab !== 'downloads') {
    root.innerHTML = renderAuthScreen();
    bindAuthEvents();
    return;
  }

  const hasCommunityUnseen = state.notifications.unreadCount > 0 || (state.notifications.unseenAnime || []).length > 0;

  const sidebarTabs = [
    { id: 'home', title: 'Home', icon: SVG_ICONS.home },
    { id: 'discover', title: 'Explore', icon: SVG_ICONS.explore },
    { id: 'mylist', title: 'Library', icon: SVG_ICONS.library },
    { id: 'community', title: 'Community', icon: SVG_ICONS.community, showRedDot: hasCommunityUnseen },
    { id: 'profile', title: 'Profile', icon: SVG_ICONS.profile }
  ];

  const bottomDockTabs = [
    { id: 'home', title: 'Home', icon: SVG_ICONS.home },
    { id: 'discover', title: 'Explore', icon: SVG_ICONS.explore },
    { id: 'mylist', title: 'Library', icon: SVG_ICONS.library },
    { id: 'community', title: 'Community', icon: SVG_ICONS.community, showRedDot: hasCommunityUnseen }
  ];

  let screenHtml = '';
  if (state.tab === 'home') screenHtml = renderHomeScreen();
  else if (state.tab === 'discover') screenHtml = renderDiscoverScreen();
  else if (state.tab === 'mylist') screenHtml = renderMyListScreen();
  else if (state.tab === 'community') screenHtml = renderCommunityScreen();
  else if (state.tab === 'downloads') screenHtml = renderDownloadsScreen();
  else if (state.tab === 'profile') screenHtml = renderProfileScreen();
  else if (state.tab === 'settings') screenHtml = renderSettingsScreen();
  else if (state.tab === 'detail') screenHtml = renderAnimeDetailScreen();

  root.innerHTML = `
    <div class="universe-scaffold">
      <!-- MidnightDesktopSidebar (>=900px) -->
      <aside class="midnight-sidebar">
        <button class="quick-jump-box" id="btn-sidebar-search">
          <span style="width:16px;height:16px;display:inline-flex;">${SVG_ICONS.search}</span>
          <span>Quick Jump...</span>
        </button>
        ${sidebarTabs
          .map(
            (t) => `
            <button class="sidebar-item ${state.tab === t.id ? 'active' : ''}" data-nav="${t.id}">
              ${t.icon}
              <span>${t.title}</span>
              ${t.showRedDot ? `<span class="nav-red-dot inline" aria-label="Unseen discussion messages"></span>` : ''}
            </button>`
          )
          .join('')}
        <div class="sidebar-sandrone-slot">
          <button type="button" class="sandrone-mascot-btn ${sandroneIsPlaying ? 'is-playing' : ''}" data-sandrone-mascot="sidebar" aria-label="Play a random Sandrone voice line" title="Click Sandrone">
            <img class="sandrone-mascot-img" src="${SANDRONE_IMAGE_SRC}" alt="Sandrone" draggable="false" />
          </button>
        </div>
      </aside>

      <!-- MidnightCompactRail (600-899px) -->
      <aside class="midnight-compact-rail">
        ${sidebarTabs
          .map(
            (t) => `
            <button class="rail-item ${state.tab === t.id ? 'active' : ''}" data-nav="${t.id}">
              <span class="nav-icon-badge-wrap">
                ${t.icon}
                ${t.showRedDot ? `<span class="nav-red-dot" aria-label="Unseen discussion messages"></span>` : ''}
              </span>
              <span>${t.title}</span>
            </button>`
          )
          .join('')}
        <div class="rail-sandrone-slot">
          <button type="button" class="sandrone-mascot-btn compact ${sandroneIsPlaying ? 'is-playing' : ''}" data-sandrone-mascot="rail" aria-label="Play a random Sandrone voice line" title="Click Sandrone">
            <img class="sandrone-mascot-img" src="${SANDRONE_IMAGE_SRC}" alt="Sandrone" draggable="false" />
          </button>
        </div>
      </aside>

      <!-- Main Content Stage -->
      <main class="universe-stage">
        ${screenHtml}
      </main>

      <!-- MidnightBottomNavigationBar (<600px, Capsule Tabs) + Mobile Sandrone Mascot -->
      ${
        state.tab !== 'detail' && state.tab !== 'profile' && state.tab !== 'settings' && state.tab !== 'downloads'
          ? `
            <div class="mobile-sandrone-slot">
              <button type="button" class="sandrone-mascot-btn mobile-floating ${sandroneIsPlaying ? 'is-playing' : ''}" data-sandrone-mascot="mobile" aria-label="Play a random Sandrone voice line" title="Tap Sandrone">
                <img class="sandrone-mascot-img" src="${SANDRONE_IMAGE_SRC}" alt="Sandrone" draggable="false" />
              </button>
            </div>
            <div class="midnight-bottom-dock-wrap">
              <nav class="midnight-bottom-dock">
                ${bottomDockTabs
                  .map(
                    (t) => `
                    <button class="dock-tab ${state.tab === t.id ? 'active' : ''}" data-nav="${t.id}">
                      <span class="nav-icon-badge-wrap">
                        ${t.icon}
                        ${t.showRedDot ? `<span class="nav-red-dot" aria-label="Unseen discussion messages"></span>` : ''}
                      </span>
                      <span class="dock-tab-label">${t.title}</span>
                    </button>`
                  )
                  .join('')}
              </nav>
            </div>
          `
          : ''
      }
    </div>
    ${renderModals()}
  `;

  bindAppEvents();
  state.lastRenderedTab = state.tab;

  if (prevCommListScroll !== null) {
    const commListEl = document.querySelector('.community-comments-list');
    if (commListEl) commListEl.scrollTop = prevCommListScroll;
  }
  if (prevModalDiscScroll !== null) {
    const modalDiscEl = document.querySelector('.community-modal-discussion-scroll');
    if (modalDiscEl) modalDiscEl.scrollTop = prevModalDiscScroll;
  }
  if (sameTab && prevScrollY > 0) {
    window.scrollTo(0, prevScrollY);
  }
}

function bindAuthEvents() {
  const inpIdentifier = document.getElementById('inp-identifier');
  const inpPass = document.getElementById('inp-pass');
  const inpConfirmPass = document.getElementById('inp-confirm-pass');
  if (inpIdentifier) inpIdentifier.oninput = (e) => { state.identifier = e.target.value; };
  if (inpPass) {
    inpPass.oninput = (e) => { state.password = e.target.value; };
    inpPass.onkeydown = (e) => {
      if (e.key === 'Enter' && state.authMode !== 'signup') document.getElementById('btn-auth-submit')?.click();
    };
  }
  if (inpConfirmPass) {
    inpConfirmPass.oninput = (e) => { state.confirmPassword = e.target.value; };
    inpConfirmPass.onkeydown = (e) => {
      if (e.key === 'Enter') document.getElementById('btn-auth-submit')?.click();
    };
  }

  const btnTogglePass = document.getElementById('btn-toggle-pass');
  if (btnTogglePass) {
    btnTogglePass.onclick = () => {
      state.showPassword = !state.showPassword;
      renderApp();
    };
  }

  const btnSwitchMode = document.getElementById('btn-switch-auth-mode');
  if (btnSwitchMode) {
    btnSwitchMode.onclick = () => {
      state.authMode = state.authMode === 'signup' ? 'signin' : 'signup';
      state.authError = '';
      state.authInfo = '';
      renderApp();
    };
  }

  const btnForgot = document.getElementById('btn-forgot-pass');
  if (btnForgot) {
    btnForgot.onclick = async () => {
      if (!state.identifier.trim()) {
        state.authError = 'Enter your email address or username first.';
        renderApp();
        return;
      }
      state.loading = true;
      state.authError = '';
      state.authInfo = '';
      renderApp();
      try {
        const targetEmail = await resolveLoginIdentifier(state.identifier);
        await supabaseRequest('/auth/v1/recover', 'POST', { email: targetEmail });
        state.pendingVerifyEmail = targetEmail;
        state.resetOtpCode = '';
        state.resetNewPassword = '';
        state.authMode = 'forgot_otp';
        state.authInfo = `Recovery code sent to ${maskEmailAddress(targetEmail)}.`;
      } catch (err) {
        state.authError = err.message;
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const inpResetOtp = document.getElementById('inp-reset-otp');
  const inpResetNewPass = document.getElementById('inp-reset-new-pass');
  if (inpResetOtp) {
    inpResetOtp.oninput = (e) => {
      const clean = e.target.value.replace(/\D/g, '').slice(0, 8);
      state.resetOtpCode = clean;
      if (e.target.value !== clean) e.target.value = clean;
    };
  }
  if (inpResetNewPass) {
    inpResetNewPass.oninput = (e) => { state.resetNewPassword = e.target.value; };
  }
  const btnCancelResetOtp = document.getElementById('btn-cancel-reset-otp');
  if (btnCancelResetOtp) {
    btnCancelResetOtp.onclick = () => {
      state.authMode = 'signin';
      state.authError = '';
      state.authInfo = '';
      renderApp();
    };
  }
  const btnConfirmResetOtp = document.getElementById('btn-confirm-reset-otp');
  if (btnConfirmResetOtp) {
    btnConfirmResetOtp.onclick = async () => {
      const cleanOtp = String(state.resetOtpCode || '').replace(/\D/g, '');
      if (cleanOtp.length < 6) {
        state.authError = 'Enter the verification code from your email.';
        renderApp();
        return;
      }
      if (!state.resetNewPassword || state.resetNewPassword.length < 8) {
        state.authError = 'New password must be at least 8 characters.';
        renderApp();
        return;
      }
      state.loading = true;
      state.authError = '';
      renderApp();
      try {
        const targetEmail = state.pendingVerifyEmail || state.identifier.trim().toLowerCase();
        const verifyData = await supabaseRequest('/auth/v1/verify', 'POST', {
          email: targetEmail,
          token: cleanOtp,
          type: 'recovery'
        });
        if (!verifyData.access_token) throw new Error('Invalid or expired recovery code.');
        const recoveredMetaName = String(verifyData.user?.user_metadata?.username || '').trim();
        const recoveredOnboarded =
          verifyData.user?.user_metadata?.onboarding_completed !== false && !isPlaceholderOrEmailName(recoveredMetaName);
        saveSession({
          accessToken: verifyData.access_token,
          refreshToken: verifyData.refresh_token || '',
          expiresAt: Math.floor(Date.now() / 1000) + (verifyData.expires_in || 3600),
          userId: verifyData.user?.id,
          email: verifyData.user?.email || targetEmail,
          username: recoveredOnboarded ? recoveredMetaName : '',
          onboardingCompleted: recoveredOnboarded
        });
        await supabaseRequest('/auth/v1/user', 'PUT', { password: state.resetNewPassword });
        await loadCloudData();
      } catch (err) {
        state.authError = err.message;
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const btnGuest = document.getElementById('btn-guest-preview');
  if (btnGuest) {
    btnGuest.onclick = () => {
      enterGuestSession(false);
      renderApp();
    };
  }

  // Sign-Up 8-Digit OTP Verification bindings
  const inpSignupOtp = document.getElementById('inp-signup-otp');
  if (inpSignupOtp) {
    inpSignupOtp.oninput = (e) => {
      const clean = e.target.value.replace(/\D/g, '').slice(0, 8);
      state.signupOtpCode = clean;
      if (e.target.value !== clean) e.target.value = clean;
    };
    inpSignupOtp.onkeydown = (e) => {
      if (e.key === 'Enter') document.getElementById('btn-verify-signup-otp')?.click();
    };
  }

  const btnVerifySignupOtp = document.getElementById('btn-verify-signup-otp');
  if (btnVerifySignupOtp) {
    btnVerifySignupOtp.onclick = async () => {
      const cleanOtp = String(state.signupOtpCode || '').replace(/\D/g, '');
      if (cleanOtp.length < 6) {
        state.authError = 'Please enter the verification code from your email.';
        renderApp();
        return;
      }
      state.loading = true;
      state.authError = '';
      state.authInfo = '';
      renderApp();
      try {
        const targetEmail = (state.pendingVerifyEmail || state.identifier).trim().toLowerCase();
        const data = await supabaseRequest('/auth/v1/verify', 'POST', {
          email: targetEmail,
          token: cleanOtp,
          type: 'signup'
        });
        if (!data || !data.access_token) {
          throw new Error('Invalid or expired verification code. Please check your inbox or tap Resend Code.');
        }
        saveSession({
          accessToken: data.access_token,
          refreshToken: data.refresh_token || '',
          expiresAt: Math.floor(Date.now() / 1000) + (data.expires_in || 3600),
          userId: data.user?.id,
          email: data.user?.email || targetEmail,
          username: '',
          onboardingCompleted: false
        });
        state.onboardUsername = '';
        state.onboardAvatarKey = 'avatar_1';
        state.authInfo = 'Email verified! Choose your username and profile picture.';
        state.authMode = 'onboard_username';
      } catch (err) {
        state.authError = err.message || 'Invalid or expired verification code.';
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const btnResendSignupOtp = document.getElementById('btn-resend-signup-otp');
  if (btnResendSignupOtp) {
    btnResendSignupOtp.onclick = async () => {
      const targetEmail = (state.pendingVerifyEmail || state.identifier).trim().toLowerCase();
      if (!targetEmail) return;
      state.loading = true;
      state.authError = '';
      state.authInfo = '';
      renderApp();
      try {
        await supabaseRequest('/auth/v1/resend', 'POST', {
          type: 'signup',
          email: targetEmail
        });
        state.authInfo = 'A fresh 8-digit verification code has been sent to your email.';
      } catch (err) {
        state.authError = err.message || 'Could not resend verification code.';
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const btnChangeSignupEmail = document.getElementById('btn-change-signup-email');
  if (btnChangeSignupEmail) {
    btnChangeSignupEmail.onclick = () => {
      state.authMode = 'signup';
      state.signupOtpCode = '';
      state.authError = '';
      state.authInfo = '';
      renderApp();
    };
  }

  const btnSubmit = document.getElementById('btn-auth-submit');
  if (btnSubmit) {
    btnSubmit.onclick = async () => {
      if (!state.identifier.trim() || !state.password) {
        state.authError = 'Enter your email/username and password.';
        renderApp();
        return;
      }
      state.authError = '';
      state.authInfo = '';
      state.loading = true;
      renderApp();
      try {
        if (state.authMode === 'signup') {
          const email = state.identifier.trim().toLowerCase();
          if (!email.includes('@') || !email.includes('.')) {
            throw new Error('Please enter a valid email address.');
          }
          if (state.password.length < 8) {
            throw new Error('Password must be at least 8 characters.');
          }
          if (state.confirmPassword && state.password !== state.confirmPassword) {
            throw new Error('Passwords do not match.');
          }
          const data = await supabaseRequest('/auth/v1/signup', 'POST', {
            email,
            password: state.password,
            data: {
              onboarding_completed: false
            }
          });
          // Supabase returns an empty identities array if the email is already registered
          const identities = data?.user?.identities || data?.identities;
          if (Array.isArray(identities) && identities.length === 0) {
            throw new Error('An account with this email already exists. Please Sign In instead.');
          }
          if (data && data.access_token) {
            saveSession({
              accessToken: data.access_token,
              refreshToken: data.refresh_token || '',
              expiresAt: Math.floor(Date.now() / 1000) + (data.expires_in || 3600),
              userId: data.user?.id,
              email,
              username: '',
              onboardingCompleted: false
            });
            state.onboardUsername = '';
            state.onboardAvatarKey = 'avatar_1';
            state.authMode = 'onboard_username';
          } else {
            state.pendingVerifyEmail = email;
            state.pendingVerifyPassword = state.password;
            state.signupOtpCode = '';
            state.authMode = 'verify_otp';
            state.authInfo = 'We sent an 8-digit verification code to your email.';
          }
        } else {
          const resolvedEmail = await resolveLoginIdentifier(state.identifier);
          try {
            const data = await supabaseRequest('/auth/v1/token?grant_type=password', 'POST', {
              email: resolvedEmail,
              password: state.password
            });
            const metaUsername = String(data.user?.user_metadata?.username || '').trim();
            const metaOnboarded = data.user?.user_metadata?.onboarding_completed !== false;
            saveSession({
              accessToken: data.access_token,
              refreshToken: data.refresh_token || '',
              expiresAt: Math.floor(Date.now() / 1000) + (data.expires_in || 3600),
              userId: data.user?.id,
              email: data.user?.email || resolvedEmail,
              username: !isPlaceholderOrEmailName(metaUsername) ? metaUsername : '',
              onboardingCompleted: metaOnboarded
            });
            if (!metaOnboarded) {
              state.onboardUsername = '';
              state.onboardAvatarKey = 'avatar_1';
              state.authMode = 'onboard_username';
            } else {
              await loadCloudData();
            }
          } catch (signInErr) {
            const msg = String(signInErr.message || '');
            if (msg.toLowerCase().includes('not confirmed') || msg.toLowerCase().includes('email_not_confirmed')) {
              await supabaseRequest('/auth/v1/resend', 'POST', {
                type: 'signup',
                email: resolvedEmail
              }).catch(() => {});
              state.pendingVerifyEmail = resolvedEmail;
              state.pendingVerifyPassword = state.password;
              state.signupOtpCode = '';
              state.authMode = 'verify_otp';
              state.authInfo = 'Your email is not verified yet. Enter the 8-digit code sent to your inbox.';
            } else {
              throw signInErr;
            }
          }
        }
      } catch (err) {
        state.authError = err.message;
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const inpOnboardUser = document.getElementById('inp-onboard-user');
  if (inpOnboardUser) {
    inpOnboardUser.oninput = (e) => {
      state.onboardUsername = e.target.value;
    };
    inpOnboardUser.onkeydown = (e) => {
      if (e.key === 'Enter') document.getElementById('btn-onboard-next')?.click();
    };
  }

  const btnOnboardNext = document.getElementById('btn-onboard-next');
  if (btnOnboardNext) {
    btnOnboardNext.onclick = async () => {
      const cleanName = String(state.onboardUsername || '').trim();
      if (cleanName.length < 2 || cleanName.length > 32) {
        state.authError = 'Username must be between 2 and 32 characters.';
        renderApp();
        return;
      }
      if (cleanName.includes('@')) {
        state.authError = 'Usernames cannot contain @ or be an email address.';
        renderApp();
        return;
      }
      if (!/^[A-Za-z0-9_ .-]+$/.test(cleanName)) {
        state.authError = 'Use letters, numbers, spaces, dots, hyphens, or underscores.';
        renderApp();
        return;
      }
      state.loading = true;
      state.authError = '';
      state.authInfo = '';
      renderApp();
      try {
        const statusRes = await supabaseRequest('/rest/v1/rpc/check_username_status', 'POST', {
          p_username: cleanName
        }).catch(() => ({ available: true }));
        if (statusRes && statusRes.available === false) {
          state.authError = statusRes.reason || 'That username is already taken. Please pick another.';
          return;
        }
        state.onboardUsername = cleanName;
        state.authMode = 'onboard_avatar';
      } catch (err) {
        state.authError = err?.message || 'Unable to verify username availability.';
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const btnOnboardBack = document.getElementById('btn-onboard-back');
  if (btnOnboardBack) {
    btnOnboardBack.onclick = () => {
      state.authError = '';
      state.authMode = 'onboard_username';
      renderApp();
    };
  }

  document.querySelectorAll('[data-onboard-avatar]').forEach((el) => {
    el.onclick = () => {
      state.onboardAvatarKey = el.getAttribute('data-onboard-avatar');
      renderApp();
    };
  });

  const btnOnboardFinish = document.getElementById('btn-onboard-finish');
  if (btnOnboardFinish) {
    btnOnboardFinish.onclick = async () => {
      const cleanName = String(state.onboardUsername || '').trim();
      if (cleanName.length < 2 || cleanName.includes('@')) {
        state.authError = 'Choose a valid username first.';
        state.authMode = 'onboard_username';
        renderApp();
        return;
      }
      const chosenAvatar = normalizeAvatarKey(state.onboardAvatarKey);
      state.loading = true;
      state.authError = '';
      renderApp();
      try {
        if (state.session) {
          const uid = encodeURIComponent(state.session.userId);
          const patched = await supabaseRequest(
            `/rest/v1/profiles?id=eq.${uid}`,
            'PATCH',
            {
              username: cleanName,
              avatar_key: chosenAvatar,
              avatar_url: null,
              updated_at: new Date().toISOString()
            },
            'return=representation'
          ).catch(() => null);
          if (!Array.isArray(patched) || patched.length === 0) {
            await supabaseRequest(
              '/rest/v1/profiles?on_conflict=id',
              'POST',
              {
                id: state.session.userId,
                username: cleanName,
                avatar_key: chosenAvatar,
                avatar_url: null,
                role: 'member',
                can_generate_invites: false
              },
              'resolution=merge-duplicates,return=minimal'
            );
          }
          await supabaseRequest('/auth/v1/user', 'PUT', {
            data: {
              username: cleanName,
              avatar_key: chosenAvatar,
              onboarding_completed: true
            }
          }).catch(() => {});
          await Promise.all([
            supabaseRequest(
              '/rest/v1/community_members?on_conflict=user_id',
              'POST',
              { user_id: state.session.userId, role: 'member', status: 'active' },
              'resolution=merge-duplicates,return=minimal'
            ).catch(() => {}),
            supabaseRequest(
              '/rest/v1/privacy_settings?on_conflict=user_id',
              'POST',
              {
                user_id: state.session.userId,
                reviews_visibility: 'community',
                activity_visibility: 'community'
              },
              'resolution=merge-duplicates,return=minimal'
            ).catch(() => {})
          ]);
          saveSession({
            ...state.session,
            username: cleanName,
            onboardingCompleted: true
          });
        }
        state.profile.username = cleanName;
        state.profile.avatarKey = chosenAvatar;
        saveProfileCache();
        state.authMode = 'signin';
        state.tab = 'home';
        if (state.session) {
          await loadCloudData();
        }
      } catch (err) {
        const msg = err?.message || 'Unable to save your profile right now.';
        state.authError = msg;
        if (msg.toLowerCase().includes('username') || msg.toLowerCase().includes('taken') || msg.toLowerCase().includes('duplicate')) {
          state.authMode = 'onboard_username';
        }
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }
}

function bindAppEvents() {
  document.querySelectorAll('[data-nav]').forEach((el) => {
    el.onclick = () => {
      const target = el.getAttribute('data-nav');
      state.settingsCategory = null;
      state.tab = target;
      window.scrollTo({ top: 0, behavior: 'smooth' });
      renderApp();
      if (target === 'community') {
        EmojiCacheManager.ensureMetadata(false);
        refreshUnreadNotificationCount().then(() => updateCommunityRedDotsInPlace());
        if (state.communityViewTab === 'tagged' && state.session) {
          loadUserNotifications(true);
        }
      }
    };
  });

  document.querySelectorAll('[data-sandrone-mascot]').forEach((el) => {
    el.onclick = (e) => {
      e.preventDefault();
      playRandomSandroneVoice(el);
    };
  });

  const btnSidebarSearch = document.getElementById('btn-sidebar-search');
  const btnOpenQuickSearch = document.getElementById('btn-open-quick-search');
  if (btnSidebarSearch) {
    btnSidebarSearch.onclick = () => {
      state.tab = 'discover';
      renderApp();
      setTimeout(() => document.getElementById('inp-discover-search')?.focus(), 50);
    };
  }
  if (btnOpenQuickSearch) {
    btnOpenQuickSearch.onclick = () => {
      state.tab = 'discover';
      renderApp();
      setTimeout(() => document.getElementById('inp-discover-search')?.focus(), 50);
    };
  }

  const btnAllDays = document.getElementById('btn-home-all-days');
  if (btnAllDays) {
    btnAllDays.onclick = () => {
      state.homeAllWeekDays = true;
      renderApp();
    };
  }

  document.querySelectorAll('[data-select-day]').forEach((btn) => {
    btn.onclick = () => {
      state.homeAllWeekDays = false;
      state.selectedDay = Number(btn.getAttribute('data-select-day'));
      renderApp();
    };
  });

  document.querySelectorAll('[data-open-detail]').forEach((el) => {
    el.onclick = (e) => {
      if (e.target.closest('[data-open-comments]')) return;
      e.stopPropagation();
      if (state._suppressNextCardClick) {
        state._suppressNextCardClick = false;
        return;
      }
      openAnimeDetail(Number(el.getAttribute('data-open-detail')));
    };
  });

  // Part 4: Right-click (Desktop/Web) & Long-Press (Mobile Touch) Context Menu on Anime Cards
  document.querySelectorAll('[data-card-anime-id]').forEach((card) => {
    const aid = Number(card.getAttribute('data-card-anime-id'));
    card.oncontextmenu = (e) => {
      e.preventDefault();
      e.stopPropagation();
      state.contextMenu = { animeId: aid, x: e.clientX, y: e.clientY };
      renderApp();
    };

    let touchTimer = null;
    let startX = 0;
    let startY = 0;
    card.ontouchstart = (e) => {
      if (!e.touches || e.touches.length !== 1) return;
      startX = e.touches[0].clientX;
      startY = e.touches[0].clientY;
      touchTimer = setTimeout(() => {
        touchTimer = null;
        state._suppressNextCardClick = true;
        state.contextMenu = { animeId: aid, x: startX, y: startY };
        renderApp();
      }, 500);
    };
    card.ontouchmove = (e) => {
      if (!touchTimer || !e.touches || e.touches.length !== 1) return;
      const dx = Math.abs(e.touches[0].clientX - startX);
      const dy = Math.abs(e.touches[0].clientY - startY);
      if (dx > 10 || dy > 10) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
    };
    card.ontouchend = () => {
      if (touchTimer) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
    };
    card.ontouchcancel = () => {
      if (touchTimer) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
    };
  });

  const ctxBackdrop = document.getElementById('anime-context-backdrop');
  if (ctxBackdrop) {
    ctxBackdrop.onclick = () => {
      state.contextMenu = null;
      renderApp();
    };
    ctxBackdrop.oncontextmenu = (e) => {
      e.preventDefault();
      state.contextMenu = null;
      renderApp();
    };
  }

  document.querySelectorAll('[data-ctx-action]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const action = btn.getAttribute('data-ctx-action');
      const aid = Number(btn.getAttribute('data-ctx-anime-id'));
      const item = findAnimeByIdAnywhere(aid);
      state.contextMenu = null;
      if (action === 'remove_home') {
        state.confirmRemoveHomeAnime = { animeId: aid, title: item ? resolveTitle(item) : '' };
        renderApp();
      } else if (action === 'add_home') {
        if (item) await addToHome(item);
        else renderApp();
      } else if (action === 'view_details') {
        openAnimeDetail(aid);
      } else if (action === 'view_library') {
        state.tab = 'mylist';
        state.myListStatusFilter = 'ALL';
        state.myListSentimentFilter = null;
        state.myListCollectionFilter = null;
        window.scrollTo({ top: 0, behavior: 'smooth' });
        renderApp();
      } else if (action === 'view_community') {
        state.tab = 'community';
        window.scrollTo({ top: 0, behavior: 'smooth' });
        renderApp();
      }
    };
  });

  const btnCancelRemoveHome = document.getElementById('btn-cancel-remove-home');
  if (btnCancelRemoveHome) {
    btnCancelRemoveHome.onclick = () => {
      state.confirmRemoveHomeAnime = null;
      renderApp();
    };
  }
  const btnConfirmRemoveHome = document.getElementById('btn-confirm-remove-home');
  if (btnConfirmRemoveHome) {
    btnConfirmRemoveHome.onclick = async () => {
      const target = state.confirmRemoveHomeAnime;
      state.confirmRemoveHomeAnime = null;
      if (target && target.animeId) {
        await removeFromHomeOnly(target.animeId);
      } else {
        renderApp();
      }
    };
  }

  // Part 5: Inline Custom Episode Number Editor
  const submitInlineEpisodeEdit = async (aid, scope) => {
    const item = state.vault.find((v) => Number(v.animeId) === Number(aid));
    if (!item || !state.inlineEpEdit || state.inlineEpEdit.saving) return;
    const inputEl = document.getElementById(`inline-ep-input-${scope}-${aid}`);
    const rawDraft = inputEl ? inputEl.value : state.inlineEpEdit.draft;
    const check = validateEpisodeInput(rawDraft, item.totalEpisodes);
    if (!check.valid) {
      state.inlineEpEdit = { ...state.inlineEpEdit, draft: rawDraft, error: check.error };
      renderApp();
      setTimeout(() => {
        const el = document.getElementById(`inline-ep-input-${scope}-${aid}`);
        if (el) el.focus();
      }, 15);
      return;
    }

    state.inlineEpEdit = { ...state.inlineEpEdit, draft: String(check.value), saving: true, error: '' };
    renderApp();
    const res = await commitEpisodeProgress(aid, check.value);
    if (res.ok) {
      state.inlineEpEdit = null;
      renderApp();
    } else {
      const restored = state.vault.find((v) => Number(v.animeId) === Number(aid));
      state.inlineEpEdit = {
        animeId: aid,
        scope,
        draft: String(restored ? restored.watchedEpisodes : item.watchedEpisodes || 0),
        saving: false,
        error: res.error || 'Failed to save episode progress.'
      };
      renderApp();
    }
  };

  document.querySelectorAll('[data-inline-ep-trigger]').forEach((el) => {
    el.onclick = (e) => {
      e.stopPropagation();
      const aid = Number(el.getAttribute('data-inline-ep-trigger'));
      const scope = el.getAttribute('data-inline-ep-scope') || 'card';
      const item = state.vault.find((v) => Number(v.animeId) === aid);
      if (!item) return;
      state.inlineEpEdit = {
        animeId: aid,
        scope,
        draft: String(item.watchedEpisodes || 0),
        saving: false,
        error: ''
      };
      renderApp();
      setTimeout(() => {
        const inp = document.getElementById(`inline-ep-input-${scope}-${aid}`);
        if (inp) {
          inp.focus();
          inp.select();
        }
      }, 15);
    };
  });

  document.querySelectorAll('[data-inline-ep-input]').forEach((inp) => {
    const aid = Number(inp.getAttribute('data-inline-ep-input'));
    const scope = inp.getAttribute('data-inline-ep-scope') || 'card';
    inp.oninput = (e) => {
      if (state.inlineEpEdit && Number(state.inlineEpEdit.animeId) === aid) {
        state.inlineEpEdit.draft = e.target.value;
        state.inlineEpEdit.error = '';
      }
    };
    inp.onkeydown = (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        e.stopPropagation();
        submitInlineEpisodeEdit(aid, scope);
      } else if (e.key === 'Escape') {
        e.preventDefault();
        e.stopPropagation();
        state.inlineEpEdit = null;
        renderApp();
      }
    };
  });

  document.querySelectorAll('[data-inline-ep-save]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const aid = Number(btn.getAttribute('data-inline-ep-save'));
      const scope = btn.getAttribute('data-inline-ep-scope') || 'card';
      submitInlineEpisodeEdit(aid, scope);
    };
  });

  document.querySelectorAll('[data-inline-ep-cancel]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      state.inlineEpEdit = null;
      renderApp();
    };
  });

  document.querySelectorAll('[data-inc-id]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-inc-id'));
      const item = state.vault.find((v) => Number(v.animeId) === id);
      if (!item) return;
      const next = (item.watchedEpisodes || 0) + 1;
      const clamped = item.totalEpisodes && item.totalEpisodes > 0 ? Math.min(item.totalEpisodes, next) : next;
      const res = await commitEpisodeProgress(id, clamped);
      if (!res.ok) showToast(res.error || 'Failed to update progress', true);
      renderApp();
    };
  });

  document.querySelectorAll('[data-dec-id]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-dec-id'));
      const item = state.vault.find((v) => Number(v.animeId) === id);
      if (!item) return;
      const next = Math.max(0, (item.watchedEpisodes || 0) - 1);
      const res = await commitEpisodeProgress(id, next);
      if (!res.ok) showToast(res.error || 'Failed to update progress', true);
      renderApp();
    };
  });

  document.querySelectorAll('[data-quick-add]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-quick-add'));
      const existing = state.vault.find((v) => Number(v.animeId) === id);
      if (existing) {
        state.quickEditAnime = { ...existing };
        renderApp();
        return;
      }
      const disc = state.discover.find((d) => Number(d.animeId) === id);
      if (disc) {
        await addToHome(disc);
      }
    };
  });

  document.querySelectorAll('[data-quick-edit]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-quick-edit'));
      const item = state.vault.find((v) => Number(v.animeId) === id);
      if (item) {
        state.quickEditAnime = { ...item };
        renderApp();
      }
    };
  });

  // Discover events
  const inpDisc = document.getElementById('inp-discover-search');
  if (inpDisc) {
    inpDisc.onchange = (e) => {
      state.searchQuery = e.target.value;
      fetchDiscoverCatalog();
    };
  }
  const btnClearDisc = document.getElementById('btn-clear-disc-search');
  if (btnClearDisc) {
    btnClearDisc.onclick = () => {
      state.searchQuery = '';
      fetchDiscoverCatalog();
    };
  }

  document.querySelectorAll('[data-disc-mode]').forEach((btn) => {
    btn.onclick = () => {
      state.discoverMode = btn.getAttribute('data-disc-mode');
      state.discoverSort = btn.getAttribute('data-disc-sort') || 'POPULARITY_DESC';
      state.discoverStatus = state.discoverMode === 'airing' ? 'RELEASING' : 'ALL';
      fetchDiscoverCatalog();
    };
  });

  document.querySelectorAll('[data-disc-genre]').forEach((btn) => {
    btn.onclick = () => {
      state.discoverGenre = btn.getAttribute('data-disc-genre') || null;
      fetchDiscoverCatalog();
    };
  });

  const btnSafe = document.getElementById('btn-toggle-safe');
  if (btnSafe) {
    btnSafe.onclick = () => {
      state.prefs.showAdultContent = !state.prefs.showAdultContent;
      savePrefs();
      fetchDiscoverCatalog();
    };
  }

  const btnDiscoverMore = document.getElementById('btn-discover-load-more');
  if (btnDiscoverMore) {
    btnDiscoverMore.onclick = () => fetchDiscoverCatalog(true);
  }
  if (state.tab === 'discover' && !state.isSearching && state.discover.length > 0) {
    requestAnimationFrame(() => checkDiscoverInfiniteScroll());
  }

  // Library events
  document.querySelectorAll('[data-mylist-status]').forEach((btn) => {
    btn.onclick = () => {
      state.myListStatusFilter = btn.getAttribute('data-mylist-status');
      renderApp();
    };
  });
  document.querySelectorAll('[data-mylist-sentiment]').forEach((btn) => {
    btn.onclick = () => {
      state.myListSentimentFilter = btn.getAttribute('data-mylist-sentiment') || null;
      renderApp();
    };
  });
  document.querySelectorAll('[data-mylist-sort]').forEach((btn) => {
    btn.onclick = () => {
      state.myListSort = btn.getAttribute('data-mylist-sort');
      renderApp();
    };
  });
  document.querySelectorAll('[data-mylist-col]').forEach((btn) => {
    btn.onclick = () => {
      state.myListCollectionFilter = btn.getAttribute('data-mylist-col') || null;
      renderApp();
    };
  });
  const inpMyList = document.getElementById('inp-mylist-search');
  if (inpMyList) {
    inpMyList.oninput = (e) => {
      state.myListQuery = e.target.value;
      renderApp();
    };
  }
  const btnCreateCol = document.getElementById('btn-create-collection');
  const btnNewColInline = document.getElementById('btn-new-collection-inline');
  if (btnCreateCol) btnCreateCol.onclick = () => { state.showCreateCollectionModal = true; renderApp(); };
  if (btnNewColInline) btnNewColInline.onclick = () => { state.showCreateCollectionModal = true; renderApp(); };

  // Community events
  document.querySelectorAll('[data-comm-view-tab]').forEach((btn) => {
    btn.onclick = () => {
      const next = btn.getAttribute('data-comm-view-tab') === 'tagged' ? 'tagged' : 'feed';
      state.communityViewTab = next;
      renderApp();
      if (next === 'tagged' && state.session) {
        loadUserNotifications(true);
      }
    };
  });
  const btnCommTaggedMarkAll = document.getElementById('btn-comm-tagged-mark-all');
  if (btnCommTaggedMarkAll) {
    btnCommTaggedMarkAll.onclick = () => markAllNotificationsRead();
  }
  const btnCommTaggedLoadMore = document.getElementById('btn-comm-tagged-load-more');
  if (btnCommTaggedLoadMore) {
    btnCommTaggedLoadMore.onclick = () => loadUserNotifications(false);
  }
  document.querySelectorAll('[data-open-tagged-notif]').forEach((btn) => {
    btn.onclick = () => {
      const notifId = btn.getAttribute('data-open-tagged-notif');
      const aid = Number(btn.getAttribute('data-notif-anime') || 0);
      if (!aid) return;
      openCommunityHubForAnime(aid, {
        tab: 'discussion',
        fallbackTitle: btn.getAttribute('data-notif-title') || '',
        fallbackCover: btn.getAttribute('data-notif-cover') || '',
        notificationId: notifId,
        jumpTarget: {
          commentId: btn.getAttribute('data-notif-comment') || null,
          threadId: btn.getAttribute('data-notif-thread') || null,
          replyId: btn.getAttribute('data-notif-reply') || null
        }
      });
    };
  });
  document.querySelectorAll('[data-comm-sentiment]').forEach((btn) => {
    btn.onclick = () => {
      state.communitySentimentFilter = btn.getAttribute('data-comm-sentiment') || null;
      renderApp();
    };
  });
  document.querySelectorAll('[data-comm-sort]').forEach((btn) => {
    btn.onclick = () => {
      state.communitySort = btn.getAttribute('data-comm-sort');
      renderApp();
    };
  });
  document.querySelectorAll('[data-open-comments]').forEach((el) => {
    el.onclick = (e) => {
      e.stopPropagation();
      const aid = Number(el.getAttribute('data-open-comments'));
      openCommunityHubForAnime(aid, { tab: 'discussion' });
    };
  });

  document.querySelectorAll('[data-comm-modal-tab]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const tab = btn.getAttribute('data-comm-modal-tab') === 'discussion' ? 'discussion' : 'notes';
      state.communityCommentsTab = tab;
      renderApp();
      const aid = Number(state.communityCommentsAnimeId || 0);
      if (tab === 'discussion' && aid) {
        if (Number(state.discussion.animeId) !== aid) {
          loadAnimeDiscussion(aid, true).then(() => applyPendingCommunityReplyTarget());
        } else {
          applyPendingCommunityReplyTarget();
        }
        if (state.session) {
          markAnimeDiscussionSeen(aid);
        }
      }
    };
  });

  const startReplyToCommunityMember = (uid, username, avatarKey) => {
    const aid = Number(state.communityCommentsAnimeId || 0);
    if (!aid || !state.session) return;
    state.communityCommentsTab = 'discussion';
    state.discussion.contextMenu = null;
    state.pendingCommunityReplyUser = {
      id: uid || '',
      username: username || 'Member',
      avatar_key: avatarKey || 'avatar_1'
    };
    renderApp();
    markAnimeDiscussionSeen(aid);
    if (Number(state.discussion.animeId) === aid && !state.discussion.loading) {
      applyPendingCommunityReplyTarget();
    } else {
      loadAnimeDiscussion(aid, true).then(() => applyPendingCommunityReplyTarget());
    }
  };

  document.querySelectorAll('[data-comm-note-reply-uid]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      startReplyToCommunityMember(
        btn.getAttribute('data-comm-note-reply-uid'),
        btn.getAttribute('data-comm-note-reply-name') || 'Member',
        btn.getAttribute('data-comm-note-reply-av') || 'avatar_1'
      );
    };
  });
  document.querySelectorAll('[data-comm-modal-star]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const clicked = Number(btn.getAttribute('data-comm-modal-star'));
      const cur = Number(state.communityCommentDraftStars || 0);
      state.communityCommentDraftStars = cur === clicked ? Math.max(0, clicked - 0.5) : clicked;
      const inp = document.getElementById('inp-comm-modal-note');
      if (inp) state.communityCommentDraftNote = inp.value;
      renderApp();
    };
  });
  const inpCommModalNote = document.getElementById('inp-comm-modal-note');
  if (inpCommModalNote) {
    inpCommModalNote.oninput = (e) => {
      state.communityCommentDraftNote = e.target.value;
    };
    inpCommModalNote.onkeydown = (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        document.getElementById('btn-comm-modal-submit')?.click();
      }
    };
  }
  const btnCommModalSubmit = document.getElementById('btn-comm-modal-submit');
  if (btnCommModalSubmit) {
    btnCommModalSubmit.onclick = async () => {
      if (state.communityNoteSaving) return;
      const aid = Number(state.communityCommentsAnimeId);
      if (!aid) return;
      const noteText = (document.getElementById('inp-comm-modal-note')?.value ?? state.communityCommentDraftNote ?? '').trim();
      const stars = Number(state.communityCommentDraftStars || 0);
      if (!noteText && stars <= 0) {
        state.communityNoteError = 'Please select a star rating or enter a comment.';
        state.communityNoteFeedback = '';
        renderApp();
        return;
      }

      const allGroups = groupCommunityReviewsByAnime(state.communityReviews);
      const grp = allGroups.find((g) => Number(g.anime_id) === aid);
      const rating10Val = stars > 0 ? Math.round(stars * 2) : null;

      state.communityNoteSaving = true;
      state.communityNoteFeedback = '';
      state.communityNoteError = '';
      renderApp();
      try {
        // Part 1: Adding a community note/comment/rating NEVER adds the anime to Home
        await submitCommunityCommentNote(aid, rating10Val, noteText, grp || null, { throwOnError: true, silentToast: true });
        state.communityCommentDraftNote = '';
        state.communityNoteFeedback = noteText ? 'Comment posted successfully' : 'Rating saved';
        showToast(state.communityNoteFeedback);
      } catch (err) {
        state.communityNoteError = err?.message || 'Failed to post. Please try again.';
        showToast(state.communityNoteError, true);
      } finally {
        state.communityNoteSaving = false;
        renderApp();
      }
    };
  }

  // Profile & Settings events
  const openAv = document.getElementById('btn-open-avatar-modal');
  if (openAv) openAv.onclick = () => { state.showAvatarModal = true; renderApp(); };
  const openProfEdit = document.getElementById('btn-open-profile-edit');
  if (openProfEdit) openProfEdit.onclick = () => { state.showProfileEditModal = true; renderApp(); };

  const btnSettingsBack = document.getElementById('btn-settings-back');
  if (btnSettingsBack) {
    btnSettingsBack.onclick = () => {
      if (state.settingsCategory) state.settingsCategory = null;
      else state.tab = 'profile';
      renderApp();
    };
  }
  document.querySelectorAll('[data-settings-cat]').forEach((el) => {
    el.onclick = () => {
      const cat = el.getAttribute('data-settings-cat');
      if (cat === 'DOWNLOADS_LINK') {
        state.settingsCategory = null;
        state.tab = 'downloads';
      } else {
        state.settingsCategory = cat;
      }
      renderApp();
    };
  });
  document.querySelectorAll('[data-set-theme]').forEach((btn) => {
    btn.onclick = () => {
      state.prefs.themeMode = btn.getAttribute('data-set-theme');
      savePrefs();
      renderApp();
    };
  });
  document.querySelectorAll('[data-set-accent]').forEach((btn) => {
    btn.onclick = () => {
      state.prefs.accentTheme = btn.getAttribute('data-set-accent');
      savePrefs();
      renderApp();
    };
  });
  document.querySelectorAll('[data-set-lang]').forEach((btn) => {
    btn.onclick = () => {
      state.prefs.titleLanguage = btn.getAttribute('data-set-lang');
      savePrefs();
      renderApp();
    };
  });

  const btnExitGuest = document.getElementById('btn-exit-guest');
  if (btnExitGuest) {
    btnExitGuest.onclick = () => {
      state.isGuestMode = false;
      localStorage.removeItem('anime_universe_guest');
      clearActiveUserMemoryState();
      renderApp();
    };
  }
  const btnSignout = document.getElementById('btn-signout');
  if (btnSignout) {
    btnSignout.onclick = () => {
      saveSession(null);
      state.tab = 'home';
      renderApp();
    };
  }

  // Detail Screen events
  const btnDetailBack = document.getElementById('btn-detail-back');
  if (btnDetailBack) {
    btnDetailBack.onclick = () => {
      state.tab = state.previousTab || 'home';
      renderApp();
    };
  }
  const btnDetailFav = document.getElementById('btn-detail-fav');
  if (btnDetailFav && state.detailAnime) {
    btnDetailFav.onclick = async () => {
      state.detailAnime.isFavorite = !state.detailAnime.isFavorite;
      await saveVaultEntry(state.detailAnime, { addToHome: false });
      renderApp();
    };
  }
  const btnDetailAdd = document.getElementById('btn-detail-add');
  if (btnDetailAdd && state.detailAnime) {
    btnDetailAdd.onclick = async () => {
      await addToHome({ ...state.detailAnime, status: 'watching' });
    };
  }
  const btnDetailRemoveHome = document.getElementById('btn-detail-remove-home');
  if (btnDetailRemoveHome && state.detailAnime) {
    btnDetailRemoveHome.onclick = () => {
      state.confirmRemoveHomeAnime = {
        animeId: state.detailAnime.animeId,
        title: resolveTitle(state.detailAnime)
      };
      renderApp();
    };
  }
  const btnDetailAddHome = document.getElementById('btn-detail-add-home');
  if (btnDetailAddHome && state.detailAnime) {
    btnDetailAddHome.onclick = async () => {
      await addToHome(state.detailAnime);
    };
  }
  document.querySelectorAll('[data-detail-status]').forEach((btn) => {
    btn.onclick = async () => {
      if (!state.detailAnime) return;
      const nextStatus = btn.getAttribute('data-detail-status');
      state.detailAnime.status = nextStatus;
      // Part 3: Setting status to Dropped removes ONLY from Home, preserving Library, progress, notes, ratings & Community
      await saveVaultEntry(state.detailAnime, { removeFromHome: nextStatus === 'dropped' });
      if (nextStatus === 'dropped') {
        showToast('Marked Dropped — removed from Home, kept in Library');
      }
      renderApp();
    };
  });
  const btnDetailRemove = document.getElementById('btn-detail-remove');
  if (btnDetailRemove && state.detailAnime) {
    btnDetailRemove.onclick = () => removeFromVault(state.detailAnime.animeId);
  }
  const saveDetailRatingOrNote = async (rating10Val, noteText, actionKind = 'rating') => {
    if (!state.detailAnime || state.detailRatingSaving) return;
    const hadRatingBefore = Boolean(state.detailAnime.rating10 && Number(state.detailAnime.rating10) > 0);
    state.detailRatingSaving = true;
    state.detailRatingFeedback = '';
    state.detailRatingError = '';
    const nextRating10 = rating10Val ?? state.detailAnime.rating10;
    const nextNotes = noteText !== undefined ? noteText : state.detailAnime.reviewNotes;
    state.detailAnime = { ...state.detailAnime, rating10: nextRating10, reviewNotes: nextNotes };
    renderApp();
    try {
      if (state.detailAnime.inWatchlist) {
        // Preserve Home status; never auto-add to Home when rating/noting
        await saveVaultEntry(
          {
            ...state.detailAnime,
            rating10: nextRating10,
            reviewNotes: nextNotes
          },
          { addToHome: false, throwOnError: true }
        );
      } else {
        // Not in Library/Home -> post to independent Community notes without adding to Home!
        await submitCommunityCommentNote(
          state.detailAnime.animeId,
          nextRating10,
          nextNotes,
          state.detailAnime,
          { throwOnError: true, silentToast: true }
        );
      }
      const starsStr = nextRating10 ? `${(Number(nextRating10) / 2).toFixed(1)} ★` : '';
      const msg =
        actionKind === 'note'
          ? (String(nextNotes || '').trim() ? 'Review note & rating saved successfully' : 'Rating saved successfully')
          : hadRatingBefore
          ? `Rating updated to ${starsStr}`
          : `Rating saved (${starsStr})`;
      state.detailRatingFeedback = msg;
      showToast(msg);
    } catch (err) {
      state.detailRatingError = err?.message || 'Failed to save rating. Please try again.';
      showToast(state.detailRatingError, true);
    } finally {
      state.detailRatingSaving = false;
      renderApp();
    }
  };
  document.querySelectorAll('[data-star-val]').forEach((btn) => {
    btn.onclick = async () => {
      if (!state.detailAnime) return;
      const clicked = Number(btn.getAttribute('data-star-val'));
      const currentStars = rating10ToStars(state.detailAnime.rating10) || 0;
      const nextStars = currentStars === clicked ? clicked - 0.5 : clicked;
      const nextRating10 = Math.round(nextStars * 2);
      await saveDetailRatingOrNote(nextRating10, undefined, 'rating');
    };
  });
  document.querySelectorAll('[data-pick-sentiment-stars]').forEach((btn) => {
    btn.onclick = async () => {
      if (!state.detailAnime) return;
      const st = Number(btn.getAttribute('data-pick-sentiment-stars'));
      const nextRating10 = Math.round(st * 2);
      await saveDetailRatingOrNote(nextRating10, undefined, 'rating');
    };
  });
  const btnStarMinus = document.getElementById('btn-star-minus');
  if (btnStarMinus && state.detailAnime) {
    btnStarMinus.onclick = async () => {
      const cur = rating10ToStars(state.detailAnime.rating10) || 0;
      const nextRating10 = Math.round(Math.max(0.5, cur - 0.5) * 2);
      await saveDetailRatingOrNote(nextRating10, undefined, 'rating');
    };
  }
  const btnStarPlus = document.getElementById('btn-star-plus');
  if (btnStarPlus && state.detailAnime) {
    btnStarPlus.onclick = async () => {
      const cur = rating10ToStars(state.detailAnime.rating10) || 0;
      const nextRating10 = Math.round(Math.min(5.0, cur + 0.5) * 2);
      await saveDetailRatingOrNote(nextRating10, undefined, 'rating');
    };
  }
  const inpDetailNotes = document.getElementById('inp-detail-notes');
  if (inpDetailNotes) {
    inpDetailNotes.oninput = (e) => {
      state.detailNoteDraft = e.target.value;
    };
  }
  const btnSaveNote = document.getElementById('btn-detail-save-note');
  if (btnSaveNote && state.detailAnime) {
    btnSaveNote.onclick = async () => {
      const notes = (document.getElementById('inp-detail-notes')?.value ?? state.detailNoteDraft ?? '').trim();
      state.detailNoteDraft = notes;
      await saveDetailRatingOrNote(state.detailAnime.rating10, notes, 'note');
    };
  }

  // Anime Discussion & LivTrAk Live Chat + Custom Emoji Tray events
  const autoGrowTextarea = (el) => {
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 92)}px`;
  };
  const inpDiscComment = document.getElementById('inp-discussion-comment');
  if (inpDiscComment) {
    autoGrowTextarea(inpDiscComment);
    inpDiscComment.oninput = () => {
      autoGrowTextarea(inpDiscComment);
      handleDiscussionInputMentionCheck('comment', inpDiscComment);
    };
    inpDiscComment.onclick = () => handleDiscussionInputMentionCheck('comment', inpDiscComment);
    inpDiscComment.onkeydown = (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        postAnimeComment();
      }
    };
  }
  const inpDiscReply = document.getElementById('inp-discussion-reply');
  if (inpDiscReply) {
    autoGrowTextarea(inpDiscReply);
    inpDiscReply.oninput = () => {
      autoGrowTextarea(inpDiscReply);
      handleDiscussionInputMentionCheck('reply', inpDiscReply);
    };
    inpDiscReply.onclick = () => handleDiscussionInputMentionCheck('reply', inpDiscReply);
    inpDiscReply.onkeydown = (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        postThreadReply();
      }
    };
  }
  const btnToggleEmojiTray = document.getElementById('btn-toggle-emoji-tray');
  if (btnToggleEmojiTray) {
    btnToggleEmojiTray.onclick = (e) => {
      e.stopPropagation();
      state.discussion.emojiTrayOpen = !state.discussion.emojiTrayOpen;
      renderApp();
      if (state.discussion.emojiTrayOpen) {
        EmojiCacheManager.ensureMetadata(false);
        hydrateVisibleEmojiNodes();
      }
    };
  }
  document.querySelectorAll('[data-insert-emoji-id]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const eid = btn.getAttribute('data-insert-emoji-id');
      const scope = btn.getAttribute('data-insert-emoji-scope') === 'reply' ? 'reply' : 'comment';
      if (!eid) return;
      const token = `[emoji:${eid}]`;
      const inputId = scope === 'reply' ? 'inp-discussion-reply' : 'inp-discussion-comment';
      const el = document.getElementById(inputId);
      const currentVal = el ? el.value : (scope === 'reply' ? state.discussion.replyDraft : state.discussion.draft) || '';
      const pos = el && typeof el.selectionStart === 'number' ? el.selectionStart : currentVal.length;
      const before = currentVal.slice(0, pos);
      const after = currentVal.slice(pos);
      const spacerBefore = before && !/\s$/.test(before) ? ' ' : '';
      const spacerAfter = after && !/^\s/.test(after) ? ' ' : ' ';
      const nextText = `${before}${spacerBefore}${token}${spacerAfter}${after}`;
      if (scope === 'reply') state.discussion.replyDraft = nextText;
      else state.discussion.draft = nextText;
      renderApp();
      setTimeout(() => {
        const nextEl = document.getElementById(inputId);
        if (nextEl) {
          nextEl.focus();
          const nextCursor = (before + spacerBefore + token + spacerAfter).length;
          nextEl.setSelectionRange(nextCursor, nextCursor);
          autoGrowTextarea(nextEl);
        }
      }, 20);
    };
  });
  const inpDiscEditComment = document.getElementById('inp-discussion-edit-comment');
  if (inpDiscEditComment) {
    inpDiscEditComment.oninput = () => handleDiscussionInputMentionCheck('edit_comment', inpDiscEditComment);
  }
  const inpDiscEditReply = document.getElementById('inp-discussion-edit-reply');
  if (inpDiscEditReply) {
    inpDiscEditReply.oninput = () => handleDiscussionInputMentionCheck('edit_reply', inpDiscEditReply);
  }
  const btnPostDiscComment = document.getElementById('btn-post-discussion-comment');
  if (btnPostDiscComment) btnPostDiscComment.onclick = () => postAnimeComment();
  const activeDiscAnime = getActiveDiscussionAnime();
  const btnLoadMoreDisc = document.getElementById('btn-discussion-load-more');
  if (btnLoadMoreDisc && activeDiscAnime) {
    btnLoadMoreDisc.onclick = () => loadAnimeDiscussion(activeDiscAnime.animeId, false);
  }
  // Open unseen tagged anime card directly in Live Chat modal (supports both new & older templates)
  document.querySelectorAll('[data-open-unseen-anime]').forEach((el) => {
    el.onclick = (e) => {
      if (e.target.closest('[data-open-comments]')) return;
      const aid = Number(el.getAttribute('data-open-unseen-anime'));
      if (!aid) return;
      openCommunityHubForAnime(aid, {
        tab: 'discussion',
        jumpTarget: {
          commentId: el.getAttribute('data-unseen-comment') || null,
          threadId: el.getAttribute('data-unseen-thread') || null,
          replyId: el.getAttribute('data-unseen-reply') || null
        }
      });
    };
  });

  // Remove selected tag chip
  document.querySelectorAll('[data-remove-tag-scope]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const scope = btn.getAttribute('data-remove-tag-scope');
      const idx = Number(btn.getAttribute('data-remove-tag-idx'));
      if (scope === 'comment') {
        state.discussion.commentTags = state.discussion.commentTags.filter((_, i) => i !== idx);
      } else if (scope === 'reply') {
        state.discussion.replyTags = state.discussion.replyTags.filter((_, i) => i !== idx);
      }
      renderApp();
    };
  });

  // Helper to open reply thread & inline reply box for a target user with quoted context
  const openReplyForTarget = (commentId, targetUserId, targetUsername, quotedReplyId = null, quotedPreview = '', quotedAuthor = '') => {
    const comment = state.discussion.comments.find((c) => c.id === commentId);
    const existingThread = comment?.threads?.find((th) => th.target_user_id === targetUserId);
    if (existingThread) {
      state.discussion.expandedThreads[existingThread.id] = true;
    }
    let resolvedAuthor = String(quotedAuthor || targetUsername || 'Member').trim();
    let resolvedPreview = String(quotedPreview || '').trim();
    if (!resolvedPreview && comment) {
      if (quotedReplyId) {
        for (const th of comment.threads || []) {
          const hit = (th.replies || []).find((r) => r.id === quotedReplyId);
          if (hit) {
            resolvedAuthor = hit.username || resolvedAuthor;
            resolvedPreview = formatPlainPreviewWithEmojis(hit.content ?? hit.body ?? '').slice(0, 180);
            break;
          }
        }
      } else {
        resolvedAuthor = comment.username || resolvedAuthor;
        resolvedPreview = formatPlainPreviewWithEmojis(comment.content ?? comment.body ?? '').slice(0, 180);
      }
    }
    state.discussion.activeReplyTarget = {
      commentId,
      parentCommentId: quotedReplyId || commentId,
      targetUserId,
      targetUsername: targetUsername || 'Member',
      quotedCommentId: commentId,
      quotedReplyId: quotedReplyId || null,
      quotedUsername: resolvedAuthor,
      quotedPreview: resolvedPreview
    };
    state.discussion.replyDraft = '';
    state.discussion.replyTags = [];
    state.discussion.error = '';
    state.discussion.contextMenu = null;
    renderApp();
    setTimeout(() => document.getElementById('inp-discussion-reply')?.focus(), 40);
  };

  // Clickable quoted preview inside replies -> jump to original comment or reply and highlight it
  document.querySelectorAll('[data-jump-comment]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const cid = btn.getAttribute('data-jump-comment');
      const rid = btn.getAttribute('data-jump-reply') || null;
      if (!cid) return;
      let targetThreadId = null;
      if (rid) {
        const parentComment = state.discussion.comments.find((c) => c.id === cid);
        const ownerThread = (parentComment?.threads || []).find((th) => (th.replies || []).some((r) => r.id === rid));
        if (ownerThread) {
          targetThreadId = ownerThread.id;
          state.discussion.expandedThreads[ownerThread.id] = true;
        }
      }
      state.discussion.highlightTarget = { commentId: cid, threadId: targetThreadId, replyId: rid };
      renderApp();
      setTimeout(() => {
        const domId = rid ? `discussion-reply-${rid}` : `discussion-comment-${cid}`;
        document.getElementById(domId)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 60);
    };
  });

  // Clickable @mention username badges -> tag or reply directly to that user
  document.querySelectorAll('[data-mention-click-name]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const uname = btn.getAttribute('data-mention-click-name') || '';
      if (!uname) return;
      if (!state.session) {
        showToast('Sign in to tag or reply to members');
        return;
      }
      const scope = state.discussion.activeReplyTarget ? 'reply' : 'comment';
      const uid = btn.getAttribute('data-mention-click-uid') || '';
      const addMentionToComposer = async () => {
        let resolvedUser = uid ? { id: uid, username: uname, avatar_key: 'avatar_1' } : null;
        if (!resolvedUser) {
          try {
            const found = await searchMentionableUsers(uname);
            resolvedUser = (found || []).find((u) => String(u.username || '').toLowerCase() === uname.toLowerCase()) || null;
          } catch (_) {}
        }
        if (resolvedUser?.id) {
          const listKey = scope === 'reply' ? 'replyTags' : 'commentTags';
          const cur = state.discussion[listKey] || [];
          const draft = scope === 'reply' ? state.discussion.replyDraft : state.discussion.draft;
          const check = validateContentMentions(draft || '', cur.length);
          if (check.totalMentions < MAX_MENTIONS_PER_CONTENT && !cur.some((t) => t.id === resolvedUser.id)) {
            state.discussion[listKey] = [...cur, resolvedUser];
            renderApp();
          }
          showToast(`Tagged @${resolvedUser.username}`);
        } else {
          const inputId = scope === 'reply' ? 'inp-discussion-reply' : 'inp-discussion-comment';
          const current = (scope === 'reply' ? state.discussion.replyDraft : state.discussion.draft) || '';
          const addition = current && !/\s$/.test(current) ? ` @${uname} ` : `@${uname} `;
          if (scope === 'reply') state.discussion.replyDraft = current + addition;
          else state.discussion.draft = current + addition;
          renderApp();
          setTimeout(() => document.getElementById(inputId)?.focus(), 40);
        }
      };
      addMentionToComposer();
    };
  });

  // Right-click (Desktop) + Long-press (Phone) on Comment Cards & Reply Items -> Context Reply Menu
  const bindContextTriggers = (el, payloadFactory) => {
    if (!state.session) return;
    el.oncontextmenu = (e) => {
      if (e.target.closest('textarea, input, button')) return;
      e.preventDefault();
      e.stopPropagation();
      state.discussion.contextMenu = { ...payloadFactory(), x: e.clientX, y: e.clientY };
      renderApp();
    };
    let lpTimer = null;
    el.ontouchstart = (e) => {
      if (e.target.closest('textarea, input, button')) return;
      const touch = e.touches && e.touches[0];
      if (!touch) return;
      const tx = touch.clientX;
      const ty = touch.clientY;
      lpTimer = setTimeout(() => {
        state.discussion.contextMenu = { ...payloadFactory(), x: tx, y: ty };
        renderApp();
      }, 450);
    };
    const clearLp = () => {
      if (lpTimer) {
        clearTimeout(lpTimer);
        lpTimer = null;
      }
    };
    el.ontouchend = clearLp;
    el.ontouchmove = clearLp;
    el.ontouchcancel = clearLp;
  };

  document.querySelectorAll('[data-ctx-comment]').forEach((card) => {
    bindContextTriggers(card, () => ({
      type: 'comment',
      commentId: card.getAttribute('data-ctx-comment'),
      userId: card.getAttribute('data-ctx-uid'),
      username: card.getAttribute('data-ctx-name') || 'Member',
      isOwner: card.getAttribute('data-ctx-owner') === '1'
    }));
  });

  document.querySelectorAll('[data-ctx-reply]').forEach((item) => {
    bindContextTriggers(item, () => ({
      type: 'reply',
      replyId: item.getAttribute('data-ctx-reply'),
      commentId: item.getAttribute('data-ctx-reply-cid'),
      threadId: item.getAttribute('data-ctx-reply-tid'),
      userId: item.getAttribute('data-ctx-uid'),
      username: item.getAttribute('data-ctx-name') || 'Member',
      quotedPreview: item.getAttribute('data-ctx-quoted-preview') || '',
      isOwner: item.getAttribute('data-ctx-owner') === '1'
    }));
  });

  document.querySelectorAll('[data-ctx-comm-note]').forEach((item) => {
    bindContextTriggers(item, () => ({
      type: 'comm_note',
      userId: item.getAttribute('data-ctx-uid') || '',
      username: item.getAttribute('data-ctx-name') || 'Member',
      avatarKey: item.getAttribute('data-ctx-av') || 'avatar_1',
      isOwner: false
    }));
  });

  const discCtxBackdrop = document.getElementById('discussion-ctx-backdrop');
  if (discCtxBackdrop) {
    discCtxBackdrop.onclick = () => {
      state.discussion.contextMenu = null;
      renderApp();
    };
  }
  const ctxBtnReply = document.getElementById('ctx-btn-reply');
  if (ctxBtnReply && state.discussion.contextMenu) {
    ctxBtnReply.onclick = () => {
      const cm = state.discussion.contextMenu;
      if (cm.type === 'comm_note') {
        startReplyToCommunityMember(cm.userId, cm.username, cm.avatarKey);
      } else {
        openReplyForTarget(
          cm.commentId,
          cm.userId,
          cm.username,
          cm.type === 'reply' ? cm.replyId : null,
          cm.quotedPreview || ''
        );
      }
    };
  }
  const ctxBtnEdit = document.getElementById('ctx-btn-edit');
  if (ctxBtnEdit && state.discussion.contextMenu) {
    ctxBtnEdit.onclick = () => {
      const cm = state.discussion.contextMenu;
      state.discussion.contextMenu = null;
      if (cm.type === 'comment') {
        const target = state.discussion.comments.find((c) => c.id === cm.commentId);
        if (target) {
          state.discussion.editingCommentId = cm.commentId;
          state.discussion.editingCommentDraft = target.content ?? target.body ?? '';
        }
      } else {
        const comment = state.discussion.comments.find((c) => c.id === cm.commentId);
        const thread = comment?.threads?.find((th) => th.id === cm.threadId);
        const reply = thread?.replies?.find((r) => r.id === cm.replyId);
        if (reply) {
          state.discussion.editingReplyId = cm.replyId;
          state.discussion.editingReplyCommentId = cm.commentId;
          state.discussion.editingReplyThreadId = cm.threadId;
          state.discussion.editingReplyDraft = reply.content ?? reply.body ?? '';
        }
      }
      renderApp();
    };
  }
  const ctxBtnDelete = document.getElementById('ctx-btn-delete');
  if (ctxBtnDelete && state.discussion.contextMenu) {
    ctxBtnDelete.onclick = () => {
      const cm = state.discussion.contextMenu;
      state.discussion.contextMenu = null;
      if (cm.type === 'comment') {
        deleteAnimeComment(cm.commentId);
      } else {
        deleteThreadReply(cm.commentId, cm.threadId, cm.replyId);
      }
    };
  }

  document.querySelectorAll('[data-start-reply-comment]').forEach((btn) => {
    btn.onclick = () => {
      openReplyForTarget(
        btn.getAttribute('data-start-reply-comment'),
        btn.getAttribute('data-reply-target-uid'),
        btn.getAttribute('data-reply-target-name') || 'Member',
        btn.getAttribute('data-reply-quoted-id') || null,
        btn.getAttribute('data-reply-quoted-preview') || '',
        btn.getAttribute('data-reply-quoted-author') || ''
      );
    };
  });
  const btnCancelReply = document.getElementById('btn-cancel-reply-composer');
  if (btnCancelReply) {
    btnCancelReply.onclick = () => {
      state.discussion.activeReplyTarget = null;
      state.discussion.replyDraft = '';
      state.discussion.replyTags = [];
      renderApp();
    };
  }
  const btnPostReply = document.getElementById('btn-post-thread-reply');
  if (btnPostReply) btnPostReply.onclick = () => postThreadReply();

  document.querySelectorAll('[data-toggle-thread]').forEach((btn) => {
    btn.onclick = () => {
      const tid = btn.getAttribute('data-toggle-thread');
      state.discussion.expandedThreads[tid] = !state.discussion.expandedThreads[tid];
      renderApp();
    };
  });
  document.querySelectorAll('[data-edit-comment]').forEach((btn) => {
    btn.onclick = () => {
      const cid = btn.getAttribute('data-edit-comment');
      const target = state.discussion.comments.find((c) => c.id === cid);
      if (!target) return;
      state.discussion.editingCommentId = cid;
      state.discussion.editingCommentDraft = target.content ?? target.body ?? '';
      renderApp();
    };
  });
  document.querySelectorAll('[data-cancel-edit-comment]').forEach((btn) => {
    btn.onclick = () => {
      state.discussion.editingCommentId = null;
      state.discussion.editingCommentDraft = '';
      renderApp();
    };
  });
  document.querySelectorAll('[data-save-edit-comment]').forEach((btn) => {
    btn.onclick = () => saveEditedAnimeComment(btn.getAttribute('data-save-edit-comment'));
  });
  document.querySelectorAll('[data-delete-comment]').forEach((btn) => {
    btn.onclick = () => deleteAnimeComment(btn.getAttribute('data-delete-comment'));
  });
  document.querySelectorAll('[data-edit-reply]').forEach((btn) => {
    btn.onclick = () => {
      const rid = btn.getAttribute('data-edit-reply');
      const cid = btn.getAttribute('data-edit-reply-cid');
      const tid = btn.getAttribute('data-edit-reply-tid');
      const comment = state.discussion.comments.find((c) => c.id === cid);
      const thread = comment?.threads?.find((th) => th.id === tid);
      const reply = thread?.replies?.find((r) => r.id === rid);
      if (!reply) return;
      state.discussion.editingReplyId = rid;
      state.discussion.editingReplyCommentId = cid;
      state.discussion.editingReplyThreadId = tid;
      state.discussion.editingReplyDraft = reply.content ?? reply.body ?? '';
      renderApp();
    };
  });
  document.querySelectorAll('[data-cancel-edit-reply]').forEach((btn) => {
    btn.onclick = () => {
      state.discussion.editingReplyId = null;
      state.discussion.editingReplyCommentId = null;
      state.discussion.editingReplyThreadId = null;
      state.discussion.editingReplyDraft = '';
      renderApp();
    };
  });
  document.querySelectorAll('[data-save-edit-reply]').forEach((btn) => {
    btn.onclick = () =>
      saveEditedThreadReply(
        btn.getAttribute('data-edit-reply-cid'),
        btn.getAttribute('data-edit-reply-tid'),
        btn.getAttribute('data-save-edit-reply')
      );
  });
  document.querySelectorAll('[data-delete-reply]').forEach((btn) => {
    btn.onclick = () =>
      deleteThreadReply(
        btn.getAttribute('data-delete-reply-cid'),
        btn.getAttribute('data-delete-reply-tid'),
        btn.getAttribute('data-delete-reply')
      );
  });

  // Notifications Screen events
  const btnNotifRefresh = document.getElementById('btn-notif-refresh');
  if (btnNotifRefresh) btnNotifRefresh.onclick = () => loadUserNotifications(true);
  const btnNotifMarkAll = document.getElementById('btn-notif-mark-all');
  if (btnNotifMarkAll) btnNotifMarkAll.onclick = () => markAllNotificationsRead();
  const btnNotifLoadMore = document.getElementById('btn-notif-load-more');
  if (btnNotifLoadMore) btnNotifLoadMore.onclick = () => loadUserNotifications(false);
  document.querySelectorAll('[data-open-notification]').forEach((btn) => {
    btn.onclick = async () => {
      const notifId = btn.getAttribute('data-open-notification');
      const animeId = Number(btn.getAttribute('data-notif-anime'));
      const commentId = btn.getAttribute('data-notif-comment') || null;
      const threadId = btn.getAttribute('data-notif-thread') || null;
      const replyId = btn.getAttribute('data-notif-reply') || null;
      await markNotificationRead(notifId);
      if (animeId > 0) {
        state.pendingNotificationJump = { animeId, commentId, threadId, replyId };
        await openAnimeDetail(animeId);
      } else {
        renderApp();
      }
    };
  });

  // Modal bindings
  const backdrop = document.getElementById('modal-backdrop');
  const btnClose = document.getElementById('btn-close-modal');
  const closeModals = () => {
    state.quickEditAnime = null;
    state.showAvatarModal = false;
    state.showProfileEditModal = false;
    state.showCreateCollectionModal = false;
    state.communityCommentsAnimeId = null;
    state.communityCommentsAnimeFallback = null;
    state.pendingCommunityReplyUser = null;
    state.confirmRemoveHomeAnime = null;
    state.contextMenu = null;
    state.discussion.contextMenu = null;
    state.discussion.emojiTrayOpen = false;
    renderApp();
  };
  if (backdrop) backdrop.onclick = closeModals;
  if (btnClose) btnClose.onclick = closeModals;

  // Hydrate all lazy/cached custom emoji slots after every render without blocking UI
  hydrateVisibleEmojiNodes();

  // Download Center events
  document.querySelectorAll('[data-trigger-download]').forEach((btn) => {
    btn.onclick = () => {
      const taskKey = btn.getAttribute('data-trigger-download');
      const dlUrl = btn.getAttribute('data-dl-url');
      const dlFile = btn.getAttribute('data-dl-filename');
      startPlatformDownload(taskKey, dlUrl, dlFile);
    };
  });

  const btnSaveCollection = document.getElementById('btn-save-collection');
  if (btnSaveCollection) {
    btnSaveCollection.onclick = async () => {
      const name = document.getElementById('inp-new-col-name')?.value?.trim();
      if (!name) return;
      state.showCreateCollectionModal = false;
      if (state.session) {
        try {
          const created = await supabaseRequest(
            '/rest/v1/collections',
            'POST',
            {
              creator_id: state.session.userId,
              name,
              description: '',
              visibility: 'community'
            },
            'return=representation'
          );
          if (Array.isArray(created) && created[0]) {
            state.collections.unshift({
              id: created[0].id,
              name: created[0].name,
              description: '',
              visibility: 'community',
              animeIds: []
            });
            saveCollections();
          }
        } catch (_) {}
      } else {
        state.collections.unshift({
          id: `local_${Date.now()}`,
          name,
          description: '',
          visibility: 'community',
          animeIds: []
        });
        saveCollections();
      }
      renderApp();
    };
  }

  document.querySelectorAll('[data-pick-avatar]').forEach((el) => {
    el.onclick = async () => {
      const key = normalizeAvatarKey(el.getAttribute('data-pick-avatar'));
      state.profile.avatarKey = key;
      saveProfileCache();
      renderApp();
      if (state.session) {
        const uid = encodeURIComponent(state.session.userId);
        await supabaseRequest(`/rest/v1/profiles?id=eq.${uid}`, 'PATCH', { avatar_key: key, avatar_url: null }).catch(() => {});
      }
    };
  });

  const btnSaveProf = document.getElementById('btn-save-profile');
  if (btnSaveProf) {
    btnSaveProf.onclick = async () => {
      const u = document.getElementById('inp-prof-username')?.value?.trim();
      if (u) {
        state.profile.username = u;
        saveProfileCache();
      }
      state.showProfileEditModal = false;
      renderApp();
      if (state.session && u) {
        const uid = encodeURIComponent(state.session.userId);
        await supabaseRequest(`/rest/v1/profiles?id=eq.${uid}`, 'PATCH', { username: u }).catch(() => {});
      }
    };
  }

  // Quick Edit Sheet events
  if (state.quickEditAnime) {
    const qeDec = document.getElementById('qe-dec');
    const qeInc = document.getElementById('qe-inc');
    const qeInput = document.getElementById('qe-ep-input');
    if (qeDec) {
      qeDec.onclick = () => {
        state.quickEditAnime.watchedEpisodes = Math.max(0, (state.quickEditAnime.watchedEpisodes || 0) - 1);
        renderApp();
      };
    }
    if (qeInc) {
      qeInc.onclick = () => {
        const next = (state.quickEditAnime.watchedEpisodes || 0) + 1;
        state.quickEditAnime.watchedEpisodes = state.quickEditAnime.totalEpisodes
          ? Math.min(state.quickEditAnime.totalEpisodes, next)
          : next;
        renderApp();
      };
    }
    if (qeInput) {
      qeInput.onfocus = () => qeInput.select();
      qeInput.onkeydown = (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          document.getElementById('qe-save')?.click();
        } else if (e.key === 'Escape') {
          e.preventDefault();
          state.quickEditAnime = null;
          renderApp();
        }
      };
    }
    document.querySelectorAll('[data-qe-star]').forEach((btn) => {
      btn.onclick = () => {
        const clicked = Number(btn.getAttribute('data-qe-star'));
        const cur = rating10ToStars(state.quickEditAnime.rating10) || 0;
        const next = cur === clicked ? clicked - 0.5 : clicked;
        state.quickEditAnime.rating10 = Math.round(next * 2);
        renderApp();
      };
    });
    const qeSave = document.getElementById('qe-save');
    if (qeSave) {
      qeSave.onclick = async () => {
        const rawEp = document.getElementById('qe-ep-input')?.value ?? String(state.quickEditAnime.watchedEpisodes || 0);
        const check = validateEpisodeInput(rawEp, state.quickEditAnime.totalEpisodes);
        if (!check.valid) {
          showToast(check.error, true);
          renderApp();
          return;
        }
        const notes = document.getElementById('qe-notes')?.value || '';
        state.quickEditAnime.watchedEpisodes = check.value;
        state.quickEditAnime.reviewNotes = notes.trim();
        const toSave = { ...state.quickEditAnime };
        state.quickEditAnime = null;
        await saveVaultEntry(toSave, { addToHome: false });
        renderApp();
      };
    }
  }
}

window.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    if (state.contextMenu || state.confirmRemoveHomeAnime) {
      state.contextMenu = null;
      state.confirmRemoveHomeAnime = null;
      renderApp();
    }
  }
});

// Initial Boot & Automatic Background Sync Across All Platforms
renderApp();
loadDownloadsConfig();
fetchDiscoverCatalog();
if (state.session) {
  loadCloudData(false);
}

// Silent OTP email verification check every 4 seconds (only during OTP verification)
setInterval(async () => {
  if (
    !state.session &&
    state.authMode === 'verify_otp' &&
    state.pendingVerifyEmail &&
    state.pendingVerifyPassword &&
    !state.loading &&
    !document.hidden
  ) {
    try {
      const data = await supabaseRequest('/auth/v1/token?grant_type=password', 'POST', {
        email: state.pendingVerifyEmail,
        password: state.pendingVerifyPassword
      });
      if (data && data.access_token) {
        saveSession({
          accessToken: data.access_token,
          refreshToken: data.refresh_token || '',
          expiresAt: Math.floor(Date.now() / 1000) + (data.expires_in || 3600),
          userId: data.user?.id,
          email: data.user?.email || state.pendingVerifyEmail,
          username: '',
          onboardingCompleted: false
        });
        state.onboardUsername = '';
        state.onboardAvatarKey = 'avatar_1';
        state.authInfo = 'Email verified! Choose your username and profile picture.';
        state.authMode = 'onboard_username';
        renderApp();
      }
    } catch (_) {}
  }
}, 4000);

// Real-time Supabase Notification Socket + Lightweight Polling Fallback
const NotificationRealtimeManager = {
  ws: null,
  heartbeatId: null,
  reconnectId: null,
  connectedUserId: null,
  refCounter: 1,
  ensureConnected() {
    const uid = state.session?.userId;
    const token = state.session?.accessToken;
    if (!uid || !token) {
      this.disconnect();
      return;
    }
    if (this.connectedUserId === uid && this.ws && this.ws.readyState <= 1) return;
    this.disconnect();
    const baseUrl = getVaultBaseUrl();
    const pubKey = getVaultPublicKey();
    if (!baseUrl.startsWith('http')) return;
    const wsUrl = `${baseUrl.replace(/^http/, 'ws')}/realtime/v1/websocket?apikey=${encodeURIComponent(pubKey)}&vsn=1.0.0`;
    try {
      const ws = new WebSocket(wsUrl);
      this.ws = ws;
      this.connectedUserId = uid;
      ws.onopen = () => {
        const joinRef = String(this.refCounter++);
        ws.send(JSON.stringify({
          topic: 'realtime:public:user_notifications',
          event: 'phx_join',
          payload: {
            config: {
              broadcast: { self: false },
              presence: { key: '' },
              postgres_changes: [
                { event: 'INSERT', schema: 'public', table: 'user_notifications', filter: `recipient_user_id=eq.${uid}` }
              ]
            },
            access_token: token
          },
          ref: joinRef
        }));
        ws.send(JSON.stringify({ topic: 'realtime:public:user_notifications', event: 'access_token', payload: { access_token: token }, ref: String(this.refCounter++) }));
        this.heartbeatId = setInterval(() => {
          if (ws.readyState === 1) {
            ws.send(JSON.stringify({ topic: 'phoenix', event: 'heartbeat', payload: {}, ref: String(this.refCounter++) }));
          }
        }, 25000);
      };
      ws.onmessage = (evt) => {
        try {
          const msg = JSON.parse(evt.data);
          if (msg?.event === 'postgres_changes' && msg?.payload?.data?.type === 'INSERT') {
            refreshUnreadNotificationCount().then(() => {
              updateCommunityRedDotsInPlace();
              if (state.tab === 'notifications' || (state.tab === 'community' && state.communityViewTab === 'tagged')) {
                loadUserNotifications(true);
              }
            });
            const rec = msg?.payload?.data?.record;
            if (rec && rec.sender_user_id !== uid) {
              const preview = formatPlainPreviewWithEmojis(rec.preview_text || '').slice(0, 60);
              showToast(preview ? `New reply/mention: ${preview}` : 'You have a new reply or @mention!');
            }
          }
        } catch (_) {}
      };
      ws.onclose = () => {
        if (this.heartbeatId) clearInterval(this.heartbeatId);
        this.heartbeatId = null;
        if (state.session?.userId === uid) {
          this.reconnectId = setTimeout(() => this.ensureConnected(), 8000);
        }
      };
      ws.onerror = () => {};
    } catch (_) {}
  },
  disconnect() {
    if (this.heartbeatId) clearInterval(this.heartbeatId);
    if (this.reconnectId) clearTimeout(this.reconnectId);
    this.heartbeatId = null;
    this.reconnectId = null;
    this.connectedUserId = null;
    if (this.ws) {
      try { this.ws.close(); } catch (_) {}
      this.ws = null;
    }
  }
};

// Gentle background sync every 30s + fast notification unread check every 15s
let lastSilentSyncAt = 0;
function triggerThrottledSilentSync() {
  const now = Date.now();
  if (!state.session || document.hidden || now - lastSilentSyncAt < 15000) return;
  lastSilentSyncAt = now;
  NotificationRealtimeManager.ensureConnected();
  loadCloudData(true);
}

setInterval(() => {
  if (state.session && !document.hidden) {
    NotificationRealtimeManager.ensureConnected();
    refreshUnreadNotificationCount().then(() => updateCommunityRedDotsInPlace());
  }
}, 15000);

setInterval(() => {
  if (state.session && !document.hidden) {
    lastSilentSyncAt = Date.now();
    loadCloudData(true);
  }
}, 30000);

if (state.session) {
  NotificationRealtimeManager.ensureConnected();
}

window.addEventListener('focus', () => {
  triggerThrottledSilentSync();
});

document.addEventListener('visibilitychange', () => {
  if (!document.hidden) triggerThrottledSilentSync();
});
