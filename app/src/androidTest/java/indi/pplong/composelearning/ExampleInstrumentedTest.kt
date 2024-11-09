package indi.pplong.composelearning

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import indi.pplong.composelearning.core.util.FileUtil
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("indi.pplong.composelearning", appContext.packageName)
        val a = "123"
        val b = "123"

        println(a === b)
    }

    @Test
    fun getRemoteVideoThumbnail() {
        val mimeType = FileUtil.getMimeType("123.png")
        Log.d("testtest", "getRemoteVideoThumbnail: $mimeType")
        Log.d("testtest", "getRemoteVideoThumbnail: ${FileUtil.getMimeType("123.mp4")}")
    }
}