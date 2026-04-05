package jetcar.nuts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class NutsBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val boardRect = RectF()
    private var level: LevelDefinition = LevelCatalog.levels.first()
    private var bolts: Set<Int> = emptySet()
    private var removedPlanks: Set<String> = emptySet()
    private var selectedAnchor: Int? = null

    // --- Animation state ---

    private data class BoltAnim(val startMs: Long, val durationMs: Long = BOLT_DURATION_MS, val screwingIn: Boolean)

    private data class FallingPlankAnim(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val color: Int,
        val startMs: Long,
        val durationMs: Long = FALL_DURATION_MS,
    )

    private data class HangState(val startMs: Long, val pivotAnchor: Int)

    private val boltAnims = mutableMapOf<Int, BoltAnim>()       // anchor → animation
    private val fallingPlanks = mutableListOf<FallingPlankAnim>()
    private val hangingPlanks = mutableMapOf<String, HangState>() // plank ID → hang state

    private var prevBolts: Set<Int> = emptySet()
    private var prevRemovedPlanks: Set<String> = emptySet()
    private var levelName: String = ""

    var onAnchorTap: ((Int) -> Unit)? = null

    // --- Paint objects ---

    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3B2A1F")
    }
    private val boardEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(4f)
        color = Color.parseColor("#6D4F3A")
    }
    private val plankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(60, 255, 248, 220)
    }
    private val holeRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B664C")
    }
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#20150F")
    }
    private val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D8D3CF")
    }
    private val boltShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7E7A77")
    }
    private val boltSlotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#5B5855")
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#FFD166")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val pad = dp(18f)
        boardRect.set(pad, pad, w - pad, h - pad)
    }

    fun render(engine: NutsGameEngine) {
        val newLevel = engine.getLevel()
        val newBolts = engine.getBolts()
        val newRemovedPlanks = engine.getRemovedPlanks()

        // Detect transitions only for the same level after the board is laid out
        if (levelName == newLevel.name && boardRect.width() > 0) {

            // Newly removed planks → fall animation
            val newlyRemovedIds = newRemovedPlanks - prevRemovedPlanks
            for (plankId in newlyRemovedIds) {
                val plank = newLevel.planks.find { it.id == plankId } ?: continue
                startPlankFallAnim(plank)
            }

            // Bolt movement → unscrew / screw-in animations
            val boltLeft = prevBolts - newBolts
            val boltArrived = newBolts - prevBolts
            if (boltLeft.size == 1 && boltArrived.size == 1) {
                val from = boltLeft.first()
                val to = boltArrived.first()
                boltAnims[from] = BoltAnim(System.currentTimeMillis(), screwingIn = false)
                boltAnims[to] = BoltAnim(System.currentTimeMillis(), screwingIn = true)
            }

            // Update hanging plank states
            for (plank in newLevel.planks) {
                if (plank.id in newRemovedPlanks) {
                    hangingPlanks.remove(plank.id)
                    continue
                }
                val startHasBolt = plank.startAnchor in newBolts
                val endHasBolt = plank.endAnchor in newBolts
                val isHanging = startHasBolt xor endHasBolt
                if (isHanging && plank.id !in hangingPlanks) {
                    val pivotAnchor = if (startHasBolt) plank.startAnchor else plank.endAnchor
                    hangingPlanks[plank.id] = HangState(System.currentTimeMillis(), pivotAnchor)
                } else if (!isHanging) {
                    hangingPlanks.remove(plank.id)
                }
            }
        }

        level = newLevel
        bolts = newBolts
        removedPlanks = newRemovedPlanks
        selectedAnchor = engine.getSelectedAnchor()
        prevBolts = newBolts
        prevRemovedPlanks = newRemovedPlanks
        levelName = newLevel.name

        invalidate()
    }

    /** Call before loading or restarting a level to clear all animation state. */
    fun cancelAnimations() {
        boltAnims.clear()
        fallingPlanks.clear()
        hangingPlanks.clear()
        levelName = ""
        prevBolts = emptySet()
        prevRemovedPlanks = emptySet()
    }

    private fun startPlankFallAnim(plank: PlankDefinition) {
        if (boardRect.width() == 0f) return
        val now = System.currentTimeMillis()
        val hangState = hangingPlanks[plank.id]

        val x1: Float
        val y1: Float
        val x2: Float
        val y2: Float

        if (hangState != null) {
            // Start fall from current hanging position
            val (px, py) = anchorPoint(hangState.pivotAnchor)
            val freeAnchor = if (hangState.pivotAnchor == plank.startAnchor) plank.endAnchor else plank.startAnchor
            val (fx, fy) = anchorPoint(freeAnchor)
            val length = hypot(fx - px, fy - py)
            val hangProgress = ((now - hangState.startMs).toFloat() / HANG_DURATION_MS).coerceIn(0f, 1f)
            val currentFreeX = lerp(fx, px, easeInOut(hangProgress))
            val currentFreeY = lerp(fy, py + length, easeInOut(hangProgress))
            if (hangState.pivotAnchor == plank.startAnchor) {
                x1 = px; y1 = py; x2 = currentFreeX; y2 = currentFreeY
            } else {
                x1 = currentFreeX; y1 = currentFreeY; x2 = px; y2 = py
            }
        } else {
            val (ax1, ay1) = anchorPoint(plank.startAnchor)
            val (ax2, ay2) = anchorPoint(plank.endAnchor)
            x1 = ax1; y1 = ay1; x2 = ax2; y2 = ay2
        }

        fallingPlanks.add(FallingPlankAnim(x1, y1, x2, y2, plankColor(plank.tone), now))
        hangingPlanks.remove(plank.id)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = dp(18f)
        boardRect.set(pad, pad, width - pad, height - pad)
        val corner = dp(28f)
        canvas.drawRoundRect(boardRect, corner, corner, boardPaint)
        canvas.drawRoundRect(boardRect, corner, corner, boardEdgePaint)

        val plankWidth = min(boardRect.width(), boardRect.height()) * 0.10f
        plankPaint.strokeWidth = plankWidth
        grainPaint.strokeWidth = plankWidth * 0.12f
        boltSlotPaint.strokeWidth = plankWidth * 0.08f
        selectedPaint.strokeWidth = plankWidth * 0.11f

        val now = System.currentTimeMillis()

        // Draw active planks (with hanging effect for one-bolt planks)
        level.planks.filter { it.id !in removedPlanks }.forEach { plank ->
            val hangState = hangingPlanks[plank.id]
            val (ax1, ay1) = anchorPoint(plank.startAnchor)
            val (ax2, ay2) = anchorPoint(plank.endAnchor)

            val x1: Float; val y1: Float; val x2: Float; val y2: Float

            if (hangState != null) {
                val (px, py) = anchorPoint(hangState.pivotAnchor)
                val freeAnchor = if (hangState.pivotAnchor == plank.startAnchor) plank.endAnchor else plank.startAnchor
                val (fx, fy) = anchorPoint(freeAnchor)
                val length = hypot(fx - px, fy - py)
                val hangProgress = ((now - hangState.startMs).toFloat() / HANG_DURATION_MS).coerceIn(0f, 1f)
                val currentFreeX = lerp(fx, px, easeInOut(hangProgress))
                val currentFreeY = lerp(fy, py + length, easeInOut(hangProgress))
                if (hangState.pivotAnchor == plank.startAnchor) {
                    x1 = px; y1 = py; x2 = currentFreeX; y2 = currentFreeY
                } else {
                    x1 = currentFreeX; y1 = currentFreeY; x2 = px; y2 = py
                }
            } else {
                x1 = ax1; y1 = ay1; x2 = ax2; y2 = ay2
            }

            drawPlankLine(canvas, x1, y1, x2, y2, plankColor(plank.tone), plankWidth, 255)
        }

        // Draw falling plank animations
        val fallIter = fallingPlanks.iterator()
        while (fallIter.hasNext()) {
            val anim = fallIter.next()
            val progress = ((now - anim.startMs).toFloat() / anim.durationMs).coerceIn(0f, 1f)
            if (progress >= 1f) { fallIter.remove(); continue }
            val dy = easeIn(progress) * boardRect.height() * 0.65f
            val alpha = ((1f - easeIn(progress)) * 255).toInt()
            canvas.save()
            canvas.translate(0f, dy)
            drawPlankLine(canvas, anim.x1, anim.y1, anim.x2, anim.y2, anim.color, plankWidth, alpha)
            canvas.restore()
        }

        // Draw anchors and bolts
        level.anchors.indices.forEach { anchorIndex ->
            val (cx, cy) = anchorPoint(anchorIndex)
            val holeRadius = plankWidth * 0.21f
            canvas.drawCircle(cx, cy, holeRadius * 1.35f, holeRimPaint)
            canvas.drawCircle(cx, cy, holeRadius, holePaint)

            val boltAnim = boltAnims[anchorIndex]
            when {
                boltAnim != null -> {
                    val progress = ((now - boltAnim.startMs).toFloat() / boltAnim.durationMs).coerceIn(0f, 1f)
                    if (progress >= 1f) {
                        boltAnims.remove(anchorIndex)
                        if (anchorIndex in bolts) drawBolt(canvas, cx, cy, holeRadius, 0f, 1f, 255)
                    } else {
                        drawAnimatedBolt(canvas, cx, cy, holeRadius, boltAnim, progress)
                    }
                }
                anchorIndex in bolts -> drawBolt(canvas, cx, cy, holeRadius, 0f, 1f, 255)
            }

            if (anchorIndex == selectedAnchor) {
                canvas.drawCircle(cx, cy, holeRadius * 1.9f, selectedPaint)
            }
        }

        // Keep animating while anything is active
        val hangStillAnimating = hangingPlanks.any { (_, s) -> (now - s.startMs) < HANG_DURATION_MS }
        if (boltAnims.isNotEmpty() || fallingPlanks.isNotEmpty() || hangStillAnimating) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawPlankLine(
        canvas: Canvas,
        x1: Float, y1: Float, x2: Float, y2: Float,
        color: Int, strokeWidth: Float, alpha: Int,
    ) {
        plankPaint.color = color
        plankPaint.alpha = alpha
        canvas.drawLine(x1, y1, x2, y2, plankPaint)

        val dx = x2 - x1
        val dy = y2 - y1
        val len = max(1f, hypot(dx, dy))
        val offX = -dy / len * strokeWidth * 0.18f
        val offY = dx / len * strokeWidth * 0.18f
        canvas.drawLine(x1 + offX, y1 + offY, x2 + offX, y2 + offY, grainPaint)
        canvas.drawLine(x1 - offX, y1 - offY, x2 - offX, y2 - offY, grainPaint)
        plankPaint.alpha = 255
    }

    private fun drawBolt(canvas: Canvas, cx: Float, cy: Float, holeRadius: Float, slotAngle: Float, scale: Float, alpha: Int) {
        val boltRadius = holeRadius * 1.28f * scale
        boltShadowPaint.alpha = alpha
        boltPaint.alpha = alpha
        boltSlotPaint.alpha = alpha
        canvas.drawCircle(cx, cy + holeRadius * 0.10f * scale, boltRadius, boltShadowPaint)
        canvas.drawCircle(cx, cy, boltRadius, boltPaint)
        val slotLen = boltRadius * 0.58f
        canvas.drawLine(
            cx - cos(slotAngle) * slotLen, cy - sin(slotAngle) * slotLen,
            cx + cos(slotAngle) * slotLen, cy + sin(slotAngle) * slotLen,
            boltSlotPaint,
        )
        boltShadowPaint.alpha = 255
        boltPaint.alpha = 255
        boltSlotPaint.alpha = 255
    }

    private fun drawAnimatedBolt(canvas: Canvas, cx: Float, cy: Float, holeRadius: Float, anim: BoltAnim, progress: Float) {
        if (anim.screwingIn) {
            val scale = lerp(0.3f, 1f, easeOut(progress))
            val angle = (1f - progress) * BOLT_ROTATION_COUNT * 2f * PI.toFloat()
            val alpha = (easeOut(progress) * 255).toInt()
            drawBolt(canvas, cx, cy, holeRadius, angle, scale, alpha)
        } else {
            val scale = lerp(1f, 0.3f, easeIn(progress))
            val angle = progress * BOLT_ROTATION_COUNT * 2f * PI.toFloat()
            val alpha = ((1f - easeIn(progress)) * 255).toInt()
            drawBolt(canvas, cx, cy, holeRadius, angle, scale, alpha)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            nearestAnchor(event.x, event.y)?.let {
                onAnchorTap?.invoke(it)
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun nearestAnchor(x: Float, y: Float): Int? {
        val threshold = min(boardRect.width(), boardRect.height()) * 0.12f
        return level.anchors.indices
            .map { it to anchorPoint(it) }
            .map { (index, point) -> index to hypot(point.first - x, point.second - y) }
            .filter { (_, distance) -> distance <= threshold }
            .minByOrNull { it.second }
            ?.first
    }

    private fun anchorPoint(index: Int): Pair<Float, Float> {
        val anchor = level.anchors[index]
        return Pair(
            boardRect.left + anchor.x * boardRect.width(),
            boardRect.top + anchor.y * boardRect.height(),
        )
    }

    private fun plankColor(tone: WoodTone): Int = when (tone) {
        WoodTone.Oak -> Color.parseColor("#C58A4D")
        WoodTone.Cedar -> Color.parseColor("#B66E4E")
        WoodTone.Walnut -> Color.parseColor("#8E613F")
        WoodTone.Maple -> Color.parseColor("#D7A56B")
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
    private fun easeIn(t: Float): Float = t * t
    private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t)
    private fun easeInOut(t: Float): Float =
        if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).let { it * it } / 2f

    companion object {
        private const val BOLT_DURATION_MS = 450L
        private const val FALL_DURATION_MS = 700L
        private const val HANG_DURATION_MS = 450L
        private const val BOLT_ROTATION_COUNT = 1.5f
    }
}
