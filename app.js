// ============================================================================
// ANIME UNIVERSE — UNIFIED CROSS-PLATFORM APPLICATION (DESKTOP, MACOS, WEB, MOBILE)
// Exact Feature Parity with Android Mobile Application + Premium Desktop UX
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

// Built-in Avatars (Exact match with BuiltInAvatar enum in AnimeUniverseModels.kt)
const BUILT_IN_AVATARS = [
  { key: 'avatar_1', name: 'Crimson Wolf', c1: '#9F1239', c2: '#334155', icon: '🐺' },
  { key: 'avatar_2', name: 'Azure Sovereign', c1: '#0284C7', c2: '#6366F1', icon: '👑' },
  { key: 'avatar_3', name: 'Amethyst Dragon', c1: '#A855F7', c2: '#E11D48', icon: '🐉' },
  { key: 'avatar_4', name: 'Frost Empress', c1: '#38BDF8', c2: '#818CF8', icon: '❄️' },
  { key: 'avatar_5', name: 'Emerald Flora', c1: '#10B981', c2: '#38BDF8', icon: '🌿' },
  { key: 'avatar_6', name: 'Amber Scholar', c1: '#F59E0B', c2: '#92400E', icon: '📜' },
  { key: 'avatar_7', name: 'Autumn Vanguard', c1: '#EA580C', c2: '#06B6D4', icon: '⚔️' },
  { key: 'avatar_8', name: 'Indigo Flame', c1: '#4F46E5', c2: '#0EA5E9', icon: '🔥' },
  { key: 'avatar_9', name: 'Solar Halo', c1: '#FBBF24', c2: '#64748B', icon: '☀️' }
];

// Sentiment Categories (Exact match with SentimentCategory in AnimeUniverseModels.kt)
const SENTIMENTS = [
  { key: 'cinema', shortLabel: 'Cinema', fullLabel: 'Absolute Cinema', minScore100: 90, css: 'sentiment-cinema' },
  { key: 'worth', shortLabel: 'Worth', fullLabel: 'Worth Watching', minScore100: 75, css: 'sentiment-worth' },
  { key: 'avg', shortLabel: 'Avg', fullLabel: 'Average', minScore100: 55, css: 'sentiment-avg' },
  { key: 'boring', shortLabel: 'Boring', fullLabel: 'Boring', minScore100: 35, css: 'sentiment-boring' },
  { key: 'waste', shortLabel: 'Waste', fullLabel: 'Waste of Time', minScore100: 1, css: 'sentiment-waste' }
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
  { dow: 1, short: 'Mon', full: 'Monday' },
  { dow: 2, short: 'Tue', full: 'Tuesday' },
  { dow: 3, short: 'Wed', full: 'Wednesday' },
  { dow: 4, short: 'Thu', full: 'Thursday' },
  { dow: 5, short: 'Fri', full: 'Friday' },
  { dow: 6, short: 'Sat', full: 'Saturday' },
  { dow: 7, short: 'Sun', full: 'Sunday' }
];

function getCurrentAnimeSeason() {
  const m = new Date().getMonth() + 1;
  if (m <= 3) return 'WINTER';
  if (m <= 6) return 'SPRING';
  if (m <= 9) return 'SUMMER';
  return 'FALL';
}

// Persistent State
const savedPrefs = JSON.parse(localStorage.getItem('anime_universe_prefs') || '{}');
const savedLocalVault = JSON.parse(localStorage.getItem('anime_universe_local_vault') || '[]');
const savedCollections = JSON.parse(localStorage.getItem('anime_universe_collections') || '[]');

const state = {
  session: JSON.parse(localStorage.getItem('anime_universe_session') || localStorage.getItem('auralog_session') || 'null'),
  isGuestMode: localStorage.getItem('anime_universe_guest') === 'true',
  tab: 'home', // 'home' | 'discover' | 'mylist' | 'community' | 'profile' | 'settings'
  settingsCategory: null, // null | 'ACCOUNT' | 'APPEARANCE' | 'PREFERENCES' | 'ABOUT'

  // User Preferences (Matches PreferencesRepository.kt)
  prefs: {
    themeMode: savedPrefs.themeMode || 'DARK', // 'DARK' | 'LIGHT' | 'SYSTEM'
    watchlistLayout: savedPrefs.watchlistLayout || 'GRID', // 'GRID' | 'LIST'
    titleLanguage: savedPrefs.titleLanguage || 'ENGLISH', // 'ENGLISH' | 'ROMAJI' | 'NATIVE'
    showAdultContent: Boolean(savedPrefs.showAdultContent),
    reviewsVisibility: savedPrefs.reviewsVisibility || 'community'
  },

  // Auth State
  authMode: 'signin', // 'signin' | 'signup' | 'otp_signup' | 'otp_recovery'
  email: '',
  username: '',
  password: '',
  otpCode: '',
  newPassword: '',
  authError: '',
  authInfo: '',
  loading: false,

  // Profile State
  profile: {
    username: 'Member',
    avatarKey: 'avatar_1',
    bio: '',
    memberStatus: 'pending'
  },
  showAvatarModal: false,
  showProfileEditModal: false,

  // Home Schedule State
  homeAllWeekDays: true,
  selectedDay: ((new Date().getDay() + 6) % 7) + 1, // 1=Mon..7=Sun

  // Library (MyList) State
  vault: savedLocalVault,
  collections: savedCollections,
  myListStatusFilter: 'ALL',
  myListSentimentFilter: null,
  myListCollectionFilter: null,
  myListQuery: '',
  myListSort: 'RECENT', // 'RECENT' | 'TITLE' | 'RATING' | 'PROGRESS'
  showCreateCollectionModal: false,

  // Discover State
  discover: [],
  discoverMode: 'trending', // 'trending' | 'popular' | 'highest_rated' | 'seasonal' | 'donghua' | 'airing'
  searchQuery: '',
  discoverFormat: 'ALL',
  discoverStatus: 'ALL',
  discoverGenre: null,
  discoverSort: 'TRENDING_DESC',
  isSearching: false,

  // Community State
  communityReviews: [],
  communitySentimentFilter: null,
  communitySort: 'RECENT', // 'RECENT' | 'MOST_LIKED' | 'HIGHEST' | 'LOWEST'

  // 500 MB Storage Quota Monitor State
  storageStats: {
    totalBytes: 0,
    limitBytes: 524288000,
    usagePercent: 0,
    warningLevel: 'normal',
    bytesPerUser: 2200,
    bytesPerReview: 180,
    estimatedRemainingUsers: 230000,
    tables: []
  },
  storageStatsLoading: false,

  // Modals & Detail View
  detailAnime: null,
  detailRich: null,
  detailLoading: false,
  showQuickSearchModal: false,
  quickSearchQuery: '',
  quickSearchResults: [],
  toastMessage: null,
  availableUpdate: null
};

const syncedCatalogHashes = new Map();

function formatStorageBytes(bytes) {
  const b = Number(bytes || 0);
  if (b <= 0) return '0 KB';
  const mb = b / (1024 * 1024);
  if (mb >= 1) return `${mb.toFixed(2)} MB`;
  return `${(b / 1024).toFixed(1)} KB`;
}

function getStorageWarningBanner(level) {
  if (level === 'critical_95') return 'CRITICAL (95%+ Quota): Database storage is nearly full. Run cleanup or archive old logs immediately.';
  if (level === 'critical_85') return 'HIGH ALERT (85%+ Quota): Database storage exceeded 85% of the 500 MB limit.';
  if (level === 'warning_75') return 'WARNING (75%+ Quota): Database storage has reached 75% of the 500 MB budget.';
  if (level === 'warning_60') return 'NOTICE (60%+ Quota): Database storage passed 60% of the 500 MB capacity.';
  return null;
}

function applyTheme() {
  const mode = state.prefs.themeMode;
  let effective = 'dark';
  if (mode === 'LIGHT') effective = 'light';
  else if (mode === 'SYSTEM') {
    effective = window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  }
  document.documentElement.setAttribute('data-theme', effective);
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

function showToast(msg) {
  state.toastMessage = msg;
  renderApp();
  setTimeout(() => {
    if (state.toastMessage === msg) {
      state.toastMessage = null;
      renderApp();
    }
  }, 3400);
}

// Keyboard Shortcuts for Desktop & macOS (Cmd/Ctrl+K for Quick Search, Esc to close modals)
window.addEventListener('keydown', (e) => {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault();
    state.showQuickSearchModal = !state.showQuickSearchModal;
    renderApp();
    const inp = document.getElementById('inp-quick-search');
    if (inp) inp.focus();
  } else if (e.key === 'Escape') {
    if (state.showQuickSearchModal || state.detailAnime || state.showAvatarModal || state.showProfileEditModal || state.showCreateCollectionModal) {
      state.showQuickSearchModal = false;
      state.detailAnime = null;
      state.showAvatarModal = false;
      state.showProfileEditModal = false;
      state.showCreateCollectionModal = false;
      renderApp();
    }
  }
});

// Cloud Supabase Request Helper (Edge Function /api/supabase + Obfuscated Vault Fallback)
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

// Gemini Edge Function Helper (/api/gemini)
export async function askGeminiViaEdge(prompt, systemInstruction = 'You are an expert anime & donghua curator for Anime Universe.') {
  const response = await fetch('/api/gemini', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      model: 'gemini-2.5-flash',
      prompt,
      systemInstruction
    })
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error?.message || data.error || 'AI service unavailable');
  }
  return data.text || '';
}

function resolveTitle(item) {
  const pref = state.prefs.titleLanguage;
  if (pref === 'ROMAJI') return item.titleRomaji || item.title || item.titleEnglish || 'Anime';
  if (pref === 'NATIVE') return item.titleNative || item.titleEnglish || item.titleRomaji || item.title || 'Anime';
  return item.titleEnglish || item.title || item.titleRomaji || 'Anime';
}

