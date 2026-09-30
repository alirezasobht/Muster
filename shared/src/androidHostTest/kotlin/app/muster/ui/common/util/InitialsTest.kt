package app.muster.ui.common.util

import kotlin.test.Test
import kotlin.test.assertEquals

class InitialsTest {

    @Test
    fun `two names take one letter each`() {
        assertEquals("AD", "Alex Doyle".initials())
    }

    @Test
    fun `three or more names use only the first two`() {
        assertEquals("AB", "Alex Bea Cal".initials())
    }

    @Test
    fun `single name takes its first two letters`() {
        assertEquals("AL", "Alex".initials())
    }

    @Test
    fun `single letter name takes just that letter`() {
        assertEquals("A", "A".initials())
    }

    @Test
    fun `blank name is empty`() {
        assertEquals("", "   ".initials())
    }

    @Test
    fun `empty name is empty`() {
        assertEquals("", "".initials())
    }

    @Test
    fun `result is uppercase regardless of input case`() {
        assertEquals("AD", "alex doyle".initials())
    }

    @Test
    fun `extra whitespace between and around names is ignored`() {
        assertEquals("AD", "  Alex   Doyle  ".initials())
    }
}
