use serde::{Deserialize, Serialize};
use std::collections::HashSet;
use std::fs;
use std::path::PathBuf;
use std::time::{SystemTime, UNIX_EPOCH};
use tauri::{AppHandle, Manager};
#[allow(unused_imports)]
use tauri_plugin_notification::{NotificationExt, PermissionState};

const APP_AUMID: &str = "com.animeuniverse.app";
const APP_DISPLAY_NAME: &str = "Anime Universe";

#[cfg(target_os = "macos")]
mod macos_notif {
    use std::ffi::CString;
    use std::os::raw::{c_char, c_int};

    extern "C" {
        fn au_macos_init_notification_delegate(pending_click_path: *const c_char);
        fn au_macos_check_permission() -> c_int;
        fn au_macos_request_permission() -> c_int;
        fn au_macos_send_notification(
            notif_id: *const c_char,
            title: *const c_char,
            body: *const c_char,
        ) -> c_int;
        fn au_macos_open_notification_settings() -> c_int;
    }

    pub fn init_delegate(pending_click_path: Option<&str>) {
        let c_path = pending_click_path.and_then(|s| CString::new(s).ok());
        unsafe {
            au_macos_init_notification_delegate(
                c_path
                    .as_ref()
                    .map(|p| p.as_ptr())
                    .unwrap_or(std::ptr::null()),
            );
        }
    }

    pub fn check_permission() -> String {
        let code = unsafe { au_macos_check_permission() };
        match code {
            1 => "granted".to_string(),
            2 => "denied".to_string(),
            0 => "prompt".to_string(),
            _ => "prompt".to_string(),
        }
    }

    pub fn request_permission() -> String {
        let code = unsafe { au_macos_request_permission() };
        match code {
            1 => "granted".to_string(),
            2 => "denied".to_string(),
            0 => "prompt".to_string(),
            _ => "denied".to_string(),
        }
    }

    pub fn send_notification(notif_id: &str, title: &str, body: &str) -> Result<(), String> {
        let c_id = CString::new(notif_id).unwrap_or_else(|_| CString::new("au-notif").unwrap());
        let c_title = CString::new(title).unwrap_or_else(|_| CString::new("Anime Universe").unwrap());
        let c_body = CString::new(body).unwrap_or_else(|_| CString::new("").unwrap());
        let ok = unsafe { au_macos_send_notification(c_id.as_ptr(), c_title.as_ptr(), c_body.as_ptr()) };
        if ok == 1 {
            Ok(())
        } else {
            Err("macOS UNUserNotificationCenter did not deliver notification (check permission and .app bundle)".to_string())
        }
    }

    pub fn open_settings() -> bool {
        unsafe { au_macos_open_notification_settings() == 1 }
    }
}

#[cfg(target_os = "windows")]
mod windows_notif {
    use super::{data_file_path, APP_AUMID, APP_DISPLAY_NAME};
    use std::ffi::OsStr;
    use std::os::windows::ffi::OsStrExt;
    use std::os::windows::process::CommandExt;
    use tauri::AppHandle;
    use winreg::enums::{HKEY_CURRENT_USER, KEY_READ};
    use winreg::RegKey;

    const CREATE_NO_WINDOW: u32 = 0x08000000;

    #[link(name = "shell32")]
    extern "system" {
        fn SetCurrentProcessExplicitAppUserModelID(app_id: *const u16) -> i32;
    }

    pub fn set_process_aumid() {
        let wide: Vec<u16> = OsStr::new(APP_AUMID)
            .encode_wide()
            .chain(std::iter::once(0))
            .collect();
        unsafe {
            let _ = SetCurrentProcessExplicitAppUserModelID(wide.as_ptr());
        }
    }

