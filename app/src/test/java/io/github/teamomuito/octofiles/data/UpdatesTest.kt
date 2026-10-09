package io.github.teamomuito.octofiles.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatesTest {
    @Test fun `a higher version is newer`() {
        assertTrue(Updates.isNewer("0.2.0", "0.1.0"))
        assertTrue(Updates.isNewer("1.0", "0.9.9"))
        assertTrue(Updates.isNewer("0.10.0", "0.9.0"))
        assertTrue(Updates.isNewer("0.1.1", "0.1"))
    }

    @Test fun `the same or an older version is not newer`() {
        assertFalse(Updates.isNewer("0.1.0", "0.1.0"))
        assertFalse(Updates.isNewer("0.1.0", "0.2.0"))
        assertFalse(Updates.isNewer("0.1", "0.1.0"))
    }

    @Test fun `a pre-release suffix counts as its number`() {
        assertTrue(Updates.isNewer("0.2.0-beta", "0.1.0"))
        assertFalse(Updates.isNewer("0.1.0-beta", "0.1.0"))
    }
}
