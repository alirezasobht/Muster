package app.muster.data.mapper

import app.muster.data.dto.ProfileDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileMapperTest {

    @Test
    fun `maps every column`() {
        val dto = ProfileDto(
            id = "id-1",
            name = "Alex Doyle",
            email = "alex.doyle@gmail.com",
            canCreateGroups = true
        )
        val profile = dto.toProfile()

        assertEquals("id-1", profile.id)
        assertEquals("Alex Doyle", profile.name)
        assertEquals("alex.doyle@gmail.com", profile.email)
        assertEquals(true, profile.canCreateGroups)
    }

    // Null means never set, and the app gates the home screen on it.
    @Test
    fun `a null name stays null`() {
        val dto = ProfileDto(id = "id-1", email = "alex.doyle@gmail.com")
        assertNull(dto.toProfile().name)
    }

    @Test
    fun `group creation defaults to off`() {
        val dto = ProfileDto(id = "id-1", email = "alex.doyle@gmail.com")
        assertEquals(false, dto.toProfile().canCreateGroups)
    }
}
