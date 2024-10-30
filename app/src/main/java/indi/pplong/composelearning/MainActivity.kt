package indi.pplong.composelearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import indi.pplong.composelearning.sys.ui.sys.widgets.CommonBottomNavigationBar
import indi.pplong.composelearning.sys.ui.sys.widgets.CommonNavigationHost
import indi.pplong.composelearning.sys.ui.theme.ComposeLearningTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

//        splashScreen.setKeepOnScreenCondition { true }

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            ComposeLearningTheme {
                Scaffold(
//                    topBar = {
//                        CommonTopBar(
//                            CommonTopBarConfig(
//                                title = stringResource(R.string.app_name),
//                                showBackAction = false
//                            )
//                        )
//                    },
                    bottomBar = { CommonBottomNavigationBar(navController) },

                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    CommonNavigationHost(navController, modifier = Modifier.padding(innerPadding))
                }
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