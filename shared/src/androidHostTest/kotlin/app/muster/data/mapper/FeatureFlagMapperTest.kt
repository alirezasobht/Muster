package app.muster.data.mapper

import app.muster.data.dto.FeatureFlagDto
import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureFlagMapperTest {

    @Test
    fun `maps each row to its flag`() {
        val dtos = listOf(
            FeatureFlagDto(flag = "member_tags", enabled = true),
            FeatureFlagDto(flag = "other", enabled = false)
        )
        assertEquals(mapOf("member_tags" to true, "other" to false), dtos.toFlagMap())
    }

    @Test
    fun `no rows map to no flags`() {
        assertEquals(emptyMap<String, Boolean>(), emptyList<FeatureFlagDto>().toFlagMap())
    }
}
