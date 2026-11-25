use serde::{Deserialize, Serialize};
use std::collections::HashSet;
use std::fs;
use std::path::PathBuf;
use tauri::{AppHandle, Manager};
use tauri_plugin_notification::NotificationExt;

#[derive(Debug, Clone, Serialize, Deserialize)]
struct NotifSessionConfig {
    supabase_url: String,
    anon_key: String,
    access_token: String,
    refresh_token: String,
    user_id: String,
}

fn session_file_path(app: &AppHandle) -> Option<PathBuf> {
    let dir = app.path().app_local_data_dir().ok()?;
    let _ = fs::create_dir_all(&dir);
    Some(dir.join("notif_session.json"))
}

fn seen_file_path(app: &AppHandle) -> Option<PathBuf> {
    let dir = app.path().app_local_data_dir().ok()?;
    let _ = fs::create_dir_all(&dir);
    Some(dir.join("seen_notifs.json"))
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
fn send_native_notification(app: AppHandle, title: String, body: String) -> Result<(), String> {
    let clean_title = if title.trim().is_empty() {
        "Anime Universe".to_string()
    } else {
        title
    };
    app.notification()
        .builder()
        .title(&clean_title)
        .body(&body)
        .show()
        .map_err(|e| e.to_string())
}

#[cfg(desktop)]
#[derive(Debug, Deserialize)]
struct SupabaseNotifRow {
    id: String,
    #[serde(default)]
    notification_type: Option<String>,
    #[serde(default)]
    preview_text: Option<String>,
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
            tokio::time::sleep(std::time::Duration::from_secs(if primed { 20 } else { 8 })).await;

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

            let url = format!(
                "{}/rest/v1/user_notifications?select=id,notification_type,preview_text,sender_user_id&recipient_user_id=eq.{}&is_read=eq.false&order=created_at.desc&limit=10",
                cfg.supabase_url, cfg.user_id
            );

            let mut resp = client
                .get(&url)
                .header("apikey", &cfg.anon_key)
                .header("Authorization", format!("Bearer {}", cfg.access_token))
                .send()
                .await;

            if let Ok(ref r) = resp {
                if r.status().as_u16() == 401 && refresh_supabase_token(&client, &mut cfg, &app).await {
                    resp = client
                        .get(&url)
                        .header("apikey", &cfg.anon_key)
                        .header("Authorization", format!("Bearer {}", cfg.access_token))
                        .send()
                        .await;
                }
            }

            let Ok(r) = resp else {
                continue;
            };
            if !r.status().is_success() {
                continue;
            }
            let Ok(rows) = r.json::<Vec<SupabaseNotifRow>>().await else {
                continue;
            };

            let mut seen = read_seen_ids(&app);
            let mut changed = false;

            if !primed && seen.is_empty() {
                // First run with no seen cache: record existing unread IDs so we only alert new ones,
                // unless the window is already hidden (background launch) in which case show the newest one.
                let window_hidden = app
                    .get_webview_window("main")
                    .and_then(|w| w.is_visible().ok())
                    .map(|vis| !vis)
                    .unwrap_or(false);
                if window_hidden {
                    if let Some(first) = rows.first() {
                        let body = first
                            .preview_text
                            .as_deref()
                            .map(|s| s.trim())
                            .filter(|s| !s.is_empty())
                            .unwrap_or("You have a new reply or mention in Anime Universe.");
                        let _ = app
                            .notification()
                            .builder()
                            .title("Anime Universe")
                            .body(body)
                            .show();
                    }
                }
                for row in &rows {
                    seen.insert(row.id.clone());
                    changed = true;
                }
                primed = true;
            } else {
                primed = true;
                for row in rows.iter().rev() {
                    if seen.contains(&row.id) {
                        continue;
                    }
                    seen.insert(row.id.clone());
                    changed = true;
                    if row.sender_user_id.as_deref() == Some(cfg.user_id.as_str()) {
                        continue;
                    }
                    let title = match row.notification_type.as_deref() {
                        Some("reply") => "Anime Universe • New Reply",
                        Some("mention") => "Anime Universe • Tagged You",
                        _ => "Anime Universe • Community Alert",
                    };
                    let body = row
                        .preview_text
                        .as_deref()
                        .map(|s| s.trim())
                        .filter(|s| !s.is_empty())
                        .unwrap_or("You have a new notification in Community.");
                    let _ = app.notification().builder().title(title).body(body).show();
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
    let mut builder = tauri::Builder::default()
        .plugin(tauri_plugin_notification::init());

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
            send_native_notification
        ])
        .setup(|app| {
            #[cfg(desktop)]
            {
                let _ = setup_desktop_tray_and_autostart(app);
                // If launched silently via autostart --background, keep hidden in system tray
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
                // Keep running quietly in system tray so OS notification bar alerts still arrive when closed
                api.prevent_close();
                let _ = window.hide();
            }
        })
        .run(tauri::generate_context!())
        .expect("error while running Anime Universe");
}
