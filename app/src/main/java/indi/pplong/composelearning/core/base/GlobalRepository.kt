package indi.pplong.composelearning.core.base

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import indi.pplong.composelearning.ftp.FTPServerPool
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Description:
 * @author PPLong
 * @date 11/3/24 4:20 PM
 */
@Singleton
class GlobalRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val pool = FTPServerPool(context)
    var x = 1
    fun addX() {
        x++
    }
}