    /// Registers Anime Universe's AUMID in HKCU so Windows 10/11 Action Center accepts
    /// Toast notifications and displays "Anime Universe" in Settings -> System -> Notifications
    /// (works for both NSIS/MSI installed builds and standalone .exe launches).
    pub fn ensure_aumid_registered(app: &AppHandle) {
        set_process_aumid();

        let hkcu = RegKey::predef(HKEY_CURRENT_USER);
        let aumid_path = format!(r"Software\Classes\AppUserModelId\{}", APP_AUMID);
        if let Ok((key, _)) = hkcu.create_subkey(&aumid_path) {
            let _ = key.set_value("DisplayName", &APP_DISPLAY_NAME);
            let _ = key.set_value("IconBackgroundColor", &"FF09090F");

            // Point IconUri to bundled icon next to exe or extracted local icon if present
            if let Ok(exe_path) = std::env::current_exe() {
                if let Some(dir) = exe_path.parent() {
                    let candidates = [
                        dir.join("icons").join("icon.ico"),
                        dir.join("icons").join("icon.png"),
                        dir.join("icon.ico"),
                    ];
                    if let Some(found) = candidates.into_iter().find(|p| p.exists()) {
                        let _ = key.set_value("IconUri", &found.to_string_lossy().to_string());
                    } else if let Some(local_icon) = data_file_path(app, "notif_icon.png") {
                        if local_icon.exists() {
                            let _ = key.set_value("IconUri", &local_icon.to_string_lossy().to_string());
                        }
                    }
                }
            }
        }

        // Ensure Windows Settings -> System -> Notifications lists Anime Universe
        // WITHOUT overwriting Enabled if the user manually disabled notifications in Windows Settings.
        let settings_path = format!(
            r"Software\Microsoft\Windows\CurrentVersion\Notifications\Settings\{}",
            APP_AUMID
        );
        if let Ok((key, _)) = hkcu.create_subkey(&settings_path) {
            let existing_show: Result<u32, _> = key.get_value("ShowInActionCenter");
            if existing_show.is_err() {
                let _ = key.set_value("ShowInActionCenter", &1u32);
            }
        }
    }

    /// Checks real Windows 10/11 system notification toggles in the Registry:
    /// 1) Global Settings -> System -> Notifications (ToastEnabled)
    /// 2) Per-app Settings -> System -> Notifications -> Anime Universe (Enabled)
    pub fn are_windows_notifications_allowed_in_os() -> bool {
        let hkcu = RegKey::predef(HKEY_CURRENT_USER);

        if let Ok(push_key) = hkcu.open_subkey_with_flags(
            r"Software\Microsoft\Windows\CurrentVersion\PushNotifications",
            KEY_READ,
        ) {
            if let Ok(toast_enabled) = push_key.get_value::<u32, _>("ToastEnabled") {
                if toast_enabled == 0 {
                    return false;
                }
            }
        }

        let app_settings_path = format!(
            r"Software\Microsoft\Windows\CurrentVersion\Notifications\Settings\{}",
            APP_AUMID
        );
        if let Ok(app_key) = hkcu.open_subkey_with_flags(&app_settings_path, KEY_READ) {
            if let Ok(app_enabled) = app_key.get_value::<u32, _>("Enabled") {
                if app_enabled == 0 {
                    return false;
                }
            }
        }

        true
    }

