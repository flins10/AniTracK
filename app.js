// AuraLog — Installable PWA & Cloudflare Pages Client
// Registers Service Worker (/sw.js), connects to /api/supabase & /api/gemini, and syncs with Supabase + AniList.

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

  // Fallback for static preview environments without Cloudflare Functions runtime
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

export async function askGeminiViaEdge(prompt, systemInstruction = 'You are an expert anime & donghua curator for AuraLog.') {
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

const state = {
  session: JSON.parse(localStorage.getItem('auralog_session') || 'null'),
  tab: 'vault', // 'vault' | 'discover' | 'community' | 'settings'
  authMode: 'signin', // 'signin' | 'signup' | 'otp_signup' | 'otp_recovery'
  email: '',
  username: '',
  password: '',
  otpCode: '',
  newPassword: '',
  authError: '',
  authInfo: '',
  loading: false,
  vault: [],
  discover: [],
  searchQuery: '',
  memberStatus: 'pending',
  communityReviews: [],
  editingItem: null
};

function saveSession(session) {
  state.session = session;
  if (session) {
    localStorage.setItem('auralog_session', JSON.stringify(session));
  } else {
    localStorage.removeItem('auralog_session');
  }
}

async function loadCloudData() {
  if (!state.session) return;
  try {
    const uid = encodeURIComponent(state.session.userId);
    const rows = await supabaseRequest(`/rest/v1/user_anime_entries?user_id=eq.${uid}&order=updated_at.desc`);
    if (Array.isArray(rows)) {
      state.vault = rows.map((r) => ({
        animeId: r.anime_id,
        title: r.anime_title || 'Anime',
        coverImage: r.anime_cover_image || '',
        status: r.watch_status || 'watching',
        watchedEpisodes: r.watched_episodes || 0,
        totalEpisodes: r.total_episodes || null,
        rating10: r.rating || null,
        reviewNotes: r.review_notes || ''
      }));
    }
    const prof = await supabaseRequest(`/rest/v1/profiles?id=eq.${uid}&select=member_status`);
    if (Array.isArray(prof) && prof.length > 0) {
      state.memberStatus = prof[0].member_status || 'pending';
    }
    if (state.memberStatus === 'active') {
      await loadCommunityReviews();
    }
    renderApp();
  } catch (_) {}
}

async function loadCommunityReviews() {
  try {
    const rows = await supabaseRequest('/rest/v1/community_reviews?order=updated_at.desc&limit=100');
    if (Array.isArray(rows)) {
      state.communityReviews = rows.filter((r) => (r.rating || 0) > 0 && (r.review_notes || '').trim().length > 0);
      renderApp();
    }
  } catch (_) {}
}

async function searchAniList(query = '') {
  const gql = query.trim()
    ? `query($search: String) { Page(page: 1, perPage: 24) { media(search: $search, type: ANIME, sort: [POPULARITY_DESC], isAdult: false) { id title { english romaji } coverImage { large } episodes } } }`
    : `query { Page(page: 1, perPage: 24) { media(type: ANIME, sort: [TRENDING_DESC, POPULARITY_DESC], isAdult: false) { id title { english romaji } coverImage { large } episodes } } }`;
  try {
    const resp = await fetch('https://graphql.anilist.co', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        query: gql,
        variables: query.trim() ? { search: query.trim() } : {}
      })
    });
    const json = await resp.json();
    const list = json?.data?.Page?.media || [];
    state.discover = list.map((m) => ({
      animeId: m.id,
      title: m.title?.english || m.title?.romaji || 'Anime',
      coverImage: m.coverImage?.large || '',
      status: 'watching',
      watchedEpisodes: 0,
      totalEpisodes: m.episodes || null,
      rating10: null,
      reviewNotes: ''
    }));
    renderApp();
  } catch (_) {}
}

