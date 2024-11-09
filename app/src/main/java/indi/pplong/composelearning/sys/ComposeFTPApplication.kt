package indi.pplong.composelearning.sys

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 9:53 PM
 */
@HiltAndroidApp
class ComposeFTPApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val imageLoader = ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))  // 设置缓存目录
//                    .maxSizeBytes(128 * 1024 * 1024)  // 设置缓存大小
                    .maxSizePercent(1.00)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .build()

        // 设置全局的 ImageLoader
        Coil.setImageLoader(imageLoader)
    }
}