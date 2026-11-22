package com.example

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

class AnimeApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        try {
            val codeCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            java.io.File(codeCacheDir, "wasm").mkdirs()
            java.io.File(codeCacheDir, "js").mkdirs()
        } catch (_: Exception) {
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.14)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(45L * 1024 * 1024) // Bounded 45 MB cache for lean storage footprint
                    .build()
            }
            .allowRgb565(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}