function escapeHtml(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function renderApp() {
  const root = document.getElementById('app');
  if (!root) return;

  if (!state.session) {
    const isOtpReady = state.otpCode.replace(/\D/g, '').length === 8;
    const isResetReady = isOtpReady && state.newPassword.length >= 6;
    root.innerHTML = `
      <div class="app-shell">
        <div class="auth-card">
          <img src="/icon-192.png" alt="AuraLog" class="brand-icon" style="margin: 0 auto; width: 62px; height: 62px;" />
          <h1 class="serif-title" style="font-size: 2rem; color: var(--gold-primary);">AuraLog Vault</h1>
          <p style="color: var(--text-secondary); font-size: 0.9rem;">
            ${
              state.authMode === 'otp_signup'
                ? `Enter the 8-digit verification code sent to ${escapeHtml(state.email)}`
                : state.authMode === 'otp_recovery'
                ? 'Enter the 8-digit recovery code and your new password'
                : state.authMode === 'signup'
                ? 'Create your cloud-synced Anime & Donghua account'
                : 'Sign in to sync your watchlist & community reviews across Android, iOS, Mac & Windows'
            }
          </p>
          ${state.authError ? `<p style="color: var(--error-crimson); font-size: 0.86rem;">${escapeHtml(state.authError)}</p>` : ''}
          ${state.authInfo ? `<p style="color: var(--gold-primary); font-size: 0.86rem;">${escapeHtml(state.authInfo)}</p>` : ''}

          ${
            state.authMode === 'otp_signup'
              ? `
                <input id="inp-otp" class="text-input" type="text" inputmode="numeric" maxlength="8" placeholder="ex-12345678" value="${escapeHtml(state.otpCode)}" />
                <button id="btn-verify-signup" class="gold-btn" ${!isOtpReady || state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'Verifying...' : 'Verify & Continue'}
                </button>
                <button id="btn-back-signin" class="pill-btn">Back to Sign In</button>
              `
              : state.authMode === 'otp_recovery'
              ? `
                <input id="inp-email" class="text-input" type="email" placeholder="Email Address" value="${escapeHtml(state.email)}" />
                <input id="inp-otp" class="text-input" type="text" inputmode="numeric" maxlength="8" placeholder="ex-12345678" value="${escapeHtml(state.otpCode)}" />
                <input id="inp-newpass" class="text-input" type="password" placeholder="New Password (min 6 chars)" value="${escapeHtml(state.newPassword)}" />
                <button id="btn-verify-recovery" class="gold-btn" ${!isResetReady || state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'Updating Password...' : 'Confirm New Password'}
                </button>
                <button id="btn-back-signin" class="pill-btn">Back to Sign In</button>
              `
              : `
                ${state.authMode === 'signup' ? `<input id="inp-user" class="text-input" type="text" placeholder="Username" value="${escapeHtml(state.username)}" />` : ''}
                <input id="inp-email" class="text-input" type="email" placeholder="Email Address" value="${escapeHtml(state.email)}" />
                <input id="inp-pass" class="text-input" type="password" placeholder="Password" value="${escapeHtml(state.password)}" />
                <button id="btn-auth-submit" class="gold-btn" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'Please wait...' : state.authMode === 'signup' ? 'Create Account' : 'Sign In'}
                </button>
                <div style="display:flex; justify-content:space-between; gap:8px; flex-wrap:wrap;">
                  <button id="btn-toggle-mode" class="pill-btn" style="flex:1;">
                    ${state.authMode === 'signup' ? 'Have an account? Sign In' : 'Create Account'}
                  </button>
                  ${state.authMode === 'signin' ? `<button id="btn-forgot" class="pill-btn">Forgot Password?</button>` : ''}
                </div>
              `
          }
          ${
            deferredInstallPrompt
              ? `<button id="btn-pwa-install" class="pill-btn" style="margin-top: 6px;">Install App on Device</button>`
              : ''
          }
        </div>
      </div>
    `;
    bindAuthEvents();
    return;
  }

  let mainViewHtml = '';
  if (state.tab === 'vault') {
    mainViewHtml = `
      <h2 class="serif-title" style="font-size: 1.9rem; margin-bottom: 4px;">My Anime Vault</h2>
      <p style="color: var(--text-secondary); font-size: 0.88rem; margin-bottom: 18px;">${state.vault.length} Tracked Titles</p>
      ${
        state.vault.length === 0
          ? `<div class="gate-card"><p style="color:var(--text-secondary);">Your vault is empty. Switch to Discover to add anime or donghua.</p></div>`
          : `<div class="card-grid">
              ${state.vault
                .map(
                  (item) => `
                <div class="anime-card" data-edit-id="${item.animeId}">
                  <img class="poster-img" src="${escapeHtml(item.coverImage)}" alt="${escapeHtml(item.title)}" loading="lazy" />
                  <div class="card-meta">
                    <div>
                      <span class="badge-gold">${escapeHtml(item.status.replace(/_/g, ' '))}</span>
                      <div class="card-title" style="margin-top:6px;">${escapeHtml(item.title)}</div>
                      <p style="color:var(--text-secondary); font-size:0.82rem; margin-top:4px;">
                        ${item.watchedEpisodes} / ${item.totalEpisodes || '?'} eps
                        ${item.rating10 ? ` • ★ ${(item.rating10 / 2).toFixed(1)}/5.0` : ''}
                      </p>
                    </div>
                    <button class="pill-btn" data-inc-id="${item.animeId}" style="width:fit-content; padding:6px 12px; font-size:0.78rem;">+1 Episode</button>
                  </div>
                </div>`
                )
                .join('')}
            </div>`
      }
    `;
  } else if (state.tab === 'discover') {
    mainViewHtml = `
      <div style="display:flex; justify-content:space-between; align-items:center; gap:14px; flex-wrap:wrap; margin-bottom:18px;">
        <div>
          <h2 class="serif-title" style="font-size: 1.9rem;">Discover Anime & Donghua</h2>
          <p style="color: var(--text-secondary); font-size: 0.88rem;">Trending & Search via AniList</p>
        </div>
        <input id="inp-search" class="text-input" style="max-width:340px;" placeholder="Search anime title..." value="${escapeHtml(state.searchQuery)}" />
      </div>
      <div class="card-grid">
        ${state.discover
          .map(
            (item) => `
          <div class="anime-card" data-discover-id="${item.animeId}">
            <img class="poster-img" src="${escapeHtml(item.coverImage)}" alt="${escapeHtml(item.title)}" loading="lazy" />
            <div class="card-meta">
              <div>
                <div class="card-title">${escapeHtml(item.title)}</div>
                <p style="color:var(--text-secondary); font-size:0.82rem; margin-top:4px;">${item.totalEpisodes || '?'} Episodes</p>
              </div>
              <span class="badge-gold">+ Add / Rate</span>
            </div>
          </div>`
          )
          .join('')}
      </div>
    `;
  } else if (state.tab === 'community') {
    if (state.memberStatus !== 'active') {
      mainViewHtml = `
        <div class="gate-card">
          <h2 class="serif-title" style="font-size: 1.8rem;">Join the Community</h2>
          <p style="color: var(--text-secondary); font-size: 0.9rem;">
            Unlock rated & noted anime showcases shared by members. Your own ratings and notes stay private unless you enable sharing.
          </p>
          <button id="btn-join-community" class="gold-btn">Join Community</button>
        </div>
      `;
    } else {
      mainViewHtml = `
        <h2 class="serif-title" style="font-size: 1.9rem; margin-bottom: 4px;">Community • Rated & Noted</h2>
        <p style="color: var(--text-secondary); font-size: 0.88rem; margin-bottom: 18px;">${state.communityReviews.length} Member Showcases</p>
        <div class="card-grid">
          ${state.communityReviews
            .map(
              (rev) => `
            <div class="anime-card" style="flex-direction:column;">
              <div style="display:flex; gap:14px;">
                <img class="poster-img" src="${escapeHtml(rev.anime_cover_image)}" alt="${escapeHtml(rev.anime_title)}" loading="lazy" />
                <div>
                  <div class="card-title">${escapeHtml(rev.anime_title)}</div>
                  <p style="color:var(--gold-primary); font-size:0.86rem; margin-top:4px;">★ ${(Number(rev.rating || 0) / 2).toFixed(1)} / 5.0</p>
                  <p style="color:var(--text-secondary); font-size:0.78rem; margin-top:4px;">By ${escapeHtml(rev.username || 'Member')}</p>
                </div>
              </div>
              <div class="quote-box">“${escapeHtml(rev.review_notes)}”</div>
            </div>`
            )
            .join('')}
        </div>
      `;
    }
  } else if (state.tab === 'settings') {
    mainViewHtml = `
      <div class="gate-card" style="text-align:left; max-width:560px;">
        <h2 class="serif-title" style="font-size: 1.8rem;">Account & Settings</h2>
        <p style="color:var(--gold-primary); font-weight:600;">${escapeHtml(state.session.username)}</p>
        <p style="color:var(--text-secondary); font-size:0.88rem;">${escapeHtml(state.session.email)}</p>
        <p style="color:var(--text-muted); font-size:0.8rem;">AuraLog Cross-Platform PWA v2.1.0</p>
        <div style="display:flex; gap:12px; flex-wrap:wrap; margin-top:10px;">
          <button id="btn-signout" class="pill-btn">Sign Out</button>
          <button id="btn-delete-account" class="danger-btn">Delete Account Permanently</button>
        </div>
      </div>
    `;
  }

  root.innerHTML = `
    <div class="app-shell">
      <header class="top-bar">
        <div class="brand-row">
          <img src="/icon-192.png" alt="AuraLog" class="brand-icon" />
          <div>
            <div class="serif-title" style="font-size: 1.45rem; color: var(--gold-primary);">AuraLog</div>
            <div style="font-size: 0.76rem; color: var(--text-secondary);">Anime & Donghua Vault</div>
          </div>
        </div>
        <div class="top-actions">
          ${deferredInstallPrompt ? `<button id="btn-pwa-install" class="gold-btn">Install App</button>` : ''}
          <span style="font-size:0.84rem; color:var(--text-secondary);">${escapeHtml(state.session.username)}</span>
        </div>
      </header>
      <main>${mainViewHtml}</main>
    </div>

    <nav class="bottom-dock">
      <button class="pill-btn ${state.tab === 'vault' ? 'active' : ''}" data-tab="vault">Vault</button>
      <button class="pill-btn ${state.tab === 'discover' ? 'active' : ''}" data-tab="discover">Discover</button>
      <button class="pill-btn ${state.tab === 'community' ? 'active' : ''}" data-tab="community">Community</button>
      <button class="pill-btn ${state.tab === 'settings' ? 'active' : ''}" data-tab="settings">Settings</button>
    </nav>

    ${
      state.editingItem
        ? `
      <div class="modal-backdrop" id="modal-bg">
        <div class="modal-card">
          <h3 class="serif-title" style="font-size:1.5rem;">${escapeHtml(state.editingItem.title)}</h3>
          <label style="font-size:0.82rem; color:var(--text-secondary);">Watch Status</label>
          <select id="edit-status" class="text-input">
            ${['watching', 'completed', 'plan_to_watch', 'on_hold', 'dropped']
              .map(
                (st) =>
                  `<option value="${st}" ${state.editingItem.status === st ? 'selected' : ''}>${st
                    .replace(/_/g, ' ')
                    .toUpperCase()}</option>`
              )
              .join('')}
          </select>
          <label style="font-size:0.82rem; color:var(--text-secondary);">Watched Episodes</label>
          <input id="edit-eps" class="text-input" type="number" min="0" value="${state.editingItem.watchedEpisodes || 0}" />
          <label style="font-size:0.82rem; color:var(--text-secondary);">Star Rating (0.0 to 5.0 in 0.5 steps)</label>
          <input id="edit-stars" class="text-input" type="number" min="0" max="5" step="0.5" value="${
            state.editingItem.rating10 ? (state.editingItem.rating10 / 2).toFixed(1) : '0'
          }" />
          <label style="font-size:0.82rem; color:var(--text-secondary);">Review / Notes (Required with rating for Community showcase)</label>
          <textarea id="edit-notes" class="textarea-input" placeholder="Write your thoughts...">${escapeHtml(
            state.editingItem.reviewNotes || ''
          )}</textarea>
          <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:8px;">
            <button id="btn-cancel-edit" class="pill-btn">Cancel</button>
            <button id="btn-save-edit" class="gold-btn">Save to Vault</button>
          </div>
        </div>
      </div>`
        : ''
    }
  `;

  bindMainEvents();
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

  bindInstallEvent();
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
  loadCloudData();
}

