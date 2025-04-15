package indi.pplong.composelearning.core.load.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import indi.pplong.composelearning.R
import indi.pplong.composelearning.core.base.GlobalRepository
import indi.pplong.composelearning.core.file.model.CommonFileInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @author PPLong
 * @date 4/15/25 3:43 PM
 */
@AndroidEntryPoint
class TransferForegroundService : Service() {

    @Inject
    lateinit var globalRepository: GlobalRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var binder: TransferBinder = TransferBinder()

    private val TAG = javaClass.name
    override fun onBind(intent: Intent?): IBinder? {
        val hostKey = intent?.getLongExtra("host_key", -1L) ?: -1L
        val downloadList =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableArrayListExtra("download_list", CommonFileInfo::class.java)
            } else {
                intent?.getParcelableArrayListExtra<CommonFileInfo>("download_list")
            }

        val uploadList =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableArrayListExtra("uploadList", CommonFileInfo::class.java)
            } else {
                intent?.getParcelableArrayListExtra<CommonFileInfo>("download_list")
            }
        binder.addTransferTask(hostKey, downloadList ?: listOf(), uploadList ?: listOf())
        return binder
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        Log.d(TAG, "onStartCommand: Begin")
        startForegroundByVersion()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    private fun startForegroundByVersion() {
        val channelId = "ftp_transfer_channel"
        val notificationChannel =
            NotificationChannel(channelId, "Transfer", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            notificationChannel
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Transferring")
            .setContentText("Your file is being transferred...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setLargeIcon(Icon.createWithResource(this, R.drawable.img_1))
            .setOngoing(true)
            .build()

        startForeground(10001, notification)
    }

    inner class TransferBinder : Binder() {
        fun addTransferTask(
            hostKey: Long,
            downloadList: List<CommonFileInfo>,
            uploadList: List<CommonFileInfo>
        ) {
            Log.d(TAG, "addDownloadTask: Download")
            globalRepository.pool.serverFTPMap.value[hostKey]?.let { cache ->
                downloadList.forEach { file ->
                    serviceScope.launch {
                        cache.downloadFile(file)
                    }
                }
                uploadList.forEach { file ->
                    serviceScope.launch {
                        cache.uploadFile(file)
                    }
                }
            }
        }
    }
}