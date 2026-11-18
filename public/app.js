// ============================================================================
// ANIME UNIVERSE — 1:1 ANDROID COMPOSE UI & SUPABASE/ANILIST PARITY (WEB & PWA)
// Matches AuthScreen, HomeScreen, DiscoverScreen, MyListScreen, CommunityScreen,
// ProfileScreen, SettingsScreen, AnimeDetailScreen & KinetoscopeComponents
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

// 9 Royal Built-In Avatars (Uses real bundled PNG portraits /assets/avatar_1.png .. /assets/avatar_9.png)
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

function sentimentFromRating10(rating10) {
  if (!rating10 || rating10 <= 0) return null;
  const s100 = Math.round(rating10 * 10);
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
  grid: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 3v8h8V3H3zm6 6H5V5h4v4zm-6 4v8h8v-8H3zm6 6H5v-4h4v4zm4-16v8h8V3h-8zm6 6h-4V5h4v4zm-6 4v8h8v-8h-8zm6 6h-4v-4h4v4z"/></svg>`,
  list: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z"/></svg>`,
  tune: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z"/></svg>`,
  folderAdd: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 6h-8l-2-2H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-1 8h-3v3h-2v-3h-3v-2h3V9h2v3h3v2z"/></svg>`,
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
  return String(d.getDate()).padStart(2, '0');
}

// Persistent State
const savedPrefs = JSON.parse(localStorage.getItem('anime_universe_prefs') || '{}');
const savedLocalVault = JSON.parse(localStorage.getItem('anime_universe_local_vault') || '[]');
const savedCollections = JSON.parse(localStorage.getItem('anime_universe_collections') || '[]');

const state = {
  session: JSON.parse(localStorage.getItem('anime_universe_session') || localStorage.getItem('auralog_session') || 'null'),
  isGuestMode: localStorage.getItem('anime_universe_guest') === 'true',
  tab: 'home', // 'home' | 'discover' | 'mylist' | 'community' | 'profile' | 'settings' | 'detail'
  previousTab: 'home',
  settingsCategory: null, // null | 'ACCOUNT' | 'APPEARANCE' | 'PREFERENCES' | 'ABOUT'

  prefs: {
    themeMode: savedPrefs.themeMode || 'DARK', // 'DARK' | 'LIGHT' | 'SYSTEM'
    accentTheme: savedPrefs.accentTheme || 'GOLD', // 'GOLD' | 'ROSE' | 'EMERALD' | 'INDIGO'
    watchlistLayout: savedPrefs.watchlistLayout || 'GRID', // 'GRID' | 'LIST'
    titleLanguage: savedPrefs.titleLanguage || 'ENGLISH', // 'ENGLISH' | 'ROMAJI' | 'NATIVE'
    showAdultContent: Boolean(savedPrefs.showAdultContent),
    reviewsVisibility: savedPrefs.reviewsVisibility || 'community'
  },

  // Auth & 2-Step Onboarding State (Matches AuthScreen.kt)
  authMode: 'signin', // 'signin' | 'signup' | 'otp_signup' | 'otp_recovery' | 'onboard_username' | 'onboard_avatar'
  email: '',
  username: '',
  password: '',
  otpCode: '',
  newPassword: '',
  onboardUsername: '',
  onboardAvatarKey: 'avatar_1',
  authError: '',
  authInfo: '',
  loading: false,

  // Profile State
  profile: {
    username: 'Guest Tracker',
    avatarKey: 'avatar_1',
    bio: '',
    memberStatus: 'pending',
    isAdmin: false
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
  discoverMode: 'trending',
  searchQuery: '',
  discoverFormat: 'ALL',
  discoverStatus: 'ALL',
  discoverGenre: null,
  discoverSort: 'TRENDING_DESC',
  showDiscoverFilterSheet: false,
  isSearching: false,

  // Community State
  communityReviews: [],
  communitySentimentFilter: null,
  communitySort: 'RECENT',

  // Modals & Detail View
  detailAnime: null,
  detailRich: null,
  detailLoading: false,
  quickEditAnime: null,
  showQuickSearchModal: false,
  quickSearchQuery: '',
  toastMessage: null,

  storageStats: {
    totalBytes: 0,
    limitBytes: 524288000,
    usagePercent: 0
  }
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

function renderAvatarCircle(avatarKey, username = 'A', sizePx = 40) {
  const av = BUILT_IN_AVATARS.find((a) => a.key === avatarKey) || BUILT_IN_AVATARS[0];
  return `
    <div style="width:${sizePx}px;height:${sizePx}px;border-radius:50%;overflow:hidden;border:1.5px solid rgba(255,255,255,0.28);background:linear-gradient(135deg,${av.c1},${av.c2});flex-shrink:0;">
      <img src="${av.img}" alt="${escapeHtml(username)}" style="width:100%;height:100%;object-fit:cover;display:block;" />
    </div>
  `;
}

// Supabase Request Helper
async function supabaseRequest(path, method = 'GET', body = undefined, prefer = '') {
  const accessToken = state.session?.accessToken || '';
  try {
    const edgeResp = await fetch('/api/supabase', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ path, method, accessToken, prefer, body })
    });
    if (edgeResp.status !== 404 && edgeResp.status !== 405) {
      const text = await edgeResp.text();
      let data = {};
      try { data = JSON.parse(text); } catch (_) {}
      if (!edgeResp.ok) {
        throw new Error(data.msg || data.error_description || data.message || data.error || 'Request failed');
      }
      return data;
    }
  } catch (err) {
    if (err.message && !err.message.includes('Failed to fetch') && !err.message.includes('Unexpected token')) {
      throw err;
    }
  }

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
  const text = await resp.text();
  let data = {};
  try { data = JSON.parse(text); } catch (_) {}
  if (!resp.ok) {
    throw new Error(data.msg || data.error_description || data.message || 'Cloud request failed');
  }
  return data;
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

