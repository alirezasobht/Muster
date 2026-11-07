package app.muster.ui.utils

import android.os.ParcelFileDescriptor
import android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
import android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE
import android.provider.Settings.Global.WINDOW_ANIMATION_SCALE
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Description
import org.junit.runner.notification.RunListener

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DisableAnimations

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnableAnimations

@Suppress("unused")
class AnimationRunListener : RunListener() {

    private val disableByDefault = true

    override fun testStarted(description: Description) {
        super.testStarted(description)

        val hasDisableAnnotation = description.getAnnotation(DisableAnimations::class.java) != null ||
            description.testClass?.getAnnotation(DisableAnimations::class.java) != null

        val hasEnableAnnotation = description.getAnnotation(EnableAnimations::class.java) != null ||
            description.testClass?.getAnnotation(EnableAnimations::class.java) != null

        // Resolve final animation scale choice
        val runWithAnimations = when {
            hasEnableAnnotation -> true
            hasDisableAnnotation -> false
            else -> !disableByDefault
        }

        setSystemAnimations(enabled = runWithAnimations)
    }

    override fun testFinished(description: Description) {
        setSystemAnimations(enabled = true)
        super.testFinished(description)
    }

    private fun setSystemAnimations(enabled: Boolean) {
        val value = if (enabled) "1.0" else "0.0"
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        listOf(WINDOW_ANIMATION_SCALE, TRANSITION_ANIMATION_SCALE, ANIMATOR_DURATION_SCALE).forEach { setting ->
            // Shell commands run async; reading to EOF waits, so a test's reset can't land after the next test's set.
            ParcelFileDescriptor.AutoCloseInputStream(
                automation.executeShellCommand("settings put global $setting $value")
            ).use { it.readBytes() }
        }
    }
}
