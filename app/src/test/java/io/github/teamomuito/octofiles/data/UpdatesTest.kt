package io.github.teamomuito.octofiles.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdatesTest {
    @Test fun `a build tag gives its run number`() {
        assertEquals(42, Updates.buildNumber("build-42"))
        assertEquals(1, Updates.buildNumber("build-1"))
    }

    @Test fun `other tags are not builds`() {
        assertNull(Updates.buildNumber("v0.1.0"))
        assertNull(Updates.buildNumber("build-"))
        assertNull(Updates.buildNumber("build-x"))
    }
}
