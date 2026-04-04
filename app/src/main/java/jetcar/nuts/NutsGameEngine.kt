package jetcar.nuts

class NutsGameEngine(
    private val levels: List<LevelDefinition> = LevelCatalog.levels,
) {
    private var currentLevelIndex = 0
    private var bolts = mutableSetOf<Int>()
    private var removedPlanks = mutableSetOf<String>()
    private var selectedAnchor: Int? = null
    private var moveCount = 0

    init {
        loadLevel(0)
    }

    fun getLevel(): LevelDefinition = levels[currentLevelIndex]

    fun getBolts(): Set<Int> = bolts.toSet()

    fun getRemovedPlanks(): Set<String> = removedPlanks.toSet()

    fun getSelectedAnchor(): Int? = selectedAnchor

    fun getMoveCount(): Int = moveCount

    fun getCurrentLevelNumber(): Int = currentLevelIndex + 1

    fun getLevelCount(): Int = levels.size

    fun isLevelComplete(): Boolean = removedPlanks.size == getLevel().planks.size

    fun loadLevel(levelIndex: Int): String {
        require(levelIndex in levels.indices)
        currentLevelIndex = levelIndex
        bolts = getLevel().startingBolts.toMutableSet()
        removedPlanks = mutableSetOf()
        selectedAnchor = null
        moveCount = 0
        return getLevel().instruction
    }

    fun restartLevel(): String = loadLevel(currentLevelIndex)

    fun advanceLevel(): String {
        return if (currentLevelIndex < levels.lastIndex) {
            loadLevel(currentLevelIndex + 1)
        } else {
            loadLevel(0)
        }
    }

    fun handleAnchorTap(anchorIndex: Int): String {
        require(anchorIndex in getLevel().anchors.indices)

        return when {
            anchorIndex in bolts -> {
                selectedAnchor = if (selectedAnchor == anchorIndex) null else anchorIndex
                if (selectedAnchor == null) {
                    "Selection cleared."
                } else {
                    "Bolt selected. Tap an empty socket."
                }
            }

            selectedAnchor != null -> moveSelectedBolt(anchorIndex)
            else -> "Tap a bolt to begin moving it."
        }
    }

    private fun moveSelectedBolt(targetAnchor: Int): String {
        val sourceAnchor = selectedAnchor ?: return "Tap a bolt to begin moving it."
        if (targetAnchor in bolts) {
            selectedAnchor = targetAnchor
            return "Bolt selected. Tap an empty socket."
        }
        if (sourceAnchor == targetAnchor) {
            return "Choose a different socket."
        }

        bolts.remove(sourceAnchor)
        bolts.add(targetAnchor)
        selectedAnchor = null
        moveCount += 1

        val newlyRemoved = getLevel().planks
            .filter { it.id !in removedPlanks }
            .filter { plank -> plank.startAnchor !in bolts && plank.endAnchor !in bolts }
            .map { it.id }

        removedPlanks.addAll(newlyRemoved)

        return when {
            isLevelComplete() -> {
                "Level cleared in $moveCount moves!"
            }

            newlyRemoved.isNotEmpty() -> {
                val cleared = newlyRemoved.size
                "$cleared plank${if (cleared == 1) "" else "s"} removed. Keep going!"
            }

            else -> "Bolt moved."
        }
    }
}

data class LevelDefinition(
    val name: String,
    val instruction: String,
    val anchors: List<AnchorDefinition>,
    val planks: List<PlankDefinition>,
    val startingBolts: Set<Int>,
)

data class AnchorDefinition(
    val x: Float,
    val y: Float,
)

data class PlankDefinition(
    val id: String,
    val startAnchor: Int,
    val endAnchor: Int,
    val tone: WoodTone,
)

enum class WoodTone {
    Oak,
    Cedar,
    Walnut,
    Maple,
}

object LevelCatalog {
    val levels: List<LevelDefinition> = listOf(
        LevelDefinition(
            name = "Level 1 · Timber Trail",
            instruction = "Clear the bolt chain from left to right.",
            anchors = listOf(
                AnchorDefinition(0.18f, 0.28f),
                AnchorDefinition(0.38f, 0.28f),
                AnchorDefinition(0.58f, 0.28f),
                AnchorDefinition(0.78f, 0.28f),
                AnchorDefinition(0.90f, 0.55f),
                AnchorDefinition(0.58f, 0.76f),
                AnchorDefinition(0.26f, 0.76f),
            ),
            planks = listOf(
                PlankDefinition("l1-p1", 0, 1, WoodTone.Oak),
                PlankDefinition("l1-p2", 1, 2, WoodTone.Maple),
                PlankDefinition("l1-p3", 2, 3, WoodTone.Cedar),
                PlankDefinition("l1-p4", 3, 4, WoodTone.Walnut),
            ),
            startingBolts = setOf(0, 1, 2, 3, 4),
        ),
        LevelDefinition(
            name = "Level 2 · Sawmill Switchback",
            instruction = "Work down the zig-zag path to free every plank.",
            anchors = listOf(
                AnchorDefinition(0.14f, 0.22f),
                AnchorDefinition(0.36f, 0.22f),
                AnchorDefinition(0.58f, 0.22f),
                AnchorDefinition(0.80f, 0.36f),
                AnchorDefinition(0.64f, 0.56f),
                AnchorDefinition(0.42f, 0.74f),
                AnchorDefinition(0.20f, 0.74f),
                AnchorDefinition(0.82f, 0.76f),
                AnchorDefinition(0.12f, 0.48f),
            ),
            planks = listOf(
                PlankDefinition("l2-p1", 0, 1, WoodTone.Maple),
                PlankDefinition("l2-p2", 1, 2, WoodTone.Oak),
                PlankDefinition("l2-p3", 2, 3, WoodTone.Cedar),
                PlankDefinition("l2-p4", 3, 4, WoodTone.Walnut),
                PlankDefinition("l2-p5", 4, 5, WoodTone.Maple),
                PlankDefinition("l2-p6", 5, 6, WoodTone.Oak),
            ),
            startingBolts = setOf(0, 1, 2, 3, 4, 5, 6),
        ),
    )
}