    pub fn open_settings() -> bool {
        std::process::Command::new("cmd")
            .args(["/C", "start", "", "ms-settings:notifications"])
            .creation_flags(CREATE_NO_WINDOW)
            .spawn()
            .is_ok()
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct NotifSessionConfig {
    supabase_url: String,
    anon_key: String,
    access_token: String,
    refresh_token: String,
    user_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct LocalNotifPreferences {
    #[serde(default = "default_true")]
    master_enabled: bool,
    #[serde(default = "default_true")]
    messages_enabled: bool,
    #[serde(default = "default_true")]
    updates_enabled: bool,
    #[serde(default = "default_true")]
    reminders_enabled: bool,
    #[serde(default = "default_prompt")]
    os_permission_status: String,
    #[serde(default)]
    windows_user_consented: bool,
}

fn default_true() -> bool {
    true
}

fn default_prompt() -> String {
    "prompt".to_string()
}

impl Default for LocalNotifPreferences {
    fn default() -> Self {
        Self {
            master_enabled: true,
            messages_enabled: true,
            updates_enabled: true,
            reminders_enabled: true,
            os_permission_status: "prompt".to_string(),
            windows_user_consented: false,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct DeviceRegistrationInfo {
    device_id: String,
    platform: String,
    push_provider: String,
    push_token: Option<String>,
    os_permission_status: String,
    device_name: String,
    app_version: String,
}

fn data_file_path(app: &AppHandle, filename: &str) -> Option<PathBuf> {
    let dir = app.path().app_local_data_dir().ok()?;
    let _ = fs::create_dir_all(&dir);
    Some(dir.join(filename))
}

fn session_file_path(app: &AppHandle) -> Option<PathBuf> {
    data_file_path(app, "notif_session.json")
}

fn seen_file_path(app: &AppHandle) -> Option<PathBuf> {
    data_file_path(app, "seen_notifs.json")
}

fn prefs_file_path(app: &AppHandle) -> Option<PathBuf> {
    data_file_path(app, "notif_prefs.json")
}

fn device_id_file_path(app: &AppHandle) -> Option<PathBuf> {
    data_file_path(app, "device_id.txt")
}

fn push_token_file_path(app: &AppHandle) -> Option<PathBuf> {
    data_file_path(app, "device_push_token.json")
}

fn pending_click_file_path(app: &AppHandle) -> Option<PathBuf> {
    data_file_path(app, "pending_notif_click.json")
}

fn current_platform_name() -> &'static str {
    if cfg!(target_os = "android") {
        "android"
    } else if cfg!(target_os = "macos") {
        "macos"
    } else if cfg!(target_os = "windows") {
        "windows"
    } else {
        "web"
    }
}

fn default_push_provider_for_platform(platform: &str, has_remote_token: bool) -> &'static str {
    if !has_remote_token {
        return "tauri_local";
    }
    match platform {
        "android" => "fcm",
        "macos" => "apns",
        "windows" => "wns",
        _ => "tauri_local",
    }
}

fn get_or_create_persistent_device_id(app: &AppHandle) -> String {
    if let Some(path) = device_id_file_path(app) {
        if let Ok(existing) = fs::read_to_string(&path) {
            let trimmed = existing.trim().to_string();
            if trimmed.len() >= 8 {
                return trimmed;
            }
        }
        let nanos = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|d| d.as_nanos())
            .unwrap_or(0);
        let pid = std::process::id();
        let generated = format!("au-{}-{:x}-{:x}", current_platform_name(), nanos, pid);
        let _ = fs::write(&path, &generated);
        return generated;
    }
    format!("au-{}-fallback", current_platform_name())
}

fn read_stored_push_token(app: &AppHandle) -> Option<String> {
    let path = push_token_file_path(app)?;
    let raw = fs::read_to_string(path).ok()?;
    if let Ok(parsed) = serde_json::from_str::<serde_json::Value>(&raw) {
        if let Some(tok) = parsed.get("token").and_then(|v| v.as_str()) {
            let clean = tok.trim().to_string();
            if !clean.is_empty() {
                return Some(clean);
            }
        }
    }
    let clean = raw.trim().to_string();
    if !clean.is_empty() && !clean.starts_with('{') {
        Some(clean)
    } else {
        None
    }
}

fn read_local_prefs(app: &AppHandle) -> LocalNotifPreferences {
    let Some(path) = prefs_file_path(app) else {
        return LocalNotifPreferences::default();
    };
    let Ok(raw) = fs::read_to_string(path) else {
        return LocalNotifPreferences::default();
    };
    serde_json::from_str::<LocalNotifPreferences>(&raw).unwrap_or_default()
}

fn write_local_prefs(app: &AppHandle, prefs: &LocalNotifPreferences) {
    let Some(path) = prefs_file_path(app) else {
        return;
    };
    if let Ok(json) = serde_json::to_string(prefs) {
        let _ = fs::write(path, json);
    }
}

fn query_os_permission_status(app: &AppHandle) -> String {
    #[cfg(target_os = "macos")]
    {
        let _ = app;
        return macos_notif::check_permission();
    }

    #[cfg(target_os = "windows")]
    {
        if !windows_notif::are_windows_notifications_allowed_in_os() {
            return "denied".to_string();
        }
        let prefs = read_local_prefs(app);
        return if prefs.windows_user_consented {
            "granted".to_string()
        } else {
            "prompt".to_string()
        };
    }

    #[cfg(not(any(target_os = "macos", target_os = "windows")))]
    {
        match app.notification().permission_state() {
            Ok(PermissionState::Granted) => "granted".to_string(),
            Ok(PermissionState::Denied) => "denied".to_string(),
            Ok(PermissionState::Prompt) | Ok(PermissionState::PromptWithRationale) => {
                "prompt".to_string()
            }
            Err(_) => "prompt".to_string(),
        }
    }
}

fn dispatch_os_notification(app: &AppHandle, notif_id: &str, title: &str, body: &str) -> Result<(), String> {
    let clean_title = if title.trim().is_empty() {
        APP_DISPLAY_NAME
    } else {
        title.trim()
    };
    let clean_body = if body.trim().is_empty() {
        "You have a new notification in Anime Universe."
    } else {
        body.trim()
    };

    #[cfg(target_os = "macos")]
    {
        let _ = app;
        return macos_notif::send_notification(notif_id, clean_title, clean_body);
    }

    #[cfg(target_os = "windows")]
    {
        let _ = notif_id;
        windows_notif::ensure_aumid_registered(app);
        if !windows_notif::are_windows_notifications_allowed_in_os() {
            return Err("Windows notifications are turned off in Windows Settings".to_string());
        }
        return app
            .notification()
            .builder()
            .title(clean_title)
            .body(clean_body)
            .show()
            .map_err(|e| e.to_string());
    }

    #[cfg(not(any(target_os = "macos", target_os = "windows")))]
    {
        let _ = notif_id;
        return app
            .notification()
            .builder()
            .title(clean_title)
            .body(clean_body)
            .show()
            .map_err(|e| e.to_string());
    }
}

fn read_seen_ids(app: &AppHandle) -> HashSet<String> {
    let Some(path) = seen_file_path(app) else {
        return HashSet::new();
    };
    let Ok(raw) = fs::read_to_string(path) else {
        return HashSet::new();
    };
    serde_json::from_str::<Vec<String>>(&raw)
        .map(|v| v.into_iter().collect())
        .unwrap_or_default()
}

fn write_seen_ids(app: &AppHandle, ids: &HashSet<String>) {
    let Some(path) = seen_file_path(app) else {
        return;
    };
    let trimmed: Vec<String> = ids.iter().take(400).cloned().collect();
    if let Ok(json) = serde_json::to_string(&trimmed) {
        let _ = fs::write(path, json);
    }
}

#[tauri::command]
fn sync_notification_auth(
    app: AppHandle,
    supabase_url: String,
    anon_key: String,
    access_token: String,
    refresh_token: String,
    user_id: String,
) -> Result<(), String> {
    let Some(path) = session_file_path(&app) else {
        return Err("Unable to resolve app data directory".to_string());
    };
    let cfg = NotifSessionConfig {
        supabase_url: supabase_url.trim_end_matches('/').to_string(),
        anon_key,
        access_token,
        refresh_token,
        user_id,
    };
    let json = serde_json::to_string(&cfg).map_err(|e| e.to_string())?;
    fs::write(path, json).map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
fn clear_notification_auth(app: AppHandle) -> Result<(), String> {
    if let Some(path) = session_file_path(&app) {
        let _ = fs::remove_file(path);
    }
    Ok(())
}

#[tauri::command]
fn mark_seen_notification_ids(app: AppHandle, ids: Vec<String>) -> Result<(), String> {
    if ids.is_empty() {
        return Ok(());
    }
    let mut seen = read_seen_ids(&app);
    for id in ids {
        let clean = id.trim().to_string();
        if !clean.is_empty() {
            seen.insert(clean);
        }
    }
    write_seen_ids(&app, &seen);
    Ok(())
}

#[tauri::command]
async fn get_device_notification_status(app: AppHandle) -> Result<DeviceRegistrationInfo, String> {
    let platform = current_platform_name().to_string();
    let device_id = get_or_create_persistent_device_id(&app);
    let push_token = read_stored_push_token(&app);
    let app_clone = app.clone();
    let os_permission_status = tauri::async_runtime::spawn_blocking(move || {
        query_os_permission_status(&app_clone)
    })
    .await
    .unwrap_or_else(|_| "prompt".to_string());

    let push_provider =
        default_push_provider_for_platform(&platform, push_token.is_some()).to_string();
    let device_name = match platform.as_str() {
        "android" => "Android APK Client".to_string(),
        "macos" => "macOS Desktop Client".to_string(),
        "windows" => "Windows Desktop Client".to_string(),
        _ => "Tauri Client".to_string(),
    };

    Ok(DeviceRegistrationInfo {
        device_id,
        platform,
        push_provider,
        push_token,
        os_permission_status,
        device_name,
        app_version: app.package_info().version.to_string(),
    })
}

#[tauri::command]
async fn request_os_notification_permission(app: AppHandle) -> Result<String, String> {
    let app_clone = app.clone();
    let status = tauri::async_runtime::spawn_blocking(move || {
        #[cfg(target_os = "macos")]
        {
            let _ = &app_clone;
            return macos_notif::request_permission();
        }

        #[cfg(target_os = "windows")]
        {
            windows_notif::ensure_aumid_registered(&app_clone);
            if !windows_notif::are_windows_notifications_allowed_in_os() {
                return "denied".to_string();
            }
            let mut prefs = read_local_prefs(&app_clone);
            prefs.windows_user_consented = true;
            prefs.os_permission_status = "granted".to_string();
            write_local_prefs(&app_clone, &prefs);
            return "granted".to_string();
        }

        #[cfg(not(any(target_os = "macos", target_os = "windows")))]
        {
            match app_clone.notification().request_permission() {
                Ok(PermissionState::Granted) => "granted".to_string(),
                Ok(PermissionState::Denied) => "denied".to_string(),
                Ok(PermissionState::Prompt) | Ok(PermissionState::PromptWithRationale) => {
                    "prompt".to_string()
                }
                Err(_) => query_os_permission_status(&app_clone),
            }
        }
    })
    .await
    .map_err(|e| e.to_string())?;

    let mut prefs = read_local_prefs(&app);
    prefs.os_permission_status = status.clone();
    #[cfg(target_os = "windows")]
    if status == "granted" {
        prefs.windows_user_consented = true;
    }
    write_local_prefs(&app, &prefs);
    Ok(status)
}

#[tauri::command]
async fn set_native_push_token(
    app: AppHandle,
    token: String,
    provider: Option<String>,
) -> Result<DeviceRegistrationInfo, String> {
    let clean_token = token.trim().to_string();
    if let Some(path) = push_token_file_path(&app) {
        if clean_token.is_empty() {
            let _ = fs::remove_file(path);
        } else {
            let payload = serde_json::json!({
                "token": clean_token,
                "provider": provider.unwrap_or_else(|| default_push_provider_for_platform(current_platform_name(), true).to_string()),
            });
            if let Ok(json) = serde_json::to_string(&payload) {
                let _ = fs::write(path, json);
            }
        }
    }
    get_device_notification_status(app).await
}

#[tauri::command]
fn sync_local_notification_preferences(
    app: AppHandle,
    master_enabled: bool,
    messages_enabled: bool,
    updates_enabled: bool,
    reminders_enabled: bool,
    os_permission_status: Option<String>,
) -> Result<(), String> {
    let existing = read_local_prefs(&app);
    let current_os_perm = os_permission_status.unwrap_or(existing.os_permission_status);
    let prefs = LocalNotifPreferences {
        master_enabled,
        messages_enabled,
        updates_enabled,
        reminders_enabled,
        os_permission_status: current_os_perm,
        windows_user_consented: existing.windows_user_consented,
    };
    write_local_prefs(&app, &prefs);
    Ok(())
}

#[tauri::command]
fn open_os_notification_settings() -> Result<bool, String> {
    #[cfg(target_os = "windows")]
    {
        return Ok(windows_notif::open_settings());
    }
    #[cfg(target_os = "macos")]
    {
        return Ok(macos_notif::open_settings());
    }
    #[cfg(not(any(target_os = "windows", target_os = "macos")))]
    {
        Ok(false)
    }
}

#[tauri::command]
fn consume_pending_notification_click(app: AppHandle) -> Result<Option<serde_json::Value>, String> {
    let Some(path) = pending_click_file_path(&app) else {
        return Ok(None);
    };
    if !path.exists() {
        return Ok(None);
    }
    let raw = fs::read_to_string(&path).ok();
    let _ = fs::remove_file(&path);
    let Some(text) = raw else {
        return Ok(None);
    };
    let parsed = serde_json::from_str::<serde_json::Value>(&text).ok();
    Ok(parsed)
}

#[tauri::command]
async fn send_native_notification(
    app: AppHandle,
    title: String,
    body: String,
) -> Result<(), String> {
    let app_clone = app.clone();
    tauri::async_runtime::spawn_blocking(move || {
        let os_perm = query_os_permission_status(&app_clone);
        if os_perm != "granted" {
            return Err(format!(
                "OS notification permission is not granted (status: {})",
                os_perm
            ));
        }
        let prefs = read_local_prefs(&app_clone);
        if !prefs.master_enabled {
            return Err("Master notifications are disabled in settings".to_string());
        }
        let notif_id = format!(
            "au-local-{}",
            SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .map(|d| d.as_millis())
                .unwrap_or(0)
        );
        dispatch_os_notification(&app_clone, &notif_id, &title, &body)
    })
    .await
    .map_err(|e| e.to_string())?
}

#[cfg(desktop)]
#[derive(Debug, Deserialize)]
struct UnifiedNotifRow {
    id: serde_json::Value,
    #[serde(default)]
    title: Option<String>,
    #[serde(default)]
    message: Option<String>,
    #[serde(default, rename = "type")]
    notif_type: Option<String>,
    #[serde(default)]
    notification_type: Option<String>,
    #[serde(default)]
    preview_text: Option<String>,
    #[serde(default)]
    message_preview: Option<String>,
    #[serde(default)]
    sender_user_id: Option<String>,
}

#[cfg(desktop)]
#[derive(Debug, Deserialize)]
struct RefreshTokenResp {
    access_token: String,
    #[serde(default)]
    refresh_token: Option<String>,
}

#[cfg(desktop)]
async fn refresh_supabase_token(
    client: &reqwest::Client,
    cfg: &mut NotifSessionConfig,
    app: &AppHandle,
) -> bool {
    if cfg.refresh_token.trim().is_empty() {
        return false;
    }
    let url = format!("{}/auth/v1/token?grant_type=refresh_token", cfg.supabase_url);
    let body = serde_json::json!({ "refresh_token": cfg.refresh_token });
    let resp = client
        .post(&url)
        .header("apikey", &cfg.anon_key)
        .header("Content-Type", "application/json")
        .json(&body)
        .send()
        .await;
    let Ok(r) = resp else {
        return false;
    };
    if !r.status().is_success() {
        return false;
    }
    let Ok(parsed) = r.json::<RefreshTokenResp>().await else {
        return false;
    };
    if parsed.access_token.trim().is_empty() {
        return false;
    }
    cfg.access_token = parsed.access_token;
    if let Some(rt) = parsed.refresh_token {
        if !rt.trim().is_empty() {
            cfg.refresh_token = rt;
        }
    }
    if let Some(path) = session_file_path(app) {
        if let Ok(json) = serde_json::to_string(cfg) {
            let _ = fs::write(path, json);
        }
    }
    true
}

#[cfg(desktop)]
fn category_allowed(prefs: &LocalNotifPreferences, notif_type: &str) -> bool {
    if !prefs.master_enabled {
        return false;
    }
    match notif_type {
        "message" | "reply" | "mention" => prefs.messages_enabled,
        "update" => prefs.updates_enabled,
        "reminder" => prefs.reminders_enabled,
        _ => true,
    }
}

#[cfg(desktop)]
fn start_desktop_notification_poller(app: AppHandle) {
    tauri::async_runtime::spawn(async move {
        let client = match reqwest::Client::builder()
            .timeout(std::time::Duration::from_secs(12))
            .build()
        {
            Ok(c) => c,
            Err(_) => return,
        };
        let mut primed = false;
        loop {
            tokio::time::sleep(std::time::Duration::from_secs(if primed { 18 } else { 7 })).await;

            let prefs = read_local_prefs(&app);
            let app_perm_check = app.clone();
            let os_perm = tauri::async_runtime::spawn_blocking(move || {
                query_os_permission_status(&app_perm_check)
            })
            .await
            .unwrap_or_else(|_| "denied".to_string());

            if !prefs.master_enabled || os_perm != "granted" {
                continue;
            }

            let Some(path) = session_file_path(&app) else {
                continue;
            };
            let Ok(raw) = fs::read_to_string(&path) else {
                continue;
            };
            let Ok(mut cfg) = serde_json::from_str::<NotifSessionConfig>(&raw) else {
                continue;
            };
            if cfg.supabase_url.is_empty() || cfg.user_id.is_empty() || cfg.access_token.is_empty() {
                continue;
            }

            let primary_url = format!(
                "{}/rest/v1/notifications?select=id,title,message,type,sender_user_id&user_id=eq.{}&is_read=eq.false&order=created_at.desc&limit=10",
                cfg.supabase_url, cfg.user_id
            );

            let mut resp = client
                .get(&primary_url)
                .header("apikey", &cfg.anon_key)
                .header("Authorization", format!("Bearer {}", cfg.access_token))
                .send()
                .await;

            if let Ok(ref r) = resp {
                if r.status().as_u16() == 401 && refresh_supabase_token(&client, &mut cfg, &app).await {
                    resp = client
                        .get(&primary_url)
                        .header("apikey", &cfg.anon_key)
                        .header("Authorization", format!("Bearer {}", cfg.access_token))
                        .send()
                        .await;
                }
            }

            let mut rows: Vec<UnifiedNotifRow> = Vec::new();
            let mut fetched_primary = false;

            if let Ok(r) = resp {
                if r.status().is_success() {
                    if let Ok(parsed) = r.json::<Vec<UnifiedNotifRow>>().await {
                        rows = parsed;
                        fetched_primary = true;
                    }
                }
            }

            if !fetched_primary {
                let fallback_url = format!(
                    "{}/rest/v1/user_notifications?select=id,notification_type,preview_text,message_preview,sender_user_id&recipient_user_id=eq.{}&is_read=eq.false&order=created_at.desc&limit=10",
                    cfg.supabase_url, cfg.user_id
                );
                if let Ok(r) = client
                    .get(&fallback_url)
                    .header("apikey", &cfg.anon_key)
                    .header("Authorization", format!("Bearer {}", cfg.access_token))
                    .send()
                    .await
                {
                    if r.status().is_success() {
                        if let Ok(parsed) = r.json::<Vec<UnifiedNotifRow>>().await {
                            rows = parsed;
                        }
                    }
                }
            }

            let mut seen = read_seen_ids(&app);
            let mut changed = false;

            if !primed && seen.is_empty() {
                let window_hidden = app
                    .get_webview_window("main")
                    .and_then(|w| w.is_visible().ok())
                    .map(|vis| !vis)
                    .unwrap_or(false);
                if window_hidden {
                    if let Some(first) = rows.first() {
                        let row_id = match &first.id {
                            serde_json::Value::String(s) => s.clone(),
                            other => other.to_string(),
                        };
                        let ntype = first
                            .notif_type
                            .as_deref()
                            .or(first.notification_type.as_deref())
                            .unwrap_or("message");
                        if category_allowed(&prefs, ntype) {
                            let title = first
                                .title
                                .as_deref()
                                .filter(|s| !s.trim().is_empty())
                                .unwrap_or(APP_DISPLAY_NAME);
                            let body = first
                                .message
                                .as_deref()
                                .or(first.preview_text.as_deref())
                                .or(first.message_preview.as_deref())
                                .map(|s| s.trim())
                                .filter(|s| !s.is_empty())
                                .unwrap_or("You have a new notification in Anime Universe.");
                            let _ = dispatch_os_notification(&app, &row_id, title, body);
                        }
                    }
                }
                for row in &rows {
                    let row_id = match &row.id {
                        serde_json::Value::String(s) => s.clone(),
                        other => other.to_string(),
                    };
                    seen.insert(row_id);
                    changed = true;
                }
                primed = true;
            } else {
                primed = true;
                for row in rows.iter().rev() {
                    let row_id = match &row.id {
                        serde_json::Value::String(s) => s.clone(),
                        other => other.to_string(),
                    };
                    if seen.contains(&row_id) {
                        continue;
                    }
                    seen.insert(row_id.clone());
                    changed = true;

                    let ntype = row
                        .notif_type
                        .as_deref()
                        .or(row.notification_type.as_deref())
                        .unwrap_or("message");

                    if !category_allowed(&prefs, ntype) {
                        continue;
                    }

                    let title = row
                        .title
                        .as_deref()
                        .filter(|s| !s.trim().is_empty())
                        .unwrap_or_else(|| match ntype {
                            "reply" => "Anime Universe • New Reply",
                            "mention" => "Anime Universe • Tagged You",
                            "update" => "Anime Universe • Update",
                            "reminder" => "Anime Universe • Reminder",
                            _ => "Anime Universe • Notification",
                        });
                    let body = row
                        .message
                        .as_deref()
                        .or(row.preview_text.as_deref())
                        .or(row.message_preview.as_deref())
                        .map(|s| s.trim())
                        .filter(|s| !s.is_empty())
                        .unwrap_or("You have a new notification in Anime Universe.");
                    let _ = dispatch_os_notification(&app, &row_id, title, body);
                }
            }

            if changed {
                write_seen_ids(&app, &seen);
            }
        }
    });
}

#[cfg(desktop)]
fn setup_desktop_tray_and_autostart(app: &mut tauri::App) -> Result<(), Box<dyn std::error::Error>> {
    use tauri::menu::{Menu, MenuItem};
    use tauri::tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent};
    use tauri_plugin_autostart::ManagerExt;

    let _ = app.autolaunch().enable();

    let show_item = MenuItem::with_id(app, "show", "Open Anime Universe", true, None::<&str>)?;
    let quit_item = MenuItem::with_id(app, "quit", "Quit Anime Universe", true, None::<&str>)?;
    let menu = Menu::with_items(app, &[&show_item, &quit_item])?;

    let mut tray_builder = TrayIconBuilder::with_id("anime-universe-tray")
        .menu(&menu)
        .tooltip("Anime Universe — Background Notifications Active")
        .on_menu_event(|app, event| match event.id.as_ref() {
            "show" => {
                if let Some(window) = app.get_webview_window("main") {
                    let _ = window.unminimize();
                    let _ = window.show();
                    let _ = window.set_focus();
                }
            }
            "quit" => {
                app.exit(0);
            }
            _ => {}
        })
        .on_tray_icon_event(|tray, event| {
            if let TrayIconEvent::Click {
                button: MouseButton::Left,
                button_state: MouseButtonState::Up,
                ..
            } = event
            {
                let app = tray.app_handle();
                if let Some(window) = app.get_webview_window("main") {
                    let _ = window.unminimize();
                    let _ = window.show();
                    let _ = window.set_focus();
                }
            }
        });

    if let Some(icon) = app.default_window_icon().cloned() {
        tray_builder = tray_builder.icon(icon);
    }
    let _ = tray_builder.build(app)?;

    Ok(())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let mut builder = tauri::Builder::default().plugin(tauri_plugin_notification::init());

    #[cfg(desktop)]
    {
        builder = builder.plugin(tauri_plugin_autostart::init(
            tauri_plugin_autostart::MacosLauncher::LaunchAgent,
            Some(vec!["--background"]),
        ));
    }

    builder
        .invoke_handler(tauri::generate_handler![
            sync_notification_auth,
            clear_notification_auth,
            mark_seen_notification_ids,
            get_device_notification_status,
            request_os_notification_permission,
            set_native_push_token,
            sync_local_notification_preferences,
            open_os_notification_settings,
            consume_pending_notification_click,
            send_native_notification
        ])
        .setup(|app| {
            let _ = get_or_create_persistent_device_id(app.handle());

            #[cfg(target_os = "macos")]
            {
                let click_path = pending_click_file_path(app.handle())
                    .map(|p| p.to_string_lossy().to_string());
                macos_notif::init_delegate(click_path.as_deref());
            }

            #[cfg(target_os = "windows")]
            {
                windows_notif::ensure_aumid_registered(app.handle());
            }

            #[cfg(desktop)]
            {
                let _ = setup_desktop_tray_and_autostart(app);
                let launched_bg = std::env::args().any(|a| a == "--background");
                if launched_bg {
                    if let Some(win) = app.get_webview_window("main") {
                        let _ = win.hide();
                    }
                }
                start_desktop_notification_poller(app.handle().clone());
            }
            Ok(())
        })
        .on_window_event(|window, event| {
            #[cfg(desktop)]
            if let tauri::WindowEvent::CloseRequested { api, .. } = event {
                api.prevent_close();
                let _ = window.hide();
            }
        })
        .run(tauri::generate_context!())
        .expect("error while running Anime Universe");
}
