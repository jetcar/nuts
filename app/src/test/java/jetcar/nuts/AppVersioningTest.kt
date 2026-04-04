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
}
