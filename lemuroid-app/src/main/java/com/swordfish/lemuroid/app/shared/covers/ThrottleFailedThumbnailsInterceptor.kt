package com.swordfish.lemuroid.app.shared.covers

import android.util.LruCache
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

object ThrottleFailedThumbnailsInterceptor : Interceptor {
    private val failedThumbnailsStatusCode = LruCache<String, Int>(256 * 1024)
    private val resolvedFallbackUrlCache = LruCache<String, String>(4096)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestUrl = originalRequest.url.toString()

        val cachedResolved = resolvedFallbackUrlCache[requestUrl]
        if (cachedResolved != null && cachedResolved != requestUrl) {
            val redirected = chain.proceed(originalRequest.newBuilder().url(cachedResolved).build())
            if (redirected.isSuccessful) {
                return redirected
            }
            redirected.close()
        }

        val previousFailure = failedThumbnailsStatusCode[requestUrl]
        if (previousFailure != null) {
            throw IOException("Thumbnail previously failed with code: $previousFailure")
        }

        val response = chain.proceed(originalRequest)
        if (response.isSuccessful) {
            return response
        }

        // Try smart fallback URLs for Libretro thumbnails (region variants & Named_Titles)
        if (requestUrl.contains("thumbnails.libretro.com") && response.code == 404) {
            val fallbacks = buildFallbackUrls(requestUrl)
            for (candidateUrl in fallbacks) {
                response.close()
                val candidateResponse =
                    runCatching {
                        chain.proceed(originalRequest.newBuilder().url(candidateUrl).build())
                    }.getOrNull() ?: continue

                if (candidateResponse.isSuccessful) {
                    resolvedFallbackUrlCache.put(requestUrl, candidateUrl)
                    return candidateResponse
                } else {
                    candidateResponse.close()
                }
            }
        }

        failedThumbnailsStatusCode.put(requestUrl, response.code)
        return response
    }

    private fun buildFallbackUrls(url: String): List<String> {
        val candidates = mutableListOf<String>()
        if (url.endsWith(".png")) {
            val withoutExt = url.removeSuffix(".png")
            // If URL has no region tag, try common regions
            if (!withoutExt.contains("(")) {
                candidates.add("$withoutExt (USA).png")
                candidates.add("$withoutExt (World).png")
                candidates.add("$withoutExt (Europe).png")
            }
            // Also try Named_Titles if Named_Boxarts is missing
            if (url.contains("/Named_Boxarts/")) {
                candidates.add(url.replace("/Named_Boxarts/", "/Named_Titles/"))
            }
        }
        return candidates
    }
}
