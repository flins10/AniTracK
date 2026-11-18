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

// Obfuscated Vault Shards (Matches Android & Desktop VaultMaterial)
const U0 = [142, 189, 7, 111, 21, 58, 232, 139, 161, 70, 101, 29, 63, 238];
const U1 = [43, 201, 206, 190, 74, 112, 5, 42, 241, 204, 185, 85, 61];
const U2 = [96, 73, 56, 236, 149, 160, 65, 121, 4, 40, 231, 158, 171];
const K0 = [33, 201, 136, 171, 66, 89, 93, 40, 211, 176, 163, 127, 113, 73, 97, 192];
const K1 = [105, 3, 52, 217, 130, 142, 98, 95, 28, 103, 247, 138, 144, 105, 66];
const K2 = [159, 77, 121, 19, 3, 255, 136, 136, 105, 67, 65, 20, 182, 173, 170];

function reconstructVault(s0, s1, s2, len, seedByte) {
  const bytes = new Uint8Array(len);
  let i0 = 0, i1 = 0, i2 = 0;
  for (let i = 0; i < len; i++) {
    const masked = i % 3 === 0 ? s0[i0++] : i % 3 === 1 ? s1[i1++] : s2[i2++];
    const salt = ((i * 73 + seedByte * 31 + 19) ^ 0x5a) & 0xff;
    bytes[i] = (masked ^ salt) & 0xff;
  }
  return new TextDecoder().decode(bytes);
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
  heartOutline: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z"/></svg>`
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

// Persistent State
const savedPrefs = JSON.parse(localStorage.getItem('anime_universe_prefs') || '{}');
const savedLocalVault = JSON.parse(localStorage.getItem('anime_universe_local_vault') || '[]');
const savedCollections = JSON.parse(localStorage.getItem('anime_universe_collections') || '[]');
const savedProfile = JSON.parse(localStorage.getItem('anime_universe_profile') || '{}');

const state = {
  session: JSON.parse(localStorage.getItem('anime_universe_session') || localStorage.getItem('auralog_session') || 'null'),
  isGuestMode: localStorage.getItem('anime_universe_guest') === 'true',
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
  authMode: 'signin', // 'signin' | 'signup' | 'forgot' | 'onboard_username' | 'onboard_avatar'
  identifier: '',
  password: '',
  showPassword: false,
  onboardUsername: '',
  onboardAvatarKey: 'avatar_1',
  authError: '',
  authInfo: '',
  loading: false,

  // Profile State
  profile: {
    username: savedProfile.username || 'Member',
    avatarKey: savedProfile.avatarKey || 'avatar_1',
    bio: savedProfile.bio || '',
    memberStatus: savedProfile.memberStatus || 'active',
    isAdmin: Boolean(savedProfile.isAdmin)
  },
  showAvatarModal: false,
  showProfileEditModal: false,

  // Home Schedule State
  homeAllWeekDays: true,
  selectedDay: ((new Date().getDay() + 6) % 7) + 1,

  // Library (MyList) State
  vault: savedLocalVault,
  collections: savedCollections,
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
  isSearching: false,

  // Community State
  communityReviews: [],
  communitySentimentFilter: null,
  communitySort: 'RECENT',

  // Modals & Detail View
  detailAnime: null,
  detailRich: null,
  detailLoading: false,
  quickEditAnime: null
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

function saveLocalVault() {
  localStorage.setItem('anime_universe_local_vault', JSON.stringify(state.vault));
}

function saveCollections() {
  localStorage.setItem('anime_universe_collections', JSON.stringify(state.collections));
}

function saveProfileCache() {
  localStorage.setItem('anime_universe_profile', JSON.stringify(state.profile));
}

function saveSession(session) {
  state.session = session;
  if (session) {
    localStorage.setItem('anime_universe_session', JSON.stringify(session));
    localStorage.setItem('auralog_session', JSON.stringify(session));
    state.isGuestMode = false;
    localStorage.removeItem('anime_universe_guest');
  } else {
    localStorage.removeItem('anime_universe_session');
    localStorage.removeItem('auralog_session');
  }
}

function escapeHtml(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function getAvatarImgUrl(avatarKey) {
  const av = BUILT_IN_AVATARS.find((a) => a.key === avatarKey) || BUILT_IN_AVATARS[0];
  return av.img;
}

function renderAvatarCircle(avatarKey, username = 'A', sizePx = 28) {
  const av = BUILT_IN_AVATARS.find((a) => a.key === avatarKey) || BUILT_IN_AVATARS[0];
  return `
    <div style="width:${sizePx}px;height:${sizePx}px;border-radius:50%;overflow:hidden;border:1.5px solid rgba(255,255,255,0.32);background:linear-gradient(135deg,${av.c1},${av.c2});flex-shrink:0;">
      <img src="${av.img}" alt="${escapeHtml(username)}" style="width:100%;height:100%;object-fit:cover;display:block;" />
    </div>
  `;
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
    const baseUrl = reconstructVault(U0, U1, U2, 40, 0x37).replace(/\/+$/, '');
    const anonKey = reconstructVault(K0, K1, K2, 46, 0x6b).trim();
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
  const baseUrl = reconstructVault(U0, U1, U2, 40, 0x37).replace(/\/+$/, '');
  const anonKey = reconstructVault(K0, K1, K2, 46, 0x6b).trim();
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
  const baseUrl = reconstructVault(U0, U1, U2, 40, 0x37).replace(/\/+$/, '');
  const anonKey = reconstructVault(K0, K1, K2, 46, 0x6b).trim();
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

// Fetch User Watchlist, Collections, Profile & Community Reviews (Exact match to SupabaseDatabaseService.kt)
async function loadCloudData(silent = false) {
  if (!state.session || !state.session.userId) return;
  try {
    const uid = encodeURIComponent(state.session.userId);
    // Exact select string from SupabaseDatabaseService.kt line 1060 (no user_season column!)
    const watchlistSelect =
      'id,user_id,anime_id,status,episode_progress,total_episodes,personal_rating,review_notes,is_favorite,updated_at,' +
      'anime(anilist_id,title_english,title_romaji,title_native,cover_image,banner_image,genres,release_year,season,episodes,average_score,popularity,format,status,studios,next_airing_at,next_episode_number)';

    const [normRows, colRows, prof, cmRows] = await Promise.all([
      supabaseRequest(`/rest/v1/user_anime?user_id=eq.${uid}&select=${watchlistSelect}&order=updated_at.desc`).catch(() => null),
      supabaseRequest(`/rest/v1/collections?creator_id=eq.${uid}&select=*,collection_anime(anime_id)&order=updated_at.desc`).catch(() => null),
      supabaseRequest(`/rest/v1/profiles?id=eq.${uid}&select=*`).catch(() => []),
      supabaseRequest(`/rest/v1/community_members?user_id=eq.${uid}&select=status`).catch(() => [])
    ]);

    if (Array.isArray(normRows)) {
      state.vault = normRows.map((r) => {
        const anime = r.anime || {};
        const existingLocal = state.vault.find((v) => v.animeId === r.anime_id) || {};
        const eng = anime.title_english && anime.title_english !== 'null' ? anime.title_english : existingLocal.titleEnglish || '';
        const rom = anime.title_romaji && anime.title_romaji !== 'null' ? anime.title_romaji : existingLocal.titleRomaji || '';
        const nextAir = anime.next_airing_at ? Number(anime.next_airing_at) : existingLocal.airingTimeUnix || null;
        const dow = nextAir && nextAir > 0
          ? (((new Date(nextAir * 1000).getDay() + 6) % 7) + 1)
          : existingLocal.airingDayOfWeek || ((r.anime_id % 7) + 1);
        return {
          animeId: r.anime_id,
          title: eng || rom || existingLocal.title || `Anime #${r.anime_id}`,
          titleEnglish: eng || rom || 'Anime',
          titleRomaji: rom || eng || 'Anime',
          titleNative: anime.title_native || existingLocal.titleNative || '',
          coverImage: anime.cover_image || existingLocal.coverImage || '',
          bannerImage: anime.banner_image || existingLocal.bannerImage || '',
          format: anime.format || existingLocal.format || 'TV',
          mediaStatus: anime.status || existingLocal.mediaStatus || 'RELEASING',
          seasonYear: anime.release_year || existingLocal.seasonYear || null,
          averageScore: anime.average_score || existingLocal.averageScore || null,
          countryOfOrigin: existingLocal.countryOfOrigin || 'JP',
          airingDayOfWeek: dow,
          airingTimeUnix: nextAir,
          nextEpisodeNumber: anime.next_episode_number || existingLocal.nextEpisodeNumber || null,
          status: r.status || 'watching',
          season: existingLocal.season || 1,
          watchedEpisodes: r.episode_progress || 0,
          totalEpisodes: r.total_episodes || anime.episodes || existingLocal.totalEpisodes || null,
          rating10: r.personal_rating !== null && r.personal_rating !== undefined ? Number(r.personal_rating) : null,
          reviewNotes: r.review_notes || '',
          isPinned: Boolean(existingLocal.isPinned),
          isFavorite: Boolean(r.is_favorite ?? existingLocal.isFavorite),
          genres: Array.isArray(anime.genres) ? anime.genres : existingLocal.genres || [],
          synopsis: existingLocal.synopsis || ''
        };
      });
      saveLocalVault();
    }

    if (Array.isArray(colRows)) {
      state.collections = colRows.map((c) => ({
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
      const p = prof[0];
      state.profile = {
        username: p.username || state.session.username || 'Member',
        avatarKey: p.avatar_key || p.avatar_url || 'avatar_1',
        bio: p.bio || '',
        memberStatus: cmStatus || p.member_status || 'active',
        isAdmin: Boolean(p.is_admin)
      };
      saveProfileCache();
    }

    await loadCommunityReviews();
    if (!silent || !document.activeElement || !['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName)) {
      renderApp();
    }
  } catch (_) {}
}

// Fetch Community Reviews (Exact match to SupabaseDatabaseService.kt line 1402 — no user_season column!)
async function loadCommunityReviews() {
  try {
    const feedSelect =
      'id,user_id,anime_id,status,episode_progress,personal_rating,review_notes,updated_at,' +
      'anime(anilist_id,title_english,title_romaji,cover_image,episodes)';
    const normRows = await supabaseRequest(
      `/rest/v1/user_anime?select=${feedSelect}&order=updated_at.desc&limit=80`
    ).catch(() => []);

    if (Array.isArray(normRows)) {
      const valid = normRows.filter(
        (r) => r.personal_rating !== null && Number(r.personal_rating) > 0 && String(r.review_notes || '').trim().length > 0
      );
      const userIds = Array.from(new Set(valid.map((r) => r.user_id).filter(Boolean)));
      const reviewIds = Array.from(new Set(valid.map((r) => r.id).filter(Boolean)));

      const [profRows, likeRows] = await Promise.all([
        userIds.length > 0
          ? supabaseRequest(`/rest/v1/profiles?id=in.(${userIds.join(',')})&select=id,username,avatar_key,avatar_url`).catch(() => [])
          : Promise.resolve([]),
        reviewIds.length > 0
          ? supabaseRequest(`/rest/v1/review_likes?review_id=in.(${reviewIds.join(',')})&select=review_id,user_id`).catch(() => [])
          : Promise.resolve([])
      ]);

      const profMap = {};
      if (Array.isArray(profRows)) {
        profRows.forEach((p) => {
          profMap[p.id] = {
            username: p.username || 'Member',
            avatar_key: p.avatar_key || p.avatar_url || 'avatar_1'
          };
        });
      }

      const likesCountMap = {};
      const likedByMeSet = new Set();
      if (Array.isArray(likeRows)) {
        likeRows.forEach((l) => {
          if (!l.review_id) return;
          likesCountMap[l.review_id] = (likesCountMap[l.review_id] || 0) + 1;
          if (state.session && l.user_id === state.session.userId) {
            likedByMeSet.add(l.review_id);
          }
        });
      }

      state.communityReviews = valid.map((r) => {
        const anime = r.anime || {};
        const prof = profMap[r.user_id] || {
          username: r.user_id === state.session?.userId ? state.profile.username : 'Member',
          avatar_key: r.user_id === state.session?.userId ? state.profile.avatarKey : 'avatar_1'
        };
        return {
          id: r.id,
          user_id: r.user_id,
          username: prof.username,
          avatar_key: prof.avatar_key,
          anime_id: r.anime_id,
          anime_title:
            (anime.title_english && anime.title_english !== 'null' ? anime.title_english : '') ||
            anime.title_romaji ||
            'Anime',
          anime_cover_image: anime.cover_image || '',
          rating: Number(r.personal_rating),
          review_notes: String(r.review_notes || '').trim(),
          updated_at: r.updated_at,
          likes_count: likesCountMap[r.id] || 0,
          liked_by_me: likedByMeSet.has(r.id)
        };
      });
    }
  } catch (_) {}
}

async function fetchDiscoverCatalog() {
  state.isSearching = true;
  renderApp();

  const variables = {
    page: 1,
    perPage: 24,
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
          description(asHtml: false)
          nextAiringEpisode { airingAt episode }
        }
      }
    }
  `;

  try {
    const resp = await fetch('https://graphql.anilist.co', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: gql, variables })
    });
    const json = await resp.json();
    const list = json?.data?.Page?.media || [];
    state.discover = list.map((m) => {
      const airingAt = m.nextAiringEpisode?.airingAt || null;
      const dow = airingAt ? (((new Date(airingAt * 1000).getDay() + 6) % 7) + 1) : ((m.id % 7) + 1);
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
        genres: m.genres || [],
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
    });
  } catch (_) {
  } finally {
    state.isSearching = false;
    renderApp();
  }
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
  window.scrollTo({ top: 0, behavior: 'smooth' });
  renderApp();

  const gql = `
    query($id: Int) {
      Media(id: $id, type: ANIME) {
        id
        title { english romaji native }
        coverImage { extraLarge large }
        bannerImage
        format
        status
        episodes
        averageScore
        season
        seasonYear
        countryOfOrigin
        genres
        description(asHtml: false)
        nextAiringEpisode { airingAt episode }
        studios(isMain: true) { nodes { name } }
        relations {
          edges {
            relationType
            node { id type title { english romaji } coverImage { large } format status episodes seasonYear }
          }
        }
      }
    }
  `;

  try {
    const resp = await fetch('https://graphql.anilist.co', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: gql, variables: { id: animeId } })
    });
    const json = await resp.json();
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
        seasonYear: m.seasonYear || null,
        countryOfOrigin: m.countryOfOrigin || 'JP',
        totalEpisodes: m.episodes || existing?.totalEpisodes || null,
        averageScore: m.averageScore || null,
        genres: m.genres || [],
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

      const relEdges = (m.relations?.edges || []).filter((e) => e.node && e.node.type === 'ANIME');
      const prequels = relEdges.filter((e) => e.relationType === 'PREQUEL').map((e) => e.node);
      const sequels = relEdges.filter((e) => e.relationType === 'SEQUEL').map((e) => e.node);
      const seasonNodes = [...prequels, { id: m.id, title: m.title, status: m.status, seasonYear: m.seasonYear, isCurrent: true }, ...sequels];
      state.detailRich = {
        seasons: seasonNodes.map((sn, idx) => ({
          id: sn.id,
          seasonNumber: idx + 1,
          seasonYear: sn.seasonYear,
          status: sn.status,
          isCurrent: sn.id === m.id
        }))
      };
    }
  } catch (_) {
  } finally {
    state.detailLoading = false;
    renderApp();
  }
}

// Save Watchlist Entry to Supabase (Exact match to SupabaseDatabaseService.upsertUserAnimeEntry — NO user_season column!)
async function saveVaultEntry(item) {
  const cleanItem = { ...item, inWatchlist: true };
  state.vault = [cleanItem, ...state.vault.filter((v) => v.animeId !== cleanItem.animeId)];
  if (state.detailAnime && state.detailAnime.animeId === cleanItem.animeId) {
    state.detailAnime = { ...cleanItem };
  }
  saveLocalVault();
  if (!state.session) return;
  try {
    const catalogKey = `${cleanItem.titleEnglish || cleanItem.title}|${cleanItem.coverImage}|${cleanItem.totalEpisodes || 0}`;
    if (syncedCatalogHashes.get(cleanItem.animeId) !== catalogKey) {
      await supabaseRequest(
        '/rest/v1/anime?on_conflict=anilist_id',
        'POST',
        {
          anilist_id: cleanItem.animeId,
          title_english: String(cleanItem.titleEnglish || cleanItem.title || 'Anime').slice(0, 140),
          title_romaji: String(cleanItem.titleRomaji || cleanItem.title || 'Anime').slice(0, 140),
          title_native: String(cleanItem.titleNative || '').slice(0, 100) || null,
          cover_image: cleanItem.coverImage || '',
          banner_image: cleanItem.bannerImage || null,
          episodes: cleanItem.totalEpisodes || null,
          release_year: cleanItem.seasonYear || null,
          average_score: cleanItem.averageScore || null,
          status: cleanItem.mediaStatus || 'RELEASING',
          format: cleanItem.format || 'TV',
          genres: Array.isArray(cleanItem.genres) ? cleanItem.genres.slice(0, 5) : [],
          next_airing_at: cleanItem.airingTimeUnix || null,
          next_episode_number: cleanItem.nextEpisodeNumber || null
        },
        'resolution=merge-duplicates,return=minimal'
      ).catch(() => {});
      syncedCatalogHashes.set(cleanItem.animeId, catalogKey);
    }

    await supabaseRequest(
      '/rest/v1/user_anime?on_conflict=user_id,anime_id',
      'POST',
      {
        user_id: state.session.userId,
        anime_id: cleanItem.animeId,
        status: cleanItem.status || 'watching',
        episode_progress: Math.max(0, Number(cleanItem.watchedEpisodes || 0)),
        total_episodes: cleanItem.totalEpisodes || null,
        personal_rating: cleanItem.rating10 ? Math.min(10, Math.max(1, Number(cleanItem.rating10))) : null,
        review_notes: String(cleanItem.reviewNotes || '').trim().slice(0, 320),
        is_favorite: Boolean(cleanItem.isFavorite)
      },
      'resolution=merge-duplicates,return=minimal'
    );
    await loadCommunityReviews();
  } catch (_) {}
}

async function removeFromVault(animeId) {
  state.vault = state.vault.filter((v) => v.animeId !== animeId);
  if (state.detailAnime && state.detailAnime.animeId === animeId) {
    state.detailAnime.inWatchlist = false;
  }
  saveLocalVault();
  renderApp();
  if (!state.session) return;
  try {
    const uid = encodeURIComponent(state.session.userId);
    await supabaseRequest(`/rest/v1/user_anime?user_id=eq.${uid}&anime_id=eq.${animeId}`, 'DELETE').catch(() => {});
  } catch (_) {}
}

// ============================================================================
// SHARED WIDGETS: OBSIDIAN TOP BAR & POSTER TRACKER CARD
// ============================================================================

function renderObsidianTopBar({ title, accentTitleTail = null, subtitle, showBrandMonogram = false, actionsHtml = '' }) {
  const avatarImg = getAvatarImgUrl(state.profile.avatarKey);
  return `
    <header class="obsidian-topbar">
      <div class="topbar-left">
        ${showBrandMonogram ? `<div class="brand-monogram">A</div>` : ''}
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
        <button class="profile-avatar-btn" data-nav="profile" title="Open Profile">
          <img src="${avatarImg}" alt="Profile" />
        </button>
      </div>
    </header>
  `;
}

function renderPosterTrackerCard(item, showTrackingStepper = true) {
  const inVault = state.vault.find((v) => v.animeId === item.animeId);
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

  const isCompleted = isInUserList && (effective.status === 'completed' || (total && total > 0 && watched >= total));
  const canDec = isInUserList && watched > 0;
  const canInc = !isInUserList || !total || total <= 0 || watched < total;
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
    <div class="poster-tracker-card">
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
              <div class="capsule-stepper">
                <button class="stepper-circle-btn" data-dec-id="${effective.animeId}" ${!canDec ? 'disabled' : ''}>−</button>
                <div class="stepper-center-count" data-quick-edit="${effective.animeId}" title="Tap to type custom episode or rate">
                  ${watched}<span>/${total && total > 0 ? total : '∞'}</span>
                </div>
                <button class="stepper-circle-btn plus" data-inc-id="${effective.animeId}" ${!canInc ? 'disabled' : ''}>+</button>
              </div>
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
                <button class="stepper-circle-btn ${isInUserList ? '' : 'plus'}" data-quick-add="${effective.animeId}" title="${isInUserList ? 'Edit tracking' : 'Quick add to Watching'}">
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
// SCREEN 1: HOME SCREEN (Matches Screenshot 3 1:1)
// ============================================================================

function renderHomeScreen() {
  const todayDow = ((new Date().getDay() + 6) % 7) + 1;
  const activeShows = state.vault;
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
    showBrandMonogram: true,
    actionsHtml: `<button class="app-icon-btn" id="btn-open-quick-search" title="Quick Search">${SVG_ICONS.search}</button>`
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
      <span class="micro-label" style="color:var(--accent-primary);">${state.discover.length} TITLES</span>
    </div>

    ${
      state.isSearching
        ? `<div class="app-card" style="text-align:center;padding:48px;"><p style="color:var(--accent-primary);">Loading Anime Universe catalog...</p></div>`
        : `<div class="poster-grid">${state.discover.map((it) => renderPosterTrackerCard(it, false)).join('')}</div>`
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
  const topBar = renderObsidianTopBar({
    title: 'Community',
    subtitle: `RATED & NOTED • ${state.communityReviews.length} REVIEWS`
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

  const filtered = state.communityReviews
    .filter((rev) => {
      if (!state.communitySentimentFilter) return true;
      const s = sentimentFromRating10(rev.rating);
      return s && s.key === state.communitySentimentFilter;
    })
    .sort((a, b) => {
      if (state.communitySort === 'HIGHEST') return (b.rating || 0) - (a.rating || 0);
      return String(b.updated_at || '').localeCompare(String(a.updated_at || ''));
    });

  return `
    ${topBar}
    <div class="capsule-strip">
      <button class="capsule-pill ${!state.communitySentimentFilter ? 'active' : ''}" data-comm-sentiment="">
        ALL (${state.communityReviews.length})
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
      filtered.length === 0
        ? `<div class="app-card" style="text-align:center;padding:42px;"><p style="color:var(--text-secondary);">No rated &amp; noted showcases match this filter yet.</p></div>`
        : `<div class="community-review-list">
            ${filtered
              .map((rev) => {
                const starsNum = rating10ToStars(rev.rating) || 0;
                const stars = starsNum.toFixed(1);
                const sentiment = sentimentFromRating10(rev.rating);
                return `
                  <div class="community-review-card" data-open-detail="${rev.anime_id}">
                    <div style="display:flex;gap:16px;align-items:flex-start;">
                      <img src="${escapeHtml(rev.anime_cover_image)}" alt="" style="width:68px;height:96px;object-fit:cover;border-radius:14px;flex-shrink:0;" />
                      <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:6px;">
                        <div class="serif-heading" style="font-size:23px;line-height:27px;color:var(--text-primary);">${escapeHtml(rev.anime_title)}</div>
                        <div style="display:flex;align-items:center;gap:8px;">
                          <div style="display:inline-flex;gap:2px;">${renderFiveStarsHtml(starsNum)}</div>
                          <span class="tabular-num" style="font-size:15px;font-weight:600;color:var(--text-primary);">${stars}</span>
                        </div>
                        ${sentiment ? `<div><span class="sentiment-pill soft ${sentiment.key}">${sentiment.fullLabel.toUpperCase()}</span></div>` : ''}
                      </div>
                    </div>

                    <div class="review-quote-box">
                      <div class="review-quote-bar"></div>
                      <div class="review-quote-text">“${escapeHtml(rev.review_notes)}”</div>
                    </div>

                    <div style="display:flex;align-items:center;justify-content:space-between;padding-top:2px;">
                      <div style="display:flex;align-items:center;gap:10px;">
                        ${renderAvatarCircle(rev.avatar_key || 'avatar_1', rev.username, 28)}
                        <span style="font-size:13.5px;font-weight:500;color:var(--text-secondary);">${escapeHtml(rev.username || 'Member')}</span>
                      </div>
                      <span style="font-size:12.5px;color:var(--text-muted);">${escapeHtml(formatReviewDate(rev.updated_at))}</span>
                    </div>
                  </div>
                `;
              })
              .join('')}
          </div>`
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
  const avatarImg = getAvatarImgUrl(state.profile.avatarKey);

  return `
    <div style="max-width:760px;margin:0 auto;">
      <div style="display:flex;align-items:center;justify-content:space-between;padding:6px 0 14px;">
        <button class="app-icon-btn" data-nav="home" title="Back">${SVG_ICONS.back}</button>
        <h1 class="serif-heading" style="font-size:24px;">Profile</h1>
        <button class="app-icon-btn" data-nav="settings" title="Settings">${SVG_ICONS.settings}</button>
      </div>

      <div style="display:flex;flex-direction:column;align-items:center;gap:8px;padding:12px 0 18px;">
        <div id="btn-open-avatar-modal" style="position:relative;width:108px;height:108px;cursor:pointer;" title="Tap to change portrait">
          <div style="width:104px;height:104px;border-radius:50%;padding:3px;background:linear-gradient(135deg,var(--accent-primary),var(--accent-secondary));box-shadow:0 12px 32px var(--accent-soft);">
            <img src="${avatarImg}" alt="Avatar" style="width:100%;height:100%;border-radius:50%;object-fit:cover;border:2.5px solid var(--bg-main);" />
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
            ? `
              <div>
                <div class="micro-label" style="color:var(--accent-primary);margin-bottom:8px;">SEASONS</div>
                <div class="capsule-strip" style="margin-bottom:4px;">
                  ${seasons
                    .map(
                      (s) => `
                      <button class="capsule-pill gold-solid ${s.isCurrent ? 'active' : ''}" data-open-detail="${s.id}" style="min-width:52px;height:44px;justify-content:center;font-size:14px;">
                        ${s.seasonNumber}
                      </button>`
                    )
                    .join('')}
                </div>
              </div>
            `
            : ''
        }

        <div class="micro-label" style="color:var(--accent-primary);">EPISODE PROGRESS</div>
        ${
          !a.inWatchlist
            ? `<button class="btn-primary" id="btn-detail-add" style="width:100%;">+ ADD TO LIBRARY</button>`
            : `
              <div class="capsule-stepper" style="height:48px;padding:6px;">
                <button class="stepper-circle-btn" id="btn-detail-dec" style="width:36px;height:36px;">−</button>
                <div class="stepper-center-count" style="font-size:18px;">
                  EP ${watched} <span>/ ${total || '∞'}</span>
                </div>
                <button class="stepper-circle-btn plus" id="btn-detail-inc" style="width:36px;height:36px;">+</button>
              </div>
              <div class="capsule-strip" style="margin-bottom:0;">
                ${WATCH_STATUSES.map(
                  (st) => `
                  <button class="capsule-pill ${a.status === st.dbValue ? 'active' : ''}" data-detail-status="${st.dbValue}">
                    ${st.label}
                  </button>`
                ).join('')}
                <button class="capsule-pill" id="btn-detail-remove" style="color:var(--error);border-color:rgba(229,90,107,0.4);">Remove</button>
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
          a.reviewNotes || ''
        )}</textarea>
        <button class="btn-primary" id="btn-detail-save-note">SAVE RATING &amp; NOTE</button>
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
    </div>
  `;
}

// ============================================================================
// AUTH SCREEN WITH MASCOT (Matches Screenshot 1 1:1)
// ============================================================================

function renderAuthScreen() {
  const isSignup = state.authMode === 'signup';
  return `
    <div class="auth-viewport">
      <div class="auth-mascot-stack">
        <img class="auth-mascot-img" src="/assets/img_login_mascot.png" alt="Anime Universe Mascot" />
        <div class="glass-auth-card">
          ${
            state.authMode === 'onboard_username'
              ? `
                <div class="micro-label" style="color:var(--accent-primary);text-align:center;">STEP 1 OF 2 • IDENTITY</div>
                <h2 class="serif-heading" style="font-size:26px;text-align:center;">Claim Your Identity</h2>
                <p style="font-size:12.5px;color:var(--text-secondary);text-align:center;">Choose your unique Anime Universe handle.</p>
                ${state.authError ? `<div style="color:var(--error);font-size:12.5px;text-align:center;">${escapeHtml(state.authError)}</div>` : ''}
                <input id="inp-onboard-user" class="auth-input" placeholder="Username (e.g. Lauma)" value="${escapeHtml(state.onboardUsername)}" />
                <button class="btn-primary" id="btn-onboard-next">CONTINUE ›</button>
              `
              : state.authMode === 'onboard_avatar'
              ? `
                <div class="micro-label" style="color:var(--accent-primary);text-align:center;">STEP 2 OF 2 • PORTRAIT</div>
                <h2 class="serif-heading" style="font-size:26px;text-align:center;">Select Your Royal Portrait</h2>
                <div class="round-avatar-carousel">
                  ${BUILT_IN_AVATARS.map(
                    (av) => `
                    <div class="avatar-carousel-item ${state.onboardAvatarKey === av.key ? 'selected' : ''}" data-onboard-avatar="${av.key}">
                      <div class="avatar-ring-wrap">
                        <img src="${av.img}" alt="${escapeHtml(av.name)}" />
                        ${state.onboardAvatarKey === av.key ? `<div class="avatar-check-badge">✓</div>` : ''}
                      </div>
                      <span style="font-size:11px;color:${state.onboardAvatarKey === av.key ? 'var(--highlight)' : 'var(--text-secondary)'};text-align:center;">
                        ${escapeHtml(av.name)}
                      </span>
                    </div>`
                  ).join('')}
                </div>
                <button class="btn-primary" id="btn-onboard-finish">ENTER ANIME UNIVERSE</button>
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
                  !isSignup
                    ? `
                      <div class="auth-forgot-row">
                        <button type="button" class="auth-forgot-btn" id="btn-forgot-pass">Forgot Password?</button>
                      </div>
                    `
                    : ''
                }

                <button class="btn-primary" id="btn-auth-submit" style="width:100%;margin-top:4px;" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'PLEASE WAIT...' : isSignup ? 'SIGN UP' : 'SIGN IN'}
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
// MODALS: QUICK CONTEXT EDIT, COLLECTION MODAL & AVATAR CAROUSEL MODAL
// ============================================================================

function renderModals() {
  if (state.quickEditAnime) {
    const a = state.quickEditAnime;
    const title = resolveTitle(a);
    const stars = rating10ToStars(a.rating10) || 0;
    return `
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
                <div class="avatar-ring-wrap">
                  <img src="${av.img}" alt="${escapeHtml(av.name)}" />
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

  return '';
}

// ============================================================================
// ROOT RENDER & EVENT BINDING
// ============================================================================

function renderApp() {
  const root = document.getElementById('app');
  if (!root) return;

  if (!state.session && !state.isGuestMode) {
    root.innerHTML = renderAuthScreen();
    bindAuthEvents();
    return;
  }

  const sidebarTabs = [
    { id: 'home', title: 'Home', icon: SVG_ICONS.home },
    { id: 'discover', title: 'Explore', icon: SVG_ICONS.explore },
    { id: 'mylist', title: 'Library', icon: SVG_ICONS.library },
    { id: 'community', title: 'Community', icon: SVG_ICONS.community },
    { id: 'profile', title: 'Profile', icon: SVG_ICONS.profile }
  ];

  const bottomDockTabs = sidebarTabs.slice(0, 4);

  let screenHtml = '';
  if (state.tab === 'home') screenHtml = renderHomeScreen();
  else if (state.tab === 'discover') screenHtml = renderDiscoverScreen();
  else if (state.tab === 'mylist') screenHtml = renderMyListScreen();
  else if (state.tab === 'community') screenHtml = renderCommunityScreen();
  else if (state.tab === 'profile') screenHtml = renderProfileScreen();
  else if (state.tab === 'settings') screenHtml = renderSettingsScreen();
  else if (state.tab === 'detail') screenHtml = renderAnimeDetailScreen();

  root.innerHTML = `
    <div class="universe-scaffold">
      <!-- MidnightDesktopSidebar (>=900px) -->
      <aside class="midnight-sidebar">
        <div class="sidebar-brand-row">
          <div class="brand-monogram">A</div>
          <span class="serif-heading" style="font-size:19px;font-weight:500;letter-spacing:0.5px;">ANIME UNIVERSE</span>
        </div>
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
            </button>`
          )
          .join('')}
      </aside>

      <!-- MidnightCompactRail (600-899px) -->
      <aside class="midnight-compact-rail">
        <div class="brand-monogram">A</div>
        ${sidebarTabs
          .map(
            (t) => `
            <button class="rail-item ${state.tab === t.id ? 'active' : ''}" data-nav="${t.id}">
              ${t.icon}
              <span>${t.title}</span>
            </button>`
          )
          .join('')}
      </aside>

      <!-- Main Content Stage -->
      <main class="universe-stage">
        ${screenHtml}
      </main>

      <!-- MidnightBottomNavigationBar (<600px, 4 Capsule Tabs) -->
      ${
        state.tab !== 'detail' && state.tab !== 'profile' && state.tab !== 'settings'
          ? `
            <div class="midnight-bottom-dock-wrap">
              <nav class="midnight-bottom-dock">
                ${bottomDockTabs
                  .map(
                    (t) => `
                    <button class="dock-tab ${state.tab === t.id ? 'active' : ''}" data-nav="${t.id}">
                      ${t.icon}
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
}

function bindAuthEvents() {
  const inpIdentifier = document.getElementById('inp-identifier');
  const inpPass = document.getElementById('inp-pass');
  if (inpIdentifier) inpIdentifier.oninput = (e) => { state.identifier = e.target.value; };
  if (inpPass) {
    inpPass.oninput = (e) => { state.password = e.target.value; };
    inpPass.onkeydown = (e) => {
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
      renderApp();
      try {
        const targetEmail = await resolveLoginIdentifier(state.identifier);
        await supabaseRequest('/auth/v1/recover', 'POST', { email: targetEmail });
        state.authInfo = `Password reset link sent to ${targetEmail}.`;
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
      state.isGuestMode = true;
      localStorage.setItem('anime_universe_guest', 'true');
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
          const email = state.identifier.trim();
          const data = await supabaseRequest('/auth/v1/signup', 'POST', {
            email,
            password: state.password
          });
          if (data.access_token) {
            saveSession({
              accessToken: data.access_token,
              refreshToken: data.refresh_token || '',
              expiresAt: Math.floor(Date.now() / 1000) + (data.expires_in || 3600),
              userId: data.user?.id,
              email,
              username: email.split('@')[0]
            });
            state.authMode = 'onboard_username';
            state.onboardUsername = email.split('@')[0];
          } else {
            state.authInfo = 'Check your email to confirm your account, then sign in.';
            state.authMode = 'signin';
          }
        } else {
          const resolvedEmail = await resolveLoginIdentifier(state.identifier);
          const data = await supabaseRequest('/auth/v1/token?grant_type=password', 'POST', {
            email: resolvedEmail,
            password: state.password
          });
          saveSession({
            accessToken: data.access_token,
            refreshToken: data.refresh_token || '',
            expiresAt: Math.floor(Date.now() / 1000) + (data.expires_in || 3600),
            userId: data.user?.id,
            email: data.user?.email || resolvedEmail,
            username: data.user?.user_metadata?.username || resolvedEmail.split('@')[0]
          });
          await loadCloudData();
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
  if (inpOnboardUser) inpOnboardUser.oninput = (e) => { state.onboardUsername = e.target.value; };

  const btnOnboardNext = document.getElementById('btn-onboard-next');
  if (btnOnboardNext) {
    btnOnboardNext.onclick = () => {
      if (!state.onboardUsername.trim()) {
        state.authError = 'Please enter a username.';
        renderApp();
        return;
      }
      state.authMode = 'onboard_avatar';
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
      state.profile.username = state.onboardUsername.trim();
      state.profile.avatarKey = state.onboardAvatarKey;
      saveProfileCache();
      state.authMode = 'signin';
      if (state.session) {
        await supabaseRequest(
          '/rest/v1/profiles?on_conflict=id',
          'POST',
          {
            id: state.session.userId,
            username: state.profile.username,
            avatar_key: state.profile.avatarKey,
            avatar_url: state.profile.avatarKey
          },
          'resolution=merge-duplicates,return=minimal'
        ).catch(() => {});
        await loadCloudData();
      }
      renderApp();
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
      e.stopPropagation();
      openAnimeDetail(Number(el.getAttribute('data-open-detail')));
    };
  });

  document.querySelectorAll('[data-inc-id]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-inc-id'));
      const item = state.vault.find((v) => v.animeId === id);
      if (!item) return;
      const next = (item.watchedEpisodes || 0) + 1;
      item.watchedEpisodes = item.totalEpisodes ? Math.min(item.totalEpisodes, next) : next;
      if (item.totalEpisodes && item.watchedEpisodes >= item.totalEpisodes) item.status = 'completed';
      renderApp();
      await saveVaultEntry(item);
    };
  });

  document.querySelectorAll('[data-dec-id]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-dec-id'));
      const item = state.vault.find((v) => v.animeId === id);
      if (!item) return;
      item.watchedEpisodes = Math.max(0, (item.watchedEpisodes || 0) - 1);
      renderApp();
      await saveVaultEntry(item);
    };
  });

  document.querySelectorAll('[data-quick-add]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-quick-add'));
      const existing = state.vault.find((v) => v.animeId === id);
      if (existing) {
        state.quickEditAnime = { ...existing };
        renderApp();
        return;
      }
      const disc = state.discover.find((d) => d.animeId === id);
      if (disc) {
        await saveVaultEntry({ ...disc, status: 'watching', watchedEpisodes: 0 });
        renderApp();
      }
    };
  });

  document.querySelectorAll('[data-quick-edit]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-quick-edit'));
      const item = state.vault.find((v) => v.animeId === id);
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
      state.settingsCategory = el.getAttribute('data-settings-cat');
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
      renderApp();
    };
  }
  const btnSignout = document.getElementById('btn-signout');
  if (btnSignout) {
    btnSignout.onclick = () => {
      saveSession(null);
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
      await saveVaultEntry(state.detailAnime);
      renderApp();
    };
  }
  const btnDetailAdd = document.getElementById('btn-detail-add');
  if (btnDetailAdd && state.detailAnime) {
    btnDetailAdd.onclick = async () => {
      await saveVaultEntry({ ...state.detailAnime, status: 'watching' });
      renderApp();
    };
  }
  const btnDetailInc = document.getElementById('btn-detail-inc');
  if (btnDetailInc && state.detailAnime) {
    btnDetailInc.onclick = async () => {
      const next = (state.detailAnime.watchedEpisodes || 0) + 1;
      state.detailAnime.watchedEpisodes = state.detailAnime.totalEpisodes ? Math.min(state.detailAnime.totalEpisodes, next) : next;
      await saveVaultEntry(state.detailAnime);
      renderApp();
    };
  }
  const btnDetailDec = document.getElementById('btn-detail-dec');
  if (btnDetailDec && state.detailAnime) {
    btnDetailDec.onclick = async () => {
      state.detailAnime.watchedEpisodes = Math.max(0, (state.detailAnime.watchedEpisodes || 0) - 1);
      await saveVaultEntry(state.detailAnime);
      renderApp();
    };
  }
  document.querySelectorAll('[data-detail-status]').forEach((btn) => {
    btn.onclick = async () => {
      if (!state.detailAnime) return;
      state.detailAnime.status = btn.getAttribute('data-detail-status');
      await saveVaultEntry(state.detailAnime);
      renderApp();
    };
  });
  const btnDetailRemove = document.getElementById('btn-detail-remove');
  if (btnDetailRemove && state.detailAnime) {
    btnDetailRemove.onclick = () => removeFromVault(state.detailAnime.animeId);
  }
  document.querySelectorAll('[data-star-val]').forEach((btn) => {
    btn.onclick = async () => {
      if (!state.detailAnime) return;
      const clicked = Number(btn.getAttribute('data-star-val'));
      const currentStars = rating10ToStars(state.detailAnime.rating10) || 0;
      const nextStars = currentStars === clicked ? clicked - 0.5 : clicked;
      state.detailAnime.rating10 = Math.round(nextStars * 2);
      await saveVaultEntry(state.detailAnime);
      renderApp();
    };
  });
  document.querySelectorAll('[data-pick-sentiment-stars]').forEach((btn) => {
    btn.onclick = async () => {
      if (!state.detailAnime) return;
      const st = Number(btn.getAttribute('data-pick-sentiment-stars'));
      state.detailAnime.rating10 = Math.round(st * 2);
      await saveVaultEntry(state.detailAnime);
      renderApp();
    };
  });
  const btnStarMinus = document.getElementById('btn-star-minus');
  if (btnStarMinus && state.detailAnime) {
    btnStarMinus.onclick = async () => {
      const cur = rating10ToStars(state.detailAnime.rating10) || 0;
      state.detailAnime.rating10 = Math.round(Math.max(0.5, cur - 0.5) * 2);
      await saveVaultEntry(state.detailAnime);
      renderApp();
    };
  }
  const btnStarPlus = document.getElementById('btn-star-plus');
  if (btnStarPlus && state.detailAnime) {
    btnStarPlus.onclick = async () => {
      const cur = rating10ToStars(state.detailAnime.rating10) || 0;
      state.detailAnime.rating10 = Math.round(Math.min(5.0, cur + 0.5) * 2);
      await saveVaultEntry(state.detailAnime);
      renderApp();
    };
  }
  const btnSaveNote = document.getElementById('btn-detail-save-note');
  if (btnSaveNote && state.detailAnime) {
    btnSaveNote.onclick = async () => {
      const notes = document.getElementById('inp-detail-notes')?.value || '';
      state.detailAnime.reviewNotes = notes.trim();
      await saveVaultEntry(state.detailAnime);
      renderApp();
    };
  }

  // Modal bindings
  const backdrop = document.getElementById('modal-backdrop');
  const btnClose = document.getElementById('btn-close-modal');
  const closeModals = () => {
    state.quickEditAnime = null;
    state.showAvatarModal = false;
    state.showProfileEditModal = false;
    state.showCreateCollectionModal = false;
    renderApp();
  };
  if (backdrop) backdrop.onclick = closeModals;
  if (btnClose) btnClose.onclick = closeModals;

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
      const key = el.getAttribute('data-pick-avatar');
      state.profile.avatarKey = key;
      saveProfileCache();
      renderApp();
      if (state.session) {
        const uid = encodeURIComponent(state.session.userId);
        await supabaseRequest(`/rest/v1/profiles?id=eq.${uid}`, 'PATCH', { avatar_key: key, avatar_url: key }).catch(() => {});
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
      qeInput.oninput = (e) => {
        state.quickEditAnime.watchedEpisodes = Math.max(0, parseInt(e.target.value || '0', 10));
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
        const notes = document.getElementById('qe-notes')?.value || '';
        state.quickEditAnime.reviewNotes = notes.trim();
        const toSave = { ...state.quickEditAnime };
        state.quickEditAnime = null;
        await saveVaultEntry(toSave);
        renderApp();
      };
    }
  }
}

// Initial Boot & Automatic Background Sync Across All Platforms
renderApp();
fetchDiscoverCatalog();
if (state.session) {
  loadCloudData(false);
}

// Automatic background sync every 15 seconds + on tab focus/visibility
setInterval(() => {
  if (state.session && !document.hidden) {
    loadCloudData(true);
  }
}, 15000);

window.addEventListener('focus', () => {
  if (state.session) loadCloudData(true);
});

document.addEventListener('visibilitychange', () => {
  if (!document.hidden && state.session) loadCloudData(true);
});
