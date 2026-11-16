package com.swordfish.lemuroid.app.shared.covers

import android.content.Context
import android.widget.ImageView
import coil.ImageLoader
import coil.disk.DiskCache
import coil.imageLoader
import coil.load
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.swordfish.lemuroid.common.drawable.TextDrawable
import com.swordfish.lemuroid.common.graphics.ColorUtils
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient

object CoverUtils {
    private val THUMB_REPLACE = Regex("[&*/:`<>?\\\\|]")
    private val BRACKETS_REGEX = Regex("\\[.*?\\]")

    fun resolveCoverUrl(game: Game): String? {
        val customOrDb = game.displayCoverUrl()
        if (!customOrDb.isNullOrBlank()) {
            return if (customOrDb.startsWith("http://thumbnails.libretro.com/")) {
                customOrDb.replaceFirst("http://", "https://")
            } else {
                customOrDb
            }
        }
        return runCatching {
            val system = GameSystem.findById(game.systemId)
            val systemName =
                if (system.id == SystemID.MAME2003PLUS) {
                    "MAME"
                } else {
                    system.libretroFullName
                }
            val cleanTitle =
                game.title
                    .replace(BRACKETS_REGEX, "")
                    .trim()
                    .replace(THUMB_REPLACE, "_")
            if (cleanTitle.isBlank()) {
                null
            } else {
                "https://thumbnails.libretro.com/$systemName/Named_Boxarts/$cleanTitle.png"
            }
        }.getOrNull()
    }

    fun loadCover(
        game: Game,
        imageView: ImageView?,
    ) {
        if (imageView == null) return

        imageView.load(resolveCoverUrl(game), imageView.context.imageLoader) {
            val fallbackDrawable = getFallbackDrawable(game)
            fallback(fallbackDrawable)
            error(fallbackDrawable)
        }
    }

    fun buildImageLoader(applicationContext: Context): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .diskCache(
                DiskCache.Builder()
                    .directory(applicationContext.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.25)
                    .build(),
            )
            .memoryCache {
                MemoryCache.Builder(applicationContext)
                    .maxSizePercent(0.20)
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor(ThrottleFailedThumbnailsInterceptor)
                    .build()
            }
            .crossfade(true)
            .interceptorDispatcher(Dispatchers.IO)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .build()
    }

    fun getFallbackDrawable(game: Game) = TextDrawable(computeTitle(game), computeColor(game))

    fun getFallbackRemoteUrl(game: Game): String {
        val color = Integer.toHexString(computeColor(game)).substring(2)
        val title = computeTitle(game)
        return "https://fakeimg.pl/512x512/$color/fff/?font=bebas&text=$title"
    }

    private fun computeTitle(game: Game): String {
        val sanitizedName =
            game.displayName()
                .replace(Regex("\\(.*\\)"), "")

        return sanitizedName.asSequence()
            .filter { it.isDigit() or it.isUpperCase() or (it == '&') }
            .take(3)
            .joinToString("")
            .ifBlank { game.displayName().firstOrNull()?.toString() ?: "?" }
            .capitalize()
    }

    private fun computeColor(game: Game): Int {
        return ColorUtils.randomColor(game.title)
    }
}
