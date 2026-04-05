package jetcar.nuts

import org.junit.Assert.assertEquals
import org.junit.Test

class AppVersioningTest {
    @Test
    fun compareVersionsOrdersSemanticValues() {
        assertEquals(0, AppVersioning.compareVersions("1.0.0", "1.0"))
        assertEquals(1, AppVersioning.compareVersions("1.2.0", "1.1.9"))
        assertEquals(-1, AppVersioning.compareVersions("2.0.0", "2.0.1"))
    }

    @Test
    fun compareVersionsHandlesDifferentLengthsAndLeadingZeroes() {
        assertEquals(-1, AppVersioning.compareVersions("1.2", "1.2.3.4"))
        assertEquals(0, AppVersioning.compareVersions("01.002.0003", "1.2.3"))
    }

    @Test
    fun compareVersionsTreatsEmptyAndMalformedSegmentsAsZero() {
        assertEquals(0, AppVersioning.compareVersions("", "0"))
        assertEquals(0, AppVersioning.compareVersions("1.alpha", "1.0"))
    }
}