async function loadCloudData() {
  if (!state.session) return;
  try {
    const uid = encodeURIComponent(state.session.userId);
    const [normRows, legacyRows, prof, cmRows] = await Promise.all([
      supabaseRequest(
        `/rest/v1/user_anime?user_id=eq.${uid}&select=id,user_id,anime_id,status,episode_progress,user_season,total_episodes,personal_rating,review_notes,is_favorite,updated_at,anime(*)&order=updated_at.desc`
      ).catch(() => []),
      supabaseRequest(`/rest/v1/user_anime_entries?user_id=eq.${uid}&order=updated_at.desc`).catch(() => []),
      supabaseRequest(`/rest/v1/profiles?id=eq.${uid}&select=*`).catch(() => []),
      supabaseRequest(`/rest/v1/community_members?user_id=eq.${uid}&select=status`).catch(() => [])
    ]);

    if (Array.isArray(normRows) && normRows.length > 0) {
      state.vault = normRows.map((r) => {
        const anime = r.anime || {};
        const existingLocal = state.vault.find((v) => v.animeId === r.anime_id) || {};
        const eng = anime.title_english || existingLocal.titleEnglish || '';
        const rom = anime.title_romaji || existingLocal.titleRomaji || '';
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
          countryOfOrigin: existingLocal.countryOfOrigin || 'JP',
          airingDayOfWeek: existingLocal.airingDayOfWeek || ((r.anime_id % 7) + 1),
          airingTimeUnix: existingLocal.airingTimeUnix || null,
          nextEpisodeNumber: existingLocal.nextEpisodeNumber || null,
          status: r.status || 'watching',
          season: r.user_season || existingLocal.season || 1,
          watchedEpisodes: r.episode_progress || 0,
          totalEpisodes: r.total_episodes || anime.episodes || existingLocal.totalEpisodes || null,
          rating10: r.personal_rating || null,
          reviewNotes: r.review_notes || '',
          isPinned: Boolean(existingLocal.isPinned),
          isFavorite: Boolean(r.is_favorite ?? existingLocal.isFavorite),
          genres: Array.isArray(anime.genres) ? anime.genres : existingLocal.genres || [],
          synopsis: anime.description || existingLocal.synopsis || ''
        };
      });
      saveLocalVault();
    } else if (Array.isArray(legacyRows) && legacyRows.length > 0) {
      state.vault = legacyRows.map((r) => {
        const existingLocal = state.vault.find((v) => v.animeId === r.anime_id) || {};
        return {
          animeId: r.anime_id,
          title: r.anime_title || existingLocal.title || 'Anime',
          titleEnglish: existingLocal.titleEnglish || r.anime_title || 'Anime',
          titleRomaji: existingLocal.titleRomaji || r.anime_title || 'Anime',
          titleNative: existingLocal.titleNative || '',
          coverImage: r.anime_cover_image || existingLocal.coverImage || '',
          bannerImage: existingLocal.bannerImage || '',
          format: existingLocal.format || 'TV',
          mediaStatus: existingLocal.mediaStatus || 'RELEASING',
          countryOfOrigin: existingLocal.countryOfOrigin || 'JP',
          airingDayOfWeek: existingLocal.airingDayOfWeek || ((r.anime_id % 7) + 1),
          airingTimeUnix: existingLocal.airingTimeUnix || null,
          nextEpisodeNumber: existingLocal.nextEpisodeNumber || null,
          status: r.watch_status || 'watching',
          season: existingLocal.season || 1,
          watchedEpisodes: r.watched_episodes || 0,
          totalEpisodes: r.total_episodes || existingLocal.totalEpisodes || null,
          rating10: r.rating || null,
          reviewNotes: r.review_notes || '',
          isPinned: Boolean(existingLocal.isPinned),
          isFavorite: Boolean(existingLocal.isFavorite),
          genres: existingLocal.genres || []
        };
      });
      saveLocalVault();
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
    } else {
      state.profile.username = state.session.username || 'Member';
      state.profile.memberStatus = cmStatus || 'active';
    }

    await loadCommunityReviews();
    renderApp();
  } catch (_) {}
}