function formatCountdown(airingUnix) {
  if (!airingUnix) return '';
  const nowSec = Math.floor(Date.now() / 1000);
  const diff = airingUnix - nowSec;
  if (diff <= 0) return 'Aired recently';
  const d = Math.floor(diff / 86400);
  const h = Math.floor((diff % 86400) / 3600);
  const m = Math.floor((diff % 3600) / 60);
  if (d > 0) return `${d}d ${h}h`;
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

function getAvatarBadgeHtml(avatarKey, username = 'M', sizePx = 38) {
  const av = BUILT_IN_AVATARS.find((a) => a.key === avatarKey) || BUILT_IN_AVATARS[0];
  return `<div class="avatar-circle" title="${escapeHtml(av.name)}" style="width:${sizePx}px;height:${sizePx}px;background:linear-gradient(135deg, ${av.c1}, ${av.c2});font-size:${Math.round(sizePx * 0.46)}px;">${av.icon}</div>`;
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
        memberStatus: cmStatus || p.member_status || 'pending'
      };
    } else {
      state.profile.username = state.session.username || 'Member';
      if (cmStatus) state.profile.memberStatus = cmStatus;
    }

    if (state.profile.memberStatus === 'active') {
      await loadCommunityReviews();
    }
    loadStorageStats();
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
      return;
    }

    const rows = await supabaseRequest('/rest/v1/community_reviews?order=updated_at.desc&limit=80').catch(() => []);
    if (Array.isArray(rows)) {
      state.communityReviews = rows
        .filter((r) => (r.rating || 0) > 0 && (r.review_notes || '').trim().length > 0)
        .map((r) => ({ ...r, likes_count: r.likes_count || 0, liked_by_me: Boolean(r.liked_by_me) }));
      renderApp();
    }
  } catch (_) {}
}

async function toggleReviewLike(reviewId) {
  if (!state.session || !reviewId) return;
  const target = state.communityReviews.find((r) => String(r.id) === String(reviewId));
  if (!target) return;
  const optimisticLiked = !target.liked_by_me;
  target.liked_by_me = optimisticLiked;
  target.likes_count = Math.max(0, (target.likes_count || 0) + (optimisticLiked ? 1 : -1));
  renderApp();

  try {
    const res = await supabaseRequest('/rest/v1/rpc/toggle_review_like', 'POST', { p_review_id: reviewId });
    if (res && typeof res.liked === 'boolean') {
      target.liked_by_me = res.liked;
      renderApp();
      return;
    }
  } catch (_) {
    try {
      const uid = encodeURIComponent(state.session.userId);
      const rid = encodeURIComponent(reviewId);
      if (optimisticLiked) {
        await supabaseRequest('/rest/v1/review_likes', 'POST', { user_id: state.session.userId, review_id: reviewId }, 'return=minimal');
      } else {
        await supabaseRequest(`/rest/v1/review_likes?user_id=eq.${uid}&review_id=eq.${rid}`, 'DELETE');
      }
    } catch (_) {}
  }
}

