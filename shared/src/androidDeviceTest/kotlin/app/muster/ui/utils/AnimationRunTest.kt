package app.muster.ui.utils

import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import kotlin.test.assertEquals

// Proves AnimationRunListener applies what the annotations ask for
class AnimationRunTest {

    private val scaleSettings = listOf(Settings.Global.WINDOW_ANIMATION_SCALE, Settings.Global.TRANSITION_ANIMATION_SCALE, Settings.Global.ANIMATOR_DURATION_SCALE)

    private fun scales(): Map<String, Float> {
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        return scaleSettings.associateWith { Settings.Global.getFloat(resolver, it, 1f) }
    }

    private fun assertAllScales(expected: Float) {
        val deadline = System.currentTimeMillis() + SETTLE_MILLIS
        while (scales().values.any { it != expected } && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_MILLIS)
        }
        assertEquals(scaleSettings.associateWith { expected }, scales())
    }

    @Test
    @DisableAnimations
    fun animationsAreOffWhenTheTestAsksForThemOff() {
        assertAllScales(0f)
    }

    @Test
    @EnableAnimations
    fun animationsAreOnWhenTheTestAsksForThemOn() {
        assertAllScales(1f)
    }

    @Test
    fun animationsAreOffByDefault() {
        assertAllScales(0f)
    }

    private companion object {
        const val SETTLE_MILLIS = 1_000L
        const val POLL_MILLIS = 50L
    }
}
