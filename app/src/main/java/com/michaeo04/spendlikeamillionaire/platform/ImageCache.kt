package com.michaeo04.spendlikeamillionaire.platform

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes bundled item photos off the main thread and keeps recently used ones in memory (24 MB). */
object ImageCache {
    private val cache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    /** Returns null when the asset is missing or cannot be decoded (the caller shows the emoji). */
    suspend fun load(assets: AssetManager, path: String): ImageBitmap? {
        cache.get(path)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        }?.also { cache.put(path, it) }
    }
}
