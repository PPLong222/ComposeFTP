package indi.pplong.composelearning.ftp.base

import android.net.Uri

/**
 * @author PPLong
 * @date 4/11/25 6:45 PM
 */
interface IThumbnailFTPClient : IBaseFTPClient {
    suspend fun launchThumbnailWork(fileName: String, key: String): Uri?
}