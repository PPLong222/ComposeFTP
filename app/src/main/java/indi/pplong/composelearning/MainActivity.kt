package indi.pplong.composelearning

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import indi.pplong.composelearning.core.base.GlobalRepository
import indi.pplong.composelearning.core.cache.GlobalCacheList
import indi.pplong.composelearning.core.load.service.IntentRelay
import indi.pplong.composelearning.core.util.getContentUri
import indi.pplong.composelearning.sys.ui.sys.widgets.CommonNavigationHost
import indi.pplong.composelearning.sys.ui.theme.ComposeLearningTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var globalRepository: GlobalRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)


//        splashScreen.setKeepOnScreenCondition { true }

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            context.cacheDir.listFiles()?.map { it.name }?.forEach {
                GlobalCacheList.map.put(
                    it.removeSuffix(".jpg"), File(cacheDir, it).getContentUri(context).toString()
                )
            }
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(), onResult = { granted ->
                    if (granted) {

                    } else {
                    }
                })


            val navController = rememberNavController()
            ComposeLearningTheme {
                LaunchedEffect(Unit) {
                    launcher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
                CommonNavigationHost(navController, modifier = Modifier)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        Log.d("123123", "onNewIntent: On NEw Intent")
        super.onNewIntent(intent)
        lifecycleScope.launch {
            Log.d("123123", "onNewIntent: ${globalRepository.pool.downloadFTPSet.first()}")
            Log.d("123123", "onNewIntent: ${globalRepository.pool.uploadFTPSet.first()}")
            // Why Stateflow can't do the work?
            if (globalRepository.pool.downloadFTPSet.first()
                    .isEmpty() && globalRepository.pool.uploadFTPSet.first().isEmpty()
            ) {
                IntentRelay.updateIntent(intent)
            }
        }


    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ComposeLearningTheme {
        Greeting("Android")
    }
}