async function loadCommunityReviews() {
  try {
    const normRows = await supabaseRequest(
      '/rest/v1/user_anime?select=id,user_id,anime_id,status,episode_progress,user_season,total_episodes,personal_rating,review_notes,updated_at,anime(anilist_id,title_english,title_romaji,cover_image)&personal_rating=not.is.null&review_notes=neq.&order=updated_at.desc&limit=60'
    ).catch(() => []);

    if (Array.isArray(normRows) && normRows.length > 0) {
      const valid = normRows.filter((r) => (r.personal_rating || 0) > 0 && String(r.review_notes || '').trim().length > 0);
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
        const prof = profMap[r.user_id] || { username: 'Member', avatar_key: 'avatar_1' };
        return {
          id: r.id,
          user_id: r.user_id,
          username: prof.username,
          avatar_key: prof.avatar_key,
          anime_id: r.anime_id,
          anime_title: anime.title_english || anime.title_romaji || 'Anime',
          anime_cover_image: anime.cover_image || '',
          rating: r.personal_rating,
          review_notes: r.review_notes,
          updated_at: r.updated_at,
          likes_count: likesCountMap[r.id] || 0,
          liked_by_me: likedByMeSet.has(r.id)
        };
      });
      renderApp();
    }
  } catch (_) {}
}

async function fetchDiscoverCatalog() {
  state.isSearching = true;
  renderApp();

  const variables = {
    page: 1,
    perPage: 30,
    isAdult: Boolean(state.prefs.showAdultContent),
    sort: [state.discoverSort || 'TRENDING_DESC']
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
        characters(sort: [ROLE, RELEVANCE], perPage: 10) {
          nodes { id name { full } image { large } }
        }
        relations {
          edges {
            relationType
            node { id type title { english romaji } coverImage { large } format status episodes seasonYear }
          }
        }
        recommendations(perPage: 8, sort: [RATING_DESC]) {
          nodes {
            mediaRecommendation { id title { english romaji } coverImage { large } format averageScore }
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
      const seasons = seasonNodes.map((sn, idx) => ({
        id: sn.id,
        seasonNumber: idx + 1,
        seasonYear: sn.seasonYear,
        status: sn.status,
        isCurrent: sn.id === m.id
      }));

      state.detailRich = {
        studios: (m.studios?.nodes || []).map((s) => s.name),
        seasons,
        characters: (m.characters?.nodes || []).map((c) => ({
          id: c.id,
          name: c.name?.full || 'Character',
          image: c.image?.large || ''
        })),
        relations: relEdges
          .filter((e) => e.relationType !== 'PREQUEL' && e.relationType !== 'SEQUEL')
          .map((e) => ({
            id: e.node.id,
            relationType: e.relationType || 'RELATED',
            title: e.node.title?.english || e.node.title?.romaji || 'Anime',
            coverImage: e.node.coverImage?.large || '',
            format: e.node.format || 'TV'
          })),
        recommendations: (m.recommendations?.nodes || [])
          .map((n) => n.mediaRecommendation)
          .filter(Boolean)
          .map((r) => ({
            id: r.id,
            title: r.title?.english || r.title?.romaji || 'Anime',
            coverImage: r.coverImage?.large || ''
          }))
      };
    }
  } catch (_) {
  } finally {
    state.detailLoading = false;
    renderApp();
  }
}

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
          title_english: String(cleanItem.titleEnglish || cleanItem.title || 'Anime').slice(0, 200),
          title_romaji: String(cleanItem.titleRomaji || cleanItem.title || 'Anime').slice(0, 200),
          title_native: String(cleanItem.titleNative || '').slice(0, 200),
          cover_image: cleanItem.coverImage || '',
          banner_image: cleanItem.bannerImage || '',
          episodes: cleanItem.totalEpisodes || null,
          status: cleanItem.mediaStatus || 'RELEASING',
          format: cleanItem.format || 'TV',
          genres: Array.isArray(cleanItem.genres) ? cleanItem.genres.slice(0, 8) : [],
          description: String(cleanItem.synopsis || '').slice(0, 360)
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
        user_season: Math.max(1, Number(cleanItem.season || 1)),
        total_episodes: cleanItem.totalEpisodes || null,
        personal_rating: cleanItem.rating10 ? Math.min(10, Math.max(1, Math.round(cleanItem.rating10))) : null,
        review_notes: String(cleanItem.reviewNotes || '').trim().slice(0, 500),
        is_favorite: Boolean(cleanItem.isFavorite)
      },
      'resolution=merge-duplicates,return=minimal'
    );
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
  const userStars = effective.rating10 ? (effective.rating10 / 2).toFixed(1) : null;
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
// SCREEN 1: HOME SCREEN (Matches HomeScreen.kt)
// ============================================================================

