const CACHE_NAME = 'anime-universe-pwa-v2.5.0';
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/styles.css',
  '/app.js',
  '/manifest.json',
  '/icon-192.png',
  '/icon-512.png',
  '/assets/img_login_mascot.png',
  '/assets/avatar_1.png',
  '/assets/avatar_2.png',
  '/assets/avatar_3.png',
  '/assets/avatar_4.png',
  '/assets/avatar_5.png',
  '/assets/avatar_6.png',
  '/assets/avatar_7.png',
  '/assets/avatar_8.png',
  '/assets/avatar_9.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(STATIC_ASSETS)).catch(() => {})
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))
      )
    )
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  if (
    url.pathname.startsWith('/api/') ||
    url.pathname.startsWith('/downloads/') ||
    url.hostname.includes('supabase.co') ||
    url.hostname.includes('anilist.co')
  ) {
    return;
  }

  event.respondWith(
    fetch(request)
      .then((networkResponse) => {
        if (networkResponse && networkResponse.status === 200 && url.origin === self.location.origin) {
          const ct = (networkResponse.headers.get('content-type') || '').toLowerCase();
          const isAssetPath = /\.(png|jpg|jpeg|webp|svg|gif|css|js|json|woff2?)$/i.test(url.pathname);
          if (!(isAssetPath && ct.includes('text/html'))) {
            const clone = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(request, clone)).catch(() => {});
          }
        }
        return networkResponse;
      })
      .catch(() => caches.match(request).then((cached) => cached || caches.match('/index.html')))
  );
});
