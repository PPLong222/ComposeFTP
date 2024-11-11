package indi.pplong.composelearning.sys

import android.app.Application
import coil.Coil
import coil.ImageLoader
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
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()

        // 设置全局的 ImageLoader
        Coil.setImageLoader(imageLoader)
    }
}