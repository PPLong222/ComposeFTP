package indi.pplong.composelearning.core.load.service

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateOf

/**
 * @author PPLong
 * @date 4/17/25 5:35 PM
 */
object IntentRelay {
    val intentState = mutableStateOf<Intent?>(null)

    fun updateIntent(intent: Intent?) {
        Log.d("123123", "updateIntent: update Intent")
        intentState.value = intent
    }
}