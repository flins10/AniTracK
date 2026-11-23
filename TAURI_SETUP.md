# ANIME UNIVERSE — Tauri v2 Native App Setup

This repository is pre-configured with **Tauri v2** to wrap `https://animeuniverse.pages.dev` as a native **Android (`.apk`)**, **Windows (`.exe` / `.msi`)**, and **macOS (`.dmg`)** application. Because the native shell loads your live Cloudflare Pages deployment directly (with `no-cache, no-store, must-revalidate` on `/index.html`), every future Cloudflare push updates all installed apps automatically with zero rebuilds.

---

## 1. App Icon (`1024x1024`)

Place your high-resolution **`1024x1024` PNG** app icon at:

```text
src-tauri/icons/icon.png
```

> A default copy of the Anime Universe app icon is already seeded at `src-tauri/icons/icon.png` so builds work immediately. Replace it anytime with a `1024x1024` transparent/full-bleed PNG — GitHub Actions automatically runs `npx tauri icon src-tauri/icons/icon.png` to generate all `.ico`, `.icns`, and Android adaptive icons.

---

## 2. Build & Download Installers via GitHub Actions

1. Push your latest commit to GitHub.
2. Open your repository on GitHub and click the **Actions** tab.
3. Select **Build Tauri v2 Native Apps (Android, Windows, macOS)** in the left sidebar.
4. Click **Run workflow** (optionally enter a custom release tag, or leave it blank to auto-generate one) and press **Run workflow**.
5. Wait for all 3 jobs (`windows-latest`, `macos-latest`, and `ubuntu-latest` for Android) to finish.
6. Open your repository's **Releases** page to download:
   - **Android:** `Anime-Universe-Android.apk`
   - **Windows:** `.exe` (NSIS installer) and `.msi`
   - **macOS:** `.dmg` (Universal Apple Silicon + Intel)

---

## 3. Local Development (Optional)

```bash
npm install
npm run tauri dev
```