async function loadStorageStats() {
  state.storageStatsLoading = true;
  renderApp();
  try {
    const res = await supabaseRequest('/rest/v1/rpc/get_database_storage_stats', 'POST', {});
    if (res && typeof res === 'object' && res.limit_bytes) {
      state.storageStats = {
        totalBytes: Number(res.total_bytes || 0),
        limitBytes: Number(res.limit_bytes || 524288000),
        usagePercent: Number(res.usage_percent || 0),
        warningLevel: res.warning_level || 'normal',
        bytesPerUser: Number(res.bytes_per_user || 2200),
        bytesPerReview: Number(res.bytes_per_review || 180),
        estimatedRemainingUsers: Number(res.estimated_remaining_users || 230000),
        tables: Array.isArray(res.tables) ? res.tables : []
      };
    }
  } catch (_) {
  } finally {
    state.storageStatsLoading = false;
    renderApp();
  }
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
        countryOfOrigin: m.countryOfOrigin || 'JP',
        totalEpisodes: m.episodes || null,
        averageScore: m.averageScore || null,
        genres: m.genres || [],
        synopsis: (m.description || '').replace(/<[^>]+>/g, ''),
        airingDayOfWeek: dow,
        airingTimeUnix: airingAt,
        nextEpisodeNumber: m.nextAiringEpisode?.episode || null,
        status: 'plan_to_watch',
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
  const inVault = state.vault.find((v) => v.animeId === animeId);
  const inDisc = state.discover.find((d) => d.animeId === animeId);
  state.detailAnime = inVault
    ? { ...inVault }
    : inDisc
    ? { ...inDisc }
    : {
        animeId,
        title: 'Loading...',
        coverImage: '',
        status: 'plan_to_watch',
        season: 1,
        watchedEpisodes: 0,
        totalEpisodes: null,
        rating10: null,
        reviewNotes: ''
      };
  state.detailRich = null;
  state.detailLoading = true;
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
        characters(sort: [ROLE, RELEVANCE], perPage: 8) {
          nodes { id name { full } image { large } }
        }
        relations {
          edges {
            relationType
            node { id type title { english romaji } coverImage { large } format status episodes }
          }
        }
        recommendations(perPage: 6, sort: [RATING_DESC]) {
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
        countryOfOrigin: m.countryOfOrigin || 'JP',
        totalEpisodes: m.episodes || existing?.totalEpisodes || null,
        averageScore: m.averageScore || null,
        genres: m.genres || [],
        synopsis: (m.description || '').replace(/<[^>]+>/g, ''),
        airingTimeUnix: m.nextAiringEpisode?.airingAt || null,
        nextEpisodeNumber: m.nextAiringEpisode?.episode || null,
        status: existing?.status || 'plan_to_watch',
        season: existing?.season || 1,
        watchedEpisodes: existing?.watchedEpisodes || 0,
        rating10: existing?.rating10 || null,
        reviewNotes: existing?.reviewNotes || '',
        isPinned: Boolean(existing?.isPinned),
        isFavorite: Boolean(existing?.isFavorite)
      };
      state.detailRich = {
        studios: (m.studios?.nodes || []).map((s) => s.name),
        characters: (m.characters?.nodes || []).map((c) => ({
          id: c.id,
          name: c.name?.full || 'Character',
          image: c.image?.large || ''
        })),
        relations: (m.relations?.edges || [])
          .filter((e) => e.node && e.node.type === 'ANIME')
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
  state.vault = [item, ...state.vault.filter((v) => v.animeId !== item.animeId)];
  saveLocalVault();
  if (!state.session) return;
  try {
    // 1. Sync shared anime metadata once into public.anime (avoids duplicating anime rows per user)
    const catalogKey = `${item.titleEnglish || item.title}|${item.coverImage}|${item.totalEpisodes || 0}`;
    if (syncedCatalogHashes.get(item.animeId) !== catalogKey) {
      const safeCover = String(item.coverImage || '').startsWith('data:image') ? '' : item.coverImage || '';
      const safeBanner = String(item.bannerImage || '').startsWith('data:image') ? '' : item.bannerImage || '';
      await supabaseRequest(
        '/rest/v1/anime?on_conflict=anilist_id',
        'POST',
        {
          anilist_id: item.animeId,
          title_english: String(item.titleEnglish || item.title || 'Anime').slice(0, 200),
          title_romaji: String(item.titleRomaji || item.title || 'Anime').slice(0, 200),
          title_native: String(item.titleNative || '').slice(0, 200),
          cover_image: safeCover,
          banner_image: safeBanner,
          episodes: item.totalEpisodes || null,
          status: item.mediaStatus || 'RELEASING',
          format: item.format || 'TV',
          genres: Array.isArray(item.genres) ? item.genres.slice(0, 8) : [],
          description: String(item.synopsis || '').slice(0, 360)
        },
        'resolution=merge-duplicates,return=minimal'
      ).catch(() => {});
      syncedCatalogHashes.set(item.animeId, catalogKey);
    }

    // 2. Upsert compact per-user record into public.user_anime (review_notes capped at 500 chars)
    await supabaseRequest(
      '/rest/v1/user_anime?on_conflict=user_id,anime_id',
      'POST',
      {
        user_id: state.session.userId,
        anime_id: item.animeId,
        status: item.status,
        episode_progress: Math.max(0, Number(item.watchedEpisodes || 0)),
        user_season: Math.max(1, Number(item.season || 1)),
        total_episodes: item.totalEpisodes || null,
        personal_rating: item.rating10 ? Math.min(10, Math.max(1, Math.round(item.rating10))) : null,
        review_notes: String(item.reviewNotes || '').trim().slice(0, 500),
        is_favorite: Boolean(item.isFavorite)
      },
      'resolution=merge-duplicates,return=minimal'
    );
    if (state.profile.memberStatus === 'active') {
      await loadCommunityReviews();
    }
  } catch (_) {}
}

function escapeHtml(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function renderPosterCard(item, showControls = true) {
  const title = resolveTitle(item);
  const stars = item.rating10 ? (item.rating10 / 2).toFixed(1) : null;
  const sentiment = sentimentFromRating10(item.rating10);
  const isDonghua = item.countryOfOrigin === 'CN';
  const pct =
    item.totalEpisodes && item.totalEpisodes > 0
      ? Math.min(100, Math.round((item.watchedEpisodes / item.totalEpisodes) * 100))
      : 0;

  return `
    <div class="poster-card" data-open-detail="${item.animeId}">
      <div class="poster-media">
        <img src="${escapeHtml(item.coverImage)}" alt="${escapeHtml(title)}" loading="lazy" />
        <div class="poster-overlay-top">
          <span class="overlay-chip">${isDonghua ? 'DONGHUA' : escapeHtml(item.format || 'TV')}</span>
          ${stars ? `<span class="overlay-chip gold">★ ${stars}</span>` : item.averageScore ? `<span class="overlay-chip">${(item.averageScore / 20).toFixed(1)}★</span>` : ''}
        </div>
      </div>
      <div class="poster-body">
        <div>
          <div class="poster-title">${escapeHtml(title)}</div>
          <div style="display:flex; align-items:center; justify-content:space-between; margin-top:5px; font-size:0.78rem; color:var(--text-secondary);">
            <span class="tabular-num">${
              showControls
                ? `${item.season > 1 ? `S${item.season} • ` : ''}${item.watchedEpisodes}/${item.totalEpisodes || '?'} eps`
                : `${item.totalEpisodes || '?'} Episodes`
            }</span>
            ${item.isPinned ? `<span style="color:var(--accent-primary);" title="Pinned">📌</span>` : ''}
          </div>
          ${
            item.nextEpisodeNumber && item.airingTimeUnix
              ? `<div style="font-size:0.73rem; color:var(--success); margin-top:4px;">Ep ${item.nextEpisodeNumber} in ${formatCountdown(item.airingTimeUnix)}</div>`
              : ''
          }
          ${sentiment ? `<div style="margin-top:6px;"><span class="sentiment-pill ${sentiment.css}">${sentiment.shortLabel}</span></div>` : ''}
        </div>
        ${
          showControls
            ? `
          <div>
            <div class="progress-track" style="margin-bottom:8px;"><div class="progress-fill" style="width:${pct}%"></div></div>
            <div style="display:flex; justify-content:space-between; align-items:center; gap:6px;">
              <span class="micro-label" style="color:var(--accent-primary);">${escapeHtml(item.status.replace(/_/g, ' '))}</span>
              <button class="capsule-pill" data-inc-id="${item.animeId}" style="padding:4px 11px; font-size:0.75rem;">+1 Ep</button>
            </div>
          </div>`
            : `<button class="capsule-pill" data-open-detail="${item.animeId}" style="width:100%; padding:6px 10px; font-size:0.76rem;">+ Track / Rate</button>`
        }
      </div>
    </div>
  `;
}

function renderDesktopTable(items, isVault = true) {
  return `
    <div class="desktop-table-wrap">
      <table class="desktop-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Format</th>
            <th>Status</th>
            <th>Progress</th>
            <th>Rating &amp; Sentiment</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          ${items
            .map((item) => {
              const title = resolveTitle(item);
              const stars = item.rating10 ? `★ ${(item.rating10 / 2).toFixed(1)}` : '—';
              const sentiment = sentimentFromRating10(item.rating10);
              return `
                <tr data-open-detail="${item.animeId}" style="cursor:pointer;">
                  <td>
                    <div style="display:flex; align-items:center; gap:12px;">
                      <img src="${escapeHtml(item.coverImage)}" alt="" style="width:42px;height:58px;object-fit:cover;border-radius:8px;" />
                      <div>
                        <div class="serif-heading" style="font-size:1.1rem;">${escapeHtml(title)}</div>
                        <div style="font-size:0.75rem; color:var(--text-muted);">${item.countryOfOrigin === 'CN' ? 'Donghua (CN)' : 'Anime (JP)'}</div>
                      </div>
                    </div>
                  </td>
                  <td><span class="micro-label">${escapeHtml(item.format || 'TV')}</span></td>
                  <td><span class="capsule-pill active" style="padding:4px 10px; font-size:0.72rem;">${escapeHtml((item.status || 'plan_to_watch').replace(/_/g, ' '))}</span></td>
                  <td class="tabular-num">${item.season > 1 ? `S${item.season} • ` : ''}${item.watchedEpisodes || 0} / ${item.totalEpisodes || '?'} eps</td>
                  <td>
                    <span style="color:var(--accent-primary); font-weight:700;">${stars}</span>
                    ${sentiment ? `<span class="sentiment-pill ${sentiment.css}" style="margin-left:6px;">${sentiment.shortLabel}</span>` : ''}
                  </td>
                  <td>
                    ${
                      isVault
                        ? `<button class="capsule-pill" data-inc-id="${item.animeId}" style="padding:5px 12px; font-size:0.76rem;">+1 Ep</button>`
                        : `<button class="capsule-pill" data-open-detail="${item.animeId}" style="padding:5px 12px; font-size:0.76rem;">Inspect</button>`
                    }
                  </td>
                </tr>
              `;
            })
            .join('')}
        </tbody>
      </table>
    </div>
  `;
}

function renderHomeScreen() {
  const todayDow = ((new Date().getDay() + 6) % 7) + 1;
  const dayCounts = {};
  for (let d = 1; d <= 7; d++) {
    dayCounts[d] = state.vault.filter((v) => (v.airingDayOfWeek || 1) === d).length;
  }

  let sectionsHtml = '';
  if (state.vault.length === 0) {
    sectionsHtml = `
      <div class="stat-box" style="padding:42px 24px; margin-top:16px;">
        <h3 class="serif-heading" style="font-size:1.7rem; margin-bottom:8px;">Your Weekly Schedule is Empty</h3>
        <p style="color:var(--text-secondary); max-width:460px; margin:0 auto 18px;">
          Explore trending anime &amp; donghua and add titles to your library to track weekly episode releases and countdowns.
        </p>
        <button class="btn-primary" data-nav="discover">Explore Catalog</button>
      </div>
    `;
  } else if (state.homeAllWeekDays) {
    const orderedDays = Array.from({ length: 7 }, (_, idx) => ((todayDow - 1 + idx) % 7) + 1);
    sectionsHtml = orderedDays
      .map((dow) => {
        const dayInfo = DAY_LABELS.find((d) => d.dow === dow);
        const items = state.vault.filter((v) => (v.airingDayOfWeek || 1) === dow);
        if (items.length === 0) return '';
        return `
          <section style="margin-bottom:28px;">
            <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:12px;">
              <h3 class="serif-heading" style="font-size:1.5rem; color:${dow === todayDow ? 'var(--accent-primary)' : 'var(--text-primary)'};">
                ${dow === todayDow ? `Today • ${dayInfo.full}` : dayInfo.full}
              </h3>
              <span class="micro-label" style="color:var(--text-muted);">${items.length} Titles</span>
            </div>
            <div class="poster-grid">${items.map((it) => renderPosterCard(it, true)).join('')}</div>
          </section>
        `;
      })
      .join('');
  } else {
    const dayInfo = DAY_LABELS.find((d) => d.dow === state.selectedDay) || DAY_LABELS[0];
    const items = state.vault.filter((v) => (v.airingDayOfWeek || 1) === state.selectedDay);
    sectionsHtml = `
      <section>
        <h3 class="serif-heading" style="font-size:1.5rem; margin-bottom:14px;">${dayInfo.full} Schedule (${items.length})</h3>
        ${
          items.length === 0
            ? `<div class="stat-box" style="padding:32px;"><p style="color:var(--text-secondary);">No tracked shows scheduled for ${dayInfo.full}.</p></div>`
            : `<div class="poster-grid">${items.map((it) => renderPosterCard(it, true)).join('')}</div>`
        }
      </section>
    `;
  }

  return `
    <div class="obsidian-topbar">
      <div class="topbar-titles">
        <div class="monogram-badge">AU</div>
        <div>
          <h1 class="serif-heading" style="font-size:1.65rem;">ANIME <span style="color:var(--accent-primary);">UNIVERSE</span></h1>
          <div class="micro-label" style="color:var(--text-secondary);">Weekly Schedule • Track Anime &amp; Share Taste</div>
        </div>
      </div>
      <div class="topbar-actions">
        <button class="btn-secondary" id="btn-open-quick-search">🔍 Quick Search <span class="kbd-badge">⌘K</span></button>
        ${deferredInstallPrompt ? `<button class="btn-primary" id="btn-pwa-install">📲 Install App</button>` : ''}
      </div>
    </div>

    <div class="capsule-row">
      <button class="capsule-pill ${state.homeAllWeekDays ? 'active' : ''}" id="btn-home-all-days">All Week (${state.vault.length})</button>
      ${DAY_LABELS.map(
        (d) => `
        <button class="capsule-pill ${!state.homeAllWeekDays && state.selectedDay === d.dow ? 'active' : ''}" data-select-day="${d.dow}">
          ${d.short} (${dayCounts[d.dow] || 0})
        </button>`
      ).join('')}
    </div>

    ${sectionsHtml}
  `;
}

function renderDiscoverScreen() {
  const modes = [
    { id: 'trending', label: 'Trending', sort: 'TRENDING_DESC' },
    { id: 'popular', label: 'Popular', sort: 'POPULARITY_DESC' },
    { id: 'highest_rated', label: 'Top Rated', sort: 'SCORE_DESC' },
    { id: 'seasonal', label: `Seasonal (${getCurrentAnimeSeason()})`, sort: 'POPULARITY_DESC' },
    { id: 'donghua', label: 'Donghua (CN)', sort: 'POPULARITY_DESC' },
    { id: 'airing', label: 'Airing Now', sort: 'TRENDING_DESC' }
  ];

  return `
    <div class="obsidian-topbar">
      <div class="topbar-titles">
        <div>
          <h1 class="serif-heading" style="font-size:1.75rem;">Explore Catalog</h1>
          <div class="micro-label" style="color:var(--text-secondary);">AniList Live GraphQL • Anime &amp; Donghua</div>
        </div>
      </div>
      <div class="topbar-actions" style="flex:1; max-width:520px; justify-content:flex-end;">
        <input id="inp-discover-search" class="field-input" style="max-width:340px;" placeholder="Search anime or donghua..." value="${escapeHtml(state.searchQuery)}" />
        <button class="btn-secondary" id="btn-toggle-layout" title="Switch Grid / Table View">
          ${state.prefs.watchlistLayout === 'GRID' ? '☰ Table' : '⊞ Grid'}
        </button>
      </div>
    </div>

    <div class="capsule-row">
      ${modes
        .map(
          (m) => `
        <button class="capsule-pill ${state.discoverMode === m.id ? 'active' : ''}" data-disc-mode="${m.id}" data-disc-sort="${m.sort}">
          ${escapeHtml(m.label)}
        </button>`
        )
        .join('')}
    </div>

    <div style="display:flex; gap:10px; flex-wrap:wrap; margin-bottom:14px;">
      <select id="sel-disc-format" class="field-select" style="width:auto;">
        <option value="ALL" ${state.discoverFormat === 'ALL' ? 'selected' : ''}>Format: All</option>
        ${['TV', 'MOVIE', 'ONA', 'OVA', 'SPECIAL']
          .map((f) => `<option value="${f}" ${state.discoverFormat === f ? 'selected' : ''}>${f}</option>`)
          .join('')}
      </select>
      <select id="sel-disc-status" class="field-select" style="width:auto;">
        <option value="ALL" ${state.discoverStatus === 'ALL' ? 'selected' : ''}>Status: All</option>
        <option value="RELEASING" ${state.discoverStatus === 'RELEASING' ? 'selected' : ''}>Releasing</option>
        <option value="FINISHED" ${state.discoverStatus === 'FINISHED' ? 'selected' : ''}>Finished</option>
        <option value="NOT_YET_RELEASED" ${state.discoverStatus === 'NOT_YET_RELEASED' ? 'selected' : ''}>Upcoming</option>
      </select>
      <select id="sel-disc-sort" class="field-select" style="width:auto;">
        <option value="TRENDING_DESC" ${state.discoverSort === 'TRENDING_DESC' ? 'selected' : ''}>Sort: Trending</option>
        <option value="POPULARITY_DESC" ${state.discoverSort === 'POPULARITY_DESC' ? 'selected' : ''}>Sort: Popular</option>
        <option value="SCORE_DESC" ${state.discoverSort === 'SCORE_DESC' ? 'selected' : ''}>Sort: Highest Rated</option>
        <option value="START_DATE_DESC" ${state.discoverSort === 'START_DATE_DESC' ? 'selected' : ''}>Sort: Newest</option>
      </select>
    </div>

    <div class="capsule-row">
      <button class="capsule-pill ${!state.discoverGenre ? 'active' : ''}" data-disc-genre="">All Genres</button>
      ${ANILIST_GENRES.map(
        (g) => `<button class="capsule-pill ${state.discoverGenre === g ? 'active' : ''}" data-disc-genre="${g}">${g}</button>`
      ).join('')}
    </div>

    ${
      state.isSearching
        ? `<div class="stat-box" style="padding:48px;"><p style="color:var(--accent-primary);">Loading Anime Universe catalog...</p></div>`
        : state.prefs.watchlistLayout === 'LIST'
        ? renderDesktopTable(state.discover, false)
        : `<div class="poster-grid">${state.discover.map((it) => renderPosterCard(it, false)).join('')}</div>`
    }
  `;
}

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
      if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;
      if (state.myListSort === 'TITLE') return resolveTitle(a).localeCompare(resolveTitle(b));
      if (state.myListSort === 'RATING') return (b.rating10 || 0) - (a.rating10 || 0);
      if (state.myListSort === 'PROGRESS') return (b.watchedEpisodes || 0) - (a.watchedEpisodes || 0);
      return 0;
    });

  return `
    <div class="obsidian-topbar">
      <div class="topbar-titles">
        <div>
          <h1 class="serif-heading" style="font-size:1.75rem;">Library</h1>
          <div class="micro-label" style="color:var(--text-secondary);">${state.vault.length} Tracked • ${state.collections.length} Collections</div>
        </div>
      </div>
      <div class="topbar-actions">
        <input id="inp-mylist-search" class="field-input" style="width:240px;" placeholder="Filter library..." value="${escapeHtml(state.myListQuery)}" />
        <select id="sel-mylist-sort" class="field-select" style="width:auto;">
          <option value="RECENT" ${state.myListSort === 'RECENT' ? 'selected' : ''}>Sort: Recent</option>
          <option value="TITLE" ${state.myListSort === 'TITLE' ? 'selected' : ''}>Sort: Title</option>
          <option value="RATING" ${state.myListSort === 'RATING' ? 'selected' : ''}>Sort: Rating</option>
          <option value="PROGRESS" ${state.myListSort === 'PROGRESS' ? 'selected' : ''}>Sort: Progress</option>
        </select>
        <button class="btn-secondary" id="btn-create-collection">+ New Collection</button>
        <button class="btn-secondary" id="btn-toggle-layout">${state.prefs.watchlistLayout === 'GRID' ? '☰ Table' : '⊞ Grid'}</button>
      </div>
    </div>

    <div class="capsule-row">
      <button class="capsule-pill ${state.myListStatusFilter === 'ALL' ? 'active' : ''}" data-mylist-status="ALL">All (${state.vault.length})</button>
      ${WATCH_STATUSES.map((st) => {
        const count = state.vault.filter((v) => v.status === st.dbValue).length;
        return `<button class="capsule-pill ${state.myListStatusFilter === st.dbValue ? 'active' : ''}" data-mylist-status="${st.dbValue}">${st.label} (${count})</button>`;
      }).join('')}
    </div>

    <div class="capsule-row">
      <button class="capsule-pill ${!state.myListSentimentFilter ? 'active' : ''}" data-mylist-sentiment="">All Sentiments</button>
      ${SENTIMENTS.map(
        (s) =>
          `<button class="capsule-pill ${state.myListSentimentFilter === s.key ? 'active' : ''}" data-mylist-sentiment="${s.key}">${s.fullLabel}</button>`
      ).join('')}
    </div>

    ${
      state.collections.length > 0
        ? `<div class="capsule-row">
            <button class="capsule-pill ${!state.myListCollectionFilter ? 'active' : ''}" data-mylist-col="">All Collections</button>
            ${state.collections
              .map(
                (c) =>
                  `<button class="capsule-pill ${state.myListCollectionFilter === c.id ? 'active' : ''}" data-mylist-col="${c.id}">📚 ${escapeHtml(
                    c.name
                  )} (${(c.animeIds || []).length})</button>`
              )
              .join('')}
          </div>`
        : ''
    }

    ${
      filtered.length === 0
        ? `<div class="stat-box" style="padding:42px;"><p style="color:var(--text-secondary);">No matching anime entries in your library.</p></div>`
        : state.prefs.watchlistLayout === 'LIST'
        ? renderDesktopTable(filtered, true)
        : `<div class="poster-grid">${filtered.map((it) => renderPosterCard(it, true)).join('')}</div>`
    }
  `;
}