function renderHomeScreen() {
  const todayDow = ((new Date().getDay() + 6) % 7) + 1;
  const activeShows = state.vault.filter((v) => v.status !== 'dropped');
  const dayBuckets = {};
  for (let d = 1; d <= 7; d++) dayBuckets[d] = [];
  const anytimeShows = [];

  activeShows.forEach((v) => {
    const isFinished = String(v.mediaStatus || '').toUpperCase() === 'FINISHED' || v.status === 'completed';
    if (!isFinished && v.airingDayOfWeek >= 1 && v.airingDayOfWeek <= 7) {
      dayBuckets[v.airingDayOfWeek].push(v);
    } else {
      anytimeShows.push(v);
    }
  });

  const topBar = renderObsidianTopBar({
    title: 'ANIME ',
    accentTitleTail: 'UNIVERSE',
    subtitle: 'Weekly Schedule',
    showBrandMonogram: true,
    actionsHtml: `<button class="app-icon-btn" id="btn-open-quick-search" title="Quick Search">${SVG_ICONS.search}</button>`
  });

  const weekStripHtml = `
    <div class="section-header-row">
      <h2 class="serif-heading" style="font-size:20px;">This Week</h2>
      <button class="capsule-pill" data-nav="mylist" style="color:var(--accent-primary);background:transparent;border:none;padding:4px 8px;">
        LIBRARY ›
      </button>
    </div>
    <div class="week-strip">
      <button class="week-day-pill ${state.homeAllWeekDays ? 'selected' : ''}" id="btn-home-all-days">
        <span class="week-day-top">ALL</span>
        <span class="week-day-mid">Week</span>
        <span class="week-day-count">${activeShows.length}</span>
      </button>
      ${DAY_LABELS.map((d) => {
        const count = (dayBuckets[d.dow] || []).length;
        const isSelected = !state.homeAllWeekDays && state.selectedDay === d.dow;
        const isToday = d.dow === todayDow;
        return `
          <button class="week-day-pill ${isSelected ? 'selected' : ''} ${isToday ? 'today' : ''}" data-select-day="${d.dow}">
            <span class="week-day-top">${d.short}</span>
            <span class="week-day-mid">${getDayNumberForDow(d.dow)}</span>
            ${count > 0 ? `<span class="week-day-count">${count}</span>` : `<span style="font-size:10px;color:var(--text-muted);">•</span>`}
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
          <section style="margin-bottom:24px;">
            <div class="section-header-row" style="padding:4px 2px;">
              <div style="display:flex;align-items:center;gap:8px;">
                ${isToday ? `<span style="width:7px;height:7px;border-radius:50%;background:var(--accent-primary);"></span>` : ''}
                <h3 class="serif-heading" style="font-size:22px;color:${isToday ? 'var(--highlight)' : 'var(--text-primary)'};">
                  ${isToday ? `Today • ${dInfo.full}` : dInfo.full}
                </h3>
              </div>
              <span class="micro-label" style="color:var(--accent-primary);">${list.length} SHOW${list.length === 1 ? '' : 'S'}</span>
            </div>
            <div class="poster-grid">${list.map((it) => renderPosterTrackerCard(it, true)).join('')}</div>
          </section>
        `;
      })
      .join('');

    const anytimeSection =
      anytimeShows.length > 0
        ? `
          <section style="margin-bottom:24px;">
            <div class="section-header-row" style="padding:4px 2px;">
              <h3 class="serif-heading" style="font-size:22px;">Anytime &amp; Completed</h3>
              <span class="micro-label" style="color:var(--accent-primary);">${anytimeShows.length} SHOW${anytimeShows.length === 1 ? '' : 'S'}</span>
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
          <h3 class="serif-heading" style="font-size:22px;">${dInfo.full} Releases</h3>
          <span class="micro-label" style="color:var(--accent-primary);">${list.length} SHOW${list.length === 1 ? '' : 'S'}</span>
        </div>
        ${
          list.length === 0
            ? `<div class="app-card" style="text-align:center;padding:36px;"><p style="color:var(--text-secondary);">No airing shows scheduled for ${dInfo.full}.</p></div>`
            : `<div class="poster-grid">${list.map((it) => renderPosterTrackerCard(it, true)).join('')}</div>`
        }
      </section>
    `;
  }

  return `${topBar}${weekStripHtml}${bodyHtml}`;
}

// ============================================================================
// SCREEN 2: DISCOVER / EXPLORE SCREEN (Matches DiscoverScreen.kt)
// ============================================================================

function renderDiscoverScreen() {
  const modes = [
    { id: 'trending', label: 'TRENDING', sort: 'TRENDING_DESC' },
    { id: 'popular', label: 'POPULAR', sort: 'POPULARITY_DESC' },
    { id: 'highest_rated', label: 'TOP RATED', sort: 'SCORE_DESC' },
    { id: 'seasonal', label: 'SEASONAL', sort: 'POPULARITY_DESC' },
    { id: 'donghua', label: 'DONGHUA', sort: 'POPULARITY_DESC' },
    { id: 'airing', label: 'AIRING', sort: 'TRENDING_DESC' }
  ];

  const topBar = renderObsidianTopBar({
    title: 'Discover',
    subtitle: 'The Global Catalog',
    actionsHtml: `
      <button class="app-icon-btn" id="btn-toggle-layout" title="Toggle Grid/List">
        ${state.prefs.watchlistLayout === 'GRID' ? SVG_ICONS.list : SVG_ICONS.grid}
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
      <button class="capsule-pill ${state.prefs.showAdultContent ? 'active' : ''}" id="btn-toggle-safe">
        ${state.prefs.showAdultContent ? '18+ SHOWN' : 'SAFE ON'}
      </button>
    </div>

    <div class="capsule-strip">
      <button class="capsule-pill ${!state.discoverGenre ? 'active' : ''}" data-disc-genre="">All Genres</button>
      ${ANILIST_GENRES.map(
        (g) => `<button class="capsule-pill ${state.discoverGenre === g ? 'active' : ''}" data-disc-genre="${g}">${g}</button>`
      ).join('')}
    </div>

    ${
      state.isSearching
        ? `<div class="app-card" style="text-align:center;padding:48px;"><p style="color:var(--accent-primary);">Loading Anime Universe catalog...</p></div>`
        : `<div class="poster-grid">${state.discover.map((it) => renderPosterTrackerCard(it, false)).join('')}</div>`
    }
  `;
}

// ============================================================================
// SCREEN 3: LIBRARY / MY LIST SCREEN (Matches MyListScreen.kt)
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
    subtitle: `${state.vault.length} Tracked • ${state.collections.length} Collections`,
    actionsHtml: `
      <button class="app-icon-btn" id="btn-create-collection" title="Create Collection">${SVG_ICONS.folderAdd}</button>
    `
  });

  return `
    ${topBar}
    <!-- Status Capsule Tabs -->
    <div class="capsule-strip">
      <button class="capsule-pill gold-solid ${state.myListStatusFilter === 'ALL' ? 'active' : ''}" data-mylist-status="ALL">
        ALL (${state.vault.length})
      </button>
      ${WATCH_STATUSES.map((st) => {
        const count = state.vault.filter((v) => v.status === st.dbValue).length;
        return `
          <button class="capsule-pill gold-solid ${state.myListStatusFilter === st.dbValue ? 'active' : ''}" data-mylist-status="${st.dbValue}">
            ${st.label.toUpperCase()} (${count})
          </button>
        `;
      }).join('')}
    </div>

    <!-- Sentiment Filter Strip -->
    <div class="capsule-strip">
      <button class="capsule-pill ${!state.myListSentimentFilter ? 'active' : ''}" data-mylist-sentiment="">ALL RATINGS</button>
      ${SENTIMENTS.map((s) => {
        const count = state.vault.filter((v) => sentimentFromRating10(v.rating10)?.key === s.key).length;
        return `
          <button class="capsule-pill ${state.myListSentimentFilter === s.key ? 'active' : ''}" data-mylist-sentiment="${s.key}">
            ${s.shortLabel.toUpperCase()} (${count})
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

    ${
      filtered.length === 0
        ? `<div class="app-card" style="text-align:center;padding:42px;"><p style="color:var(--text-secondary);">No matching anime entries in your library.</p></div>`
        : `<div class="poster-grid">${filtered.map((it) => renderPosterTrackerCard(it, true)).join('')}</div>`
    }
  `;
}

// ============================================================================
// SCREEN 4: COMMUNITY SCREEN (Matches CommunityScreen.kt)
// ============================================================================

function renderCommunityScreen() {
  const topBar = renderObsidianTopBar({
    title: 'Community',
    subtitle: `Rated & Noted • ${state.communityReviews.length} Reviews`
  });

  if (!state.session) {
    return `
      ${topBar}
      <div class="app-card" style="text-align:center;padding:42px 24px;max-width:480px;margin:28px auto;">
        <h2 class="serif-heading" style="font-size:28px;margin-bottom:8px;">Sign In for Community Lounge</h2>
        <p style="color:var(--text-secondary);font-size:13.5px;margin-bottom:20px;">
          Sign in to browse rated &amp; noted anime showcases shared by members and share your own taste.
        </p>
        <button class="btn-primary" id="btn-exit-guest" style="width:100%;">Sign In / Create Account</button>
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
      if (state.communitySort === 'MOST_LIKED') return (b.likes_count || 0) - (a.likes_count || 0);
      if (state.communitySort === 'HIGHEST') return (b.rating || 0) - (a.rating || 0);
      return String(b.updated_at || '').localeCompare(String(a.updated_at || ''));
    });

  return `
    ${topBar}
    <div class="capsule-strip">
      <button class="capsule-pill gold-solid ${!state.communitySentimentFilter ? 'active' : ''}" data-comm-sentiment="">
        ALL (${state.communityReviews.length})
      </button>
      ${SENTIMENTS.map(
        (s) =>
          `<button class="capsule-pill gold-solid ${state.communitySentimentFilter === s.key ? 'active' : ''}" data-comm-sentiment="${s.key}">
            ${s.shortLabel.toUpperCase()}
          </button>`
      ).join('')}
    </div>

    <div class="capsule-strip">
      ${[
        { k: 'RECENT', label: 'Newest' },
        { k: 'HIGHEST', label: 'Highest Rated' },
        { k: 'MOST_LIKED', label: 'Most Liked ♥' }
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
        : `<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:14px;">
            ${filtered
              .map((rev) => {
                const stars = ((rev.rating || 0) / 2).toFixed(1);
                const sentiment = sentimentFromRating10(rev.rating);
                return `
                  <div class="app-card clickable" data-open-detail="${rev.anime_id}" style="display:flex;flex-direction:column;gap:12px;">
                    <div style="display:flex;gap:14px;">
                      <img src="${escapeHtml(rev.anime_cover_image)}" alt="" style="width:64px;height:94px;object-fit:cover;border-radius:14px;flex-shrink:0;" />
                      <div style="flex:1;min-width:0;">
                        <div class="serif-heading" style="font-size:21px;line-height:24px;">${escapeHtml(rev.anime_title)}</div>
                        <div style="display:flex;align-items:center;gap:8px;margin-top:6px;">
                          <span class="rating-badge"><span style="color:var(--accent-primary);">★</span> ${stars}</span>
                          ${sentiment ? `<span class="sentiment-pill soft ${sentiment.key}">${sentiment.fullLabel}</span>` : ''}
                        </div>
                      </div>
                    </div>
                    <p style="font-style:italic;color:var(--text-primary);font-size:13.5px;line-height:1.55;border-left:2px solid var(--accent-primary);padding-left:12px;">
                      “${escapeHtml(rev.review_notes)}”
                    </p>
                    <div style="display:flex;align-items:center;justify-content:space-between;padding-top:4px;">
                      <div style="display:flex;align-items:center;gap:8px;">
                        ${renderAvatarCircle(rev.avatar_key || 'avatar_1', rev.username, 28)}
                        <span style="font-size:12.5px;font-weight:600;">${escapeHtml(rev.username || 'Member')}</span>
                      </div>
                      <span class="micro-label" style="color:var(--text-muted);">${escapeHtml(String(rev.updated_at || '').slice(0, 10))}</span>
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
// SCREEN 5: PROFILE SCREEN (Matches ProfileScreen.kt 1:1)
// ============================================================================

function renderProfileScreen() {
  const ratedItems = state.vault.filter((v) => (v.rating10 || 0) > 0);
  const watchedCount = state.vault.filter((v) => (v.watchedEpisodes || 0) >= 1).length;
  const meanScore =
    ratedItems.length > 0
      ? `★ ${(ratedItems.reduce((acc, v) => acc + v.rating10 / 2, 0) / ratedItems.length).toFixed(1)}`
      : '—';
  const favorites = state.vault.filter((v) => v.isFavorite);
  const avatarImg = getAvatarImgUrl(state.profile.avatarKey);

  return `
    <div style="max-width:760px;margin:0 auto;">
      <!-- Header with Back & Settings icon buttons -->
      <div style="display:flex;align-items:center;justify-content:space-between;padding:6px 0 14px;">
        <button class="app-icon-btn" data-nav="home" title="Back">${SVG_ICONS.back}</button>
        <h1 class="serif-heading" style="font-size:24px;">Profile</h1>
        <button class="app-icon-btn" data-nav="settings" title="Settings">${SVG_ICONS.settings}</button>
      </div>

      <!-- Centered Account Hero -->
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

      <!-- 4-Tile Editorial Stats Grid (RATED, WATCHED, SAVED, MEAN) -->
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

      <!-- Community Status Card -->
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
              ? 'Your rated & noted anime showcases sync with the Anime Universe community lounge.'
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
// SCREEN 6: SETTINGS SCREEN (Matches 4-Category Drill-Down SettingsScreen.kt)
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
                { id: 'ACCOUNT', title: 'Account', sub: state.session ? `Signed in as ${state.profile.username}` : 'Local Preview Mode • Sign in or sync' },
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
              <div>
                <div class="micro-label" style="color:var(--accent-primary);margin-bottom:8px;">DEFAULT COLLECTION LAYOUT</div>
                <div style="display:flex;gap:8px;">
                  <button class="capsule-pill gold-solid ${state.prefs.watchlistLayout === 'GRID' ? 'active' : ''}" data-set-layout="GRID">Poster Grid</button>
                  <button class="capsule-pill gold-solid ${state.prefs.watchlistLayout === 'LIST' ? 'active' : ''}" data-set-layout="LIST">Compact List</button>
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
              <div>
                <div class="micro-label" style="color:var(--accent-primary);margin-bottom:8px;">BACKUP &amp; RESTORE</div>
                <div style="display:flex;gap:10px;flex-wrap:wrap;">
                  <button class="btn-secondary" id="btn-export-json">Export JSON Backup</button>
                  <label class="btn-secondary" style="cursor:pointer;">
                    Import JSON Backup
                    <input type="file" id="inp-import-json" accept="application/json" style="display:none;" />
                  </label>
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
                    <div style="display:flex;gap:10px;flex-wrap:wrap;">
                      <button class="btn-secondary" id="btn-sync-now">Sync Cloud Vault</button>
                      <button class="btn-secondary" id="btn-signout">Sign Out</button>
                    </div>
                  `
                  : `
                    <div class="serif-heading" style="font-size:22px;">Local Preview Mode</div>
                    <p style="font-size:13px;color:var(--text-secondary);">Sign in to sync your tracked anime across Android, Desktop &amp; Web.</p>
                    <button class="btn-primary" id="btn-exit-guest">Sign In / Create Account</button>
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
// SCREEN 7: ANIME DETAIL SCREEN (Matches AnimeDetailScreen.kt 1:1)
// ============================================================================

function renderAnimeDetailScreen() {
  const a = state.detailAnime;
  if (!a) return '';
  const title = resolveTitle(a);
  const subJp = a.titleNative || (a.titleRomaji !== title ? a.titleRomaji : '');
  const total = a.totalEpisodes;
  const watched = a.watchedEpisodes || 0;
  const stars = a.rating10 ? a.rating10 / 2 : 0;
  const sentiment = sentimentFromRating10(a.rating10);
  const seasons = state.detailRich?.seasons || [];

  return `
    <div style="max-width:940px;margin:0 auto;">
      <!-- Hero Banner Backdrop -->
      <div style="position:relative;width:100%;height:300px;border-radius:26px;overflow:hidden;margin-bottom:16px;background:var(--card-bg);">
        <img src="${escapeHtml(a.bannerImage || a.coverImage)}" alt="" style="width:100%;height:100%;object-fit:cover;display:block;" />
        <div style="position:absolute;inset:0;background:linear-gradient(180deg,rgba(0,0,0,0.45) 0%,rgba(0,0,0,0.18) 32%,rgba(3,3,4,0.85) 72%,var(--bg-main) 100%);"></div>

        <!-- Top Circular Back & Favorite Buttons -->
        <div style="position:absolute;top:14px;left:14px;right:14px;display:flex;justify-content:space-between;">
          <button class="app-icon-btn" id="btn-detail-back" style="background:rgba(8,8,12,0.68);color:#FFF;">${SVG_ICONS.back}</button>
          <button class="app-icon-btn" id="btn-detail-fav" style="background:rgba(8,8,12,0.68);color:${a.isFavorite ? 'var(--error)' : '#FFF'};">
            ${a.isFavorite ? SVG_ICONS.heartFilled : SVG_ICONS.heartOutline}
          </button>
        </div>

        <!-- Inset Poster + Serif Title Overlay -->
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

      <!-- Editorial Big-Card 1: SEASONS & EPISODE PROGRESS -->
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
            ? `<button class="btn-primary" id="btn-detail-add" style="width:100%;">+ Add to My List</button>`
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
                  <button class="capsule-pill gold-solid ${a.status === st.dbValue ? 'active' : ''}" data-detail-status="${st.dbValue}">
                    ${st.label}
                  </button>`
                ).join('')}
                <button class="capsule-pill" id="btn-detail-remove" style="color:var(--error);border-color:rgba(229,90,107,0.4);">Remove</button>
              </div>
            `
        }
      </div>

      <!-- Editorial Big-Card 2: YOUR STAR RATING & WRITTEN NOTE -->
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
        <button class="btn-primary" id="btn-detail-save-note">Save Rating &amp; Note</button>
      </div>

      <!-- Editorial Big-Card 3: SYNOPSIS & GENRES -->
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
// AUTH SCREEN WITH MASCOT & 9 ROYAL PORTRAITS (Matches AuthScreen.kt 1:1)
// ============================================================================

function renderAuthScreen() {
  return `
    <div class="auth-viewport">
      <div class="auth-brand-header">
        <div class="topbar-title-brand" style="font-size:32px;">ANIME <em>UNIVERSE</em></div>
        <div class="topbar-subtitle" style="margin-top:4px;">TRACK ANIME • SHARE TASTE</div>
      </div>

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
                <input id="inp-onboard-user" class="auth-input" placeholder="Username (e.g. shadow_monarch)" value="${escapeHtml(state.onboardUsername)}" />
                <button class="btn-primary" id="btn-onboard-next">Continue to Royal Portrait ›</button>
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
                <button class="btn-primary" id="btn-onboard-finish">Enter Anime Universe</button>
              `
              : `
                <div class="auth-tab-switcher">
                  <button class="auth-tab-btn ${state.authMode !== 'signup' ? 'active' : ''}" data-auth-tab="signin">Sign In</button>
                  <button class="auth-tab-btn ${state.authMode === 'signup' ? 'active' : ''}" data-auth-tab="signup">Create Account</button>
                </div>

                ${state.authError ? `<div style="color:var(--error);font-size:12.5px;text-align:center;">${escapeHtml(state.authError)}</div>` : ''}
                ${state.authInfo ? `<div style="color:var(--accent-primary);font-size:12.5px;text-align:center;">${escapeHtml(state.authInfo)}</div>` : ''}

                <input id="inp-email" class="auth-input" type="email" placeholder="Email Address" value="${escapeHtml(state.email)}" />
                <input id="inp-pass" class="auth-input" type="password" placeholder="Password" value="${escapeHtml(state.password)}" />

                <button class="btn-primary" id="btn-auth-submit" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'Please wait...' : state.authMode === 'signup' ? 'Create Account' : 'Sign In'}
                </button>

                <button class="btn-secondary" id="btn-guest-preview" style="width:100%;">
                  Continue in Local Preview Mode
                </button>
              `
          }
        </div>
      </div>
    </div>
  `;
}

// ============================================================================
// MODALS: QUICK CONTEXT EDIT & AVATAR CAROUSEL MODAL
// ============================================================================

function renderModals() {
  if (state.quickEditAnime) {
    const a = state.quickEditAnime;
    const title = resolveTitle(a);
    const stars = a.rating10 ? a.rating10 / 2 : 0;
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
            <button class="btn-primary" id="qe-save" style="flex:1;">Save</button>
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
            <button class="btn-primary" id="btn-save-profile" style="flex:1;">Save</button>
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

  // Bottom dock has 4 tabs (Home, Explore, Library, Community) matching MainActivity.kt
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
          <div class="brand-monogram" style="width:34px;height:34px;font-size:18px;">A</div>
          <span class="serif-heading" style="font-size:17px;font-weight:600;">ANIME UNIVERSE</span>
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
  document.querySelectorAll('[data-auth-tab]').forEach((btn) => {
    btn.onclick = () => {
      state.authMode = btn.getAttribute('data-auth-tab');
      state.authError = '';
      renderApp();
    };
  });

  const inpEmail = document.getElementById('inp-email');
  const inpPass = document.getElementById('inp-pass');
  if (inpEmail) inpEmail.oninput = (e) => { state.email = e.target.value; };
  if (inpPass) inpPass.oninput = (e) => { state.password = e.target.value; };

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
      state.authError = '';
      state.loading = true;
      renderApp();
      try {
        if (state.authMode === 'signup') {
          const data = await supabaseRequest('/auth/v1/signup', 'POST', {
            email: state.email.trim(),
            password: state.password
          });
          if (data.access_token) {
            saveSession({
              accessToken: data.access_token,
              refreshToken: data.refresh_token || '',
              userId: data.user?.id,
              email: state.email.trim(),
              username: state.email.split('@')[0]
            });
            state.authMode = 'onboard_username';
            state.onboardUsername = state.email.split('@')[0];
          } else {
            state.authInfo = 'Check your email to confirm your account, then sign in.';
            state.authMode = 'signin';
          }
        } else {
          const data = await supabaseRequest('/auth/v1/token?grant_type=password', 'POST', {
            email: state.email.trim(),
            password: state.password
          });
          saveSession({
            accessToken: data.access_token,
            refreshToken: data.refresh_token || '',
            userId: data.user?.id,
            email: data.user?.email || state.email.trim(),
            username: data.user?.user_metadata?.username || state.email.split('@')[0]
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
      state.discoverSort = btn.getAttribute('data-disc-sort') || 'TRENDING_DESC';
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
  const inpMyList = document.getElementById('inp-mylist-search');
  if (inpMyList) {
    inpMyList.oninput = (e) => {
      state.myListQuery = e.target.value;
      renderApp();
    };
  }

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
  document.querySelectorAll('[data-set-layout]').forEach((btn) => {
    btn.onclick = () => {
      state.prefs.watchlistLayout = btn.getAttribute('data-set-layout');
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
      const currentStars = state.detailAnime.rating10 ? state.detailAnime.rating10 / 2 : 0;
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
      const cur = state.detailAnime.rating10 ? state.detailAnime.rating10 / 2 : 0;
      state.detailAnime.rating10 = Math.round(Math.max(0.5, cur - 0.5) * 2);
      await saveVaultEntry(state.detailAnime);
      renderApp();
    };
  }
  const btnStarPlus = document.getElementById('btn-star-plus');
  if (btnStarPlus && state.detailAnime) {
    btnStarPlus.onclick = async () => {
      const cur = state.detailAnime.rating10 ? state.detailAnime.rating10 / 2 : 0;
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
    renderApp();
  };
  if (backdrop) backdrop.onclick = closeModals;
  if (btnClose) btnClose.onclick = closeModals;

  document.querySelectorAll('[data-pick-avatar]').forEach((el) => {
    el.onclick = async () => {
      const key = el.getAttribute('data-pick-avatar');
      state.profile.avatarKey = key;
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
      if (u) state.profile.username = u;
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
        const cur = state.quickEditAnime.rating10 ? state.quickEditAnime.rating10 / 2 : 0;
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

// Initial Boot
renderApp();
fetchDiscoverCatalog();
if (state.session) {
  loadCloudData();
}
