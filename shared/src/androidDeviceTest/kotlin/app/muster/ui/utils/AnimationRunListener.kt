package app.muster.ui.utils

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
        InstrumentationRegistry.getInstrumentation().uiAutomation.run {
            executeShellCommand("settings put global $WINDOW_ANIMATION_SCALE $value")
            executeShellCommand("settings put global $TRANSITION_ANIMATION_SCALE $value")
            executeShellCommand("settings put global $ANIMATOR_DURATION_SCALE $value")
        }
    }
}