function renderCommunityScreen() {
  if (!state.session) {
    return `
      <div class="modal-panel narrow" style="margin:48px auto; text-align:center;">
        <h2 class="serif-heading" style="font-size:1.85rem; margin-bottom:8px;">Sign In for Community Reviews</h2>
        <p style="color:var(--text-secondary); margin-bottom:18px;">
          Sign in or create an account to browse rated anime showcases and written notes from your circle.
        </p>
        <button class="btn-primary" id="btn-exit-guest" style="width:100%;">Sign In / Create Account</button>
      </div>
    `;
  }

  if (state.profile.memberStatus !== 'active') {
    return `
      <div class="modal-panel narrow" style="margin:48px auto; text-align:center;">
        <h2 class="serif-heading" style="font-size:1.95rem; margin-bottom:10px;">Join the Community</h2>
        <p style="color:var(--text-secondary); margin-bottom:20px; line-height:1.5;">
          Unlock rated &amp; noted anime showcases shared by members. Your own ratings and notes stay private unless you enable sharing.
        </p>
        <button class="btn-primary" id="btn-join-community" style="width:100%;">Join Community</button>
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
      if (state.communitySort === 'MOST_LIKED') {
        const diff = (b.likes_count || 0) - (a.likes_count || 0);
        if (diff !== 0) return diff;
        return (b.rating || 0) - (a.rating || 0);
      }
      if (state.communitySort === 'HIGHEST') return (b.rating || 0) - (a.rating || 0);
      if (state.communitySort === 'LOWEST') return (a.rating || 0) - (b.rating || 0);
      return String(b.updated_at || '').localeCompare(String(a.updated_at || ''));
    });

  return `
    <div class="obsidian-topbar">
      <div class="topbar-titles">
        <div>
          <h1 class="serif-heading" style="font-size:1.75rem;">Community</h1>
          <div class="micro-label" style="color:var(--text-secondary);">Rated &amp; Noted • ${state.communityReviews.length} Reviews</div>
        </div>
      </div>
      <div class="topbar-actions">
        <button class="btn-secondary" id="btn-refresh-community">↻ Refresh</button>
      </div>
    </div>

    <div class="capsule-row">
      <button class="capsule-pill ${!state.communitySentimentFilter ? 'active' : ''}" data-comm-sentiment="">ALL (${state.communityReviews.length})</button>
      ${SENTIMENTS.map(
        (s) =>
          `<button class="capsule-pill ${state.communitySentimentFilter === s.key ? 'active' : ''}" data-comm-sentiment="${s.key}">${s.shortLabel.toUpperCase()}</button>`
      ).join('')}
    </div>

    <div class="capsule-row">
      ${[
        { k: 'RECENT', label: 'Recent' },
        { k: 'MOST_LIKED', label: 'Most Liked ♥' },
        { k: 'HIGHEST', label: 'Highest Rated' },
        { k: 'LOWEST', label: 'Lowest Rated' }
      ]
        .map(
          (opt) =>
            `<button class="capsule-pill ${state.communitySort === opt.k ? 'active' : ''}" data-comm-sort="${opt.k}">${opt.label}</button>`
        )
        .join('')}
    </div>

    ${
      filtered.length === 0
        ? `<div class="stat-box" style="padding:42px;"><p style="color:var(--text-secondary);">No rated &amp; noted anime showcases match your filter yet.</p></div>`
        : `<div style="display:grid; grid-template-columns:repeat(auto-fill, minmax(360px, 1fr)); gap:18px;">
            ${filtered
              .map((rev) => {
                const stars = ((rev.rating || 0) / 2).toFixed(1);
                const sentiment = sentimentFromRating10(rev.rating);
                const liked = Boolean(rev.liked_by_me);
                const likesCount = Number(rev.likes_count || 0);
                return `
                  <div class="stat-box" data-open-detail="${rev.anime_id}" style="text-align:left; cursor:pointer; display:flex; flex-direction:column; gap:12px;">
                    <div style="display:flex; gap:14px;">
                      <img src="${escapeHtml(rev.anime_cover_image)}" alt="" style="width:66px;height:96px;object-fit:cover;border-radius:14px;flex-shrink:0;" />
                      <div style="flex:1; min-width:0;">
                        <div class="serif-heading" style="font-size:1.3rem;">${escapeHtml(rev.anime_title)}</div>
                        <div style="color:var(--accent-primary); font-weight:700; margin-top:4px;">★ ${stars} / 5.0</div>
                        ${sentiment ? `<div style="margin-top:6px;"><span class="sentiment-pill ${sentiment.css}">${sentiment.fullLabel}</span></div>` : ''}
                      </div>
                    </div>
                    <div class="editorial-quote">“${escapeHtml(rev.review_notes)}”</div>
                    <div style="display:flex; align-items:center; justify-content:space-between; font-size:0.8rem; color:var(--text-secondary);">
                      <div style="display:flex; align-items:center; gap:8px;">
                        ${getAvatarBadgeHtml(rev.avatar_key || 'avatar_1', rev.username, 26)}
                        <span>${escapeHtml(rev.username || 'Member')}</span>
                        <span class="tabular-num" style="color:var(--text-muted);">• ${escapeHtml(String(rev.updated_at || '').slice(0, 10))}</span>
                      </div>
                      <button class="capsule-pill ${liked ? 'active' : ''}" data-like-review="${escapeHtml(rev.id)}" style="padding:4px 11px; font-size:0.76rem;">
                        ${liked ? '♥' : '♡'} ${likesCount > 0 ? likesCount : 'Like'}
                      </button>
                    </div>
                  </div>
                `;
              })
              .join('')}
          </div>`
    }
  `;
}

function renderProfileScreen() {
  const watchedCount = state.vault.filter((v) => (v.watchedEpisodes || 0) >= 1).length;
  const ratedItems = state.vault.filter((v) => (v.rating10 || 0) > 0);
  const meanStars =
    ratedItems.length > 0
      ? `★ ${(ratedItems.reduce((acc, v) => acc + v.rating10 / 2, 0) / ratedItems.length).toFixed(1)}`
      : '—';

  return `
    <div class="obsidian-topbar">
      <div class="topbar-titles">
        <h1 class="serif-heading" style="font-size:1.75rem;">Profile</h1>
      </div>
      <div class="topbar-actions">
        <button class="btn-secondary" data-nav="settings">⚙️ Settings</button>
      </div>
    </div>

    <div class="stat-box" style="padding:30px; margin-bottom:20px;">
      <div style="display:flex; flex-direction:column; align-items:center; gap:10px;">
        <div id="btn-open-avatar-modal" style="cursor:pointer;" title="Click to change built-in avatar">
          ${getAvatarBadgeHtml(state.profile.avatarKey, state.profile.username, 92)}
        </div>
        <h2 class="serif-heading" style="font-size:2rem;">${escapeHtml(state.profile.username)}</h2>
        ${state.profile.bio ? `<p style="color:var(--text-secondary); max-width:460px;">${escapeHtml(state.profile.bio)}</p>` : ''}
        <div style="display:flex; gap:10px; margin-top:6px;">
          <button class="btn-secondary" id="btn-open-avatar-modal-2">Change Avatar</button>
          <button class="btn-secondary" id="btn-open-profile-edit">Edit Username &amp; Bio</button>
        </div>
      </div>
    </div>

    <div class="stats-grid" style="margin-bottom:22px;">
      <div class="stat-box">
        <div class="micro-label" style="color:var(--text-muted);">Watched</div>
        <div class="serif-heading tabular-num" style="font-size:2rem; color:var(--accent-primary); margin-top:4px;">${watchedCount}</div>
      </div>
      <div class="stat-box">
        <div class="micro-label" style="color:var(--text-muted);">Rated Titles</div>
        <div class="serif-heading tabular-num" style="font-size:2rem; color:var(--accent-primary); margin-top:4px;">${ratedItems.length}</div>
      </div>
      <div class="stat-box">
        <div class="micro-label" style="color:var(--text-muted);">Mean Score</div>
        <div class="serif-heading tabular-num" style="font-size:2rem; color:var(--accent-primary); margin-top:4px;">${meanStars}</div>
      </div>
      <div class="stat-box">
        <div class="micro-label" style="color:var(--text-muted);">Collections</div>
        <div class="serif-heading tabular-num" style="font-size:2rem; color:var(--accent-primary); margin-top:4px;">${state.collections.length}</div>
      </div>
    </div>
  `;
}

function renderSettingsScreen() {
  return `
    <div class="obsidian-topbar">
      <div class="topbar-titles">
        <h1 class="serif-heading" style="font-size:1.75rem;">Settings</h1>
      </div>
    </div>

    <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(340px, 1fr)); gap:18px;">
      <div class="stat-box" style="text-align:left; display:flex; flex-direction:column; gap:12px;">
        <h3 class="serif-heading" style="font-size:1.4rem;">Appearance &amp; macOS/Desktop Display</h3>
        <label class="micro-label" style="color:var(--text-secondary);">Theme Palette</label>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          ${['DARK', 'LIGHT', 'SYSTEM']
            .map(
              (m) =>
                `<button class="capsule-pill ${state.prefs.themeMode === m ? 'active' : ''}" data-set-theme="${m}">${
                  m === 'DARK' ? 'Obsidian Dark' : m === 'LIGHT' ? 'Gallery Porcelain Light' : 'System Auto'
                }</button>`
            )
            .join('')}
        </div>
        <label class="micro-label" style="color:var(--text-secondary); margin-top:6px;">Default Library View</label>
        <div style="display:flex; gap:8px;">
          <button class="capsule-pill ${state.prefs.watchlistLayout === 'GRID' ? 'active' : ''}" data-set-layout="GRID">⊞ Poster Grid</button>
          <button class="capsule-pill ${state.prefs.watchlistLayout === 'LIST' ? 'active' : ''}" data-set-layout="LIST">☰ Desktop Table</button>
        </div>
      </div>

      <div class="stat-box" style="text-align:left; display:flex; flex-direction:column; gap:12px;">
        <h3 class="serif-heading" style="font-size:1.4rem;">Preferences &amp; Library Backup</h3>
        <label class="micro-label" style="color:var(--text-secondary);">Title Language</label>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          ${['ENGLISH', 'ROMAJI', 'NATIVE']
            .map(
              (lang) =>
                `<button class="capsule-pill ${state.prefs.titleLanguage === lang ? 'active' : ''}" data-set-lang="${lang}">${lang}</button>`
            )
            .join('')}
        </div>
        <label class="micro-label" style="color:var(--text-secondary); margin-top:6px;">Community Review Sharing</label>
        <div style="display:flex; gap:8px;">
          <button class="capsule-pill ${state.prefs.reviewsVisibility === 'community' ? 'active' : ''}" data-set-vis="community">Public to Community</button>
          <button class="capsule-pill ${state.prefs.reviewsVisibility === 'private' ? 'active' : ''}" data-set-vis="private">Private Only</button>
        </div>
        <div style="display:flex; gap:10px; flex-wrap:wrap; margin-top:8px;">
          <button class="btn-secondary" id="btn-export-json">⬇ Export Backup (.json)</button>
          <label class="btn-secondary" style="cursor:pointer;">
            ⬆ Import Backup (.json)
            <input type="file" id="inp-import-json" accept="application/json" style="display:none;" />
          </label>
        </div>
      </div>

      <div class="stat-box" style="text-align:left; display:flex; flex-direction:column; gap:12px;">
        <div style="display:flex; justify-content:space-between; align-items:center; gap:8px;">
          <div>
            <h3 class="serif-heading" style="font-size:1.4rem;">Cloud Storage &amp; 500 MB Quota</h3>
            <div class="micro-label" style="color:var(--text-secondary);">Normalized Schema • Shared Anime Metadata</div>
          </div>
          <button class="btn-secondary" id="btn-refresh-storage" style="padding:6px 12px; font-size:0.78rem;">
            ${state.storageStatsLoading ? 'Refreshing...' : '↻ Refresh'}
          </button>
        </div>
        <div style="display:flex; justify-content:space-between; align-items:center; font-size:0.88rem; font-weight:700;">
          <span style="color:var(--accent-primary);">${formatStorageBytes(state.storageStats.totalBytes)} / ${formatStorageBytes(state.storageStats.limitBytes)}</span>
          <span>${Number(state.storageStats.usagePercent || 0).toFixed(2)}% used</span>
        </div>
        <div class="progress-track" style="height:8px;">
          <div class="progress-fill" style="width:${Math.max(1, Math.min(100, Number(state.storageStats.usagePercent || 0)))}%;"></div>
        </div>
        ${
          getStorageWarningBanner(state.storageStats.warningLevel)
            ? `<div style="padding:10px 12px; border-radius:10px; border:1px solid var(--error); color:var(--error); font-size:0.8rem; font-weight:600;">
                ${escapeHtml(getStorageWarningBanner(state.storageStats.warningLevel))}
              </div>`
            : ''
        }
        <div style="display:grid; grid-template-columns:repeat(3, 1fr); gap:8px; font-size:0.78rem;">
          <div style="padding:8px; border-radius:10px; background:var(--bg-elevated);">
            <div style="color:var(--text-muted);">Avg / User</div>
            <div style="font-weight:700; color:var(--accent-primary);">~${(Number(state.storageStats.bytesPerUser || 2200) / 1024).toFixed(1)} KB</div>
          </div>
          <div style="padding:8px; border-radius:10px; background:var(--bg-elevated);">
            <div style="color:var(--text-muted);">Avg / Review</div>
            <div style="font-weight:700; color:var(--accent-primary);">~${Number(state.storageStats.bytesPerReview || 180)} B</div>
          </div>
          <div style="padding:8px; border-radius:10px; background:var(--bg-elevated);">
            <div style="color:var(--text-muted);">Capacity</div>
            <div style="font-weight:700; color:var(--accent-primary);">~${Number(state.storageStats.estimatedRemainingUsers || 230000).toLocaleString()}</div>
          </div>
        </div>
        ${
          Array.isArray(state.storageStats.tables) && state.storageStats.tables.length > 0
            ? `<div style="display:flex; flex-direction:column; gap:4px; font-size:0.76rem; color:var(--text-secondary); padding-top:4px; border-top:1px solid var(--border-subtle);">
                ${state.storageStats.tables
                  .slice(0, 6)
                  .map(
                    (t) => `
                  <div style="display:flex; justify-content:space-between;">
                    <span>public.${escapeHtml(t.table)}</span>
                    <span class="tabular-num">${Number(t.rows || 0)} rows • ${formatStorageBytes(t.bytes)}</span>
                  </div>`
                  )
                  .join('')}
              </div>`
            : ''
        }
      </div>

      <div class="stat-box" style="text-align:left; display:flex; flex-direction:column; gap:12px;">
        <h3 class="serif-heading" style="font-size:1.4rem;">Account &amp; Cloud Sync</h3>
        ${
          state.session
            ? `
          <p style="color:var(--accent-primary); font-weight:600;">Signed in as ${escapeHtml(state.profile.username)}</p>
          <div style="display:flex; gap:10px; flex-wrap:wrap;">
            <button class="btn-secondary" id="btn-sync-now">↻ Sync Cloud Vault</button>
            <button class="btn-secondary" id="btn-signout">Sign Out</button>
            <button class="btn-danger" id="btn-delete-account">Delete Account Permanently</button>
          </div>`
            : `
          <p style="color:var(--text-secondary);">Currently browsing in Local Preview Mode.</p>
          <button class="btn-primary" id="btn-exit-guest">Sign In / Create Cloud Account</button>`
        }
      </div>
    </div>
  `;
}

function renderApp() {
  const root = document.getElementById('app');
  if (!root) return;

  if (!state.session && !state.isGuestMode) {
    const isOtpReady = state.otpCode.replace(/\D/g, '').length === 8;
    const isResetReady = isOtpReady && state.newPassword.length >= 6;
    root.innerHTML = `
      <div style="min-height:100vh; display:flex; align-items:center; justify-content:center; padding:20px;">
        <div class="modal-panel narrow" style="text-align:center; display:flex; flex-direction:column; gap:14px;">
          <div class="monogram-badge" style="margin:0 auto; width:56px; height:56px; font-size:1.5rem;">AU</div>
          <h1 class="serif-heading" style="font-size:2.15rem;">ANIME <span style="color:var(--accent-primary);">UNIVERSE</span></h1>
          <p style="color:var(--text-secondary); font-size:0.9rem;">
            ${
              state.authMode === 'otp_signup'
                ? `Enter the 8-digit verification code sent to ${escapeHtml(state.email)}`
                : state.authMode === 'otp_recovery'
                ? 'Enter the 8-digit recovery code and choose your new password'
                : state.authMode === 'signup'
                ? 'Create your cloud-synced Anime Universe account'
                : 'Track anime. Share taste. Sync seamlessly across Android, iOS, macOS & Windows.'
            }
          </p>
          ${state.authError ? `<div style="color:var(--error); font-size:0.86rem;">${escapeHtml(state.authError)}</div>` : ''}
          ${state.authInfo ? `<div style="color:var(--accent-primary); font-size:0.86rem;">${escapeHtml(state.authInfo)}</div>` : ''}

          ${
            state.authMode === 'otp_signup'
              ? `
                <input id="inp-otp" class="field-input" type="text" inputmode="numeric" maxlength="8" placeholder="ex-12345678" value="${escapeHtml(state.otpCode)}" />
                <button id="btn-verify-signup" class="btn-primary" ${!isOtpReady || state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'Verifying...' : 'Verify & Continue'}
                </button>
                <button id="btn-back-signin" class="btn-secondary">Back to Sign In</button>
              `
              : state.authMode === 'otp_recovery'
              ? `
                <input id="inp-email" class="field-input" type="email" placeholder="Email Address" value="${escapeHtml(state.email)}" />
                <input id="inp-otp" class="field-input" type="text" inputmode="numeric" maxlength="8" placeholder="ex-12345678" value="${escapeHtml(state.otpCode)}" />
                <input id="inp-newpass" class="field-input" type="password" placeholder="New Password (min 6 chars)" value="${escapeHtml(state.newPassword)}" />
                <button id="btn-verify-recovery" class="btn-primary" ${!isResetReady || state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'Updating Password...' : 'Confirm New Password'}
                </button>
                <button id="btn-back-signin" class="btn-secondary">Back to Sign In</button>
              `
              : `
                ${state.authMode === 'signup' ? `<input id="inp-user" class="field-input" type="text" placeholder="Username" value="${escapeHtml(state.username)}" />` : ''}
                <input id="inp-email" class="field-input" type="email" placeholder="Email Address" value="${escapeHtml(state.email)}" />
                <input id="inp-pass" class="field-input" type="password" placeholder="Password" value="${escapeHtml(state.password)}" />
                <button id="btn-auth-submit" class="btn-primary" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'Please wait...' : state.authMode === 'signup' ? 'Create Account' : 'Sign In'}
                </button>
                <div style="display:flex; justify-content:space-between; gap:8px; flex-wrap:wrap;">
                  <button id="btn-toggle-mode" class="btn-secondary" style="flex:1;">
                    ${state.authMode === 'signup' ? 'Already have an account? Sign In' : 'Create Account'}
                  </button>
                  ${state.authMode === 'signin' ? `<button id="btn-forgot" class="btn-secondary">Forgot Password?</button>` : ''}
                </div>
                <button id="btn-guest-preview" class="btn-secondary" style="margin-top:4px;">Continue in Local Preview Mode</button>
              `
          }
        </div>
      </div>
    `;
    bindAuthEvents();
    return;
  }

  const navItems = [
    { id: 'home', label: 'Home', icon: '🏠', kbd: '1' },
    { id: 'discover', label: 'Explore', icon: '🧭', kbd: '2' },
    { id: 'mylist', label: 'Library', icon: '🔖', kbd: '3' },
    { id: 'community', label: 'Community', icon: '👥', kbd: '4' },
    { id: 'profile', label: 'Profile', icon: '👤', kbd: '5' },
    { id: 'settings', label: 'Settings', icon: '⚙️', kbd: '6' }
  ];

  let screenContent = '';
  if (state.tab === 'home') screenContent = renderHomeScreen();
  else if (state.tab === 'discover') screenContent = renderDiscoverScreen();
  else if (state.tab === 'mylist') screenContent = renderMyListScreen();
  else if (state.tab === 'community') screenContent = renderCommunityScreen();
  else if (state.tab === 'profile') screenContent = renderProfileScreen();
  else if (state.tab === 'settings') screenContent = renderSettingsScreen();

  root.innerHTML = `
    <div class="universe-layout">
      <aside class="desktop-sidebar">
        <div>
          <div class="sidebar-brand">
            <div class="monogram-badge">AU</div>
            <div>
              <div class="serif-heading" style="font-size:1.25rem;">ANIME <span style="color:var(--accent-primary);">UNIVERSE</span></div>
              <div class="micro-label" style="color:var(--text-muted); font-size:0.62rem;">Track Anime &amp; Share Taste</div>
            </div>
          </div>
          <nav class="sidebar-nav">
            ${navItems
              .map(
                (item) => `
              <button class="sidebar-btn ${state.tab === item.id ? 'active' : ''}" data-nav="${item.id}">
                <span class="sidebar-btn-left"><span>${item.icon}</span> <span>${item.label}</span></span>
                <span class="kbd-badge">${item.kbd}</span>
              </button>`
              )
              .join('')}
          </nav>
        </div>

        <div class="sidebar-footer">
          <button class="sidebar-btn" id="btn-sidebar-theme">
            <span class="sidebar-btn-left">🌓 <span>Theme: ${state.prefs.themeMode}</span></span>
          </button>
          <div class="profile-mini-card" data-nav="profile">
            ${getAvatarBadgeHtml(state.profile.avatarKey, state.profile.username, 36)}
            <div style="min-width:0; flex:1;">
              <div style="font-weight:700; font-size:0.86rem; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
                ${escapeHtml(state.session ? state.profile.username : 'Guest Preview')}
              </div>
              <div style="font-size:0.72rem; color:var(--text-muted);">${state.vault.length} Tracked Titles</div>
            </div>
          </div>
        </div>
      </aside>

      <main class="workspace-main">
        ${screenContent}
      </main>

      <nav class="mobile-dock">
        ${navItems
          .slice(0, 5)
          .map(
            (item) => `
          <button class="capsule-pill ${state.tab === item.id ? 'active' : ''}" data-nav="${item.id}" style="padding:8px 13px; font-size:0.78rem;">
            ${item.icon} ${item.label}
          </button>`
          )
          .join('')}
      </nav>
    </div>

    ${renderModalsHtml()}
  `;

  bindWorkspaceEvents();
}

function renderModalsHtml() {
  if (state.detailAnime) {
    const a = state.detailAnime;
    const title = resolveTitle(a);
    const stars = a.rating10 ? (a.rating10 / 2).toFixed(1) : '0.0';
    const sentiment = sentimentFromRating10(a.rating10);
    return `
      <div class="modal-overlay" id="modal-backdrop">
        <div class="modal-panel" onclick="event.stopPropagation()">
          <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:16px; margin-bottom:16px;">
            <div style="display:flex; gap:18px; flex-wrap:wrap;">
              <img src="${escapeHtml(a.coverImage)}" alt="" style="width:128px;height:184px;object-fit:cover;border-radius:16px;" />
              <div style="max-width:520px;">
                <span class="overlay-chip gold">${a.countryOfOrigin === 'CN' ? 'DONGHUA' : escapeHtml(a.format || 'TV')} • ${escapeHtml(a.mediaStatus || 'RELEASING')}</span>
                <h2 class="serif-heading" style="font-size:2rem; margin-top:6px;">${escapeHtml(title)}</h2>
                ${a.titleRomaji && a.titleRomaji !== title ? `<div style="color:var(--text-muted); font-size:0.84rem;">${escapeHtml(a.titleRomaji)}</div>` : ''}
                <div style="display:flex; gap:8px; flex-wrap:wrap; margin-top:10px;">
                  ${(a.genres || []).map((g) => `<span class="capsule-pill" style="padding:3px 10px; font-size:0.72rem;">${escapeHtml(g)}</span>`).join('')}
                </div>
                ${a.synopsis ? `<p style="color:var(--text-secondary); font-size:0.88rem; margin-top:12px; line-height:1.55; max-height:110px; overflow-y:auto;">${escapeHtml(a.synopsis)}</p>` : ''}
              </div>
            </div>
            <button class="icon-btn" id="btn-close-modal">✕</button>
          </div>

          <div class="stat-box" style="text-align:left; display:flex; flex-direction:column; gap:12px; margin-bottom:16px;">
            <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
              <h3 class="serif-heading" style="font-size:1.35rem;">Tracking &amp; Editorial Review</h3>
              <div style="display:flex; gap:8px;">
                <button class="capsule-pill ${a.isPinned ? 'active' : ''}" id="btn-detail-pin">📌 ${a.isPinned ? 'Pinned' : 'Pin'}</button>
                <button class="capsule-pill ${a.isFavorite ? 'active' : ''}" id="btn-detail-fav">❤️ ${a.isFavorite ? 'Favorite' : 'Fav'}</button>
              </div>
            </div>
            <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(160px, 1fr)); gap:12px;">
              <div>
                <label class="micro-label" style="color:var(--text-muted);">Watch Status</label>
                <select id="detail-status" class="field-select" style="margin-top:4px;">
                  ${WATCH_STATUSES.map((st) => `<option value="${st.dbValue}" ${a.status === st.dbValue ? 'selected' : ''}>${st.label}</option>`).join('')}
                </select>
              </div>
              <div>
                <label class="micro-label" style="color:var(--text-muted);">Season Number</label>
                <input id="detail-season" class="field-input" type="number" min="1" max="99" value="${a.season || 1}" style="margin-top:4px;" />
              </div>
              <div>
                <label class="micro-label" style="color:var(--text-muted);">Watched Episodes (${a.totalEpisodes || '?'} total)</label>
                <input id="detail-eps" class="field-input" type="number" min="0" value="${a.watchedEpisodes || 0}" style="margin-top:4px;" />
              </div>
              <div>
                <label class="micro-label" style="color:var(--text-muted);">5-Star Rating (0.5 steps)</label>
                <input id="detail-stars" class="field-input" type="number" min="0" max="5" step="0.5" value="${stars}" style="margin-top:4px;" />
              </div>
            </div>
            ${sentiment ? `<div><span class="sentiment-pill ${sentiment.css}">${sentiment.fullLabel}</span></div>` : ''}
            <div>
              <label class="micro-label" style="color:var(--text-muted);">Written Review Notes (Showcased in Community when rated)</label>
              <textarea id="detail-notes" class="field-textarea" style="margin-top:4px;" placeholder="Share your editorial thoughts...">${escapeHtml(a.reviewNotes || '')}</textarea>
            </div>
            <div style="display:flex; justify-content:flex-end; gap:10px;">
              <button class="btn-secondary" id="btn-close-modal-2">Cancel</button>
              <button class="btn-primary" id="btn-save-detail">Save to Library</button>
            </div>
          </div>

          ${
            state.detailRich && state.detailRich.relations.length > 0
              ? `<div>
                  <h4 class="serif-heading" style="font-size:1.25rem; margin-bottom:10px;">Related Seasons, Movies &amp; Spin-Offs</h4>
                  <div style="display:flex; gap:12px; overflow-x:auto; padding-bottom:8px;">
                    ${state.detailRich.relations
                      .map(
                        (rel) => `
                      <div data-open-detail="${rel.id}" style="width:130px; flex-shrink:0; cursor:pointer;">
                        <img src="${escapeHtml(rel.coverImage)}" alt="" style="width:130px;height:180px;object-fit:cover;border-radius:12px;" />
                        <div style="font-size:0.78rem; font-weight:600; margin-top:4px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${escapeHtml(rel.title)}</div>
                      </div>`
                      )
                      .join('')}
                  </div>
                </div>`
              : ''
          }
        </div>
      </div>
    `;
  }

  if (state.showAvatarModal) {
    return `
      <div class="modal-overlay" id="modal-backdrop">
        <div class="modal-panel narrow" onclick="event.stopPropagation()">
          <h3 class="serif-heading" style="font-size:1.6rem;">Choose Built-In Avatar</h3>
          <div style="display:grid; grid-template-columns:repeat(3, 1fr); gap:12px; margin:14px 0;">
            ${BUILT_IN_AVATARS.map(
              (av) => `
              <div class="stat-box" data-pick-avatar="${av.key}" style="cursor:pointer; border-color:${
                state.profile.avatarKey === av.key ? 'var(--accent-primary)' : 'var(--border-subtle)'
              };">
                ${getAvatarBadgeHtml(av.key, av.name, 54)}
                <div style="font-size:0.76rem; margin-top:6px; font-weight:600;">${escapeHtml(av.name)}</div>
              </div>`
            ).join('')}
          </div>
          <button class="btn-secondary" id="btn-close-modal" style="width:100%;">Close</button>
        </div>
      </div>
    `;
  }

  if (state.showProfileEditModal) {
    return `
      <div class="modal-overlay" id="modal-backdrop">
        <div class="modal-panel narrow" onclick="event.stopPropagation()">
          <h3 class="serif-heading" style="font-size:1.6rem;">Edit Profile</h3>
          <label class="micro-label" style="color:var(--text-muted);">Username</label>
          <input id="inp-prof-username" class="field-input" value="${escapeHtml(state.profile.username)}" />
          <label class="micro-label" style="color:var(--text-muted);">Bio</label>
          <textarea id="inp-prof-bio" class="field-textarea">${escapeHtml(state.profile.bio)}</textarea>
          <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:8px;">
            <button class="btn-secondary" id="btn-close-modal">Cancel</button>
            <button class="btn-primary" id="btn-save-profile">Save Profile</button>
          </div>
        </div>
      </div>
    `;
  }

  if (state.showCreateCollectionModal) {
    return `
      <div class="modal-overlay" id="modal-backdrop">
        <div class="modal-panel narrow" onclick="event.stopPropagation()">
          <h3 class="serif-heading" style="font-size:1.6rem;">Create Collection</h3>
          <input id="inp-col-name" class="field-input" placeholder="Collection Name (e.g. Peak Donghua)" />
          <textarea id="inp-col-desc" class="field-textarea" placeholder="Description (optional)"></textarea>
          <div style="display:flex; justify-content:flex-end; gap:10px;">
            <button class="btn-secondary" id="btn-close-modal">Cancel</button>
            <button class="btn-primary" id="btn-save-collection">Create</button>
          </div>
        </div>
      </div>
    `;
  }

  if (state.showQuickSearchModal) {
    return `
      <div class="modal-overlay" id="modal-backdrop">
        <div class="modal-panel narrow" onclick="event.stopPropagation()">
          <h3 class="serif-heading" style="font-size:1.5rem; margin-bottom:8px;">Quick Search (⌘K)</h3>
          <input id="inp-quick-search" class="field-input" placeholder="Type anime title and press Enter..." value="${escapeHtml(state.searchQuery)}" />
          <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:12px;">
            <button class="btn-secondary" id="btn-close-modal">Close</button>
            <button class="btn-primary" id="btn-exec-quick-search">Search in Explore</button>
          </div>
        </div>
      </div>
    `;
  }

  return '';
}

function bindAuthEvents() {
  const inpEmail = document.getElementById('inp-email');
  const inpUser = document.getElementById('inp-user');
  const inpPass = document.getElementById('inp-pass');
  const inpOtp = document.getElementById('inp-otp');
  const inpNewPass = document.getElementById('inp-newpass');

  if (inpEmail) inpEmail.oninput = (e) => { state.email = e.target.value; };
  if (inpUser) inpUser.oninput = (e) => { state.username = e.target.value; };
  if (inpPass) inpPass.oninput = (e) => { state.password = e.target.value; };
  if (inpOtp) {
    inpOtp.oninput = (e) => {
      state.otpCode = e.target.value.replace(/\D/g, '').slice(0, 8);
      e.target.value = state.otpCode;
      const btnSignup = document.getElementById('btn-verify-signup');
      if (btnSignup) btnSignup.disabled = state.otpCode.length !== 8 || state.loading;
      const btnRec = document.getElementById('btn-verify-recovery');
      if (btnRec) btnRec.disabled = state.otpCode.length !== 8 || state.newPassword.length < 6 || state.loading;
    };
  }
  if (inpNewPass) {
    inpNewPass.oninput = (e) => {
      state.newPassword = e.target.value;
      const btnRec = document.getElementById('btn-verify-recovery');
      if (btnRec) btnRec.disabled = state.otpCode.length !== 8 || state.newPassword.length < 6 || state.loading;
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

  const btnToggle = document.getElementById('btn-toggle-mode');
  if (btnToggle) {
    btnToggle.onclick = () => {
      state.authMode = state.authMode === 'signup' ? 'signin' : 'signup';
      state.authError = '';
      state.authInfo = '';
      renderApp();
    };
  }

  const btnBack = document.getElementById('btn-back-signin');
  if (btnBack) {
    btnBack.onclick = () => {
      state.authMode = 'signin';
      state.otpCode = '';
      state.authError = '';
      renderApp();
    };
  }

  const btnForgot = document.getElementById('btn-forgot');
  if (btnForgot) {
    btnForgot.onclick = async () => {
      if (!state.email.trim()) {
        state.authError = 'Enter your email address first to receive an 8-digit recovery code.';
        renderApp();
        return;
      }
      try {
        state.loading = true;
        renderApp();
        await supabaseRequest('/auth/v1/recover', 'POST', { email: state.email.trim() });
        state.authMode = 'otp_recovery';
        state.authInfo = `8-digit recovery code sent to ${state.email.trim()}`;
        state.authError = '';
      } catch (err) {
        state.authError = err.message;
      } finally {
        state.loading = false;
        renderApp();
      }
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
            password: state.password,
            data: { username: state.username.trim() || state.email.split('@')[0] }
          });
          if (data.access_token) {
            completeLogin(data);
          } else {
            state.authMode = 'otp_signup';
            state.authInfo = `Enter the 8-digit verification code sent to ${state.email.trim()}`;
          }
        } else {
          const data = await supabaseRequest('/auth/v1/token?grant_type=password', 'POST', {
            email: state.email.trim(),
            password: state.password
          });
          if (data?.user?.user_metadata?.account_deleted === true) {
            throw new Error('This account has been permanently deleted.');
          }
          completeLogin(data);
        }
      } catch (err) {
        state.authError = err.message;
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const btnVerifySignup = document.getElementById('btn-verify-signup');
  if (btnVerifySignup) {
    btnVerifySignup.onclick = async () => {
      state.loading = true;
      state.authError = '';
      renderApp();
      try {
        const data = await supabaseRequest('/auth/v1/verify', 'POST', {
          email: state.email.trim(),
          token: state.otpCode,
          type: 'signup'
        });
        completeLogin(data);
      } catch (err) {
        state.authError = err.message;
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const btnVerifyRecovery = document.getElementById('btn-verify-recovery');
  if (btnVerifyRecovery) {
    btnVerifyRecovery.onclick = async () => {
      state.loading = true;
      state.authError = '';
      renderApp();
      try {
        const data = await supabaseRequest('/auth/v1/verify', 'POST', {
          email: state.email.trim(),
          token: state.otpCode,
          type: 'recovery'
        });
        completeLogin(data);
        await supabaseRequest('/auth/v1/user', 'PUT', { password: state.newPassword });
      } catch (err) {
        state.authError = err.message;
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }
}

function completeLogin(data) {
  const user = data.user || {};
  const email = user.email || state.email.trim();
  const username = user.user_metadata?.username || email.split('@')[0] || 'Member';
  saveSession({
    accessToken: data.access_token,
    refreshToken: data.refresh_token || '',
    userId: user.id,
    email,
    username
  });
  state.profile.username = username;
  loadCloudData();
}

function bindWorkspaceEvents() {
  document.querySelectorAll('[data-nav]').forEach((el) => {
    el.onclick = () => {
      state.tab = el.getAttribute('data-nav');
      renderApp();
    };
  });

  const btnSidebarTheme = document.getElementById('btn-sidebar-theme');
  if (btnSidebarTheme) {
    btnSidebarTheme.onclick = () => {
      const order = ['DARK', 'LIGHT', 'SYSTEM'];
      state.prefs.themeMode = order[(order.indexOf(state.prefs.themeMode) + 1) % order.length];
      savePrefs();
      renderApp();
    };
  }

  const btnQuickSearch = document.getElementById('btn-open-quick-search');
  if (btnQuickSearch) {
    btnQuickSearch.onclick = () => {
      state.showQuickSearchModal = true;
      renderApp();
      const inp = document.getElementById('inp-quick-search');
      if (inp) inp.focus();
    };
  }

  const btnInstall = document.getElementById('btn-pwa-install');
  if (btnInstall && deferredInstallPrompt) {
    btnInstall.onclick = async () => {
      deferredInstallPrompt.prompt();
      await deferredInstallPrompt.userChoice;
      deferredInstallPrompt = null;
      renderApp();
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

  document.querySelectorAll('[data-inc-id]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-inc-id'));
      const item = state.vault.find((v) => v.animeId === id);
      if (!item) return;
      const nextEp = (item.watchedEpisodes || 0) + 1;
      item.watchedEpisodes = item.totalEpisodes ? Math.min(item.totalEpisodes, nextEp) : nextEp;
      if (item.totalEpisodes && item.watchedEpisodes >= item.totalEpisodes) {
        item.status = 'completed';
      }
      renderApp();
      await saveVaultEntry(item);
    };
  });

  document.querySelectorAll('[data-open-detail]').forEach((el) => {
    el.onclick = () => {
      const id = Number(el.getAttribute('data-open-detail'));
      openAnimeDetail(id);
    };
  });

  const inpDiscSearch = document.getElementById('inp-discover-search');
  if (inpDiscSearch) {
    inpDiscSearch.onchange = (e) => {
      state.searchQuery = e.target.value;
      fetchDiscoverCatalog();
    };
  }

  document.querySelectorAll('[data-disc-mode]').forEach((btn) => {
    btn.onclick = () => {
      state.discoverMode = btn.getAttribute('data-disc-mode');
      state.discoverSort = btn.getAttribute('data-disc-sort') || 'TRENDING_DESC';
      if (state.discoverMode === 'airing') state.discoverStatus = 'RELEASING';
      else state.discoverStatus = 'ALL';
      fetchDiscoverCatalog();
    };
  });

  const selFormat = document.getElementById('sel-disc-format');
  if (selFormat) selFormat.onchange = (e) => { state.discoverFormat = e.target.value; fetchDiscoverCatalog(); };

  const selStatus = document.getElementById('sel-disc-status');
  if (selStatus) selStatus.onchange = (e) => { state.discoverStatus = e.target.value; fetchDiscoverCatalog(); };

  const selSort = document.getElementById('sel-disc-sort');
  if (selSort) selSort.onchange = (e) => { state.discoverSort = e.target.value; fetchDiscoverCatalog(); };

  document.querySelectorAll('[data-disc-genre]').forEach((btn) => {
    btn.onclick = () => {
      const g = btn.getAttribute('data-disc-genre');
      state.discoverGenre = g || null;
      fetchDiscoverCatalog();
    };
  });

  const btnToggleLayout = document.getElementById('btn-toggle-layout');
  if (btnToggleLayout) {
    btnToggleLayout.onclick = () => {
      state.prefs.watchlistLayout = state.prefs.watchlistLayout === 'GRID' ? 'LIST' : 'GRID';
      savePrefs();
      renderApp();
    };
  }

  const inpMyListSearch = document.getElementById('inp-mylist-search');
  if (inpMyListSearch) {
    inpMyListSearch.oninput = (e) => {
      state.myListQuery = e.target.value;
      renderApp();
    };
  }

  const selMyListSort = document.getElementById('sel-mylist-sort');
  if (selMyListSort) {
    selMyListSort.onchange = (e) => {
      state.myListSort = e.target.value;
      renderApp();
    };
  }

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

  document.querySelectorAll('[data-mylist-col]').forEach((btn) => {
    btn.onclick = () => {
      state.myListCollectionFilter = btn.getAttribute('data-mylist-col') || null;
      renderApp();
    };
  });

  const btnCreateCol = document.getElementById('btn-create-collection');
  if (btnCreateCol) {
    btnCreateCol.onclick = () => {
      state.showCreateCollectionModal = true;
      renderApp();
    };
  }

  const btnJoin = document.getElementById('btn-join-community');
  if (btnJoin) {
    btnJoin.onclick = async () => {
      try {
        await supabaseRequest('/rest/v1/rpc/join_community', 'POST', {
          p_username: state.profile.username || 'Member',
          p_avatar_key: state.profile.avatarKey || 'avatar_1',
          p_code: ''
        });
      } catch (_) {
        if (state.session) {
          await supabaseRequest(
            '/rest/v1/community_members?on_conflict=user_id',
            'POST',
            { user_id: state.session.userId, status: 'active', role: 'member' },
            'resolution=merge-duplicates,return=minimal'
          ).catch(() => {});
        }
      }
      state.profile.memberStatus = 'active';
      await loadCommunityReviews();
      renderApp();
    };
  }

  const btnRefreshComm = document.getElementById('btn-refresh-community');
  if (btnRefreshComm) btnRefreshComm.onclick = () => loadCommunityReviews();

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

  document.querySelectorAll('[data-like-review]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const reviewId = btn.getAttribute('data-like-review');
      if (reviewId) await toggleReviewLike(reviewId);
    };
  });

  const btnRefreshStorage = document.getElementById('btn-refresh-storage');
  if (btnRefreshStorage) {
    btnRefreshStorage.onclick = () => loadStorageStats();
  }

  const openAv1 = document.getElementById('btn-open-avatar-modal');
  const openAv2 = document.getElementById('btn-open-avatar-modal-2');
  if (openAv1) openAv1.onclick = () => { state.showAvatarModal = true; renderApp(); };
  if (openAv2) openAv2.onclick = () => { state.showAvatarModal = true; renderApp(); };

  const openProfEdit = document.getElementById('btn-open-profile-edit');
  if (openProfEdit) openProfEdit.onclick = () => { state.showProfileEditModal = true; renderApp(); };

  document.querySelectorAll('[data-set-theme]').forEach((btn) => {
    btn.onclick = () => {
      state.prefs.themeMode = btn.getAttribute('data-set-theme');
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

  document.querySelectorAll('[data-set-vis]').forEach((btn) => {
    btn.onclick = () => {
      state.prefs.reviewsVisibility = btn.getAttribute('data-set-vis');
      savePrefs();
      renderApp();
    };
  });

  const btnExportJson = document.getElementById('btn-export-json');
  if (btnExportJson) {
    btnExportJson.onclick = () => {
      const payload = JSON.stringify({ version: 2, exportedAt: new Date().toISOString(), watchlist: state.vault, collections: state.collections }, null, 2);
      const blob = new Blob([payload], { type: 'application/json' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `anime-universe-backup-${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
    };
  }

  const inpImportJson = document.getElementById('inp-import-json');
  if (inpImportJson) {
    inpImportJson.onchange = async (e) => {
      const file = e.target.files?.[0];
      if (!file) return;
      try {
        const text = await file.text();
        const parsed = JSON.parse(text);
        if (Array.isArray(parsed.watchlist)) {
          state.vault = parsed.watchlist;
          saveLocalVault();
          renderApp();
        }
      } catch (_) {}
    };
  }

  const btnSyncNow = document.getElementById('btn-sync-now');
  if (btnSyncNow) btnSyncNow.onclick = () => loadCloudData();

  const btnSignOut = document.getElementById('btn-signout');
  if (btnSignOut) {
    btnSignOut.onclick = () => {
      saveSession(null);
      renderApp();
    };
  }

  const btnExitGuest = document.getElementById('btn-exit-guest');
  if (btnExitGuest) {
    btnExitGuest.onclick = () => {
      state.isGuestMode = false;
      localStorage.removeItem('anime_universe_guest');
      renderApp();
    };
  }

  const btnDelete = document.getElementById('btn-delete-account');
  if (btnDelete) {
    btnDelete.onclick = async () => {
      if (!confirm('Permanently delete your Anime Universe account, library, and community reviews? This cannot be undone.')) return;
      try {
        const uid = encodeURIComponent(state.session.userId);
        await supabaseRequest('/rest/v1/rpc/delete_my_account', 'POST', {}).catch(() => {});
        await supabaseRequest(`/rest/v1/user_anime?user_id=eq.${uid}`, 'DELETE').catch(() => {});
        await supabaseRequest(`/rest/v1/user_anime_entries?user_id=eq.${uid}`, 'DELETE').catch(() => {});
        await supabaseRequest(`/rest/v1/profiles?id=eq.${uid}`, 'DELETE').catch(() => {});
        const rnd = Array.from(crypto.getRandomValues(new Uint8Array(16)))
          .map((b) => b.toString(16).padStart(2, '0'))
          .join('');
        await supabaseRequest('/auth/v1/user', 'PUT', {
          password: `Del!${rnd}9A#`,
          data: { account_deleted: true }
        }).catch(() => {});
      } finally {
        saveSession(null);
        renderApp();
      }
    };
  }

  const backdrop = document.getElementById('modal-backdrop');
  const close1 = document.getElementById('btn-close-modal');
  const close2 = document.getElementById('btn-close-modal-2');
  const closeFn = () => {
    state.detailAnime = null;
    state.showAvatarModal = false;
    state.showProfileEditModal = false;
    state.showCreateCollectionModal = false;
    state.showQuickSearchModal = false;
    renderApp();
  };
  if (backdrop) backdrop.onclick = closeFn;
  if (close1) close1.onclick = closeFn;
  if (close2) close2.onclick = closeFn;

  const btnDetailPin = document.getElementById('btn-detail-pin');
  if (btnDetailPin && state.detailAnime) {
    btnDetailPin.onclick = () => {
      state.detailAnime.isPinned = !state.detailAnime.isPinned;
      renderApp();
    };
  }

  const btnDetailFav = document.getElementById('btn-detail-fav');
  if (btnDetailFav && state.detailAnime) {
    btnDetailFav.onclick = () => {
      state.detailAnime.isFavorite = !state.detailAnime.isFavorite;
      renderApp();
    };
  }

  const btnSaveDetail = document.getElementById('btn-save-detail');
  if (btnSaveDetail && state.detailAnime) {
    btnSaveDetail.onclick = async () => {
      const st = document.getElementById('detail-status').value;
      const season = Math.max(1, parseInt(document.getElementById('detail-season').value || '1', 10));
      const eps = Math.max(0, parseInt(document.getElementById('detail-eps').value || '0', 10));
      const stars = Math.min(5, Math.max(0, parseFloat(document.getElementById('detail-stars').value || '0')));
      const notes = document.getElementById('detail-notes').value.trim().slice(0, 500);

      const updated = {
        ...state.detailAnime,
        status: st,
        season,
        watchedEpisodes: eps,
        rating10: stars > 0 ? stars * 2.0 : null,
        reviewNotes: notes
      };
      state.detailAnime = null;
      await saveVaultEntry(updated);
      renderApp();
    };
  }

  document.querySelectorAll('[data-pick-avatar]').forEach((el) => {
    el.onclick = async () => {
      const key = el.getAttribute('data-pick-avatar');
      state.profile.avatarKey = key;
      state.showAvatarModal = false;
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
      const uname = (document.getElementById('inp-prof-username').value.trim() || state.profile.username).slice(0, 40);
      const bio = document.getElementById('inp-prof-bio').value.trim().slice(0, 160);
      state.profile.username = uname;
      state.profile.bio = bio;
      state.showProfileEditModal = false;
      renderApp();
      if (state.session) {
        const uid = encodeURIComponent(state.session.userId);
        await supabaseRequest(`/rest/v1/profiles?id=eq.${uid}`, 'PATCH', { username: uname, bio }).catch(() => {});
      }
    };
  }

  const btnSaveCol = document.getElementById('btn-save-collection');
  if (btnSaveCol) {
    btnSaveCol.onclick = () => {
      const name = document.getElementById('inp-col-name').value.trim();
      const description = document.getElementById('inp-col-desc').value.trim();
      if (!name) return;
      state.collections.push({
        id: `col_${Date.now()}`,
        name,
        description,
        animeIds: []
      });
      saveCollections();
      state.showCreateCollectionModal = false;
      renderApp();
    };
  }

  const btnExecQuick = document.getElementById('btn-exec-quick-search');
  if (btnExecQuick) {
    btnExecQuick.onclick = () => {
      const q = document.getElementById('inp-quick-search').value.trim();
      state.searchQuery = q;
      state.showQuickSearchModal = false;
      state.tab = 'discover';
      fetchDiscoverCatalog();
    };
  }
}

renderApp();
fetchDiscoverCatalog();
loadStorageStats();
if (state.session) {
  loadCloudData();
}
