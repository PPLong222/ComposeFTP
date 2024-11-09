package indi.pplong.composelearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import indi.pplong.composelearning.core.cache.GlobalCacheList
import indi.pplong.composelearning.core.util.getContentUri
import indi.pplong.composelearning.sys.ui.sys.widgets.CommonNavigationHost
import indi.pplong.composelearning.sys.ui.theme.ComposeLearningTheme
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)


//        splashScreen.setKeepOnScreenCondition { true }

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            context.cacheDir.listFiles()?.map { it.name }?.forEach {
                GlobalCacheList.map.put(
                    it.removeSuffix(".jpg"),
                    File(cacheDir, it).getContentUri(context).toString()
                )
            }

            val navController = rememberNavController()
            ComposeLearningTheme {
                CommonNavigationHost(navController, modifier = Modifier)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ComposeLearningTheme {
        Greeting("Android")
    }
}