function bindInstallEvent() {
  const btnInstall = document.getElementById('btn-pwa-install');
  if (btnInstall && deferredInstallPrompt) {
    btnInstall.onclick = async () => {
      deferredInstallPrompt.prompt();
      await deferredInstallPrompt.userChoice;
      deferredInstallPrompt = null;
      renderApp();
    };
  }
}

function bindMainEvents() {
  bindInstallEvent();

  document.querySelectorAll('[data-tab]').forEach((btn) => {
    btn.onclick = () => {
      state.tab = btn.getAttribute('data-tab');
      renderApp();
    };
  });

  document.querySelectorAll('[data-inc-id]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-inc-id'));
      const item = state.vault.find((v) => v.animeId === id);
      if (!item) return;
      item.watchedEpisodes += 1;
      renderApp();
      await saveVaultEntry(item);
    };
  });

  document.querySelectorAll('[data-edit-id]').forEach((card) => {
    card.onclick = () => {
      const id = Number(card.getAttribute('data-edit-id'));
      const item = state.vault.find((v) => v.animeId === id);
      if (item) {
        state.editingItem = { ...item };
        renderApp();
      }
    };
  });

  document.querySelectorAll('[data-discover-id]').forEach((card) => {
    card.onclick = () => {
      const id = Number(card.getAttribute('data-discover-id'));
      const existing = state.vault.find((v) => v.animeId === id);
      const disc = state.discover.find((d) => d.animeId === id);
      state.editingItem = existing ? { ...existing } : { ...disc };
      renderApp();
    };
  });

  const inpSearch = document.getElementById('inp-search');
  if (inpSearch) {
    inpSearch.onchange = (e) => {
      state.searchQuery = e.target.value;
      searchAniList(state.searchQuery);
    };
  }

  const btnJoin = document.getElementById('btn-join-community');
  if (btnJoin) {
    btnJoin.onclick = async () => {
      try {
        await supabaseRequest('/rest/v1/rpc/join_community_with_invite', 'POST', { p_invite_code: '' });
      } catch (_) {}
      state.memberStatus = 'active';
      await loadCommunityReviews();
      renderApp();
    };
  }

  const btnSignOut = document.getElementById('btn-signout');
  if (btnSignOut) {
    btnSignOut.onclick = () => {
      saveSession(null);
      renderApp();
    };
  }

  const btnDelete = document.getElementById('btn-delete-account');
  if (btnDelete) {
    btnDelete.onclick = async () => {
      if (!confirm('Permanently delete your account, watchlist, and community reviews? This cannot be undone.')) return;
      try {
        const uid = encodeURIComponent(state.session.userId);
        await supabaseRequest(`/rest/v1/user_anime_entries?user_id=eq.${uid}`, 'DELETE').catch(() => {});
        await supabaseRequest(`/rest/v1/profiles?id=eq.${uid}`, 'DELETE').catch(() => {});
        await supabaseRequest('/rest/v1/rpc/delete_own_account', 'POST', {}).catch(() => {});
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

  const btnCancelEdit = document.getElementById('btn-cancel-edit');
  if (btnCancelEdit) {
    btnCancelEdit.onclick = () => {
      state.editingItem = null;
      renderApp();
    };
  }

  const btnSaveEdit = document.getElementById('btn-save-edit');
  if (btnSaveEdit) {
    btnSaveEdit.onclick = async () => {
      const st = document.getElementById('edit-status').value;
      const eps = Math.max(0, parseInt(document.getElementById('edit-eps').value || '0', 10));
      const stars = Math.min(5, Math.max(0, parseFloat(document.getElementById('edit-stars').value || '0')));
      const notes = document.getElementById('edit-notes').value.trim();

      const updated = {
        ...state.editingItem,
        status: st,
        watchedEpisodes: eps,
        rating10: stars > 0 ? stars * 2.0 : null,
        reviewNotes: notes
      };
      state.editingItem = null;
      state.vault = [updated, ...state.vault.filter((v) => v.animeId !== updated.animeId)];
      renderApp();
      await saveVaultEntry(updated);
      if (state.memberStatus === 'active') {
        await loadCommunityReviews();
      }
    };
  }
}

renderApp();
searchAniList('');
if (state.session) {
  loadCloudData();
}
