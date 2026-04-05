package jetcar.nuts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutsGameEngineTest {
    @Test
    fun levelOneSolutionClearsTheBoard() {
        val engine = NutsGameEngine()

        play(engine, 0, 5)
        play(engine, 1, 6)
        play(engine, 2, 0)
        play(engine, 3, 1)
        play(engine, 4, 2)

        assertTrue(engine.isLevelComplete())
        assertEquals(5, engine.getMoveCount())
    }

    @Test
    fun levelTwoSolutionClearsTheBoard() {
        val engine = NutsGameEngine()
        engine.loadLevel(1)

        play(engine, 0, 7)
        play(engine, 1, 8)
        play(engine, 2, 0)
        play(engine, 3, 1)
        play(engine, 4, 2)
        play(engine, 5, 3)
        play(engine, 6, 4)

        assertTrue(engine.isLevelComplete())
        assertEquals(7, engine.getMoveCount())
    }

    private fun play(engine: NutsGameEngine, from: Int, to: Int) {
        engine.handleAnchorTap(from)
        engine.handleAnchorTap(to)
    }
}
