package app.muster.data.local

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class FeatureFlagLocalSourceTest {

    @Test
    fun `a missing flag is off`() = runTest {
        assertFalse(FeatureFlagLocalSource(MapSettings()).observe("my_flag").first())
    }

    @Test
    fun `a replaced flag is observed`() = runTest {
        val source = FeatureFlagLocalSource(MapSettings())
        source.replaceAll(mapOf("my_flag" to true, "other_flag" to false))

        assertTrue(source.observe("my_flag").first())
        assertFalse(source.observe("other_flag").first())
    }

    @Test
    fun `a replace drops flags it doesn't carry`() = runTest {
        val source = FeatureFlagLocalSource(MapSettings())
        source.replaceAll(mapOf("my_flag" to true))
        source.replaceAll(mapOf("other_flag" to true))

        assertFalse(source.observe("my_flag").first())
        assertTrue(source.observe("other_flag").first())
    }

    @Test
    fun `flags survive a new instance on the same settings`() = runTest {
        val settings = MapSettings()
        FeatureFlagLocalSource(settings).replaceAll(mapOf("my_flag" to true))

        assertTrue(FeatureFlagLocalSource(settings).observe("my_flag").first())
    }

    @Test
    fun `corrupt stored flags read as off`() = runTest {
        val settings = MapSettings("muster.feature_flags" to "{not json")

        assertFalse(FeatureFlagLocalSource(settings).observe("my_flag").first())
    }

    @Test
    fun `throwing storage keeps flags in memory`() = runTest {
        val source = FeatureFlagLocalSource(ThrowingSettings())
        source.replaceAll(mapOf("my_flag" to true))

        assertTrue(source.observe("my_flag").first())
    }

    private class ThrowingSettings : Settings by MapSettings() {
        override fun getStringOrNull(key: String): String? = throw IllegalStateException("storage blocked")

        override fun putString(
            key: String,
            value: String
        ): Unit = throw IllegalStateException("storage blocked")
    }
}
