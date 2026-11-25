fn main() {
    let target_os = std::env::var("CARGO_CFG_TARGET_OS").unwrap_or_default();
    if target_os == "macos" {
        println!("cargo:rerun-if-changed=macos/notifications.m");
        cc::Build::new()
            .file("macos/notifications.m")
            .flag("-fobjc-arc")
            .compile("anime_macos_notifications");
        println!("cargo:rustc-link-lib=framework=UserNotifications");
        println!("cargo:rustc-link-lib=framework=Foundation");
        println!("cargo:rustc-link-lib=framework=AppKit");
    }

    tauri_build::try_build(
        tauri_build::Attributes::new().app_manifest(
            tauri_build::AppManifest::new().commands(&[
                "sync_notification_auth",
                "clear_notification_auth",
                "mark_seen_notification_ids",
                "get_device_notification_status",
                "request_os_notification_permission",
                "set_native_push_token",
                "sync_local_notification_preferences",
                "open_os_notification_settings",
                "consume_pending_notification_click",
                "send_native_notification",
            ]),
        ),
    )
    .expect("failed to run tauri-build");
}
